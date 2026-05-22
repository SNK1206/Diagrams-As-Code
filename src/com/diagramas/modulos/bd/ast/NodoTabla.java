package com.diagramas.modulos.bd.ast;

import java.util.List;

public class NodoTabla extends NodeAST {
    private final String nombre;
    private final List<Atributo> atributos;

    public NodoTabla(String nombre, List<Atributo> atributos) {
        this.nombre = nombre;
        this.atributos = atributos;
    }

    public String getNombre() { return nombre; }
    public List<Atributo> getAtributos() { return atributos; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("NodoAST_Tabla [").append(nombre).append("]\n");
        for (Atributo attr : atributos) {
            sb.append("      ├─ ").append(attr.toString()).append("\n");
        }
        return sb.toString();
    }
}