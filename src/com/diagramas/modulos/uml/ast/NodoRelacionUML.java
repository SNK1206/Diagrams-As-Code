package com.diagramas.modulos.uml.ast;

public class NodoRelacionUML extends NodeAST {
    private final String origen;
    private final String verbo;
    private final String destino;

    public NodoRelacionUML(String origen, String verbo, String destino) {
        this.origen = origen;
        this.verbo = verbo;
        this.destino = destino;
    }

    public String getOrigen() { return origen; }
    public String getVerbo() { return verbo; }
    public String getDestino() { return destino; }

    @Override
    public String toString() {
        return "NodoAST_Relacion [" + origen + " ──" + verbo + "──► " + destino + "]";
    }
}
