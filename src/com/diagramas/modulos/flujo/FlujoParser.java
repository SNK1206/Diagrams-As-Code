package com.diagramas.modulos.flujo;

import com.diagramas.core.ManejadorErrores;
import com.diagramas.core.TablaSimbolos;
import com.diagramas.core.Token;
import com.diagramas.modulos.flujo.ast.*;
import java.util.List;

public class FlujoParser {
    private final List<Token> tokens;
    private final TablaSimbolos tabla;
    private final ManejadorErrores errores;
    private int pos;

    public FlujoParser(List<Token> tokens, TablaSimbolos tabla, ManejadorErrores errores) {
        this.tokens = tokens;
        this.tabla = tabla;
        this.errores = errores;
        this.pos = 0;
    }

    public RaizFlujoAST parsear() {
        RaizFlujoAST raiz = new RaizFlujoAST();

        while (pos < tokens.size() && tokens.get(pos).getTipo() != Token.Tipo.EOF) {
            Token tokenActual = tokens.get(pos);

            if (tokenActual.getTipo() == Token.Tipo.IDENTIFICADOR
                    || tokenActual.getTipo() == Token.Tipo.PALABRA_RESERVADA) {
                String lexema = tokenActual.getLexema();

                if (lexema.equals("inicio")) {
                    procesarNodoSimple(raiz, "inicio");
                } else if (lexema.equals("fin")) {
                    procesarNodoSimple(raiz, "fin");
                } else if (lexema.equals("nodo") || lexema.equals("condicion") || lexema.equals("bucle") ||
                        lexema.equals("subproceso") || lexema.equals("entrada") ||
                        lexema.equals("salida") || lexema.equals("parada")) {
                    procesarNodoConTexto(raiz, lexema);
                } else {
                    procesarConexionOError(raiz);
                }
            } else {
                errores.reportarError("ES06", tokenActual.getLinea(), "Sintáctico Flujo",
                        "Token inesperado '" + tokenActual.getLexema() + "'.",
                        "Inicia la línea con una declaración válida (nodo, condicion, etc.) o una conexión.");
                pos++;
            }
        }
        return raiz;
    }

    private void procesarNodoSimple(RaizFlujoAST raiz, String rol) {
        int lineaKeyword = tokens.get(pos).getLinea();
        pos++;

        if (!validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "ES07",
                "Falta el nombre del identificador después de '" + rol + "'.", lineaKeyword)) {
            recuperarPanico();
            return;
        }
        int lineaId = tokens.get(pos).getLinea();
        String nombre = tokens.get(pos).getLexema();
        pos++;

        if (!validarSiguienteTipo(Token.Tipo.PUNTO_Y_COMA, "ES08",
                "Falta ';' al final de la declaración de '" + nombre + "'.", lineaId)) {
            recuperarPanico();
            return;
        }
        pos++;
        tabla.registrar(nombre, rol, lineaId);
        raiz.agregarElemento(new NodoProceso(nombre, "[" + rol.toUpperCase() + "]"));
    }

    private void procesarNodoConTexto(RaizFlujoAST raiz, String rol) {
        int lineaKeyword = tokens.get(pos).getLinea();
        pos++;

        if (!validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "ES07", "Falta el nombre del identificador.",
                lineaKeyword)) {
            recuperarPanico();
            return;
        }
        int lineaId = tokens.get(pos).getLinea();
        String nombre = tokens.get(pos).getLexema();
        pos++;

        if (!validarSiguienteTipo(Token.Tipo.TEXTO_LITERAL, "ES09",
                "Falta la descripción entre comillas para la instrucción '" + rol + "'.", lineaId)) {
            recuperarPanico();
            return;
        }
        int lineaTexto = tokens.get(pos).getLinea();
        String texto = tokens.get(pos).getLexema();
        pos++;

        if (!validarSiguienteTipo(Token.Tipo.PUNTO_Y_COMA, "ES10", "Falta ';' al final de la línea.", lineaTexto)) {
            recuperarPanico();
            return;
        }
        pos++;
        tabla.registrar(nombre, rol, lineaId);
        raiz.agregarElemento(new NodoProceso(nombre, "[" + rol.toUpperCase() + "] " + texto));
    }

    private void procesarConexionOError(RaizFlujoAST raiz) {
        Token tOrigen = tokens.get(pos);
        String idOrigen = tOrigen.getLexema();
        int lineaRelacion = tOrigen.getLinea();
        pos++;

        if (pos < tokens.size()
                && (tokens.get(pos).getTipo() == Token.Tipo.IDENTIFICADOR
                        || tokens.get(pos).getTipo() == Token.Tipo.PALABRA_RESERVADA)
                && tokens.get(pos).getLexema().equals("conecta")) {
            pos++;

            if (!validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "ES11", "Falta el elemento destino para la conexión.",
                    lineaRelacion)) {
                recuperarPanico();
                return;
            }
            String idDestino = tokens.get(pos).getLexema();
            int lineaDestino = tokens.get(pos).getLinea();
            pos++;

            if (!validarSiguienteTipo(Token.Tipo.PUNTO_Y_COMA, "ES12",
                    "Falta ';' al finalizar la instrucción de conexión.", lineaDestino)) {
                recuperarPanico();
                return;
            }
            pos++;
            raiz.agregarElemento(new NodoConexion(idOrigen, idDestino));
        } else {
            errores.reportarError("ES13", lineaRelacion, "Sintáctico Flujo",
                    "Instrucción o verbo inválido en '" + idOrigen + "'.",
                    "Si deseas conectar elementos utiliza el verbo exclusivo 'conecta'.");
            recuperarPanico();
        }
    }

    private boolean validarSiguienteTipo(Token.Tipo esperado, String codigo, String mensajeError, int linea) {
        if (pos >= tokens.size()) {
            errores.reportarError(codigo, linea, "Sintáctico Flujo", mensajeError, "Revisa la sintaxis del diagrama.");
            return false;
        }
        Token t = tokens.get(pos);
        if (esperado == Token.Tipo.IDENTIFICADOR &&
                (t.getTipo() == Token.Tipo.PALABRA_RESERVADA || t.getTipo() == Token.Tipo.PR_DIAGRAMA)) {
            errores.reportarError("ES56", linea, "Sintáctico Flujo",
                    "La palabra reservada '" + t.getLexema() + "' no puede usarse como nombre de identificador.",
                    "Usa un nombre definido por el usuario, no una palabra reservada del lenguaje.");
            return false;
        }
        if (t.getTipo() != esperado) {
            errores.reportarError(codigo, linea, "Sintáctico Flujo", mensajeError, "Revisa la sintaxis del diagrama.");
            return false;
        }
        return true;
    }

    private void recuperarPanico() {
        while (pos < tokens.size() && tokens.get(pos).getTipo() != Token.Tipo.PUNTO_Y_COMA
                && tokens.get(pos).getTipo() != Token.Tipo.EOF) {
            String lex = tokens.get(pos).getLexema();
            if (lex.equals("inicio") || lex.equals("fin") || lex.equals("nodo") ||
                    lex.equals("condicion") || lex.equals("bucle") || lex.equals("subproceso") ||
                    lex.equals("entrada") || lex.equals("salida") || lex.equals("parada")) {
                break; // Freno de emergencia: nueva instrucción detectada
            }
            pos++;
        }
        if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.PUNTO_Y_COMA)
            pos++;
    }
}
