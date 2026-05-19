package com.diagramas.modulos.flujo.ast;

public class NodoProceso extends NodeAST {
    private final String id;
    private final String descripcion;

    public NodoProceso(String id, String descripcion) {
        this.id = id;
        this.descripcion = descripcion;
    }

    public String getId() { return id; }
    public String getDescripcion() { return descripcion; }

    @Override
    public String toString() {
        return "NodoAST_Proceso [ID: " + id + " | Descripción: \"" + descripcion + "\"]";
    }
}