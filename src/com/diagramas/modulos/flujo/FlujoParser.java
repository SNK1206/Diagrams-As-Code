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

        // Procesar todos los tokens asignados hasta llegar al Fin de Archivo (EOF)
        while (pos < tokens.size() && tokens.get(pos).getTipo() != Token.Tipo.EOF) {
            Token tokenActual = tokens.get(pos);

            if (tokenActual.getTipo() == Token.Tipo.IDENTIFICADOR) {
                String lexema = tokenActual.getLexema();

                // Evaluar palabras clave específicas del lenguaje de flujo
                if (lexema.equals("inicio")) {
                    procesarInicio(raiz);
                } else if (lexema.equals("nodo")) {
                    procesarNodoProceso(raiz);
                } else {
                    // Si no es palabra reservada, asumimos que intenta iniciar una conexión semántica
                    procesarConexionOError(raiz);
                }
            } else {
                errores.reportarError(
                        tokenActual.getLinea(),
                        "Sintáctico Flujo",
                        "Token inesperado '" + tokenActual.getLexema() + "'.",
                        "Asegúrate de iniciar la línea declarando un componente ('inicio', 'nodo') o una conexión."
                );
                pos++;
            }
        }
        return raiz;
    }

    private void procesarInicio(RaizFlujoAST raiz) {
        int lineaOriginal = tokens.get(pos).getLinea();
        pos++; // Consumir 'inicio'

        if (validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "Falta el identificador del punto de partida.")) {
            String id = tokens.get(pos).getLexema();
            pos++;

            if (validarSiguienteTipo(Token.Tipo.PUNTO_Y_COMA, "Falta el ';' al cerrar la declaración de inicio.")) {
                pos++;

                // CONTROL SEMÁNTICO: Registrar en memoria y validar unicidad
                if (!tabla.registrar(id, "inicio")) {
                    errores.reportarError(lineaOriginal, "Semántico Flujo", "El identificador '" + id + "' ya se encuentra registrado.", "Usa un nombre diferente para este componente.");
                } else {
                    raiz.agregarElemento(new NodoInicio(id));
                }
            }
        }
    }

    private void procesarNodoProceso(RaizFlujoAST raiz) {
        int lineaOriginal = tokens.get(pos).getLinea();
        pos++; // Consumir 'nodo'

        if (validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "Falta el identificador del nodo.")) {
            String id = tokens.get(pos).getLexema();
            pos++;

            if (validarSiguienteTipo(Token.Tipo.TEXTO_LITERAL, "Falta la descripción del nodo. Debe ir entre comillas dobles (\"\").")) {
                String descripcion = tokens.get(pos).getLexema();
                pos++;

                if (validarSiguienteTipo(Token.Tipo.PUNTO_Y_COMA, "Falta el ';' al cerrar la definición del nodo.")) {
                    pos++;

                    // CONTROL SEMÁNTICO: Evitar colisiones de nombres
                    if (!tabla.registrar(id, "nodo")) {
                        errores.reportarError(lineaOriginal, "Semántico Flujo", "El identificador '" + id + "' ya está ocupado.", "Intenta renombrar el nodo.");
                    } else {
                        raiz.agregarElemento(new NodoProceso(id, descripcion));
                    }
                }
            }
        }
    }

    private void procesarConexionOError(RaizFlujoAST raiz) {
        Token tOrigen = tokens.get(pos);
        String idOrigen = tOrigen.getLexema();
        pos++; // Consumir posible identificador origen

        // Validar si el conector semántico del módulo es correcto
        if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.IDENTIFICADOR && tokens.get(pos).getLexema().equals("conecta")) {
            pos++; // Consumir conector 'conecta'

            if (validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "Falta el identificador del nodo destino en la conexión.")) {
                String idDestino = tokens.get(pos).getLexema();
                pos++;

                if (validarSiguienteTipo(Token.Tipo.PUNTO_Y_COMA, "Falta el ';' al finalizar la instrucción de enlace.")) {
                    pos++;

                    // CONTROL SEMÁNTICO CRÍTICO: ¿Existen ambos nodos en la Tabla de Símbolos?
                    if (!tabla.existe(idOrigen)) {
                        errores.reportarError(tOrigen.getLinea(), "Semántico Flujo", "El origen '" + idOrigen + "' no existe en el diagrama.", "Declara el componente antes con 'inicio' o 'nodo'.");
                    } else if (!tabla.existe(idDestino)) {
                        errores.reportarError(tOrigen.getLinea(), "Semántico Flujo", "El destino '" + idDestino + "' no está registrado.", "Verifica errores de dedo o declara el nodo de destino.");
                    } else {
                        raiz.agregarElemento(new NodoConexion(idOrigen, idDestino));
                    }
                }
            }
        } else {
            // Manejo pedagógico en caso de sintaxis rota
            errores.reportarError(
                    tOrigen.getLinea(),
                    "Sintáctico Flujo",
                    "Instrucción no válida o verbo incorrecto en '" + idOrigen + "'.",
                    "Para entrelazar flujos de manera legible usa el verbo conector 'conecta' (Ej: NodoA conecta NodoB;)"
            );
            recuperacionPanico();
        }
    }

    private boolean validarSiguienteTipo(Token.Tipo esperado, String mensajeError) {
        if (pos >= tokens.size() || tokens.get(pos).getTipo() != esperado) {
            int l = (pos < tokens.size()) ? tokens.get(pos).getLinea() : tokens.get(pos - 1).getLinea();
            errores.reportarError(l, "Sintáctico Flujo", mensajeError, "Verifica el orden de los parámetros según los ejemplos de la guía.");
            return false;
        }
        return true;
    }

    private void recuperacionPanico() {
        // Sincronizar el parser saltando tokens hasta el próximo fin de instrucción ';' para no encadenar falsas alarmas
        while (pos < tokens.size() && tokens.get(pos).getTipo() != Token.Tipo.PUNTO_Y_COMA && tokens.get(pos).getTipo() != Token.Tipo.EOF) {
            pos++;
        }
        if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.PUNTO_Y_COMA) {
            pos++;
        }
    }
}