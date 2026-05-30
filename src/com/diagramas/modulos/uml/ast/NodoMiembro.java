package com.diagramas.modulos.uml.ast;

public class NodoMiembro {
    private final String rol;
    private final String nombre;
    private final String tipo;

    public NodoMiembro(String rol, String nombre, String tipo) {
        this.rol = rol;
        this.nombre = nombre;
        this.tipo = tipo;
    }

    public String getRol() { return rol; }
    public String getNombre() { return nombre; }
    public String getTipo() { return tipo; }

    @Override
    public String toString() {
        return "[" + rol.toUpperCase() + "] " + nombre + " : " + tipo;
    }
}
