package com.diagramas.modulos.flujo.ast;

public class NodoInicio extends NodeAST {
    private final String id;

    public NodoInicio(String id) {
        this.id = id;
    }

    public String getId() { return id; }

    @Override
    public String toString() {
        return "NodoAST_Inicio [ID: " + id + "]";
    }
}