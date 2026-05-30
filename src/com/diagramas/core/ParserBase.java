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

            if (t.getTipo() == Token.Tipo.IDENTIFICADOR || t.getTipo() == Token.Tipo.PR_DIAGRAMA) {
                String lexema = t.getLexema();

                // 1. META-INSTRUCCIONES
                if (lexema.equals("autor") || lexema.equals("version") ||
                        lexema.equals("tema") || lexema.equals("exportar") || lexema.equals("importar")) {

                    pos++;
                    if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.TEXTO_LITERAL) {
                        pos++;
                        if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.PUNTO_Y_COMA) {
                            pos++;
                        } else {
                            manejadorErrores.reportarError("ES01", t.getLinea(), "Sintáctico Núcleo",
                                "Falta ';' al final de la instrucción '" + lexema + "'.",
                                "Añade un punto y coma al final.");
                        }
                        continue;
                    } else {
                        manejadorErrores.reportarError("ES02", t.getLinea(), "Sintáctico Núcleo",
                            "Se esperaba un texto entre comillas después de '" + lexema + "'.",
                            "Usa comillas dobles para el valor, ej: " + lexema + " \"valor\";");
                        pos++;
                        continue;
                    }
                }
                // 2. CABECERA DEL DIAGRAMA
                else if (lexema.equals("diagrama")) {
                    pos++;

                    if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.IDENTIFICADOR) {
                        String tipoDiagrama = tokens.get(pos).getLexema().toLowerCase();
                        pos++;

                        if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.PUNTO_Y_COMA) {
                            pos++;
                            tablaSimbolos.bloquearContexto(tipoDiagrama.substring(0, 1).toUpperCase() + tipoDiagrama.substring(1));
                            List<Token> tokensRestantes = new ArrayList<>(tokens.subList(pos, tokens.size()));
                            delegarAlModulo(tipoDiagrama, tokensRestantes);
                            diagramaEncontrado = true;
                            break;
                        } else {
                            manejadorErrores.reportarError("ES03", t.getLinea(), "Sintáctico",
                                "Falta ';' en la cabecera 'diagrama " + tipoDiagrama + "'.",
                                "Termina la declaración con punto y coma.");
                        }
                    } else {
                        manejadorErrores.reportarError("ES04", t.getLinea(), "Sintáctico",
                            "Falta el tipo de diagrama después de 'diagrama'.",
                            "Especifica 'Flujo', 'BD', 'Redes', 'Conceptual' o 'UML'.");
                    }
                    break;
                }
            }
            pos++;
        }

        if (!diagramaEncontrado && !manejadorErrores.tieneErrores()) {
            manejadorErrores.reportarError("ES05", 1, "Sintáctico",
                "No se encontró la cabecera principal 'diagrama <Tipo>;'.",
                "Asegúrate de incluir 'diagrama Flujo;', 'diagrama BD;' o 'diagrama Redes;'.");
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
            case "conceptual":
                com.diagramas.modulos.conceptual.ConceptualParser parserConceptual = new com.diagramas.modulos.conceptual.ConceptualParser(tokensRestantes, tablaSimbolos, manejadorErrores);
                return parserConceptual.parsear();
            case "uml":
                com.diagramas.modulos.uml.UMLParser parserUML = new com.diagramas.modulos.uml.UMLParser(tokensRestantes, tablaSimbolos, manejadorErrores);
                return parserUML.parsear();
            default:
                manejadorErrores.reportarError("ES05", 1, "Sintáctico",
                    "Módulo '" + tipoDiagrama + "' no reconocido.",
                    "Los módulos válidos son: Flujo, BD, Redes, Conceptual, UML.");
                return null;
        }
    }
}
