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

    public void analizarCabecera() {
        if (tokens.isEmpty() || tokens.get(0).getTipo() == Token.Tipo.EOF) {
            manejadorErrores.reportarError(1, "Parser Base", "El archivo .dac está completamente vacío.", "Escriba la instrucción de cabecera: 'diagrama [Tipo];'");
            return;
        }

        Token tokenActual = tokens.get(posicion);

        // 1. Validar palabra clave obligatoria 'diagrama'
        if (tokenActual.getTipo() != Token.Tipo.PR_DIAGRAMA) {
            manejadorErrores.reportarError(
                    tokenActual.getLinea(),
                    "Parser Base",
                    "Falta la cabecera obligatoria del lenguaje.",
                    "Todo archivo .dac debe iniciar estrictamente con la palabra clave 'diagrama' (ej. diagrama Flujo;)."
            );
            return;
        }

        posicion++;
        Token tokenTipo = tokens.get(posicion);

        // 2. Validar que lo siguiente sea el Tipo de diagrama (Identificador)
        if (tokenTipo.getTipo() != Token.Tipo.IDENTIFICADOR) {
            manejadorErrores.reportarError(
                    tokenTipo.getLinea(),
                    "Parser Base",
                    "Se esperaba el tipo de diagrama.",
                    "Especifique un contexto válido después de la palabra 'diagrama' (ej. Flujo, BD, Redes)."
            );
            return;
        }

        posicion++;
        Token tokenPuntoYComa = tokens.get(posicion);

        // 3. Validar el cierre con punto y coma ';'
        if (tokenPuntoYComa.getTipo() != Token.Tipo.PUNTO_Y_COMA) {
            manejadorErrores.reportarError(
                    tokenPuntoYComa.getLinea(),
                    "Parser Base",
                    "Falta el punto y coma ';' al final de la cabecera.",
                    "Coloque un ';' inmediatamente después del nombre del tipo de diagrama."
            );
            return;
        }

        posicion++; // Consumir el ';' exitosamente

        // 4. Bloqueo de contexto estricto
        String tipoDiagrama = tokenTipo.getLexema();
        tablaSimbolos.setContextoActivo(tipoDiagrama);
        System.out.println("🔒 [ParserBase] Contexto '" + tipoDiagrama + "' bloqueado y activado de manera estricta.");

        // Pasar los tokens restantes al subsistema modular correspondiente
        delegarAlModulo(tipoDiagrama);
    }

    private void delegarAlModulo(String tipoDiagrama) {
        List<Token> tokensRestantes = tokens.subList(posicion, tokens.size());

        System.out.println("✈️ [Core] Transfiriendo " + (tokensRestantes.size() - 1) +
                " tokens al ecosistema modular.");

        switch (tipoDiagrama.toLowerCase()) {
            case "flujo":
                // Instanciamos el parser especializado del módulo de flujo
                com.diagramas.modulos.flujo.FlujoParser parserFlujo =
                        new com.diagramas.modulos.flujo.FlujoParser(tokensRestantes, tablaSimbolos, manejadorErrores);

                // Ejecutamos el análisis e imprimimos el AST resultante
                com.diagramas.modulos.flujo.ast.RaizFlujoAST ast = parserFlujo.parsear();

                if (!manejadorErrores.tieneErrores()) {
                    System.out.println(ast);
                }
                break;

            case "bd":
                // Instancia futura del parser de Base de Datos
                break;

            default:
                manejadorErrores.reportarError(
                        1,
                        "Infraestructura",
                        "El módulo para '" + tipoDiagrama + "' no está dado de alta en el ecosistema.",
                        "Asegúrate de que el paquete exista en 'com.diagramas.modulos." + tipoDiagrama.toLowerCase() + "'"
                );
        }
    }
}