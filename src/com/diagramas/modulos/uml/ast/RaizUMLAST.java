package com.diagramas.modulos.uml.ast;

import java.util.ArrayList;
import java.util.List;

public class RaizUMLAST extends NodeAST {
    private final List<NodeAST> elementos = new ArrayList<>();

    public void agregarElemento(NodeAST nodo) { elementos.add(nodo); }
    public List<NodeAST> getElementos() { return elementos; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n==================================================\n");
        sb.append("📐 ÁRBOL DE SINTAXIS ABSTRACTA (AST) - DIAGRAMA UML:\n");
        sb.append("==================================================\n");
        for (NodeAST el : elementos) {
            sb.append("  ").append(el.toString()).append("\n");
        }
        sb.append("==================================================");
        return sb.toString();
    }
}
