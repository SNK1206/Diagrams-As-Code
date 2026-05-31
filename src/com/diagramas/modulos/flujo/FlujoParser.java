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

            if (tokenActual.getTipo() == Token.Tipo.IDENTIFICADOR) {
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
        pos++;

        if (!validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "ES07", "Falta el nombre del identificador después de '" + rol + "'.")) {
            recuperarPanico(); return;
        }
        int lineaId = tokens.get(pos).getLinea();
        String nombre = tokens.get(pos).getLexema();
        pos++;

        if (!validarSiguienteTipo(Token.Tipo.PUNTO_Y_COMA, "ES08", "Falta ';' al final de la declaración de '" + nombre + "'.")) {
            recuperarPanico(); return;
        }
        pos++;
        tabla.registrar(nombre, rol, lineaId);
        raiz.agregarElemento(new NodoProceso(nombre, "[" + rol.toUpperCase() + "]"));
    }

    private void procesarNodoConTexto(RaizFlujoAST raiz, String rol) {
        pos++;

        if (!validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "ES07", "Falta el nombre del identificador.")) {
            recuperarPanico(); return;
        }
        int lineaId = tokens.get(pos).getLinea();
        String nombre = tokens.get(pos).getLexema();
        pos++;

        if (!validarSiguienteTipo(Token.Tipo.TEXTO_LITERAL, "ES09", "Falta la descripción entre comillas para la instrucción '" + rol + "'.")) {
            recuperarPanico(); return;
        }
        String texto = tokens.get(pos).getLexema();
        pos++;

        if (!validarSiguienteTipo(Token.Tipo.PUNTO_Y_COMA, "ES10", "Falta ';' al final de la línea.")) {
            recuperarPanico(); return;
        }
        pos++;
        tabla.registrar(nombre, rol, lineaId);
        raiz.agregarElemento(new NodoProceso(nombre, "[" + rol.toUpperCase() + "] " + texto));
    }

    private void procesarConexionOError(RaizFlujoAST raiz) {
        Token tOrigen = tokens.get(pos);
        String idOrigen = tOrigen.getLexema();
        pos++;

        if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.IDENTIFICADOR && tokens.get(pos).getLexema().equals("conecta")) {
            pos++;

            if (!validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "ES11", "Falta el elemento destino para la conexión.")) {
                recuperarPanico(); return;
            }
            String idDestino = tokens.get(pos).getLexema();
            pos++;

            if (!validarSiguienteTipo(Token.Tipo.PUNTO_Y_COMA, "ES12", "Falta ';' al finalizar la instrucción de conexión.")) {
                recuperarPanico(); return;
            }
            pos++;
            raiz.agregarElemento(new NodoConexion(idOrigen, idDestino));
        } else {
            errores.reportarError("ES13", tOrigen.getLinea(), "Sintáctico Flujo",
                "Instrucción o verbo inválido en '" + idOrigen + "'.",
                "Si deseas conectar elementos utiliza el verbo exclusivo 'conecta'.");
            recuperarPanico();
        }
    }

    private boolean validarSiguienteTipo(Token.Tipo esperado, String codigo, String mensajeError) {
        if (pos >= tokens.size() || tokens.get(pos).getTipo() != esperado) {
            int l = (pos < tokens.size()) ? tokens.get(pos).getLinea() : (pos > 0 ? tokens.get(pos - 1).getLinea() : 1);
            errores.reportarError(codigo, l, "Sintáctico Flujo", mensajeError, "Revisa la sintaxis del diagrama.");
            return false;
        }
        return true;
    }

    private void recuperarPanico() {
        while (pos < tokens.size() && tokens.get(pos).getTipo() != Token.Tipo.PUNTO_Y_COMA && tokens.get(pos).getTipo() != Token.Tipo.EOF) {
            pos++;
        }
        if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.PUNTO_Y_COMA) pos++;
    }
}
