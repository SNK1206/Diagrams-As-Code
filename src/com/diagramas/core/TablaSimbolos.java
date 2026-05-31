package com.diagramas.core;

import java.util.HashMap;
import java.util.Map;

public class TablaSimbolos {
    // Estructura para guardar: Nombre -> Rol/Tipo
    private final Map<String, String> simbolos = new HashMap<>();

    // NUEVO: Variable para recordar qué diagrama está activo
    private String contextoActivo = "Ninguno";

    // NUEVO: El método que invoca el ParserBase
    public void bloquearContexto(String contexto) {
        this.contextoActivo = contexto;
        System.out.println("[TABLA] Contexto bloqueado para: " + contexto);
    }

    public boolean registrar(String nombre, String rol) {
        if (simbolos.containsKey(nombre)) {
            return false; // Ya existe, error semántico de duplicado
        }
        simbolos.put(nombre, rol);
        System.out.println("[TABLA] Registrado: " + nombre + " -> " + rol + " [" + contextoActivo + "]");
        return true;
    }

    public boolean existe(String nombre) {
        return simbolos.containsKey(nombre);
    }

    public void limpiar() {
        simbolos.clear();
        contextoActivo = "Ninguno";
    }

    // Para pintar en el panel derecho del IDE
    public Map<String, String> getSimbolos() {
        return simbolos;
    }

    public String getContextoActivo() {
        return contextoActivo;
    }
}