package com.diagramas.core;

import java.util.ArrayList;
import java.util.List;

public class ParserBase {
    private final List<Token> tokens;
    private final TablaSimbolos tablaSimbolos;
    private final ManejadorErrores manejadorErrores;
    private int pos;

    public ParserBase(List<Token> tokens, TablaSimbolos tablaSimbolos, ManejadorErrores manejadorErrores) {
        this.tokens = tokens;
        this.tablaSimbolos = tablaSimbolos;
        this.manejadorErrores = manejadorErrores;
        this.pos = 0;
    }

    public void analizarCabecera() {
        boolean diagramaEncontrado = false;

        while (pos < tokens.size() && tokens.get(pos).getTipo() != Token.Tipo.EOF) {
            Token t = tokens.get(pos);

            // Evaluamos si el token es un identificador genérico o la palabra reservada 'diagrama'
            if (t.getTipo() == Token.Tipo.IDENTIFICADOR || t.getTipo() == Token.Tipo.PR_DIAGRAMA) {
                String lexema = t.getLexema();

                // 1. INTERCEPTAR META-INSTRUCCIONES DEL NÚCLEO
                if (lexema.equals("autor") || lexema.equals("version") ||
                        lexema.equals("tema") || lexema.equals("exportar") || lexema.equals("importar")) {

                    pos++; // Consumir la palabra clave
                    if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.TEXTO_LITERAL) {
                        String valor = tokens.get(pos).getLexema();
                        System.out.println("⚙️ [NÚCLEO] Meta-Instrucción procesada: " + lexema + " -> " + valor);
                        pos++; // Consumir el valor entre comillas

                        // Consumir el punto y coma (;) obligatorio
                        if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.PUNTO_Y_COMA) {
                            pos++;
                        } else {
                            manejadorErrores.reportarError(t.getLinea(), "Sintáctico Núcleo", "Falta ';' al final de la instrucción '" + lexema + "'.", "Añade un punto y coma.");
                        }
                        continue; // Volver al inicio del bucle para leer la siguiente línea
                    } else {
                        manejadorErrores.reportarError(t.getLinea(), "Sintáctico Núcleo", "Se esperaba un texto entre comillas después de '" + lexema + "'.", "Usa comillas dobles para el valor.");
                        pos++;
                        continue;
                    }
                }
                // 2. PROCESAR LA CABECERA DEL DIAGRAMA
                else if (lexema.equals("diagrama")) {
                    pos++; // Consumir 'diagrama'

                    if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.IDENTIFICADOR) {
                        String tipoDiagrama = tokens.get(pos).getLexema().toLowerCase();
                        pos++;

                        if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.PUNTO_Y_COMA) {
                            pos++; // Consumir ';'

                            // Bloquear la tabla de símbolos para el ecosistema específico
                            tablaSimbolos.bloquearContexto(tipoDiagrama.substring(0, 1).toUpperCase() + tipoDiagrama.substring(1));

                            // Recortar los tokens restantes y enviarlos al submódulo
                            List<Token> tokensRestantes = new ArrayList<>(tokens.subList(pos, tokens.size()));
                            delegarAlModulo(tipoDiagrama, tokensRestantes);

                            diagramaEncontrado = true;
                            break; // Romper el bucle del núcleo, el submódulo toma el control total
                        } else {
                            manejadorErrores.reportarError(t.getLinea(), "Sintáctico", "Falta ';' en la cabecera.", "Termina la declaración con punto y coma.");
                        }
                    } else {
                        manejadorErrores.reportarError(t.getLinea(), "Sintáctico", "Falta el tipo de diagrama.", "Especifica 'Flujo', 'BD' o 'Redes'.");
                    }
                    break;
                }
            }
            pos++;
        }

        if (!diagramaEncontrado && !manejadorErrores.tieneErrores()) {
            manejadorErrores.reportarError(1, "Sintáctico", "No se encontró la cabecera principal.", "Asegúrate de incluir 'diagrama [Tipo];' en el archivo.");
        }
    }

    private Object delegarAlModulo(String tipoDiagrama, List<Token> tokensRestantes) {
        switch (tipoDiagrama) {
            case "flujo":
                com.diagramas.modulos.flujo.FlujoParser parserFlujo = new com.diagramas.modulos.flujo.FlujoParser(tokensRestantes, tablaSimbolos, manejadorErrores);
                return parserFlujo.parsear();
            case "bd":
                com.diagramas.modulos.bd.BDParser parserBD = new com.diagramas.modulos.bd.BDParser(tokensRestantes, tablaSimbolos, manejadorErrores);
                return parserBD.parsear();
            case "redes":
                com.diagramas.modulos.redes.RedesParser parserRedes = new com.diagramas.modulos.redes.RedesParser(tokensRestantes, tablaSimbolos, manejadorErrores);
                return parserRedes.parsear();
            default:
                manejadorErrores.reportarError(1, "Semántico", "Módulo '" + tipoDiagrama + "' no reconocido.", "Los módulos válidos son: Flujo, BD, Redes.");
                return null;
        }
    }
}