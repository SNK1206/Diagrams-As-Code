package com.diagramas.core;

import java.util.ArrayList;
import java.util.List;

public class LexerBase {
    private final String codigo;
    private final ManejadorErrores manejadorErrores;
    private int indice;
    private int lineaActual;

    public LexerBase(String codigo, ManejadorErrores manejadorErrores) {
        this.codigo = codigo;
        this.manejadorErrores = manejadorErrores;
        this.indice = 0;
        this.lineaActual = 1;
    }

    public List<Token> tokenizar() {
        List<Token> tokens = new ArrayList<>();

        while (indice < codigo.length()) {
            char actual = codigo.charAt(indice);

            // 1. Manejo de saltos de línea y espacios en blanco
            if (actual == '\n') {
                lineaActual++;
                indice++;
                continue;
            }
            if (Character.isWhitespace(actual)) {
                indice++;
                continue;
            }

            // --- NUEVO: 2. Manejo de Comentarios (Absorción Léxica) ---
            // Soporta comentarios estilo Java (//) o estilo Script (#)
            if (actual == '#' || (actual == '/' && indice + 1 < codigo.length() && codigo.charAt(indice + 1) == '/')) {
                // Avanzamos el índice velozmente hasta encontrar un salto de línea (fin del comentario)
                while (indice < codigo.length() && codigo.charAt(indice) != '\n') {
                    indice++;
                }
                continue; // Volvemos al inicio del bucle (el \n se procesará en la siguiente vuelta)
            }
            // -----------------------------------------------------------

            // 3. Signos de puntuación de un solo carácter
            if (actual == ';') {
                tokens.add(new Token(Token.Tipo.PUNTO_Y_COMA, ";", lineaActual));
                indice++;
                continue;
            }
            if (actual == ':') {
                tokens.add(new Token(Token.Tipo.DOS_PUNTOS, ":", lineaActual));
                indice++;
                continue;
            }
            if (actual == '{') {
                tokens.add(new Token(Token.Tipo.LLAVE_IZQ, "{", lineaActual));
                indice++;
                continue;
            }
            if (actual == '}') {
                tokens.add(new Token(Token.Tipo.LLAVE_DER, "}", lineaActual));
                indice++;
                continue;
            }

            // 4. Cadenas de texto literales (entre comillas)
            if (actual == '"') {
                StringBuilder sb = new StringBuilder();
                indice++; // Saltar la comilla de apertura
                while (indice < codigo.length() && codigo.charAt(indice) != '"' && codigo.charAt(indice) != '\n') {
                    sb.append(codigo.charAt(indice));
                    indice++;
                }
                if (indice >= codigo.length() || codigo.charAt(indice) == '\n') {
                    manejadorErrores.reportarError("EL02", lineaActual, "Léxico", "Cadena de texto sin cerrar.", "Añade comillas dobles (\") al final del texto.");
                    // Tokens sintéticos para que el parser pueda continuar sin cascada
                    tokens.add(new Token(Token.Tipo.TEXTO_LITERAL, sb.toString(), lineaActual));
                    tokens.add(new Token(Token.Tipo.PUNTO_Y_COMA, ";", lineaActual));
                } else {
                    indice++; // Saltar la comilla de cierre
                    tokens.add(new Token(Token.Tipo.TEXTO_LITERAL, sb.toString(), lineaActual));
                }
                continue;
            }

            // 5. Identificadores y Palabras Clave
            if (Character.isLetter(actual) || actual == '_') {
                StringBuilder sb = new StringBuilder();
                while (indice < codigo.length()) {
                    char c = codigo.charAt(indice);
                    if (Character.isLetterOrDigit(c) || c == '_') {
                        sb.append(c);
                        indice++;
                    } else if (indice + 1 < codigo.length()
                            && (Character.isLetterOrDigit(codigo.charAt(indice + 1)) || codigo.charAt(indice + 1) == '_')
                            && !Character.isWhitespace(c)
                            && c != ';' && c != ':' && c != '{' && c != '}' && c != '"' && c != '\n') {
                        // Carácter inválido dentro de un identificador (ej: id@cliente)
                        // Se reporta pero se fusiona para no partir el token
                        manejadorErrores.reportarErrorLéxico(lineaActual, c);
                        indice++;
                    } else {
                        break;
                    }
                }
                String lexema = sb.toString();
                if (lexema.equals("diagrama")) {
                    tokens.add(new Token(Token.Tipo.PR_DIAGRAMA, lexema, lineaActual));
                } else {
                    tokens.add(new Token(Token.Tipo.IDENTIFICADOR, lexema, lineaActual));
                }
                continue;
            }

            // 6. Captura pedagógica de caracteres inválidos (Errores Léxicos)
            manejadorErrores.reportarErrorLéxico(lineaActual, actual);
            indice++;
        }

        tokens.add(new Token(Token.Tipo.EOF, "EOF", lineaActual));
        return tokens;
    }
}