package com.diagramas.core;

import java.util.List;

public class ParserBase {
    private final List<Token> tokens;
    private final TablaSimbolos tablaSimbolos;
    private final ManejadorErrores manejadorErrores;
    private int posicion;

    public ParserBase(List<Token> tokens, TablaSimbolos tablaSimbolos, ManejadorErrores manejadorErrores) {
        this.tokens = tokens;
        this.tablaSimbolos = tablaSimbolos;
        this.manejadorErrores = manejadorErrores;
        this.posicion = 0;
    }

    // AHORA RETORNA EL AST GENERADO (Object para dar soporte polimórfico a cualquier diagrama)
    public Object analizarCabecera() {
        if (tokens.isEmpty() || tokens.get(0).getTipo() == Token.Tipo.EOF) {
            manejadorErrores.reportarError(1, "Parser Base", "El archivo .dac está completamente vacío.", "Escriba la instrucción de cabecera: 'diagrama [Tipo];'");
            return null;
        }

        Token tokenActual = tokens.get(posicion);

        if (tokenActual.getTipo() != Token.Tipo.PR_DIAGRAMA) {
            manejadorErrores.reportarError(
                    tokenActual.getLinea(),
                    "Parser Base",
                    "Falta la cabecera obligatoria del lenguaje.",
                    "Todo archivo .dac debe iniciar estrictamente con la palabra clave 'diagrama' (ej. diagrama Flujo;)."
            );
            return null;
        }

        posicion++;
        Token tokenTipo = tokens.get(posicion);

        if (tokenTipo.getTipo() != Token.Tipo.IDENTIFICADOR) {
            manejadorErrores.reportarError(
                    tokenTipo.getLinea(),
                    "Parser Base",
                    "Se esperaba el tipo de diagrama.",
                    "Especifique un contexto válido después de la palabra 'diagrama' (ej. Flujo, BD, Redes)."
            );
            return null;
        }

        posicion++;
        Token tokenPuntoYComa = tokens.get(posicion);

        if (tokenPuntoYComa.getTipo() != Token.Tipo.PUNTO_Y_COMA) {
            manejadorErrores.reportarError(
                    tokenPuntoYComa.getLinea(),
                    "Parser Base",
                    "Falta el punto y coma ';' al final de la cabecera.",
                    "Coloque un ';' inmediatamente después del nombre del tipo de diagrama."
            );
            return null;
        }

        posicion++; // Consumir el ';'

        String tipoDiagrama = tokenTipo.getLexema();
        tablaSimbolos.setContextoActivo(tipoDiagrama);
        System.out.println("🔒 [ParserBase] Contexto '" + tipoDiagrama + "' bloqueado de manera estricta.");

        return delegarAlModulo(tipoDiagrama);
    }

    private Object delegarAlModulo(String tipoDiagrama) {
        List<Token> tokensRestantes = tokens.subList(posicion, tokens.size());

        switch (tipoDiagrama.toLowerCase()) {
            case "flujo":
                com.diagramas.modulos.flujo.FlujoParser parserFlujo =
                        new com.diagramas.modulos.flujo.FlujoParser(tokensRestantes, tablaSimbolos, manejadorErrores);

                com.diagramas.modulos.flujo.ast.RaizFlujoAST ast = parserFlujo.parsear();

                if (!manejadorErrores.tieneErrores()) {
                    System.out.println(ast);
                    return ast; // Retornamos el AST para que la GUI lo grafique
                }
                break;

            case "bd":
                com.diagramas.modulos.bd.BDParser parserBD =
                        new com.diagramas.modulos.bd.BDParser(tokensRestantes, tablaSimbolos, manejadorErrores);
                com.diagramas.modulos.bd.ast.RaizBDAST astBD = parserBD.parsear();

                if (!manejadorErrores.tieneErrores()) {
                    System.out.println(astBD);
                    return astBD;
                }
                break;
            case "redes":
                com.diagramas.modulos.redes.RedesParser parserRedes =
                        new com.diagramas.modulos.redes.RedesParser(tokensRestantes, tablaSimbolos, manejadorErrores);
                com.diagramas.modulos.redes.ast.RaizRedesAST astRedes = parserRedes.parsear();
                if (!manejadorErrores.tieneErrores()) {
                    System.out.println(astRedes);
                    return astRedes;
                }
                break;

            default:
                manejadorErrores.reportarError(
                        1,
                        "Infraestructura",
                        "El módulo para '" + tipoDiagrama + "' no está dado de alta en el ecosistema.",
                        "Asegúrate de que el paquete exista en 'com.diagramas.modulos." + tipoDiagrama.toLowerCase() + "'"
                );
        }
        return null;
    }
}