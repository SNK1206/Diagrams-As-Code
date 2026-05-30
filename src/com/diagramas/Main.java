package com.diagramas;

import com.diagramas.core.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("====== COMPILADOR DE CONSOLA: DIAGRAMS AS CODE ======\n");

        // 1. Validar que el usuario pase el archivo por la terminal
        if (args.length == 0) {
            System.out.println("❌ ERROR: No se proporcionó ningún archivo de entrada.");
            System.out.println("💡 Uso correcto desde la terminal:");
            System.out.println("   java com.diagramas.Main <ruta_del_archivo>.dac\n");
            return;
        }

        String rutaArchivo = args[0];

        // 2. Validar extensión estricta '.dac'
        if (!rutaArchivo.endsWith(".dac")) {
            System.err.println("❌ ERROR ARCHITECTURAL: Extensión inválida. El archivo debe terminar en '.dac'");
            return;
        }

        try {
            // 3. Leer el archivo plano utilizando UTF-8 de forma nativa
            String codigoFuente = new String(Files.readAllBytes(Paths.get(rutaArchivo)), StandardCharsets.UTF_8);

            System.out.println("📖 Leyendo archivo fuente: " + rutaArchivo);
            System.out.println("--------------------------------------------------");

            // Inicializar la infraestructura estática y fija del núcleo
            ManejadorErrores manejadorErrores = new ManejadorErrores();
            TablaSimbolos tablaSimbolos = new TablaSimbolos();

            // FASE 1: Análisis Léxico (Extracción de componentes)
            LexerBase lexer = new LexerBase(codigoFuente, manejadorErrores);
            List<Token> tokens = lexer.tokenizar();

            System.out.println("[Lexer] Análisis de caracteres completado. Tokens:");
            for (Token t : tokens) {
                System.out.println("  " + t);
            }

            // Si el Lexer detectó errores, detenemos el pipeline de inmediato
            if (manejadorErrores.tieneErrores()) {
                System.out.println("\n🛑 [Resultado] Proceso detenido por errores léxicos:");
                manejadorErrores.imprimirErrores();
                return;
            }

            // FASE 2: Análisis Sintáctico de Cabecera (Aislamiento de contexto)
            System.out.println("\n[ParserBase] Evaluando Regla de Cabecera...");
            ParserBase parserBase = new ParserBase(tokens, tablaSimbolos, manejadorErrores);
            parserBase.analizarCabecera();

            // Diagnóstico Final por Consola
            System.out.println("--------------------------------------------------");
            if (manejadorErrores.tieneErrores()) {
                System.out.println("❌ COMPILACIÓN FALLIDA");
                manejadorErrores.imprimirErrores();
            } else {
                System.out.println("✅ COMPILACIÓN EXITOSA: Estructura base validada sin anomalías.");
            }

        } catch (IOException e) {
            System.err.println("❌ ERROR DE E/S: No se pudo encontrar o leer el archivo especificado.");
            System.err.println("   Verifica la ruta: " + rutaArchivo);
        }
    }
}