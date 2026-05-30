package com.diagramas.modulos.conceptual.ast;

import java.util.ArrayList;
import java.util.List;

public class RaizConceptualAST extends NodeAST {
    private final List<NodeAST> elementos = new ArrayList<>();

    public void agregarElemento(NodeAST nodo) { elementos.add(nodo); }
    public List<NodeAST> getElementos() { return elementos; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n==================================================\n");
        sb.append("🧠 ÁRBOL DE SINTAXIS ABSTRACTA (AST) - MAPA CONCEPTUAL:\n");
        sb.append("==================================================\n");
        for (NodeAST el : elementos) {
            sb.append("  ").append(el.toString()).append("\n");
        }
        sb.append("==================================================");
        return sb.toString();
    }
}
