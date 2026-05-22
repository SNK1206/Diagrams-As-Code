package com.diagramas.modulos.redes.ast;

public class NodoEnlace {
    private final String origen;
    private final String destino;

    public NodoEnlace(String origen, String destino) {
        this.origen = origen;
        this.destino = destino;
    }

    @Override
    public String toString() {
        return "📡 Enlace: " + origen + " ⚡──────⚡ " + destino;
    }
}