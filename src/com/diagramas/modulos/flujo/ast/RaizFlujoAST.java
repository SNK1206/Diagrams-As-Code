package com.diagramas.modulos.flujo.ast;

import java.util.ArrayList;
import java.util.List;

public class RaizFlujoAST extends NodeAST {
    private final List<NodeAST> elementos = new ArrayList<>();

    public void agregarElemento(NodeAST nodo) {
        elementos.add(nodo);
    }

    public List<NodeAST> getElementos() { return elementos; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n==================================================\n");
        sb.append("🌳 ÁRBOL DE SINTAXIS ABSTRACTA (AST) GENERADO:\n");
        sb.append("==================================================\n");
        for (NodeAST elemento : elementos) {
            sb.append("  ").append(elemento.toString()).append("\n");
        }
        sb.append("==================================================");
        return sb.toString();
    }
}