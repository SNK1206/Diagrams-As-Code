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

    private TextArea txtTokens;
    private TextArea txtSimbolos;

    // Contador para nombrar los archivos nuevos por defecto
    private int contadorNuevos = 1;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("⚡ IDE Pedagógico: Análisis Interno e Integración de Archivos");

        // --- 1. BARRA DE HERRAMIENTAS (TOP) ---
        HBox toolbar = new HBox(15);
        toolbar.setPadding(new Insets(10));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: #2c3e50;");

        Label lblTitulo = new Label("Diagrams As Code");
        lblTitulo.setTextFill(Color.WHITE);
        lblTitulo.setFont(Font.font("System", FontWeight.BOLD, 16));

        // NUEVO: Botón para crear un lienzo/archivo en blanco
        Button btnNuevo = new Button("➕ Nuevo");
        btnNuevo.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold;");
        btnNuevo.setOnAction(e -> crearPestana("sin_titulo_" + (contadorNuevos++) + ".dac", "diagrama Flujo;\n", null));

        Button btnAbrir = new Button("📂 Abrir .dac");
        btnAbrir.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
        btnAbrir.setOnAction(e -> abrirArchivo(primaryStage));

        // NUEVO: Botón para persistir y guardar los cambios en disco
        Button btnGuardar = new Button("💾 Guardar");
        btnGuardar.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-font-weight: bold;");
        btnGuardar.setOnAction(e -> guardarArchivoActual(primaryStage));

        Button btnCompilar = new Button("⚡ Compilar e Inspeccionar");
        btnCompilar.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        btnCompilar.setOnAction(e -> ejecutarCompilador());

        toolbar.getChildren().addAll(lblTitulo, btnNuevo, btnAbrir, btnGuardar, btnCompilar);

        // --- 2. EDITOR DE CÓDIGO CON PESTAÑAS (IZQUIERDA) ---
        VBox panelEditor = new VBox(5);
        panelEditor.setPadding(new Insets(10));
        Label lblEditor = new Label("Archivos Abiertos:");
        lblEditor.setStyle("-fx-font-weight: bold;");

        panelPestanas = new TabPane();
        VBox.setVgrow(panelPestanas, Priority.ALWAYS);

        panelEditor.getChildren().addAll(lblEditor, panelPestanas);

        // --- 3. PANEL DE ANÁLISIS INTERNO (DERECHA) ---
        VBox panelAnalisis = new VBox(10);
        panelAnalisis.setPadding(new Insets(10));

        Label lblTokens = new Label("1. Flujo Léxico (Tokens Extracted):");
        lblTokens.setStyle("-fx-font-weight: bold; -fx-text-fill: #8e44ad;");
        txtTokens = new TextArea();
        txtTokens.setEditable(false);
        txtTokens.setFont(Font.font("Monospaced", 13));
        VBox.setVgrow(txtTokens, Priority.ALWAYS);

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
                crearPestana(archivoSeleccionado.getName(), contenido, archivoSeleccionado);
                txtConsola.setText("✅ Archivo '" + archivoSeleccionado.getName() + "' cargado correctamente.");
            } catch (IOException ex) {
                txtConsola.setText("❌ ERROR DE E/S: No se pudo leer el archivo.\nDetalle: " + ex.getMessage());
            }
        }
    }

    // --- NUEVO MÉTODO CRÍTICO DE GUARDADO (PERSISTENCIA) ---
    private void guardarArchivoActual(Stage stage) {
        Tab pestañaActiva = panelPestanas.getSelectionModel().getSelectedItem();
        if (pestañaActiva == null) {
            txtConsola.setText("⚠️ ERROR: No hay ninguna pestaña abierta para guardar.");
            return;
        }

        TextArea txtEditorActual = (TextArea) pestañaActiva.getContent();
        String contenido = txtEditorActual.getText();

        // Recuperar la vinculación física con el archivo almacenado en el userData de la Tab
        File archivoAsociado = (File) pestañaActiva.getUserData();

        // Escenario A: Es un archivo en blanco creado con el botón "Nuevo" (Pide localización)
        if (archivoAsociado == null) {
            FileChooser exploradorGuardado = new FileChooser();
            exploradorGuardado.setTitle("Guardar nuevo archivo Diagrams As Code");
            exploradorGuardado.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos DAC (*.dac)", "*.dac"));
            exploradorGuardado.setInitialFileName(pestañaActiva.getText());

            File nuevoArchivo = exploradorGuardado.showSaveDialog(stage);
            if (nuevoArchivo != null) {
                archivoAsociado = nuevoArchivo;
                pestañaActiva.setUserData(archivoAsociado); // Vincular permanentemente el archivo a la pestaña
                pestañaActiva.setText(archivoAsociado.getName()); // Renombrar título de pestaña
            } else {
                txtConsola.setText("⚠️ Operación de guardado cancelada por el usuario.");
                return;
            }
        }

        // Escenario B: El archivo ya tiene un archivo asociado en disco (Sobrescribe directamente)
        try {
            Files.writeString(archivoAsociado.toPath(), contenido, StandardCharsets.UTF_8);
            txtConsola.setText("💾 Archivo '" + archivoAsociado.getName() + "' guardado con éxito en el almacenamiento local.");
        } catch (IOException ex) {
            txtConsola.setText("❌ ERROR CRÍTICO AL GUARDAR: No se pudo escribir sobre el archivo.\nDetalle: " + ex.getMessage());
        }
    }

    private void crearPestana(String titulo, String contenido, File archivo) {
        Tab nuevaPestana = new Tab(titulo);
        // Almacenar el puntero del archivo físico en la propiedad de la pestaña
        nuevaPestana.setUserData(archivo);

        TextArea areaTexto = new TextArea(contenido);
        areaTexto.setFont(Font.font("Monospaced", 14));

        nuevaPestana.setContent(areaTexto);
        panelPestanas.getTabs().add(nuevaPestana);
        panelPestanas.getSelectionModel().select(nuevaPestana);
    }

    private void ejecutarCompilador() {
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

        StringBuilder sbSimbolos = new StringBuilder();
        sbSimbolos.append("🔒 Contexto Bloqueado: ").append(tablaSimbolos.getContextoActivo()).append("\n\n");
        sbSimbolos.append(String.format("%-25s | %-15s\n", "IDENTIFICADOR ENCONTRADO", "TIPO/ROL ASIGNADO"));
        sbSimbolos.append("----------------------------------------------------\n");

        for (Map.Entry<String, String> entry : tablaSimbolos.getSimbolos().entrySet()) {
            sbSimbolos.append(String.format("%-25s | %-15s\n", entry.getKey(), entry.getValue()));
        }
        txtSimbolos.setText(sbSimbolos.toString());

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