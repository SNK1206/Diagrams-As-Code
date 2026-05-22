package com.diagramas;

import com.diagramas.core.*;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

public class MainFX extends Application {

    private TabPane panelPestanas;
    private TextArea txtConsola;

    // Nuevas áreas para visualizar el interior del compilador
    private TextArea txtTokens;
    private TextArea txtSimbolos;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("⚡ IDE Pedagógico: Análisis Interno (Tokens y Memoria)");

        // --- 1. BARRA DE HERRAMIENTAS (TOP) ---
        HBox toolbar = new HBox(15);
        toolbar.setPadding(new Insets(10));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: #2c3e50;");

        Label lblTitulo = new Label("Diagrams As Code");
        lblTitulo.setTextFill(Color.WHITE);
        lblTitulo.setFont(Font.font("System", FontWeight.BOLD, 16));

        Button btnAbrir = new Button("📂 Abrir .dac");
        btnAbrir.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
        btnAbrir.setOnAction(e -> abrirArchivo(primaryStage));

        Button btnCompilar = new Button("⚡ Compilar e Inspeccionar");
        btnCompilar.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        btnCompilar.setOnAction(e -> ejecutarCompilador());

        toolbar.getChildren().addAll(lblTitulo, btnAbrir, btnCompilar);

        // --- 2. EDITOR DE CÓDIGO CON PESTAÑAS (IZQUIERDA) ---
        VBox panelEditor = new VBox(5);
        panelEditor.setPadding(new Insets(10));
        Label lblEditor = new Label("Archivos Abiertos:");
        lblEditor.setStyle("-fx-font-weight: bold;");

        panelPestanas = new TabPane();
        VBox.setVgrow(panelPestanas, Priority.ALWAYS);

        String codigoEjemplo = "diagrama Flujo;\ninicio Comenzar;\nnodo LeerDatos \"Leer credenciales\";\nnodo Validar \"Verificar base de datos\";\nComenzar conecta LeerDatos;\nLeerDatos conecta Validar;";
        crearPestana("ejemplo.dac", codigoEjemplo);

        panelEditor.getChildren().addAll(lblEditor, panelPestanas);

        // --- 3. PANEL DE ANÁLISIS INTERNO (DERECHA) ---
        VBox panelAnalisis = new VBox(10);
        panelAnalisis.setPadding(new Insets(10));

        // 3.1 Sub-panel para Tokens
        Label lblTokens = new Label("1. Flujo Léxico (Tokens Extracted):");
        lblTokens.setStyle("-fx-font-weight: bold; -fx-text-fill: #8e44ad;");
        txtTokens = new TextArea();
        txtTokens.setEditable(false);
        txtTokens.setFont(Font.font("Monospaced", 13));
        VBox.setVgrow(txtTokens, Priority.ALWAYS);

        // 3.2 Sub-panel para Tabla de Símbolos
        Label lblSimbolos = new Label("2. Tabla de Símbolos (Memoria del Semántico):");
        lblSimbolos.setStyle("-fx-font-weight: bold; -fx-text-fill: #d35400;");
        txtSimbolos = new TextArea();
        txtSimbolos.setEditable(false);
        txtSimbolos.setFont(Font.font("Monospaced", 13));
        VBox.setVgrow(txtSimbolos, Priority.ALWAYS);

        panelAnalisis.getChildren().addAll(lblTokens, txtTokens, lblSimbolos, txtSimbolos);

        SplitPane splitCentro = new SplitPane(panelEditor, panelAnalisis);
        splitCentro.setDividerPositions(0.45f);

        // --- 4. CONSOLA PEDAGÓGICA (ABAJO) ---
        VBox panelConsola = new VBox(5);
        panelConsola.setPadding(new Insets(10));
        Label lblConsola = new Label("Consola de Diagnóstico Humano:");
        lblConsola.setStyle("-fx-font-weight: bold;");
        txtConsola = new TextArea();
        txtConsola.setEditable(false);
        txtConsola.setFont(Font.font("Monospaced", 12));
        txtConsola.setPrefHeight(120);
        panelConsola.getChildren().addAll(lblConsola, txtConsola);

        // --- LAYOUT PRINCIPAL ---
        BorderPane root = new BorderPane();
        root.setTop(toolbar);
        root.setCenter(splitCentro);
        root.setBottom(panelConsola);

        Scene scene = new Scene(root, 1100, 700);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void abrirArchivo(Stage stage) {
        FileChooser explorador = new FileChooser();
        explorador.setTitle("Abrir archivo Diagrams As Code");
        explorador.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos DAC (*.dac)", "*.dac"));

        File archivoSeleccionado = explorador.showOpenDialog(stage);

        if (archivoSeleccionado != null) {
            try {
                String contenido = new String(Files.readAllBytes(archivoSeleccionado.toPath()), StandardCharsets.UTF_8);
                crearPestana(archivoSeleccionado.getName(), contenido);
                txtConsola.setText("✅ Archivo '" + archivoSeleccionado.getName() + "' cargado correctamente en memoria.");
            } catch (IOException ex) {
                txtConsola.setText("❌ ERROR DE E/S: No se pudo leer el archivo.\nDetalle: " + ex.getMessage());
            }
        }
    }

    private void crearPestana(String titulo, String contenido) {
        Tab nuevaPestana = new Tab(titulo);
        TextArea areaTexto = new TextArea(contenido);
        areaTexto.setFont(Font.font("Monospaced", 14));

        nuevaPestana.setContent(areaTexto);
        panelPestanas.getTabs().add(nuevaPestana);
        panelPestanas.getSelectionModel().select(nuevaPestana);
    }

    private void ejecutarCompilador() {
        // Limpiar análisis previos
        txtTokens.clear();
        txtSimbolos.clear();
        txtConsola.clear();

        Tab pestañaActiva = panelPestanas.getSelectionModel().getSelectedItem();

        if (pestañaActiva == null) {
            txtConsola.setText("⚠️ ERROR: No hay ningún archivo abierto para compilar.");
            return;
        }

        TextArea txtEditorActual = (TextArea) pestañaActiva.getContent();
        String codigoFuente = txtEditorActual.getText();

        ManejadorErrores manejadorErrores = new ManejadorErrores();
        TablaSimbolos tablaSimbolos = new TablaSimbolos();

        // 1. FASE LÉXICA
        LexerBase lexer = new LexerBase(codigoFuente, manejadorErrores);
        List<Token> tokens = lexer.tokenizar();

        // Imprimir los Tokens en la pantalla
        StringBuilder sbTokens = new StringBuilder();
        for (Token t : tokens) {
            sbTokens.append(t.toString()).append("\n");
        }
        txtTokens.setText(sbTokens.toString());

        if (manejadorErrores.tieneErrores()) {
            txtConsola.setText("🛑 PROCESO DETENIDO POR ERRORES LÉXICOS:\n\n" + obtenerErroresString(manejadorErrores));
            return;
        }

        // 2. FASE SINTÁCTICA Y SEMÁNTICA
        ParserBase parserBase = new ParserBase(tokens, tablaSimbolos, manejadorErrores);
        parserBase.analizarCabecera();

        // Imprimir la Tabla de Símbolos en la pantalla
        StringBuilder sbSimbolos = new StringBuilder();
        sbSimbolos.append("🔒 Contexto Bloqueado: ").append(tablaSimbolos.getContextoActivo()).append("\n\n");
        sbSimbolos.append(String.format("%-25s | %-15s\n", "IDENTIFICADOR ENCONTRADO", "TIPO/ROL ASIGNADO"));
        sbSimbolos.append("----------------------------------------------------\n");

        for (Map.Entry<String, String> entry : tablaSimbolos.getElementos().entrySet()) {
            sbSimbolos.append(String.format("%-25s | %-15s\n", entry.getKey(), entry.getValue()));
        }
        txtSimbolos.setText(sbSimbolos.toString());

        // 3. RESULTADO FINAL
        if (manejadorErrores.tieneErrores()) {
            txtConsola.setText("❌ COMPILACIÓN FALLIDA:\n\n" + obtenerErroresString(manejadorErrores));
        } else {
            txtConsola.setText("✅ COMPILACIÓN EXITOSA de [" + pestañaActiva.getText() + "]\nLas fases Léxica y Sintáctica finalizaron sin anomalías.");
        }
    }

    private String obtenerErroresString(ManejadorErrores manejador) {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.PrintStream ps = new java.io.PrintStream(baos);
        java.io.PrintStream viejoErr = System.err;
        System.setErr(ps);
        manejador.imprimirErrores();
        System.setErr(viejoErr);
        return baos.toString();
    }
}