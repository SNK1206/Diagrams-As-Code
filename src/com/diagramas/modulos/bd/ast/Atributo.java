package com.diagramas.modulos.bd.ast;

public class Atributo {
    private final String nombre;
    private final String tipoDato;
    private final String modificador; // Puede estar vacío, o contener "PK", "FK"

    public Atributo(String nombre, String tipoDato, String modificador) {
        this.nombre = nombre;
        this.tipoDato = tipoDato;
        this.modificador = modificador;
    }

    public String getNombre() { return nombre; }
    public String getTipoDato() { return tipoDato; }
    public String getModificador() { return modificador; }

    @Override
    public String toString() {
        String mod = modificador.isEmpty() ? "" : " <" + modificador + ">";
        return nombre + ": " + tipoDato + mod;
    }
}