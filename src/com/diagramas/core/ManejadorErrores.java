package com.diagramas.core;

import java.util.ArrayList;
import java.util.List;

public class ManejadorErrores {
    private final List<String> errores;

    public ManejadorErrores() {
        this.errores = new ArrayList<>();
    }

    public void reportarError(int linea, String contexto, String mensaje, String sugerencia) {
        String formato = String.format(
                "==================================================\n" +
                        "❌ ERROR DE COMPILACIÓN [Línea %d] [Contexto: %s]\n" +
                        "💡 Detalle: %s\n" +
                        "🔍 Sugerencia: %s\n" +
                        "==================================================",
                linea, contexto, mensaje, sugerencia
        );
        errores.add(formato);
    }

    public void reportarError(String codigo, int linea, String contexto, String mensaje, String sugerencia) {
        String codigoStr = (codigo != null && !codigo.isEmpty()) ? " [" + codigo + "]" : "";
        String formato = String.format(
                "==================================================\n" +
                        "❌ ERROR DE COMPILACIÓN%s [Línea %d] [Contexto: %s]\n" +
                        "💡 Detalle: %s\n" +
                        "🔍 Sugerencia: %s\n" +
                        "==================================================",
                codigoStr, linea, contexto, mensaje, sugerencia
        );
        errores.add(formato);
    }

    public void reportarErrorLéxico(int linea, char caracterInvalido) {
        reportarError(
                "EL01",
                linea,
                "Léxico",
                "El carácter '" + caracterInvalido + "' no pertenece al alfabeto del lenguaje DAC.",
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
    }

    public void limpiar() {
        errores.clear();
    }
}