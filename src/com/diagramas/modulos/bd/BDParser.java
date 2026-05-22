package com.diagramas.modulos.bd;

import com.diagramas.core.ManejadorErrores;
import com.diagramas.core.TablaSimbolos;
import com.diagramas.core.Token;
import com.diagramas.modulos.bd.ast.*;
import java.util.ArrayList;
import java.util.List;

public class BDParser {
    private final List<Token> tokens;
    private final TablaSimbolos tabla;
    private final ManejadorErrores errores;
    private int pos;

    public BDParser(List<Token> tokens, TablaSimbolos tabla, ManejadorErrores errores) {
        this.tokens = tokens;
        this.tabla = tabla;
        this.errores = errores;
        this.pos = 0;
    }

    public RaizBDAST parsear() {
        RaizBDAST raiz = new RaizBDAST();

        while (pos < tokens.size() && tokens.get(pos).getTipo() != Token.Tipo.EOF) {
            Token tokenActual = tokens.get(pos);

            if (tokenActual.getTipo() == Token.Tipo.IDENTIFICADOR) {
                String lexema = tokenActual.getLexema();

                if (lexema.equals("tabla") || lexema.equals("vista")) {
                    procesarBloqueComplejo(raiz, lexema);
                } else if (lexema.equals("procedimiento") || lexema.equals("indice") || lexema.equals("disparador")) {
                    procesarComponenteLineal(raiz, lexema);
                } else {
                    procesarRelacion(raiz);
                }
            } else {
                errores.reportarError(tokenActual.getLinea(), "Sintáctico BD", "Token inesperado '" + tokenActual.getLexema() + "'.", "Inicia la línea con un componente (tabla, vista, etc.) o una relación.");
                pos++;
            }
        }
        return raiz;
    }

    private void procesarBloqueComplejo(RaizBDAST raiz, String rol) {
        int lineaOriginal = tokens.get(pos).getLinea();
        pos++; // Consumir 'tabla' o 'vista'

        if (validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "Falta el nombre de la " + rol + ".")) {
            String nombreBloque = tokens.get(pos).getLexema();
            pos++;

            if (validarSiguienteTipo(Token.Tipo.LLAVE_IZQ, "Falta abrir '{' para la definición.")) {
                pos++;

                if (!tabla.registrar(nombreBloque, rol)) {
                    errores.reportarError(lineaOriginal, "Semántico BD", "El elemento '" + nombreBloque + "' ya existe.", "Elige otro identificador.");
                }

                List<Atributo> atributos = new ArrayList<>();
                while (pos < tokens.size() && tokens.get(pos).getTipo() != Token.Tipo.LLAVE_DER && tokens.get(pos).getTipo() != Token.Tipo.EOF) {
                    Atributo attr = leerAtributo();
                    if (attr != null) atributos.add(attr);
                    else recuperarPanicoAtributo();
                }

                if (validarSiguienteTipo(Token.Tipo.LLAVE_DER, "Falta cerrar llaves '}'.")) {
                    pos++;
                    raiz.agregarElemento(new NodoTabla(nombreBloque + " (" + rol.toUpperCase() + ")", atributos));
                }
            }
        }
    }

    private Atributo leerAtributo() {
        if (!validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "Falta el nombre del atributo.")) return null;
        String nombre = tokens.get(pos).getLexema();
        pos++;

        if (!validarSiguienteTipo(Token.Tipo.DOS_PUNTOS, "Falta ':'.")) return null;
        pos++;

        if (!validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "Falta el tipo de dato.")) return null;
        String tipoDato = tokens.get(pos).getLexema();
        pos++;

        String modificador = "";
        if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.IDENTIFICADOR) {
            modificador = tokens.get(pos).getLexema();
            pos++;
        }

        if (!validarSiguienteTipo(Token.Tipo.PUNTO_Y_COMA, "Falta ';' al final del atributo.")) return null;
        pos++;

        return new Atributo(nombre, tipoDato, modificador);
    }

    private void procesarComponenteLineal(RaizBDAST raiz, String rol) {
        int lineaOriginal = tokens.get(pos).getLinea();
        pos++; // Consumir palabra clave

        if (validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "Falta el identificador para el " + rol + ".")) {
            String nombre = tokens.get(pos).getLexema();
            pos++;

            if (validarSiguienteTipo(Token.Tipo.PUNTO_Y_COMA, "Falta ';' al final.")) {
                pos++;
                if (!tabla.registrar(nombre, rol)) {
                    errores.reportarError(lineaOriginal, "Semántico BD", "El identificador '" + nombre + "' ya está ocupado.", "Usa otro nombre.");
                }
                raiz.agregarElemento(new NodoTabla(nombre + " [" + rol.toUpperCase() + "]", new ArrayList<>()));
            }
        }
    }

    private void procesarRelacion(RaizBDAST raiz) {
        Token tOrigen = tokens.get(pos);
        String idOrigen = tOrigen.getLexema();
        pos++;

        if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.IDENTIFICADOR && tokens.get(pos).getLexema().equals("relaciona")) {
            pos++;

            if (validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "Falta el destino de la relación.")) {
                String idDestino = tokens.get(pos).getLexema();
                pos++;

                if (validarSiguienteTipo(Token.Tipo.PUNTO_Y_COMA, "Falta ';'.")) {
                    pos++;

                    if (!tabla.existe(idOrigen)) {
                        errores.reportarError(tOrigen.getLinea(), "Semántico BD", "La entidad origen '" + idOrigen + "' no existe.", "Declárala primero.");
                    } else if (!tabla.existe(idDestino)) {
                        errores.reportarError(tOrigen.getLinea(), "Semántico BD", "La entidad destino '" + idDestino + "' no existe.", "Declárala primero.");
                    } else {
                        raiz.agregarElemento(new NodoRelacion(idOrigen, idDestino));
                    }
                }
            }
        } else {
            errores.reportarError(tOrigen.getLinea(), "Sintáctico BD", "Verbo inválido en '" + idOrigen + "'.", "Usa el verbo 'relaciona' para bases de datos.");
            recuperarPanicoTabla();
        }
    }

    private boolean validarSiguienteTipo(Token.Tipo esperado, String mensajeError) {
        if (pos >= tokens.size() || tokens.get(pos).getTipo() != esperado) {
            int l = (pos < tokens.size()) ? tokens.get(pos).getLinea() : tokens.get(pos - 1).getLinea();
            errores.reportarError(l, "Sintáctico BD", mensajeError, "Verifica las reglas de sintaxis.");
            return false;
        }
        return true;
    }

    private void recuperarPanicoAtributo() {
        while (pos < tokens.size() && tokens.get(pos).getTipo() != Token.Tipo.PUNTO_Y_COMA && tokens.get(pos).getTipo() != Token.Tipo.LLAVE_DER && tokens.get(pos).getTipo() != Token.Tipo.EOF) pos++;
        if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.PUNTO_Y_COMA) pos++;
    }

    private void recuperarPanicoTabla() {
        while (pos < tokens.size() && tokens.get(pos).getTipo() != Token.Tipo.PUNTO_Y_COMA && tokens.get(pos).getTipo() != Token.Tipo.EOF) pos++;
        if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.PUNTO_Y_COMA) pos++;
    }
}