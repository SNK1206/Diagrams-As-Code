package com.diagramas.modulos.bd;

import com.diagramas.core.ManejadorErrores;
import com.diagramas.core.TablaSimbolos;
import com.diagramas.core.Token;
import com.diagramas.modulos.bd.ast.*;
import java.util.ArrayList;
import java.util.List;

public class BDParser {
    private final List<Token> tokens;
    private final TablaSimbolos tabla;
    private final ManejadorErrores errores;
    private int pos;

    public BDParser(List<Token> tokens, TablaSimbolos tabla, ManejadorErrores errores) {
        this.tokens = tokens;
        this.tabla = tabla;
        this.errores = errores;
        this.pos = 0;
    }

    public RaizBDAST parsear() {
        RaizBDAST raiz = new RaizBDAST();

        while (pos < tokens.size() && tokens.get(pos).getTipo() != Token.Tipo.EOF) {
            Token tokenActual = tokens.get(pos);

            if (tokenActual.getTipo() == Token.Tipo.IDENTIFICADOR) {
                String lexema = tokenActual.getLexema();

                if (lexema.equals("tabla") || lexema.equals("vista") ||
                        lexema.equals("esquema") || lexema.equals("paquete")) {
                    procesarBloqueComplejo(raiz, lexema);
                } else if (lexema.equals("procedimiento") || lexema.equals("indice") ||
                        lexema.equals("disparador") || lexema.equals("secuencia") ||
                        lexema.equals("funcion")) {
                    procesarComponenteLineal(raiz, lexema);
                } else if (pos + 1 < tokens.size() && tokens.get(pos + 1).getTipo() == Token.Tipo.LLAVE_IZQ) {
                    errores.reportarError("ES14", tokenActual.getLinea(), "Sintáctico BD",
                        "Falta la palabra clave antes de '" + tokenActual.getLexema() + "'.",
                        "Especifica el tipo: tabla, vista, esquema o paquete.");
                    tabla.registrar(tokenActual.getLexema(), "desconocido");
                    pos++;
                    recuperarPanicoBloque();
                } else {
                    procesarRelacion(raiz);
                }
            } else {
                errores.reportarError("ES14", tokenActual.getLinea(), "Sintáctico BD",
                    "Token inesperado '" + tokenActual.getLexema() + "'.",
                    "Inicia la línea con un componente (tabla, vista, etc.) o una relación.");
                pos++;
            }
        }
        return raiz;
    }

    private void procesarBloqueComplejo(RaizBDAST raiz, String rol) {
        int lineaOriginal = tokens.get(pos).getLinea();
        pos++;

        if (!validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "ES15", "Falta el nombre de la " + rol + ".")) {
            recuperarPanicoBloque(); return;
        }
        String nombreBloque = tokens.get(pos).getLexema();
        pos++;

        if (!tabla.registrar(nombreBloque, rol)) {
            errores.reportarError("", lineaOriginal, "Semántico BD", "El elemento '" + nombreBloque + "' ya existe.", "Elige otro identificador.");
        }

        if (!validarSiguienteTipo(Token.Tipo.LLAVE_IZQ, "ES16", "Falta abrir '{' para la definición de '" + nombreBloque + "'.")) {
            recuperarPanicoBloque(); return;
        }
        pos++;

        List<Atributo> atributos = new ArrayList<>();
        while (pos < tokens.size() && tokens.get(pos).getTipo() != Token.Tipo.LLAVE_DER && tokens.get(pos).getTipo() != Token.Tipo.EOF) {
            Atributo attr = leerAtributo();
            if (attr != null) atributos.add(attr);
            else recuperarPanicoAtributo();
        }

        if (!validarSiguienteTipo(Token.Tipo.LLAVE_DER, "ES21", "Falta cerrar '}' para el bloque '" + nombreBloque + "'.")) {
            return;
        }
        pos++;
        raiz.agregarElemento(new NodoTabla(nombreBloque + " (" + rol.toUpperCase() + ")", atributos));
    }

    private Atributo leerAtributo() {
        if (!validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "ES17", "Falta el nombre del atributo.")) return null;
        String nombre = tokens.get(pos).getLexema();
        pos++;

        if (!validarSiguienteTipo(Token.Tipo.DOS_PUNTOS, "ES18", "Falta ':' en la definición del atributo.")) return null;
        pos++;

        if (!validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "ES19", "Falta el tipo de dato del atributo.")) return null;
        String tipoDato = tokens.get(pos).getLexema();
        pos++;

        String modificador = "";
        if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.IDENTIFICADOR) {
            modificador = tokens.get(pos).getLexema();
            pos++;
        }

        if (!validarSiguienteTipo(Token.Tipo.PUNTO_Y_COMA, "ES20", "Falta ';' al final del atributo.")) return null;
        pos++;

        return new Atributo(nombre, tipoDato, modificador);
    }

    private void procesarComponenteLineal(RaizBDAST raiz, String rol) {
        int lineaOriginal = tokens.get(pos).getLinea();
        pos++;

        if (!validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "ES22", "Falta el identificador del " + rol + ".")) {
            recuperarPanicoTabla(); return;
        }
        String nombre = tokens.get(pos).getLexema();
        pos++;

        if (!validarSiguienteTipo(Token.Tipo.PUNTO_Y_COMA, "ES23", "Falta ';' al final del componente lineal.")) {
            recuperarPanicoTabla(); return;
        }
        pos++;
        if (!tabla.registrar(nombre, rol)) {
            errores.reportarError("", lineaOriginal, "Semántico BD", "El identificador '" + nombre + "' ya está ocupado.", "Usa otro nombre.");
        }
        raiz.agregarElemento(new NodoTabla(nombre + " [" + rol.toUpperCase() + "]", new ArrayList<>()));
    }

    private void procesarRelacion(RaizBDAST raiz) {
        Token tOrigen = tokens.get(pos);
        String idOrigen = tOrigen.getLexema();
        pos++;

        if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.IDENTIFICADOR && tokens.get(pos).getLexema().equals("relaciona")) {
            pos++;

            if (!validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "ES24", "Falta el identificador destino en instrucción 'relaciona'.")) {
                recuperarPanicoTabla(); return;
            }
            String idDestino = tokens.get(pos).getLexema();
            pos++;

            if (!validarSiguienteTipo(Token.Tipo.PUNTO_Y_COMA, "ES25", "Falta ';' al finalizar la instrucción 'relaciona'.")) {
                recuperarPanicoTabla(); return;
            }
            pos++;
            raiz.agregarElemento(new NodoRelacion(idOrigen, idDestino));
        } else {
            errores.reportarError("ES26", tOrigen.getLinea(), "Sintáctico BD",
                "Verbo inválido en '" + idOrigen + "'.",
                "Usa el verbo 'relaciona' para bases de datos.");
            recuperarPanicoTabla();
        }
    }

    private boolean validarSiguienteTipo(Token.Tipo esperado, String codigo, String mensajeError) {
        if (pos >= tokens.size() || tokens.get(pos).getTipo() != esperado) {
            int l = (pos < tokens.size()) ? tokens.get(pos).getLinea() : tokens.get(pos - 1).getLinea();
            errores.reportarError(codigo, l, "Sintáctico BD", mensajeError, "Verifica las reglas de sintaxis.");
            return false;
        }
        return true;
    }

    private void recuperarPanicoAtributo() {
        while (pos < tokens.size() && tokens.get(pos).getTipo() != Token.Tipo.PUNTO_Y_COMA && tokens.get(pos).getTipo() != Token.Tipo.LLAVE_DER && tokens.get(pos).getTipo() != Token.Tipo.EOF) pos++;
        if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.PUNTO_Y_COMA) pos++;
    }

    private void recuperarPanicoTabla() {
        while (pos < tokens.size() && tokens.get(pos).getTipo() != Token.Tipo.PUNTO_Y_COMA && tokens.get(pos).getTipo() != Token.Tipo.EOF) pos++;
        if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.PUNTO_Y_COMA) pos++;
    }

    private void recuperarPanicoBloque() {
        while (pos < tokens.size() && tokens.get(pos).getTipo() != Token.Tipo.LLAVE_DER && tokens.get(pos).getTipo() != Token.Tipo.EOF) pos++;
        if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.LLAVE_DER) pos++;
    }
}
