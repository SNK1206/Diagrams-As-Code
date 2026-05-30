package com.diagramas.modulos.conceptual.ast;

public class NodoConcepto extends NodeAST {
    private final String nombre;
    private final String rol;
    private final String descripcion;

    public NodoConcepto(String nombre, String rol, String descripcion) {
        this.nombre = nombre;
        this.rol = rol;
        this.descripcion = descripcion;
    }

    public String getNombre() { return nombre; }
    public String getRol() { return rol; }
    public String getDescripcion() { return descripcion; }

    @Override
    public String toString() {
        return "NodoAST_Concepto [" + rol.toUpperCase() + " | ID: " + nombre + " | \"" + descripcion + "\"]";
    }
}
