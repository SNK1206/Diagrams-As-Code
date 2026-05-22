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

                if (lexema.equals("tabla")) {
                    procesarTabla(raiz);
                } else {
                    // Si no es "tabla", verificamos si intenta establecer una relación
                    procesarRelacion(raiz);
                }
            } else {
                errores.reportarError(
                        tokenActual.getLinea(),
                        "Sintáctico BD",
                        "Token inesperado '" + tokenActual.getLexema() + "'.",
                        "Verifica que estés iniciando la línea declarando una 'tabla' o una relación."
                );
                pos++;
            }
        }
        return raiz;
    }

    private void procesarTabla(RaizBDAST raiz) {
        int lineaOriginal = tokens.get(pos).getLinea();
        pos++; // Consumir 'tabla'

        if (validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "Falta el nombre de la tabla.")) {
            String nombreTabla = tokens.get(pos).getLexema();
            pos++;

            if (validarSiguienteTipo(Token.Tipo.LLAVE_IZQ, "Falta abrir llaves '{' para definir los atributos.")) {
                pos++; // Consumir '{'

                // CONTROL SEMÁNTICO: Validar unicidad de la tabla
                if (!tabla.registrar(nombreTabla, "tabla")) {
                    errores.reportarError(lineaOriginal, "Semántico BD", "La tabla '" + nombreTabla + "' ya fue declarada.", "Renombra la tabla para evitar conflictos.");
                }

                List<Atributo> atributos = new ArrayList<>();

                // Bucle interno para leer atributos hasta encontrar '}' o fin de archivo
                while (pos < tokens.size() && tokens.get(pos).getTipo() != Token.Tipo.LLAVE_DER && tokens.get(pos).getTipo() != Token.Tipo.EOF) {
                    Atributo attr = leerAtributo();
                    if (attr != null) {
                        atributos.add(attr);
                    } else {
                        recuperacionPanicoAtributo();
                    }
                }

                if (validarSiguienteTipo(Token.Tipo.LLAVE_DER, "Falta cerrar llaves '}' al final de la tabla.")) {
                    pos++; // Consumir '}'
                    raiz.agregarElemento(new NodoTabla(nombreTabla, atributos));
                }
            }
        }
    }

    private Atributo leerAtributo() {
        if (!validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "Falta el nombre del atributo (ej. 'id').")) return null;
        String nombre = tokens.get(pos).getLexema();
        pos++;

        if (!validarSiguienteTipo(Token.Tipo.DOS_PUNTOS, "Se esperaba ':' después del nombre del atributo.")) return null;
        pos++;

        if (!validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "Falta el tipo de dato (ej. INT, VARCHAR).")) return null;
        String tipoDato = tokens.get(pos).getLexema();
        pos++;

        // Manejo de modificadores opcionales (ej. PK, FK)
        String modificador = "";
        if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.IDENTIFICADOR) {
            modificador = tokens.get(pos).getLexema();
            pos++;
        }

        if (!validarSiguienteTipo(Token.Tipo.PUNTO_Y_COMA, "Falta ';' al final del atributo.")) return null;
        pos++;

        return new Atributo(nombre, tipoDato, modificador);
    }

    private void procesarRelacion(RaizBDAST raiz) {
        Token tOrigen = tokens.get(pos);
        String idOrigen = tOrigen.getLexema();
        pos++; // Consumir Origen

        if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.IDENTIFICADOR && tokens.get(pos).getLexema().equals("relaciona")) {
            pos++; // Consumir 'relaciona'

            if (validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "Falta la tabla destino para la relación.")) {
                String idDestino = tokens.get(pos).getLexema();
                pos++;

                if (validarSiguienteTipo(Token.Tipo.PUNTO_Y_COMA, "Falta ';' al terminar la instrucción.")) {
                    pos++;

                    // CONTROL SEMÁNTICO: Ambas tablas deben existir en memoria
                    if (!tabla.existe(idOrigen)) {
                        errores.reportarError(tOrigen.getLinea(), "Semántico BD", "La tabla origen '" + idOrigen + "' no existe.", "Declara la tabla antes de usarla.");
                    } else if (!tabla.existe(idDestino)) {
                        errores.reportarError(tOrigen.getLinea(), "Semántico BD", "La tabla destino '" + idDestino + "' no existe.", "Declara la tabla destino antes de enlazarla.");
                    } else {
                        raiz.agregarElemento(new NodoRelacion(idOrigen, idDestino));
                    }
                }
            }
        } else {
            errores.reportarError(
                    tOrigen.getLinea(),
                    "Sintáctico BD",
                    "Instrucción desconocida o verbo inválido en '" + idOrigen + "'.",
                    "Para unir tablas usa el verbo exclusivo 'relaciona' (Ej: TablaA relaciona TablaB;)"
            );
            recuperacionPanicoTabla();
        }
    }

    private boolean validarSiguienteTipo(Token.Tipo esperado, String mensajeError) {
        if (pos >= tokens.size() || tokens.get(pos).getTipo() != esperado) {
            int l = (pos < tokens.size()) ? tokens.get(pos).getLinea() : tokens.get(pos - 1).getLinea();
            errores.reportarError(l, "Sintáctico BD", mensajeError, "Revisa la documentación del lenguaje BD.");
            return false;
        }
        return true;
    }

    private void recuperacionPanicoAtributo() {
        while (pos < tokens.size() && tokens.get(pos).getTipo() != Token.Tipo.PUNTO_Y_COMA && tokens.get(pos).getTipo() != Token.Tipo.LLAVE_DER && tokens.get(pos).getTipo() != Token.Tipo.EOF) {
            pos++;
        }
        if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.PUNTO_Y_COMA) pos++;
    }

    private void recuperacionPanicoTabla() {
        while (pos < tokens.size() && tokens.get(pos).getTipo() != Token.Tipo.PUNTO_Y_COMA && tokens.get(pos).getTipo() != Token.Tipo.EOF) {
            pos++;
        }
        if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.PUNTO_Y_COMA) pos++;
    }
}
