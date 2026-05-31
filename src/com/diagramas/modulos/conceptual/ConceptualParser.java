package com.diagramas.modulos.conceptual;

import com.diagramas.core.ManejadorErrores;
import com.diagramas.core.TablaSimbolos;
import com.diagramas.core.Token;
import com.diagramas.modulos.conceptual.ast.*;
import java.util.List;

public class ConceptualParser {
    private final List<Token> tokens;
    private final TablaSimbolos tabla;
    private final ManejadorErrores errores;
    private int pos;

    public ConceptualParser(List<Token> tokens, TablaSimbolos tabla, ManejadorErrores errores) {
        this.tokens = tokens;
        this.tabla = tabla;
        this.errores = errores;
        this.pos = 0;
    }

    public RaizConceptualAST parsear() {
        RaizConceptualAST raiz = new RaizConceptualAST();

        while (pos < tokens.size() && tokens.get(pos).getTipo() != Token.Tipo.EOF) {
            Token tokenActual = tokens.get(pos);

            if (tokenActual.getTipo() == Token.Tipo.IDENTIFICADOR) {
                String lexema = tokenActual.getLexema();

                if (lexema.equals("concepto") || lexema.equals("categoria") || lexema.equals("propiedad")) {
                    procesarNodoConceptual(raiz, lexema);
                } else {
                    procesarRelacion(raiz);
                }
            } else {
                errores.reportarError("ES35", tokenActual.getLinea(), "Sintáctico Conceptual",
                    "Token inesperado '" + tokenActual.getLexema() + "'.",
                    "Inicia con 'concepto', 'categoria' o 'propiedad', o define una relación (agrupa, asocia, depende).");
                pos++;
            }
        }
        return raiz;
    }

    private void procesarNodoConceptual(RaizConceptualAST raiz, String rol) {
        pos++;

        if (!validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "ES36", "Falta el nombre del " + rol + ".")) {
            recuperarPanico(); return;
        }
        String nombre = tokens.get(pos).getLexema();
        pos++;

        if (!validarSiguienteTipo(Token.Tipo.TEXTO_LITERAL, "ES37", "Falta la descripción entre comillas para '" + nombre + "'.")) {
            recuperarPanico(); return;
        }
        String descripcion = tokens.get(pos).getLexema();
        pos++;

        if (!validarSiguienteTipo(Token.Tipo.PUNTO_Y_COMA, "ES38", "Falta ';' al final de la declaración de '" + nombre + "'.")) {
            recuperarPanico(); return;
        }
        pos++;

        tabla.registrar(nombre, rol);
        raiz.agregarElemento(new NodoConcepto(nombre, rol, descripcion));
    }

    private void procesarRelacion(RaizConceptualAST raiz) {
        Token tOrigen = tokens.get(pos);
        String idOrigen = tOrigen.getLexema();
        pos++;

        if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.IDENTIFICADOR) {
            String verbo = tokens.get(pos).getLexema();
            if (verbo.equals("agrupa") || verbo.equals("asocia") || verbo.equals("depende")) {
                pos++;

                if (!validarSiguienteTipo(Token.Tipo.IDENTIFICADOR, "ES39", "Falta el identificador destino en la instrucción '" + verbo + "'.")) {
                    recuperarPanico(); return;
                }
                String idDestino = tokens.get(pos).getLexema();
                pos++;

                if (!validarSiguienteTipo(Token.Tipo.PUNTO_Y_COMA, "ES40", "Falta ';' al finalizar la instrucción '" + verbo + "'.")) {
                    recuperarPanico(); return;
                }
                pos++;
                raiz.agregarElemento(new NodoRelacionConceptual(idOrigen, verbo, idDestino));
            } else {
                errores.reportarError("ES42", tOrigen.getLinea(), "Sintáctico Conceptual",
                    "Verbo inválido '" + verbo + "' en '" + idOrigen + "'.",
                    "Usa 'agrupa', 'asocia' o 'depende' para definir relaciones conceptuales.");
                recuperarPanico();
            }
        } else {
            errores.reportarError("ES41", tOrigen.getLinea(), "Sintáctico Conceptual",
                "Se esperaba un verbo de relación después de '" + idOrigen + "'.",
                "Usa 'agrupa', 'asocia' o 'depende' para definir relaciones conceptuales.");
            recuperarPanico();
        }
    }

    private boolean validarSiguienteTipo(Token.Tipo esperado, String codigo, String mensajeError) {
        if (pos >= tokens.size() || tokens.get(pos).getTipo() != esperado) {
            int l = (pos < tokens.size()) ? tokens.get(pos).getLinea() : (pos > 0 ? tokens.get(pos - 1).getLinea() : 1);
            errores.reportarError(codigo, l, "Sintáctico Conceptual", mensajeError, "Revisa la guía del módulo Conceptual.");
            return false;
        }
        return true;
    }

    private void recuperarPanico() {
        while (pos < tokens.size() && tokens.get(pos).getTipo() != Token.Tipo.PUNTO_Y_COMA && tokens.get(pos).getTipo() != Token.Tipo.EOF) pos++;
        if (pos < tokens.size() && tokens.get(pos).getTipo() == Token.Tipo.PUNTO_Y_COMA) pos++;
    }
}
