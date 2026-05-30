package com.diagramas.core;

import java.util.ArrayList;
import java.util.List;

public class ManejadorErrores {
    private static final int MAX_ERRORES = 10;

    private final List<String> errores;
    private boolean limiteSuperado = false;

    public ManejadorErrores() {
        this.errores = new ArrayList<>();
    }

    public void reportarError(String codigo, int linea, String contexto, String mensaje, String consejo) {
        if (contexto.contains("Semántico") || contexto.contains("Semantico")) return;
        if (errores.size() >= MAX_ERRORES) { limiteSuperado = true; return; }

        boolean esLexico = contexto.contains("Léxico") || contexto.contains("Lexico");
        String tipo = esLexico ? "LÉXICO" : "SINTÁCTICO";
        String etiqueta = (codigo == null || codigo.isEmpty()) ? "" : "[" + codigo + "] ";

        String formato = String.format(
                "==================================================\n" +
                "❌ %sERROR %s [Línea %d]\n" +
                "💡 Detalle: %s\n" +
                "🔍 Consejo: %s\n" +
                "==================================================",
                etiqueta, tipo, linea, mensaje, consejo
        );
        errores.add(formato);
    }

    public void reportarError(int linea, String contexto, String mensaje, String consejo) {
        reportarError("", linea, contexto, mensaje, consejo);
    }

    public void reportarErrorLéxico(int linea, char caracterInvalido) {
        reportarError(
                "EL01", linea, "Análisis Léxico",
                "El carácter '" + caracterInvalido + "' no pertenece al alfabeto del lenguaje.",
                "Elimina el carácter o verifica si querías escribir un identificador o una cadena entre comillas \".\""
        );
    }

    public boolean tieneErrores() {
        return !errores.isEmpty();
    }

    public void imprimirErrores() {
        for (String error : errores) {
            System.err.println(error);
        }
        if (limiteSuperado) {
            System.err.println("==================================================");
            System.err.println("⚠️  Se suprimieron errores adicionales para evitar cascada.");
            System.err.println("   Corrige los errores mostrados y vuelve a compilar.");
            System.err.println("==================================================");
        }
    }

    public void limpiar() {
        errores.clear();
        limiteSuperado = false;
    }
}
