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
        long numDiagramas = tokens.stream().filter(t -> t.getTipo() == Token.Tipo.PR_DIAGRAMA).count();
        if (numDiagramas > 1) {
            manejadorErrores.reportarError("ES57", tokens.get(0).getLinea(), "Sintáctico Núcleo",
                "Solo se permite un diagrama por archivo.",
                "Elimina las declaraciones múltiples de 'diagrama'. Un archivo .dac solo puede contener un diagrama a la vez.");
            return;
        }

        boolean diagramaEncontrado = false;

        while (pos < tokens.size() && tokens.get(pos).getTipo() != Token.Tipo.EOF) {
            Token t = tokens.get(pos);

            // Token flotante: cadena sin instrucción previa
            if (t.getTipo() == Token.Tipo.TEXTO_LITERAL) {
                manejadorErrores.reportarError("ES02", t.getLinea(), "Sintáctico Núcleo",
                    "Cadena de texto " + t.getLexema() + " sin instrucción previa en la cabecera.",
                    "Una cadena debe ir después de: autor, version o tema.");
                pos++;
                if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.PUNTO_Y_COMA) pos++;
                continue;
            }

            // Token inesperado que no es identificador, palabra reservada ni PR_DIAGRAMA
            if (t.getTipo() != Token.Tipo.IDENTIFICADOR && t.getTipo() != Token.Tipo.PALABRA_RESERVADA && t.getTipo() != Token.Tipo.PR_DIAGRAMA) {
                manejadorErrores.reportarError("ES01", t.getLinea(), "Sintáctico Núcleo",
                    "Token inesperado '" + t.getLexema() + "' en la cabecera del diagrama.",
                    "La cabecera solo acepta instrucciones: autor, version, tema, diagrama.");
                pos++;
                continue;
            }

            if (t.getTipo() == Token.Tipo.IDENTIFICADOR || t.getTipo() == Token.Tipo.PALABRA_RESERVADA || t.getTipo() == Token.Tipo.PR_DIAGRAMA) {
                String lexema = t.getLexema();

                // 1. META-INSTRUCCIONES
                if (lexema.equals("autor") || lexema.equals("version") ||
                        lexema.equals("tema")) {

                    pos++;
                    if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.TEXTO_LITERAL) {
                        String valor = tokens.get(pos).getLexema();
                        int lineaValor = tokens.get(pos).getLinea();
                        tablaSimbolos.registrar(lexema, valor, t.getLinea());
                        pos++;
                        if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.PUNTO_Y_COMA) {
                            pos++;
                        } else {
                            manejadorErrores.reportarError("ES01", lineaValor, "Sintáctico Núcleo",
                                "Falta ';' al final de la instrucción '" + lexema + "'.",
                                "Añade un punto y coma al final.");
                        }
                        continue;
                    } else {
                        int lineaError = pos < tokens.size() ? tokens.get(pos).getLinea() : t.getLinea();
                        manejadorErrores.reportarError("ES02", lineaError, "Sintáctico Núcleo",
                            "Se esperaba un texto entre comillas después de '" + lexema + "'.",
                            "Usa comillas dobles para el valor, ej: " + lexema + " \"valor\";");
                        continue;
                    }
                }
                // 2. CABECERA DEL DIAGRAMA
                else if (lexema.equals("diagrama")) {
                    pos++;

                    if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.IDENTIFICADOR) {
                        String tipoDiagrama = tokens.get(pos).getLexema().toLowerCase();
                        String tipoCapitalizado = tipoDiagrama.substring(0, 1).toUpperCase() + tipoDiagrama.substring(1);
                        pos++;

                        if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.PUNTO_Y_COMA) {
                            pos++;
                            tablaSimbolos.registrar("diagrama", tipoCapitalizado, t.getLinea());
                            tablaSimbolos.registrar(tipoCapitalizado, "tipo_diagrama", t.getLinea());
                            tablaSimbolos.bloquearContexto(tipoCapitalizado);
                            List<Token> tokensRestantes = new ArrayList<>(tokens.subList(pos, tokens.size()));
                            delegarAlModulo(tipoDiagrama, tokensRestantes);
                            diagramaEncontrado = true;
                            break;
                        } else {
                            int lineaError = pos < tokens.size() ? tokens.get(pos).getLinea() : t.getLinea();
                            manejadorErrores.reportarError("ES03", lineaError, "Sintáctico",
                                "Falta ';' en la cabecera 'diagrama " + tipoDiagrama + "'.",
                                "Termina la declaración con punto y coma.");
                        }
                    } else {
                        int lineaError = pos < tokens.size() ? tokens.get(pos).getLinea() : t.getLinea();
                        manejadorErrores.reportarError("ES04", lineaError, "Sintáctico",
                            "Falta el tipo de diagrama después de 'diagrama'.",
                            "Especifica 'Flujo', 'BD', 'Redes', 'Conceptual' o 'UML'.");
                    }
                    break;
                }
                // 3. IDENTIFICADOR DESCONOCIDO EN CABECERA
                else {
                    manejadorErrores.reportarError("ES01", t.getLinea(), "Sintáctico Núcleo",
                        "Instrucción desconocida '" + lexema + "' en la cabecera. Instrucciones válidas: autor, version, tema, diagrama.",
                        "Verifica el nombre de la instrucción o elimina esta línea.");
                    pos++; // saltar solo el token desconocido
                    if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.PUNTO_Y_COMA) pos++;
                    continue;
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
