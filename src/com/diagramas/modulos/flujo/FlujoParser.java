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
                    // ¡Un solo método procesa dinámicamente las 7 instrucciones!
                    procesarNodoConTexto(raiz, lexema);
                } else {
                    procesarConexionOError(raiz);
                }
            } else {
                errores.reportarError("ES06", tokenActual.getLinea(), "Sintáctico Flujo", "Token inesperado '" + tokenActual.getLexema() + "'.", "Inicia la línea con una declaración válida (nodo, condicion, etc.) o una conexión.");
                pos++;
            }
        }
        return raiz;
    }

    private void procesarNodoSimple(RaizFlujoAST raiz, String rol) {
        int lineaOriginal = tokens.get(pos).getLinea();
        pos++; // Consumir palabra clave

        if (validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "ES07", "Falta el nombre del identificador después de '" + rol + "'.")) {
            String nombre = tokens.get(pos).getLexema();
            pos++;

            if (validarSiguienteTipo(Token.Tipo.PUNTO_Y_COMA, "ES08", "Falta ';' al final de la declaración de '" + nombre + "'.")) {
                pos++;
                if (!tabla.registrar(nombre, rol)) {
                    errores.reportarError(lineaOriginal, "Semántico Flujo", "El identificador '" + nombre + "' ya existe.", "Usa un nombre diferente.");
                }
                raiz.agregarElemento(new NodoProceso(nombre, "[" + rol.toUpperCase() + "]"));
            }
        }
    }

    private void procesarNodoConTexto(RaizFlujoAST raiz, String rol) {
        int lineaOriginal = tokens.get(pos).getLinea();
        pos++; // Consumir palabra clave (nodo, condicion, bucle, subproceso)

        if (validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "ES07", "Falta el nombre del identificador.")) {
            String nombre = tokens.get(pos).getLexema();
            pos++;

            if (validarSiguienteTipo(Token.Tipo.TEXTO_LITERAL, "ES09", "Falta la descripción entre comillas para la instrucción '" + rol + "'.")) {
                String texto = tokens.get(pos).getLexema();
                pos++;

                if (validarSiguienteTipo(Token.Tipo.PUNTO_Y_COMA, "ES10", "Falta ';' al final de la línea.")) {
                    pos++;
                    if (!tabla.registrar(nombre, rol)) {
                        errores.reportarError(lineaOriginal, "Semántico Flujo", "El identificador '" + nombre + "' ya está registrado.", "Cambia el nombre para evitar colisiones.");
                    }
                    raiz.agregarElemento(new NodoProceso(nombre, "[" + rol.toUpperCase() + "] " + texto));
                }
            }
        }
    }

    private void procesarConexionOError(RaizFlujoAST raiz) {
        Token tOrigen = tokens.get(pos);
        String idOrigen = tOrigen.getLexema();
        pos++;

        if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.IDENTIFICADOR && tokens.get(pos).getLexema().equals("conecta")) {
            pos++; // Consumir 'conecta'

            if (validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "ES11", "Falta el elemento destino para la conexión.")) {
                String idDestino = tokens.get(pos).getLexema();
                pos++;

                if (validarSiguienteTipo(Token.Tipo.PUNTO_Y_COMA, "ES12", "Falta ';' al finalizar la instrucción de conexión.")) {
                    pos++;

                    if (!tabla.existe(idOrigen)) {
                        errores.reportarError(tOrigen.getLinea(), "Semántico Flujo", "El origen '" + idOrigen + "' no ha sido declarado.", "Declara el elemento antes de conectarlo.");
                    } else if (!tabla.existe(idDestino)) {
                        errores.reportarError(tOrigen.getLinea(), "Semántico Flujo", "El destino '" + idDestino + "' no ha sido declarado.", "Declara el elemento de destino antes de conectarlo.");
                    } else {
                        raiz.agregarElemento(new NodoConexion(idOrigen, idDestino));
                    }
                }
            }
        } else {
            errores.reportarError("ES13", tOrigen.getLinea(), "Sintáctico Flujo", "Instrucción o verbo inválido en '" + idOrigen + "'.", "Si deseas conectar elementos utiliza el verbo exclusivo 'conecta'.");
            recuperarPanico();
        }
    }

    private boolean validarSiguienteTipo(Token.Tipo esperado, String codigo, String mensajeError) {
        if (pos >= tokens.size() || tokens.get(pos).getTipo() != esperado) {
            int l = (pos < tokens.size()) ? tokens.get(pos).getLinea() : tokens.get(pos - 1).getLinea();
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