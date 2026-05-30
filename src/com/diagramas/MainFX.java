package com.diagramas;

import com.diagramas.core.*;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
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
        primaryStage.setTitle("Diagrams As Code");

        // --- 1. BARRA DE HERRAMIENTAS (TOP) ---
        HBox toolbar = new HBox(15);
        toolbar.setPadding(new Insets(10));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: #2c3e50;");

        Label lblTitulo = new Label("Diagrams As Code");
        lblTitulo.setTextFill(Color.WHITE);
        lblTitulo.setFont(Font.font("System", FontWeight.BOLD, 16));

        // NUEVO: Botón para crear un lienzo/archivo en blanco
        Button btnNuevo = new Button("Nuevo");
        btnNuevo.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold;");
        btnNuevo.setOnAction(e -> crearPestana("sin_titulo_" + (contadorNuevos++) + ".dac", "diagrama Flujo;\n", null));

        Button btnAbrir = new Button("Abrir");
        btnAbrir.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
        btnAbrir.setOnAction(e -> abrirArchivo(primaryStage));

        // NUEVO: Botón para persistir y guardar los cambios en disco
        Button btnGuardar = new Button("Guardar");
        btnGuardar.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-font-weight: bold;");
        btnGuardar.setOnAction(e -> guardarArchivoActual(primaryStage));

        Button btnCompilar = new Button("Compilar e Inspeccionar");
        btnCompilar.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        btnCompilar.setOnAction(e -> ejecutarCompilador());

        Button btnSimbologia = new Button("Simbologia del Lenguaje");
        btnSimbologia.setStyle("-fx-background-color: #16a085; -fx-text-fill: white; -fx-font-weight: bold;");
        btnSimbologia.setOnAction(e -> mostrarTablaSimbologia(primaryStage));

        Button btnManual = new Button("Manual de Usuario");
        btnManual.setStyle("-fx-background-color: #1a5276; -fx-text-fill: white; -fx-font-weight: bold;");
        btnManual.setOnAction(e -> mostrarManual(primaryStage, "manual_diagrams_as_code.md", "Manual de Usuario — Diagrams As Code v2.0"));

        Button btnDocs = new Button("Documentacion Tecnica");
        btnDocs.setStyle("-fx-background-color: #6c3483; -fx-text-fill: white; -fx-font-weight: bold;");
        btnDocs.setOnAction(e -> mostrarManual(primaryStage, "documentacion_tecnica.md", "Documentacion Tecnica — Diagrams As Code"));

        // Espaciador que empuja los botones de referencia al extremo derecho
        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);

        toolbar.getChildren().addAll(lblTitulo, btnNuevo, btnAbrir, btnGuardar, btnCompilar, espaciador, btnSimbologia, btnManual, btnDocs);

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

        Label lblTokens = new Label("1. Tokens:");
        lblTokens.setStyle("-fx-font-weight: bold; -fx-text-fill: #8e44ad;");
        txtTokens = new TextArea();
        txtTokens.setEditable(false);
        txtTokens.setFont(Font.font("Monospaced", 13));
        VBox.setVgrow(txtTokens, Priority.ALWAYS);

        Label lblSimbolos = new Label("2. Tabla de Símbolos:");
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
        Label lblConsola = new Label("Consola:");
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
                txtConsola.setText("Archivo '" + archivoSeleccionado.getName() + "' cargado correctamente.");
            } catch (IOException ex) {
                txtConsola.setText("ERROR DE E/S: No se pudo leer el archivo.\nDetalle: " + ex.getMessage());
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
            txtConsola.setText("COMPILACIÓN FALLIDA:\n\n" + obtenerErroresString(manejadorErrores));
        } else {
            txtConsola.setText("COMPILACIÓN EXITOSA de [" + pestañaActiva.getText() + "]");
        }
    }

    private void mostrarTablaSimbologia(Stage owner) {
        Stage ventana = new Stage();
        ventana.initOwner(owner);
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.setTitle("Tabla de Simbologia — Diagrams As Code");
        ventana.setMinWidth(860);
        ventana.setMinHeight(520);

        // --- ENCABEZADO ---
        Label lblTitulo = new Label("Simbologia del Lenguaje .DAC");
        lblTitulo.setFont(Font.font("System", FontWeight.BOLD, 15));
        lblTitulo.setTextFill(Color.WHITE);

        // --- FILTRO ---
        Label lblFiltro = new Label("Filtrar por modulo:");
        lblFiltro.setTextFill(Color.WHITE);
        lblFiltro.setFont(Font.font("System", FontWeight.BOLD, 13));

        ComboBox<String> cmbFiltro = new ComboBox<>();
        cmbFiltro.getItems().addAll("Todos", "Flujo", "BD", "Redes", "Global", "Puntuacion");
        cmbFiltro.setValue("Todos");
        cmbFiltro.setStyle("-fx-font-size: 13px;");

        Label lblConteo = new Label("42 simbolos");
        lblConteo.setTextFill(Color.LIGHTGRAY);
        lblConteo.setFont(Font.font("System", 12));

        HBox barraFiltro = new HBox(12, lblTitulo, new Separator(), lblFiltro, cmbFiltro, lblConteo);
        barraFiltro.setAlignment(Pos.CENTER_LEFT);
        barraFiltro.setPadding(new Insets(10, 15, 10, 15));
        barraFiltro.setStyle("-fx-background-color: #1a252f;");

        // --- TABLA ---
        TableView<TablaSimbologiaEstatica.EntradaSimbolo> tabla = new TableView<>();
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setStyle("-fx-font-size: 13px;");

        TableColumn<TablaSimbologiaEstatica.EntradaSimbolo, String> colLexema = new TableColumn<>("Lexema");
        colLexema.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().lexema));
        colLexema.setPrefWidth(120);

        TableColumn<TablaSimbologiaEstatica.EntradaSimbolo, String> colTipo = new TableColumn<>("Tipo Token");
        colTipo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().tipoToken));
        colTipo.setPrefWidth(150);

        TableColumn<TablaSimbologiaEstatica.EntradaSimbolo, String> colCategoria = new TableColumn<>("Categoria");
        colCategoria.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().categoria));
        colCategoria.setPrefWidth(170);

        TableColumn<TablaSimbologiaEstatica.EntradaSimbolo, String> colDesc = new TableColumn<>("Descripcion");
        colDesc.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().descripcion));
        colDesc.setPrefWidth(380);

        tabla.getColumns().addAll(colLexema, colTipo, colCategoria, colDesc);
        tabla.setItems(FXCollections.observableArrayList(TablaSimbologiaEstatica.TABLA));

        // Color de filas por categoria
        tabla.setRowFactory(tv -> new TableRow<TablaSimbologiaEstatica.EntradaSimbolo>() {
            @Override
            protected void updateItem(TablaSimbologiaEstatica.EntradaSimbolo item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else if (item.categoria.startsWith("Flujo")) {
                    setStyle("-fx-background-color: #eaf4fb;");
                } else if (item.categoria.startsWith("BD")) {
                    setStyle("-fx-background-color: #fef9e7;");
                } else if (item.categoria.startsWith("Redes")) {
                    setStyle("-fx-background-color: #eafaf1;");
                } else if (item.categoria.equals("Cabecera") || item.categoria.equals("Meta-Instruccion")) {
                    setStyle("-fx-background-color: #f5eef8;");
                } else {
                    setStyle("-fx-background-color: #f2f3f4;");
                }
            }
        });

        // --- LEYENDA DE COLORES ---
        HBox leyenda = new HBox(16);
        leyenda.setPadding(new Insets(6, 15, 6, 15));
        leyenda.setAlignment(Pos.CENTER_LEFT);
        leyenda.setStyle("-fx-background-color: #ecf0f1; -fx-border-color: #bdc3c7; -fx-border-width: 1 0 0 0;");
        leyenda.getChildren().addAll(
            etiquetaColor("Global",     "#f5eef8"),
            etiquetaColor("Flujo",      "#eaf4fb"),
            etiquetaColor("BD",         "#fef9e7"),
            etiquetaColor("Redes",      "#eafaf1"),
            etiquetaColor("Puntacion y Literales", "#f2f3f4")
        );

        // --- LOGICA DEL FILTRO ---
        cmbFiltro.setOnAction(e -> {
            String sel = cmbFiltro.getValue();
            java.util.List<TablaSimbologiaEstatica.EntradaSimbolo> resultado =
                TablaSimbologiaEstatica.filtrar(sel);
            tabla.setItems(FXCollections.observableArrayList(resultado));
            lblConteo.setText(resultado.size() + " simbolos");
        });

        // --- LAYOUT FINAL ---
        BorderPane layout = new BorderPane();
        layout.setTop(barraFiltro);
        layout.setCenter(tabla);
        layout.setBottom(leyenda);

        ventana.setScene(new Scene(layout, 880, 560));
        ventana.show();
    }

    private void mostrarManual(Stage owner, String nombreArchivo, String tituloVentana) {
        Stage ventana = new Stage();
        ventana.initOwner(owner);
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.setTitle(tituloVentana);
        ventana.setMinWidth(780);
        ventana.setMinHeight(580);

        // --- BARRA SUPERIOR ---
        Label lblTitulo = new Label(tituloVentana);
        lblTitulo.setFont(Font.font("System", FontWeight.BOLD, 14));
        lblTitulo.setTextFill(Color.WHITE);

        String colorCabecera = nombreArchivo.contains("tecnica") ? "#6c3483" : "#1a5276";
        HBox cabecera = new HBox(lblTitulo);
        cabecera.setAlignment(Pos.CENTER_LEFT);
        cabecera.setPadding(new Insets(10, 15, 10, 15));
        cabecera.setStyle("-fx-background-color: " + colorCabecera + ";");

        // --- CONTENIDO ---
        TextArea txtContenido = new TextArea();
        txtContenido.setEditable(false);
        txtContenido.setFont(Font.font("Monospaced", 13));
        txtContenido.setWrapText(false);
        txtContenido.setStyle("-fx-background-color: #fdfefe;");

        String contenido;
        try {
            java.nio.file.Path ruta = java.nio.file.Paths.get(nombreArchivo);
            contenido = new String(java.nio.file.Files.readAllBytes(ruta), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException ex) {
            contenido = "No se pudo cargar el archivo: " + nombreArchivo + "\n" +
                        "Asegurate de ejecutar el programa desde el directorio raiz del proyecto.\n\n" +
                        "Ruta esperada: " + nombreArchivo;
        }
        txtContenido.setText(contenido);
        txtContenido.setScrollTop(0);

        // --- PIE ---
        Label lblPie = new Label(nombreArchivo + "  |  Solo lectura");
        lblPie.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
        lblPie.setPadding(new Insets(5, 15, 5, 15));

        HBox pie = new HBox(lblPie);
        pie.setStyle("-fx-background-color: #ecf0f1; -fx-border-color: #bdc3c7; -fx-border-width: 1 0 0 0;");

        // --- LAYOUT ---
        BorderPane layout = new BorderPane();
        layout.setTop(cabecera);
        layout.setCenter(txtContenido);
        layout.setBottom(pie);

        ventana.setScene(new Scene(layout, 820, 620));
        ventana.show();
        txtContenido.positionCaret(0);
    }

    private HBox etiquetaColor(String texto, String color) {
        Label cuadro = new Label("   ");
        cuadro.setStyle("-fx-background-color: " + color + "; -fx-border-color: #aaa; -fx-border-width: 1;");
        Label lbl = new Label(texto);
        lbl.setFont(Font.font("System", 11));
        HBox caja = new HBox(5, cuadro, lbl);
        caja.setAlignment(Pos.CENTER_LEFT);
        return caja;
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