package com.diagramas.modulos.redes.ast;

import java.util.ArrayList;
import java.util.List;

public class RaizRedesAST {
    private final List<Object> elementos = new ArrayList<>();

    public void agregarElemento(Object nodo) { elementos.add(nodo); }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n==================================================\n");
        sb.append("🌐 ÁRBOL DE SINTAXIS ABSTRACTA (AST) - TOPOLOGÍA DE RED:\n");
        sb.append("==================================================\n");
        for (Object el : elementos) {
            sb.append("  ").append(el.toString()).append("\n");
        }
        sb.append("==================================================");
        return sb.toString();
    }
}