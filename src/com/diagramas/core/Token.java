package com.diagramas.core;

public class Token {

    // Enumerador para clasificar cada componente léxico
    public enum Tipo {
        // Palabras clave de infraestructura global
        PR_DIAGRAMA,       // "diagrama"
        PALABRA_RESERVADA, // Palabras clave de módulo (nodo, clase, tabla, conecta, etc.)
        IDENTIFICADOR,     // Nombres definidos por el usuario (Ej: Usuarios, R1, miClase)
        TEXTO_LITERAL,     // Cadenas entre comillas (Ej: "192.168.1.1", "Leer credenciales")

        // Signos de puntuación comunes
        PUNTO_Y_COMA,   // ;
        DOS_PUNTOS,     // :
        LLAVE_IZQ,      // {
        LLAVE_DER,      // }

        // Control de flujo del archivo
        EOF             // End of File (Fin de archivo)
    }

    private final Tipo   tipo;
    private final String lexema;
    private final int    linea;
    private final int    columna;   // columna donde inicia el token (base 1)

    public Token(Tipo tipo, String lexema, int linea, int columna) {
        this.tipo    = tipo;
        this.lexema  = lexema;
        this.linea   = linea;
        this.columna = columna;
    }

    // Constructor de compatibilidad sin columna (columna = 0 indica "no registrada")
    public Token(Tipo tipo, String lexema, int linea) {
        this(tipo, lexema, linea, 0);
    }

    public Tipo   getTipo()    { return tipo;    }
    public String getLexema()  { return lexema;  }
    public int    getLinea()   { return linea;   }
    public int    getColumna() { return columna; }

    @Override
    public String toString() {
        return String.format("Token [%s | '%s' | Línea: %d | Col: %d]",
                             tipo, lexema, linea, columna);
    }
}
