package com.diagramas.modulos.redes;

import com.diagramas.core.ManejadorErrores;
import com.diagramas.core.TablaSimbolos;
import com.diagramas.core.Token;
import com.diagramas.modulos.redes.ast.*;
import java.util.List;

public class RedesParser {
    private final List<Token> tokens;
    private final TablaSimbolos tabla;
    private final ManejadorErrores errores;
    private int pos;

    public RedesParser(List<Token> tokens, TablaSimbolos tabla, ManejadorErrores errores) {
        this.tokens = tokens;
        this.tabla = tabla;
        this.errores = errores;
        this.pos = 0;
    }

    public RaizRedesAST parsear() {
        RaizRedesAST raiz = new RaizRedesAST();

        while (pos < tokens.size() && tokens.get(pos).getTipo() != Token.Tipo.EOF) {
            Token tokenActual = tokens.get(pos);

            if (tokenActual.getTipo() == Token.Tipo.IDENTIFICADOR
                    || tokenActual.getTipo() == Token.Tipo.PALABRA_RESERVADA) {
                String lexema = tokenActual.getLexema();

                if (lexema.equals("dispositivo") || lexema.equals("nube") ||
                        lexema.equals("vlan") || lexema.equals("subred") ||
                        lexema.equals("cluster") || lexema.equals("tunel") ||
                        lexema.equals("zona") || lexema.equals("puerto") ||
                        lexema.equals("politica")) {
                    procesarComponenteRed(raiz, lexema);
                } else {
                    procesarEnlace(raiz);
                }
            } else {
                errores.reportarError("ES27", tokenActual.getLinea(), "Sintáctico Redes",
                        "Token inesperado '" + tokenActual.getLexema() + "'.",
                        "Usa una palabra clave de dispositivo o define un enlace con 'enlaza'.");
                pos++;
            }
        }
        return raiz;
    }

    private void procesarComponenteRed(RaizRedesAST raiz, String rol) {
        int lineaKeyword = tokens.get(pos).getLinea();
        pos++;

        if (!validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "ES28", "Falta el nombre del dispositivo.", lineaKeyword)) {
            recuperarPanico();
            return;
        }
        int lineaId = tokens.get(pos).getLinea();
        String nombre = tokens.get(pos).getLexema();
        pos++;

        if (!validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "ES29",
                "Falta el tipo del dispositivo (ej. Router, Switch).", lineaId)) {
            recuperarPanico();
            return;
        }
        String tipo = tokens.get(pos).getLexema();
        int lineaTipo = tokens.get(pos).getLinea();
        pos++;

        tabla.registrar(nombre, "dispositivo_" + tipo, lineaId);

        String config = "";
        if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.LLAVE_IZQ) {
            pos++;
            StringBuilder sb = new StringBuilder();
            while (pos < tokens.size() && tokens.get(pos).getTipo() != Token.Tipo.LLAVE_DER
                    && tokens.get(pos).getTipo() != Token.Tipo.EOF) {
                Token.Tipo tipoActual = tokens.get(pos).getTipo();
                if (tipoActual == Token.Tipo.LLAVE_IZQ) {
                    break;
                }
                if (tipoActual == Token.Tipo.IDENTIFICADOR || tipoActual == Token.Tipo.PALABRA_RESERVADA) {
                    String lex = tokens.get(pos).getLexema();
                    if (lex.equals("dispositivo") || lex.equals("nube") ||
                            lex.equals("vlan") || lex.equals("subred") ||
                            lex.equals("cluster") || lex.equals("tunel") ||
                            lex.equals("zona") || lex.equals("puerto") ||
                            lex.equals("politica") || lex.equals("enlaza")) {
                        break;
                    }

                    // Lookahead: Si el siguiente token NO es ':', significa que nos salimos del
                    // bloque
                    // (porque alguien olvidó cerrar la llave '}') y estamos leyendo una nueva
                    // instrucción.
                    if (pos + 1 < tokens.size() && tokens.get(pos + 1).getTipo() != Token.Tipo.DOS_PUNTOS) {
                        break;
                    }
                }

                int lineaProp = tokens.get(pos).getLinea();
                if (!validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "ES35", "Falta el identificador de la propiedad.",
                        lineaProp)) {
                    recuperarPanicoPropiedad();
                    continue;
                }
                String key = tokens.get(pos).getLexema();
                pos++;

                int lineaDP = pos < tokens.size() ? tokens.get(pos).getLinea() : lineaProp;
                if (!validarSiguienteTipo(Token.Tipo.DOS_PUNTOS, "ES36", "Falta ':' en la propiedad.", lineaDP)) {
                    recuperarPanicoPropiedad();
                    continue;
                }
                pos++;

                int lineaVal = pos < tokens.size() ? tokens.get(pos).getLinea() : lineaDP;
                if (!validarSiguienteTipo(Token.Tipo.TEXTO_LITERAL, "ES37", "Falta el valor (entre comillas).",
                        lineaVal)) {
                    recuperarPanicoPropiedad();
                    continue;
                }
                String val = tokens.get(pos).getLexema();
                pos++;

                int lineaPC = pos < tokens.size() ? tokens.get(pos).getLinea() : lineaVal;
                if (!validarSiguienteTipo(Token.Tipo.PUNTO_Y_COMA, "ES38", "Falta ';' al final de la propiedad.",
                        lineaPC)) {
                    recuperarPanicoPropiedad();
                    continue;
                }
                pos++;

                sb.append(key).append(": ").append(val).append("; ");
            }
            config = sb.toString().trim();
            int lineaBloque = pos > 0 ? tokens.get(pos - 1).getLinea() : lineaTipo;
            if (!validarSiguienteTipo(Token.Tipo.LLAVE_DER, "ES30", "Falta '}' para cerrar el bloque de propiedades.",
                    lineaBloque)) {
                return;
            }
            pos++;
        }

        int lineaFinal = pos > 0 ? tokens.get(pos - 1).getLinea() : lineaTipo;
        if (!validarSiguienteTipo(Token.Tipo.PUNTO_Y_COMA, "ES31",
                "Falta ';' al final de la declaración del dispositivo.", lineaFinal)) {
            recuperarPanico();
            return;
        }
        pos++;
        raiz.agregarElemento(new NodoDispositivo(nombre, tipo, config));
    }

    private void procesarEnlace(RaizRedesAST raiz) {
        Token tOrigen = tokens.get(pos);
        String idOrigen = tOrigen.getLexema();
        int lineaRelacion = tOrigen.getLinea();
        pos++;

        if (pos >= tokens.size() ||
                (tokens.get(pos).getTipo() != Token.Tipo.IDENTIFICADOR
                        && tokens.get(pos).getTipo() != Token.Tipo.PALABRA_RESERVADA)
                ||
                !tokens.get(pos).getLexema().equals("enlaza")) {
            errores.reportarError("ES34", lineaRelacion, "Sintáctico Redes",
                    "Verbo incorrecto en '" + idOrigen + "'.",
                    "Para redes usa el verbo exclusivo 'enlaza'.");
            recuperarPanico();
            return;
        }
        pos++;

        if (!validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "ES32",
                "Falta el identificador destino en instrucción 'enlaza'.", lineaRelacion)) {
            recuperarPanico();
            return;
        }
        String idDestino = tokens.get(pos).getLexema();
        int lineaDestino = tokens.get(pos).getLinea();
        pos++;

        if (!validarSiguienteTipo(Token.Tipo.PUNTO_Y_COMA, "ES33", "Falta ';' al finalizar la instrucción 'enlaza'.",
                lineaDestino)) {
            recuperarPanico();
            return;
        }
        pos++;
        raiz.agregarElemento(new NodoEnlace(idOrigen, idDestino));

    }

    private boolean validarSiguienteTipo(Token.Tipo esperado, String codigo, String mensajeError, int linea) {
        if (pos >= tokens.size()) {
            errores.reportarError(codigo, linea, "Sintáctico Redes", mensajeError, "Revisa la guía del módulo Redes.");
            return false;
        }
        Token t = tokens.get(pos);
        if (esperado == Token.Tipo.IDENTIFICADOR &&
                (t.getTipo() == Token.Tipo.PALABRA_RESERVADA || t.getTipo() == Token.Tipo.PR_DIAGRAMA)) {
            errores.reportarError("ES56", linea, "Sintáctico Redes",
                    "La palabra reservada '" + t.getLexema() + "' no puede usarse como nombre de identificador.",
                    "Usa un nombre definido por el usuario, no una palabra reservada del lenguaje.");
            return false;
        }
        if (t.getTipo() != esperado) {
            errores.reportarError(codigo, linea, "Sintáctico Redes", mensajeError, "Revisa la guía del módulo Redes.");
            return false;
        }
        return true;
    }

    private void recuperarPanico() {
        while (pos < tokens.size() && tokens.get(pos).getTipo() != Token.Tipo.PUNTO_Y_COMA
                && tokens.get(pos).getTipo() != Token.Tipo.EOF) {
            String lex = tokens.get(pos).getLexema();
            if (lex.equals("dispositivo") || lex.equals("nube") ||
                    lex.equals("vlan") || lex.equals("subred") ||
                    lex.equals("cluster") || lex.equals("tunel") ||
                    lex.equals("zona") || lex.equals("puerto") ||
                    lex.equals("politica")) {
                break;
            }
            pos++;
        }
        if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.PUNTO_Y_COMA)
            pos++;
    }

    private void recuperarPanicoPropiedad() {
        while (pos < tokens.size() && tokens.get(pos).getTipo() != Token.Tipo.PUNTO_Y_COMA
                && tokens.get(pos).getTipo() != Token.Tipo.LLAVE_DER
                && tokens.get(pos).getTipo() != Token.Tipo.EOF) {
            pos++;
        }
        if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.PUNTO_Y_COMA)
            pos++;
    }
}
