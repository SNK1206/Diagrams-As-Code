package com.diagramas.core;

import java.util.ArrayList;
import java.util.List;

public class LexerBase {
    private final String          codigo;
    private final ManejadorErrores manejadorErrores;
    private int indice;
    private int lineaActual;
    private int columnaActual;   // posicion de columna del caracter actual (base 1)

    public LexerBase(String codigo, ManejadorErrores manejadorErrores) {
        this.codigo            = codigo;
        this.manejadorErrores  = manejadorErrores;
        this.indice            = 0;
        this.lineaActual       = 1;
        this.columnaActual     = 1;
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
                columnaActual++; indice++;
                continue;
            }
            if (actual == ':') {
                tokens.add(new Token(Token.Tipo.DOS_PUNTOS, ":", lineaActual, columnaActual));
                columnaActual++; indice++;
                continue;
            }
            if (actual == '{') {
                tokens.add(new Token(Token.Tipo.LLAVE_IZQ, "{", lineaActual, columnaActual));
                columnaActual++; indice++;
                continue;
            }
            if (actual == '}') {
                tokens.add(new Token(Token.Tipo.LLAVE_DER, "}", lineaActual, columnaActual));
                columnaActual++; indice++;
                continue;
            }

            // 4. Cadenas de texto literales (entre comillas dobles)
            if (actual == '"') {
                int colInicio = columnaActual;
                StringBuilder sb = new StringBuilder();
                columnaActual++; indice++;  // saltar comilla de apertura
                // EL02: terminar también en \n (cadena no puede abarcar varias líneas)
                while (indice < codigo.length() && codigo.charAt(indice) != '"' && codigo.charAt(indice) != '\n') {
                    sb.append(codigo.charAt(indice));
                    columnaActual++;
                    indice++;
                }
                if (indice >= codigo.length() || codigo.charAt(indice) == '\n') {
                    manejadorErrores.reportarError("EL02", lineaActual, "Léxico",
                        "Cadena de texto sin cerrar.",
                        "Añade comillas dobles (\") al final del texto.");
                    // Tokens sintéticos para que el parser pueda continuar sin cascada
                    tokens.add(new Token(Token.Tipo.TEXTO_LITERAL, sb.toString(), lineaActual, colInicio));
                    tokens.add(new Token(Token.Tipo.PUNTO_Y_COMA, ";", lineaActual, columnaActual));
                } else {
                    columnaActual++; indice++;  // saltar comilla de cierre
                    tokens.add(new Token(Token.Tipo.TEXTO_LITERAL, sb.toString(), lineaActual, colInicio));
                }
                continue;
            }

            // 5. Identificador que inicia con dígito — EL03
            if (Character.isDigit(actual)) {
                StringBuilder sb = new StringBuilder();
                while (indice < codigo.length() &&
                       (Character.isLetterOrDigit(codigo.charAt(indice)) || codigo.charAt(indice) == '_')) {
                    sb.append(codigo.charAt(indice));
                    columnaActual++;
                    indice++;
                }
                manejadorErrores.reportarError(
                    "EL03", lineaActual, "Análisis Léxico",
                    "El identificador '" + sb.toString() + "' no puede iniciar con un dígito.",
                    "Los identificadores deben comenzar con una letra (a-z, A-Z) o guión bajo (_)."
                );
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
                            && (Character.isLetterOrDigit(codigo.charAt(indice + 1)) || codigo.charAt(indice + 1) == '_')
                            && !Character.isWhitespace(c)
                            && c != ';' && c != ':' && c != '{' && c != '}' && c != '"' && c != '\n') {
                        // EL04: carácter inválido dentro de un identificador (ej: mi@nodo)
                        manejadorErrores.reportarError(
                            "EL04", lineaActual, "Análisis Léxico",
                            "El carácter '" + c + "' no es válido dentro del identificador '" + sb.toString() + "...'.",
                            "Los identificadores solo pueden contener letras, dígitos y guión bajo (_)."
                        );
                        columnaActual++;
                        indice++;
                    } else {
                        break;
                    }
                }
                String lexema = sb.toString();
                if (lexema.equals("diagrama")) {
                    tokens.add(new Token(Token.Tipo.PR_DIAGRAMA, lexema, lineaActual, colInicio));
                } else {
                    tokens.add(new Token(Token.Tipo.IDENTIFICADOR, lexema, lineaActual, colInicio));
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
}
