package com.diagramas.core;

import java.util.HashMap;
import java.util.Map;

public class TablaSimbolos {
    // Almacena: Identificador -> Tipo de Elemento (ej: "Usuarios" -> "tabla")
    private final Map<String, String> tabla;
    private String contextoActivo;

    public TablaSimbolos() {
        this.tabla = new HashMap<>();
        this.contextoActivo = "GLOBAL";
    }

    public void setContextoActivo(String contexto) {
        this.contextoActivo = contexto;
    }

    public String getContextoActivo() {
        return contextoActivo;
    }

    public boolean registrar(String id, String tipoElemento) {
        if (tabla.containsKey(id)) {
            return false; // Elemento duplicado
        }
        tabla.put(id, tipoElemento);
        return true;
    }

    public boolean existe(String id) {
        return tabla.containsKey(id);
    }

    public String obtenerTipo(String id) {
        return tabla.get(id);
    }

    public void limpiar() {
        tabla.clear();
        contextoActivo = "GLOBAL";
    }
}