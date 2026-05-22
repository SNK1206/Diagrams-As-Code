package com.diagramas.modulos.redes.ast;

public class NodoDispositivo {
    private final String nombre;
    private final String tipo;
    private final String configuracion;

    public NodoDispositivo(String nombre, String tipo, String configuracion) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.configuracion = configuracion;
    }

    @Override
    public String toString() {
        return "🖥️ Dispositivo [" + nombre + "] Tipo: " + tipo + (configuracion.isEmpty() ? "" : " Config: {" + configuracion + "}");
    }
}