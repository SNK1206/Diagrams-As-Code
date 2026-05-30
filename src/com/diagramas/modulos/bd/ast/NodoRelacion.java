package com.diagramas.modulos.bd.ast;

public class NodoRelacion extends NodeAST {
    private final String origen;
    private final String destino;

    public NodoRelacion(String origen, String destino) {
        this.origen = origen;
        this.destino = destino;
    }

    public String getOrigen() { return origen; }
    public String getDestino() { return destino; }

    @Override
    public String toString() {
        return "NodoAST_Relacion [" + origen + " ──relaciona──► " + destino + "]";
    }
}