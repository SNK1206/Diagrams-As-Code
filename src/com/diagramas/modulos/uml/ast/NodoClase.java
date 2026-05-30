package com.diagramas.modulos.uml.ast;

import java.util.List;

public class NodoClase extends NodeAST {
    private final String nombre;
    private final List<NodoMiembro> miembros;

    public NodoClase(String nombre, List<NodoMiembro> miembros) {
        this.nombre = nombre;
        this.miembros = miembros;
    }

    public String getNombre() { return nombre; }
    public List<NodoMiembro> getMiembros() { return miembros; }

    @Override
    public String toString() {
        if (miembros.isEmpty()) return "NodoAST_Clase [" + nombre + "]";
        StringBuilder sb = new StringBuilder("NodoAST_Clase [" + nombre + "]");
        for (NodoMiembro m : miembros) {
            sb.append("\n      ├─ ").append(m.toString());
        }
        return sb.toString();
    }
}
