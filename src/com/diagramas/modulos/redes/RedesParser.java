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

            if (tokenActual.getTipo() == Token.Tipo.IDENTIFICADOR) {
                String lexema = tokenActual.getLexema();

                if (lexema.equals("dispositivo") || lexema.equals("nube") ||
                        lexema.equals("vlan") || lexema.equals("subred") ||
                        lexema.equals("cluster") || lexema.equals("tunel") ||
                        lexema.equals("zona") || lexema.equals("puerto") ||
                        lexema.equals("politica")) {

                    procesarComponenteRed(raiz, lexema); // <-- Renombra tu método a procesarComponenteRed y pásale el lexema
                } else {
                    procesarEnlace(raiz);
                }
            } else {
                errores.reportarError(tokenActual.getLinea(), "Sintáctico Redes", "Token inesperado '" + tokenActual.getLexema() + "'.", "Usa la palabra 'dispositivo' o define un enlace.");
                pos++;
            }
        }
        return raiz;
    }

    private void procesarComponenteRed(RaizRedesAST raiz, String rol) {
        int lineaOriginal = tokens.get(pos).getLinea();
        pos++; // Consumir 'dispositivo'

        if (validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "Falta el nombre del dispositivo.")) {
            String nombre = tokens.get(pos).getLexema();
            pos++;

            if (validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "Falta el tipo de dispositivo (ej. Router, Switch).")) {
                String tipo = tokens.get(pos).getLexema();
                pos++;

                String config = "";
                // Bloque opcional de propiedades { IP: "192..."; }
                if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.LLAVE_IZQ) {
                    pos++; // Consumir '{'
                    StringBuilder sb = new StringBuilder();
                    while (pos < tokens.size() && tokens.get(pos).getTipo() != Token.Tipo.LLAVE_DER && tokens.get(pos).getTipo() != Token.Tipo.EOF) {
                        sb.append(tokens.get(pos).getLexema()).append(" ");
                        pos++;
                    }
                    config = sb.toString().trim();
                    if (validarSiguienteTipo(Token.Tipo.LLAVE_DER, "Falta cerrar llaves '}'.")) pos++;
                }

                if (validarSiguienteTipo(Token.Tipo.PUNTO_Y_COMA, "Falta ';' al final del dispositivo.")) {
                    pos++;
                    if (!tabla.registrar(nombre, "dispositivo_" + tipo)) {
                        errores.reportarError(lineaOriginal, "Semántico Redes", "El dispositivo '" + nombre + "' ya existe.", "Asigna un nombre único.");
                    }
                    raiz.agregarElemento(new NodoDispositivo(nombre, tipo, config));
                }
            }
        }
    }

    private void procesarEnlace(RaizRedesAST raiz) {
        Token tOrigen = tokens.get(pos);
        String idOrigen = tOrigen.getLexema();
        pos++;

        if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.IDENTIFICADOR && tokens.get(pos).getLexema().equals("enlaza")) {
            pos++; // Consumir 'enlaza'

            if (validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "Falta el dispositivo destino.")) {
                String idDestino = tokens.get(pos).getLexema();
                pos++;

                if (validarSiguienteTipo(Token.Tipo.PUNTO_Y_COMA, "Falta ';'.")) {
                    pos++;

                    if (!tabla.existe(idOrigen)) {
                        errores.reportarError(tOrigen.getLinea(), "Semántico Redes", "El dispositivo '" + idOrigen + "' no existe.", "Decláralo antes de enlazarlo.");
                    } else if (!tabla.existe(idDestino)) {
                        errores.reportarError(tOrigen.getLinea(), "Semántico Redes", "El dispositivo '" + idDestino + "' no existe.", "Decláralo antes de enlazarlo.");
                    } else {
                        raiz.agregarElemento(new NodoEnlace(idOrigen, idDestino));
                    }
                }
            }
        } else {
            errores.reportarError(tOrigen.getLinea(), "Sintáctico Redes", "Verbo incorrecto en '" + idOrigen + "'.", "Para redes usa el verbo exclusivo 'enlaza'.");
            while (pos < tokens.size() && tokens.get(pos).getTipo() != Token.Tipo.PUNTO_Y_COMA && tokens.get(pos).getTipo() != Token.Tipo.EOF) pos++;
            if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.PUNTO_Y_COMA) pos++;
        }
    }

    private boolean validarSiguienteTipo(Token.Tipo esperado, String mensajeError) {
        if (pos >= tokens.size() || tokens.get(pos).getTipo() != esperado) {
            int l = (pos < tokens.size()) ? tokens.get(pos).getLinea() : tokens.get(pos - 1).getLinea();
            errores.reportarError(l, "Sintáctico Redes", mensajeError, "Revisa la guía del módulo Redes.");
            return false;
        }
        return true;
    }
}