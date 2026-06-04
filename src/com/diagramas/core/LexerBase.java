package com.diagramas.core;

import java.util.ArrayList;
import java.util.List;

public class LexerBase {

    private static final java.util.Set<String> PALABRAS_RESERVADAS = new java.util.HashSet<>(java.util.Arrays.asList(
            // Meta-instrucciones globales
            "autor", "version", "tema",
            // Módulo Flujo
            "inicio", "fin", "nodo", "condicion", "bucle", "subproceso", "entrada", "salida", "parada", "conecta",
            // Módulo BD
            "tabla", "vista", "esquema", "paquete", "procedimiento", "indice", "disparador", "secuencia", "funcion",
            "relaciona",
            // Módulo Redes
            "dispositivo", "nube", "vlan", "subred", "cluster", "tunel", "zona", "puerto", "politica", "enlaza",
            // Módulo Conceptual
            "concepto", "categoria", "propiedad", "agrupa", "asocia", "depende", "abarca", "incluye",
            // Módulo UML
            "clase", "interfaz", "enum", "atributo", "metodo", "extiende", "implementa", "usa"));

    private final String codigo;
    private final ManejadorErrores manejadorErrores;
    private int indice;
    private int lineaActual;
    private int columnaActual; // posicion de columna del caracter actual (base 1)

    public LexerBase(String codigo, ManejadorErrores manejadorErrores) {
        this.codigo = codigo;
        this.manejadorErrores = manejadorErrores;
        this.indice = 0;
        this.lineaActual = 1;
        this.columnaActual = 1;
    }

    public List<Token> tokenizar() {
        List<Token> tokens = new ArrayList<>();

        while (indice < codigo.length()) {
            char actual = codigo.charAt(indice);

            // 1. Saltos de línea: avanzan línea y reinician columna
            if (actual == '\n') {
                lineaActual++;
                columnaActual = 1;
                indice++;
                continue;
            }

            // Espacios en blanco (sin salto de línea): solo avanzan columna
            if (Character.isWhitespace(actual)) {
                columnaActual++;
                indice++;
                continue;
            }

            // 2. Comentarios de línea (// o #): se absorben hasta el \n
            if (actual == '#' || (actual == '/' && indice + 1 < codigo.length() && codigo.charAt(indice + 1) == '/')) {
                while (indice < codigo.length() && codigo.charAt(indice) != '\n') {
                    columnaActual++;
                    indice++;
                }
                continue;
            }

            // 3. Signos de puntuación de un solo carácter
            if (actual == ';') {
                tokens.add(new Token(Token.Tipo.PUNTO_Y_COMA, ";", lineaActual, columnaActual));
                columnaActual++;
                indice++;
                continue;
            }
            if (actual == ':') {
                tokens.add(new Token(Token.Tipo.DOS_PUNTOS, ":", lineaActual, columnaActual));
                columnaActual++;
                indice++;
                continue;
            }
            if (actual == '{') {
                tokens.add(new Token(Token.Tipo.LLAVE_IZQ, "{", lineaActual, columnaActual));
                columnaActual++;
                indice++;
                continue;
            }
            if (actual == '}') {
                tokens.add(new Token(Token.Tipo.LLAVE_DER, "}", lineaActual, columnaActual));
                columnaActual++;
                indice++;
                continue;
            }

            // 4. Cadenas de texto literales (entre comillas dobles)
            if (actual == '"') {
                int colInicio = columnaActual;
                StringBuilder sb = new StringBuilder();
                columnaActual++;
                indice++; // saltar comilla de apertura
                // EL02: terminar también en \n (cadena no puede abarcar varias líneas)
                while (indice < codigo.length() && codigo.charAt(indice) != '\n') {
                    char c = codigo.charAt(indice);
                    // Soportar secuencias de escape como \" o \\
                    if (c == '\\' && indice + 1 < codigo.length() && codigo.charAt(indice + 1) != '\n') {
                        char next = codigo.charAt(indice + 1);
                        if (next == '"') { sb.append('"'); }
                        else if (next == '\\') { sb.append('\\'); }
                        else if (next == 'n') { sb.append('\n'); }
                        else { sb.append('\\'); sb.append(next); }
                        columnaActual += 2;
                        indice += 2;
                        continue;
                    }
                    if (c == '"') {
                        break;
                    }
                    sb.append(c);
                    columnaActual++;
                    indice++;
                }
                if (indice >= codigo.length() || codigo.charAt(indice) == '\n') {
                    manejadorErrores.reportarError("ES55", lineaActual, "Sintáctico",
                            "Cadena de texto sin cerrar.",
                            "Añade comillas dobles (\") al final del texto.");
                    // Tokens sintéticos para que el parser pueda continuar sin cascada
                    tokens.add(new Token(Token.Tipo.TEXTO_LITERAL, sb.toString(), lineaActual, colInicio));
                    tokens.add(new Token(Token.Tipo.PUNTO_Y_COMA, ";", lineaActual, columnaActual));
                } else {
                    columnaActual++;
                    indice++; // saltar comilla de cierre
                    tokens.add(new Token(Token.Tipo.TEXTO_LITERAL, sb.toString(), lineaActual, colInicio));
                }
                continue;
            }

            // 5. Identificador que inicia con dígito — EL02
            if (Character.isDigit(actual)) {
                StringBuilder sb = new StringBuilder();
                while (indice < codigo.length() &&
                        (Character.isLetterOrDigit(codigo.charAt(indice)) || codigo.charAt(indice) == '_')) {
                    sb.append(codigo.charAt(indice));
                    columnaActual++;
                    indice++;
                }
                manejadorErrores.reportarError(
                        "EL02", lineaActual, "Análisis Léxico",
                        "Identificador inválido: '" + sb.toString() + "' no puede iniciar con un dígito.",
                        "Los identificadores deben comenzar con una letra (a-z, A-Z) o guión bajo (_).");
                continue;
            }

            // 6. Identificadores y Palabras Clave
            if (Character.isLetter(actual) || actual == '_') {
                int colInicio = columnaActual;
                StringBuilder sb = new StringBuilder();
                while (indice < codigo.length()) {
                    char c = codigo.charAt(indice);
                    if (Character.isLetterOrDigit(c) || c == '_') {
                        sb.append(c);
                        columnaActual++;
                        indice++;
                    } else if (indice + 1 < codigo.length()
                            && (Character.isLetterOrDigit(codigo.charAt(indice + 1))
                                    || codigo.charAt(indice + 1) == '_')
                            && !Character.isWhitespace(c)
                            && c != ';' && c != ':' && c != '{' && c != '}' && c != '"' && c != '\n') {
                        // EL04: carácter inválido dentro de un identificador (ej: mi@nodo)
                        manejadorErrores.reportarError(
                                "EL04", lineaActual, "Análisis Léxico",
                                "Identificador inválido: carácter '" + c + "' no permitido en '" + sb.toString() + "'.",
                                "Los identificadores solo pueden contener letras, dígitos y guión bajo (_).");
                        columnaActual++;
                        indice++;
                    } else {
                        break;
                    }
                }
                String lexema = sb.toString();
                String lexemaLower = lexema.toLowerCase();
                if (lexema.equals("diagrama")) {
                    tokens.add(new Token(Token.Tipo.PR_DIAGRAMA, lexema, lineaActual, colInicio));
                } else if (PALABRAS_RESERVADAS.contains(lexema)) {
                    tokens.add(new Token(Token.Tipo.PALABRA_RESERVADA, lexema, lineaActual, colInicio));
                } else if (lexemaLower.equals("diagrama")) {
                    manejadorErrores.reportarError("EL03", lineaActual, "Análisis Léxico",
                            "Palabra reservada mal escrita: '" + lexema
                                    + "'. El lenguaje es sensible a mayúsculas/minúsculas.",
                            "Escribe la palabra reservada en minúsculas: 'diagrama'.");
                    tokens.add(new Token(Token.Tipo.PR_DIAGRAMA, "diagrama", lineaActual, colInicio));
                } else if (PALABRAS_RESERVADAS.contains(lexemaLower)) {
                    manejadorErrores.reportarError("EL03", lineaActual, "Análisis Léxico",
                            "Palabra reservada mal escrita: '" + lexema
                                    + "'. El lenguaje es sensible a mayúsculas/minúsculas.",
                            "Escribe la palabra reservada en minúsculas: '" + lexemaLower + "'.");
                    tokens.add(new Token(Token.Tipo.PALABRA_RESERVADA, lexemaLower, lineaActual, colInicio));
                } else {
                    String palabraSimilar = buscarPalabraReservadaSimilar(lexemaLower);
                    if (palabraSimilar != null) {
                        manejadorErrores.reportarError("EL05", lineaActual, "Análisis Léxico",
                                "Palabra reservada mal escrita o irreconocible: '" + lexema + "'.",
                                "¿Quisiste decir '" + palabraSimilar + "'?");
                        if (palabraSimilar.equals("diagrama")) {
                            tokens.add(new Token(Token.Tipo.PR_DIAGRAMA, "diagrama", lineaActual, colInicio));
                        } else {
                            tokens.add(new Token(Token.Tipo.PALABRA_RESERVADA, palabraSimilar, lineaActual, colInicio));
                        }
                    } else {
                        tokens.add(new Token(Token.Tipo.IDENTIFICADOR, lexema, lineaActual, colInicio));
                    }
                }
                continue;
            }

            // 7. Captura pedagógica de caracteres inválidos — EL01
            manejadorErrores.reportarErrorLéxico(lineaActual, actual);
            columnaActual++;
            indice++;
        }

        tokens.add(new Token(Token.Tipo.EOF, "EOF", lineaActual, columnaActual));
        return tokens;
    }

    private String buscarPalabraReservadaSimilar(String palabra) {
        if (palabra.length() <= 2) return null;
        
        int distDiagrama = calcularDistanciaLevenshtein(palabra, "diagrama");
        if (distDiagrama <= 2 && Math.abs(palabra.length() - "diagrama".length()) <= 1) {
            return "diagrama";
        }

        for (String reservada : PALABRAS_RESERVADAS) {
            int distancia = calcularDistanciaLevenshtein(palabra, reservada);
            int tolerancia = reservada.length() <= 5 ? 1 : 2;
            if (distancia <= tolerancia && Math.abs(palabra.length() - reservada.length()) <= 1) {
                return reservada;
            }
        }
        return null;
    }

    private int calcularDistanciaLevenshtein(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1),
                            Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1));
                }
            }
        }
        return dp[s1.length()][s2.length()];
    }

}
