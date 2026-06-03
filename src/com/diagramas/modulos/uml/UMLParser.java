package com.diagramas.modulos.uml;

import com.diagramas.core.ManejadorErrores;
import com.diagramas.core.TablaSimbolos;
import com.diagramas.core.Token;
import com.diagramas.modulos.uml.ast.*;
import java.util.ArrayList;
import java.util.List;

public class UMLParser {
    private final List<Token> tokens;
    private final TablaSimbolos tabla;
    private final ManejadorErrores errores;
    private int pos;

    public UMLParser(List<Token> tokens, TablaSimbolos tabla, ManejadorErrores errores) {
        this.tokens = tokens;
        this.tabla = tabla;
        this.errores = errores;
        this.pos = 0;
    }

    public RaizUMLAST parsear() {
        RaizUMLAST raiz = new RaizUMLAST();

        while (pos < tokens.size() && tokens.get(pos).getTipo() != Token.Tipo.EOF) {
            Token tokenActual = tokens.get(pos);

            if (tokenActual.getTipo() == Token.Tipo.IDENTIFICADOR || tokenActual.getTipo() == Token.Tipo.PALABRA_RESERVADA) {
                String lexema = tokenActual.getLexema();

                if (lexema.equals("clase")) {
                    procesarClase(raiz);
                } else if (lexema.equals("interfaz") || lexema.equals("enum")) {
                    procesarComponenteLineal(raiz, lexema);
                } else {
                    procesarRelacion(raiz);
                }
            } else {
                errores.reportarError("ES43", tokenActual.getLinea(), "Sintáctico UML",
                    "Token inesperado '" + tokenActual.getLexema() + "'.",
                    "Inicia con 'clase', 'interfaz', 'enum' o define una relación (extiende, implementa, usa).");
                pos++;
            }
        }
        return raiz;
    }

    private void procesarClase(RaizUMLAST raiz) {
        int lineaKeyword = tokens.get(pos).getLinea();
        pos++;

        if (!validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "ES44", "Falta el nombre de la clase.", lineaKeyword)) {
            recuperarPanicoBloque(); return;
        }
        int lineaId = tokens.get(pos).getLinea();
        String nombre = tokens.get(pos).getLexema();
        pos++;

        tabla.registrar(nombre, "clase", lineaId);

        if (!validarSiguienteTipo(Token.Tipo.LLAVE_IZQ, "ES45", "Falta '{' para abrir el cuerpo de la clase '" + nombre + "'.", lineaId)) {
            recuperarPanicoBloque(); return;
        }
        pos++;

        List<NodoMiembro> miembros = new ArrayList<>();
        while (pos < tokens.size() && tokens.get(pos).getTipo() != Token.Tipo.LLAVE_DER && tokens.get(pos).getTipo() != Token.Tipo.EOF) {
            NodoMiembro m = leerMiembro();
            if (m != null) miembros.add(m);
            else recuperarPanicoMiembro();
        }

        int lineaCierre = pos > 0 ? tokens.get(pos - 1).getLinea() : lineaId;
        if (!validarSiguienteTipo(Token.Tipo.LLAVE_DER, "ES51", "Falta '}' para cerrar la clase '" + nombre + "'.", lineaCierre)) {
            return;
        }
        pos++;
        raiz.agregarElemento(new NodoClase(nombre, miembros));
    }

    private NodoMiembro leerMiembro() {
        if (pos >= tokens.size() || tokens.get(pos).getTipo() == Token.Tipo.LLAVE_DER) return null;

        Token t = tokens.get(pos);
        int lineaMiembro = t.getLinea();
        if ((t.getTipo() != Token.Tipo.IDENTIFICADOR && t.getTipo() != Token.Tipo.PALABRA_RESERVADA) ||
                (!t.getLexema().equals("atributo") && !t.getLexema().equals("metodo"))) {
            errores.reportarError("ES46", lineaMiembro, "Sintáctico UML",
                "Se esperaba 'atributo' o 'metodo' dentro de la clase.",
                "Define miembros como: 'atributo velocidad : INT;' o 'metodo acelerar : VOID;'.");
            return null;
        }
        String rolMiembro = t.getLexema();
        pos++;

        if (!validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "ES47", "Falta el nombre del " + rolMiembro + ".", lineaMiembro)) return null;
        String nombre = tokens.get(pos).getLexema();
        pos++;

        if (!validarSiguienteTipo(Token.Tipo.DOS_PUNTOS, "ES48", "Falta ':' en la definición de '" + nombre + "'.", lineaMiembro)) return null;
        pos++;

        if (!validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "ES49", "Falta el tipo del " + rolMiembro + " '" + nombre + "'.", lineaMiembro)) return null;
        String tipo = tokens.get(pos).getLexema();
        pos++;

        if (!validarSiguienteTipo(Token.Tipo.PUNTO_Y_COMA, "ES50", "Falta ';' al final del " + rolMiembro + " '" + nombre + "'.", lineaMiembro)) return null;
        pos++;

        return new NodoMiembro(rolMiembro, nombre, tipo);
    }

    private void procesarComponenteLineal(RaizUMLAST raiz, String rol) {
        int lineaKeyword = tokens.get(pos).getLinea();
        pos++;

        if (!validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "ES44", "Falta el nombre del " + rol + ".", lineaKeyword)) {
            recuperarPanico(); return;
        }
        int lineaId = tokens.get(pos).getLinea();
        String nombre = tokens.get(pos).getLexema();
        pos++;

        if (!validarSiguienteTipo(Token.Tipo.PUNTO_Y_COMA, "ES50", "Falta ';' al final de la declaración de '" + nombre + "'.", lineaId)) {
            recuperarPanico(); return;
        }
        pos++;

        tabla.registrar(nombre, rol, lineaId);
        raiz.agregarElemento(new NodoClase(nombre + " [" + rol.toUpperCase() + "]", new ArrayList<>()));
    }

    private void procesarRelacion(RaizUMLAST raiz) {
        Token tOrigen = tokens.get(pos);
        String idOrigen = tOrigen.getLexema();
        int lineaRelacion = tOrigen.getLinea();
        pos++;

        if (pos < tokens.size() && (tokens.get(pos).getTipo() == Token.Tipo.IDENTIFICADOR || tokens.get(pos).getTipo() == Token.Tipo.PALABRA_RESERVADA)) {
            String verbo = tokens.get(pos).getLexema();
            if (verbo.equals("extiende") || verbo.equals("implementa") || verbo.equals("usa")) {
                pos++;

                if (!validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "ES52", "Falta el identificador destino en la instrucción '" + verbo + "'.", lineaRelacion)) {
                    recuperarPanico(); return;
                }
                String idDestino = tokens.get(pos).getLexema();
                int lineaDestino = tokens.get(pos).getLinea();
                pos++;

                if (!validarSiguienteTipo(Token.Tipo.PUNTO_Y_COMA, "ES53", "Falta ';' al finalizar la instrucción '" + verbo + "'.", lineaDestino)) {
                    recuperarPanico(); return;
                }
                pos++;
                raiz.agregarElemento(new NodoRelacionUML(idOrigen, verbo, idDestino));
            } else {
                errores.reportarError("ES54", lineaRelacion, "Sintáctico UML",
                    "Verbo inválido '" + verbo + "' en '" + idOrigen + "'.",
                    "Usa 'extiende', 'implementa' o 'usa' para definir relaciones UML.");
                recuperarPanico();
            }
        } else {
            errores.reportarError("ES54", lineaRelacion, "Sintáctico UML",
                "Instrucción inválida después de '" + idOrigen + "'.",
                "Usa 'extiende', 'implementa' o 'usa' para definir relaciones UML.");
            recuperarPanico();
        }
    }

    private boolean validarSiguienteTipo(Token.Tipo esperado, String codigo, String mensajeError, int linea) {
        if (pos >= tokens.size()) {
            errores.reportarError(codigo, linea, "Sintáctico UML", mensajeError, "Revisa la guía del módulo UML.");
            return false;
        }
        Token t = tokens.get(pos);
        if (esperado == Token.Tipo.IDENTIFICADOR &&
                (t.getTipo() == Token.Tipo.PALABRA_RESERVADA || t.getTipo() == Token.Tipo.PR_DIAGRAMA)) {
            errores.reportarError("ES56", linea, "Sintáctico UML",
                "La palabra reservada '" + t.getLexema() + "' no puede usarse como nombre de identificador.",
                "Usa un nombre definido por el usuario, no una palabra reservada del lenguaje.");
            return false;
        }
        if (t.getTipo() != esperado) {
            errores.reportarError(codigo, linea, "Sintáctico UML", mensajeError, "Revisa la guía del módulo UML.");
            return false;
        }
        return true;
    }

    private void recuperarPanico() {
        while (pos < tokens.size() && tokens.get(pos).getTipo() != Token.Tipo.PUNTO_Y_COMA && tokens.get(pos).getTipo() != Token.Tipo.EOF) pos++;
        if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.PUNTO_Y_COMA) pos++;
    }

    private void recuperarPanicoBloque() {
        while (pos < tokens.size() && tokens.get(pos).getTipo() != Token.Tipo.LLAVE_DER && tokens.get(pos).getTipo() != Token.Tipo.EOF) pos++;
        if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.LLAVE_DER) pos++;
    }

    private void recuperarPanicoMiembro() {
        while (pos < tokens.size() && tokens.get(pos).getTipo() != Token.Tipo.PUNTO_Y_COMA && tokens.get(pos).getTipo() != Token.Tipo.LLAVE_DER && tokens.get(pos).getTipo() != Token.Tipo.EOF) pos++;
        if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.PUNTO_Y_COMA) pos++;
    }
}
