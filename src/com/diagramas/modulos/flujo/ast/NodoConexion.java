package com.diagramas.modulos.flujo.ast;

public class NodoConexion extends NodeAST {
    private final String origen;
    private final String destino;

    public NodoConexion(String origen, String destino) {
        this.origen = origen;
        this.destino = destino;
    }

    public String getOrigen() { return origen; }
    public String getDestino() { return destino; }

    @Override
    public String toString() {
        return "NodoAST_Conexion [" + origen + " ──conecta──► " + destino + "]";
    }
}