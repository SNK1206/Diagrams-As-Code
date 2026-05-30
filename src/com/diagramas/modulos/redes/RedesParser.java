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
        pos++;

        if (!validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "ES28", "Falta el nombre del dispositivo.")) {
            recuperarPanico(); return;
        }
        String nombre = tokens.get(pos).getLexema();
        pos++;

        if (!validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "ES29", "Falta el tipo del dispositivo (ej. Router, Switch).")) {
            recuperarPanico(); return;
        }
        String tipo = tokens.get(pos).getLexema();
        pos++;

        // Pre-registrar antes del bloque para evitar cascada en instrucciones enlaza
        tabla.registrar(nombre, "dispositivo_" + tipo);

        String config = "";
        if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.LLAVE_IZQ) {
            pos++;
            StringBuilder sb = new StringBuilder();
            while (pos < tokens.size() && tokens.get(pos).getTipo() != Token.Tipo.LLAVE_DER && tokens.get(pos).getTipo() != Token.Tipo.EOF) {
                sb.append(tokens.get(pos).getLexema()).append(" ");
                pos++;
            }
            config = sb.toString().trim();
            if (!validarSiguienteTipo(Token.Tipo.LLAVE_DER, "ES30", "Falta '}' para cerrar el bloque de propiedades.")) {
                return;
            }
            pos++;
        }

        if (!validarSiguienteTipo(Token.Tipo.PUNTO_Y_COMA, "ES31", "Falta ';' al final de la declaración del dispositivo.")) {
            recuperarPanico(); return;
        }
        pos++;
        raiz.agregarElemento(new NodoDispositivo(nombre, tipo, config));
    }

    private void procesarEnlace(RaizRedesAST raiz) {
        Token tOrigen = tokens.get(pos);
        String idOrigen = tOrigen.getLexema();
        pos++;

        if (pos >= tokens.size() || tokens.get(pos).getTipo() != Token.Tipo.IDENTIFICADOR || !tokens.get(pos).getLexema().equals("enlaza")) {
            errores.reportarError("ES34", tOrigen.getLinea(), "Sintáctico Redes",
                "Verbo incorrecto en '" + idOrigen + "'.",
                "Para redes usa el verbo exclusivo 'enlaza'.");
            recuperarPanico(); return;
        }
        pos++;

        if (!validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "ES32", "Falta el identificador destino en instrucción 'enlaza'.")) {
            recuperarPanico(); return;
        }
        String idDestino = tokens.get(pos).getLexema();
        pos++;

        if (!validarSiguienteTipo(Token.Tipo.PUNTO_Y_COMA, "ES33", "Falta ';' al finalizar la instrucción 'enlaza'.")) {
            recuperarPanico(); return;
        }
        pos++;
        raiz.agregarElemento(new NodoEnlace(idOrigen, idDestino));
    }

    private boolean validarSiguienteTipo(Token.Tipo esperado, String codigo, String mensajeError) {
        if (pos >= tokens.size() || tokens.get(pos).getTipo() != esperado) {
            int l = (pos < tokens.size()) ? tokens.get(pos).getLinea() : tokens.get(pos - 1).getLinea();
            errores.reportarError(codigo, l, "Sintáctico Redes", mensajeError, "Revisa la guía del módulo Redes.");
            return false;
        }
        return true;
    }

    private void recuperarPanico() {
        while (pos < tokens.size() && tokens.get(pos).getTipo() != Token.Tipo.PUNTO_Y_COMA && tokens.get(pos).getTipo() != Token.Tipo.EOF) pos++;
        if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.PUNTO_Y_COMA) pos++;
    }
}
