package com.diagramas.core;

public class Token {

    // Enumerador para clasificar cada componente léxico
    public enum Tipo {
        // Palabras clave de infraestructura global
        PR_DIAGRAMA,    // "diagrama"
        IDENTIFICADOR,  // Nombres de variables, tablas, nodos, etc. (Ej: Usuarios, R1)
        TEXTO_LITERAL,  // Cadenas entre comillas (Ej: "192.168.1.1", "Leer credenciales")

        // Signos de puntuación comunes
        PUNTO_Y_COMA,   // ;
        DOS_PUNTOS,     // :
        LLAVE_IZQ,      // {
        LLAVE_DER,      // }

        // Control de flujo del archivo
        EOF             // End of File (Fin de archivo)
    }

    private final Tipo tipo;
    private final String lexema;
    private final int linea;

    public Token(Tipo tipo, String lexema, int linea) {
        this.tipo = tipo;
        this.lexema = lexema;
        this.linea = linea;
    }

    public Tipo getTipo() { return tipo; }
    public String getLexema() { return lexema; }
    public int getLinea() { return linea; }

    @Override
    public String toString() {
        return String.format("Token [%s | '%s' | Línea: %d]", tipo, lexema, linea);
    }
}