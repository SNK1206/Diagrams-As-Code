package com.diagramas;

import com.diagramas.core.*;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Tab;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainFX extends Application {

    private TabPane panelPestanas;
    private TextArea txtConsola;

    private TextArea txtTokens;
    private TextArea txtSimbolos;

    // Contador para nombrar los archivos nuevos por defecto
    private int contadorNuevos = 1;

    // Mapa de tab -> TextArea de codigo (separa el editor del area de numeros de linea)
    private final Map<Tab, TextArea> editores = new HashMap<>();

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

        Button btnErroresLexicos = new Button("Errores Léxicos");
        btnErroresLexicos.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold;");
        btnErroresLexicos.setOnAction(e -> mostrarTablaErroresLexicos());

        Button btnArbol = new Button("Árbol de Derivación");
        btnArbol.setStyle("-fx-background-color: #16a085; -fx-text-fill: white; -fx-font-weight: bold;");
        btnArbol.setOnAction(e -> mostrarArbolDerivacion());

        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);

        Button btnSimbologia = new Button("Simbologia del Lenguaje");
        btnSimbologia.setStyle("-fx-background-color: #16a085; -fx-text-fill: white; -fx-font-weight: bold;");
        btnSimbologia.setOnAction(e -> mostrarTablaSimbologia(primaryStage));

        Button btnManual = new Button("Manual de Usuario");
        btnManual.setStyle("-fx-background-color: #1a5276; -fx-text-fill: white; -fx-font-weight: bold;");
        btnManual.setOnAction(e -> mostrarManual(primaryStage, "manual_diagrams_as_code.md", "Manual de Usuario — Diagrams As Code v2.0"));

        Button btnGramatica = new Button("Gramática DAC");
        btnGramatica.setStyle("-fx-background-color: #0e6655; -fx-text-fill: white; -fx-font-weight: bold;");
        btnGramatica.setOnAction(e -> mostrarManual(primaryStage, "gramatica_dac.md", "Gramática del Lenguaje — Diagrams As Code"));

        toolbar.getChildren().addAll(lblTitulo, btnNuevo, btnAbrir, btnGuardar, btnCompilar, btnErroresLexicos, btnArbol, espaciador, btnSimbologia, btnManual, btnGramatica);

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

        // --- 4. CONSOLA PEDAGÓGICA (ABAJO, ARRASTRABLE) ---
        VBox panelConsola = new VBox(5);
        panelConsola.setPadding(new Insets(10));
        Label lblConsola = new Label("Consola:");
        lblConsola.setStyle("-fx-font-weight: bold;");
        txtConsola = new TextArea();
        txtConsola.setEditable(false);
        txtConsola.setFont(Font.font("Monospaced", 12));
        VBox.setVgrow(txtConsola, Priority.ALWAYS);
        panelConsola.getChildren().addAll(lblConsola, txtConsola);

        SplitPane splitPrincipal = new SplitPane(splitCentro, panelConsola);
        splitPrincipal.setOrientation(Orientation.VERTICAL);
        splitPrincipal.setDividerPositions(0.72);

        BorderPane root = new BorderPane();
        root.setTop(toolbar);
        root.setCenter(splitPrincipal);

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
        TextArea txtEditorActual = getEditorDePestana(pestañaActiva);
        if (txtEditorActual == null) {
            txtConsola.setText("ERROR: Selecciona un archivo .dac para guardar.");
            return;
        }
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
                txtConsola.setText("Guardado cancelado.");
                return;
            }
        }

        // Escenario B: El archivo ya tiene un archivo asociado en disco (Sobrescribe directamente)
        try {
            Files.writeString(archivoAsociado.toPath(), contenido, StandardCharsets.UTF_8);
            txtConsola.setText("💾 Archivo '" + archivoAsociado.getName() + "' guardado con éxito en el almacenamiento local.");
        } catch (IOException ex) {
            txtConsola.setText("ERROR AL GUARDAR: No se pudo escribir el archivo.\nDetalle: " + ex.getMessage());
        }
    }

    private void crearPestana(String titulo, String contenido, File archivo) {
        Tab nuevaPestana = new Tab(titulo);
        nuevaPestana.setUserData(archivo);

        // ── Gutter de números de línea (izquierda, solo lectura) ─────────────
        TextArea lineNums = new TextArea();
        lineNums.setEditable(false);
        lineNums.setFocusTraversable(false);
        lineNums.setPrefWidth(55);
        lineNums.setMinWidth(55);
        lineNums.setMaxWidth(55);
        lineNums.setFont(Font.font("Monospaced", 14));
        lineNums.setStyle(
            "-fx-control-inner-background: #ecf0f1;" +
            "-fx-text-fill: #95a5a6;" +
            "-fx-padding: 2 4 2 4;" +
            "-fx-border-color: #bdc3c7; -fx-border-width: 0 1 0 0;");
        lineNums.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, e -> e.consume());

        // ── Área de código principal ──────────────────────────────────────────
        TextArea areaTexto = new TextArea(contenido);
        areaTexto.setFont(Font.font("Monospaced", 14));
        areaTexto.setWrapText(false);
        HBox.setHgrow(areaTexto, Priority.ALWAYS);

        lineNums.setText(generarNumerosLinea(contenido));
        areaTexto.textProperty().addListener((obs, viejo, nuevo) ->
            lineNums.setText(generarNumerosLinea(nuevo)));
        areaTexto.scrollTopProperty().addListener((obs, viejo, nuevo) ->
            lineNums.setScrollTop(nuevo.doubleValue()));

        HBox panel = new HBox(lineNums, areaTexto);
        nuevaPestana.setContent(panel);

        editores.put(nuevaPestana, areaTexto);
        nuevaPestana.setOnClosed(e -> editores.remove(nuevaPestana));
        panelPestanas.getTabs().add(nuevaPestana);
        panelPestanas.getSelectionModel().select(nuevaPestana);
    }

    private String generarNumerosLinea(String texto) {
        int total = texto.isEmpty() ? 1 : texto.split("\n", -1).length;
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= total; i++) {
            if (i > 1) sb.append('\n');
            sb.append(i);
        }
        return sb.toString();
    }

    private TextArea getEditorDePestana(Tab tab) {
        if (tab == null) return null;
        return editores.get(tab);
    }

    private void ejecutarCompilador() {
        txtTokens.clear();
        txtSimbolos.clear();
        txtConsola.clear();

        Tab pestañaActiva = panelPestanas.getSelectionModel().getSelectedItem();
        TextArea txtEditorActual = getEditorDePestana(pestañaActiva);
        if (txtEditorActual == null) {
            txtConsola.setText("ERROR: Selecciona un archivo .dac para compilar.");
            return;
        }
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

        boolean hayErroresLexicos = manejadorErrores.tieneErroresLexicos();

        // 2. FASE SINTÁCTICA — corre siempre para registrar los símbolos válidos
        ParserBase parserBase = new ParserBase(tokens, tablaSimbolos, manejadorErrores);
        parserBase.analizarCabecera();

        // Tabla de símbolos: muestra lo que sí se registró (si diagrama es inválido queda vacía)
        StringBuilder sbSimbolos = new StringBuilder();
        sbSimbolos.append("Contexto: ").append(tablaSimbolos.getContextoActivo()).append("\n\n");
        sbSimbolos.append(String.format("%-22s | %-18s | %5s | %s\n",
            "IDENTIFICADOR", "TIPO / ROL", "LÍNEA", "DESCRIPCIÓN"));
        sbSimbolos.append("─".repeat(85)).append("\n");

        for (Map.Entry<String, TablaSimbolos.Entrada> entry : tablaSimbolos.getSimbolos().entrySet()) {
            String id    = entry.getKey();
            TablaSimbolos.Entrada e = entry.getValue();
            String lineaStr = e.linea > 0 ? String.valueOf(e.linea) : "—";
            String desc  = TablaSimbolos.descripcionPara(id, e.rol);
            sbSimbolos.append(String.format("%-22s | %-18s | %5s | %s\n",
                id, e.rol, lineaStr, desc));
        }
        txtSimbolos.setText(sbSimbolos.toString());

        if (manejadorErrores.tieneErrores()) {
            String prefijo = hayErroresLexicos ? "ERRORES LEXICOS + SINTACTICOS:\n\n" : "COMPILACION FALLIDA:\n\n";
            txtConsola.setText(prefijo + obtenerErroresString(manejadorErrores));
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
        cmbFiltro.getItems().addAll("Todos", "Flujo", "BD", "Redes", "Conceptual", "UML", "Global", "Puntuacion");
        cmbFiltro.setValue("Todos");
        cmbFiltro.setStyle("-fx-font-size: 13px;");

        Label lblConteo = new Label(TablaSimbologiaEstatica.TABLA.size() + " simbolos");
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

    // ─────────────────────────────────────────────────────────────────────────
    // ÁRBOL DE DERIVACIÓN
    // ── Constantes de altura y espaciado (el ancho se calcula dinamicamente) ──
    private static final double RY_N    = 18;   // radio Y fijo del ovalo (altura)
    private static final double TH_N    = 22;   // alto fijo del rectangulo terminal
    private static final double FONT_PT = 10.5; // fuente base a escala 1:1
    private static final double HGAP   = 14;    // espacio horizontal entre hermanos
    private static final double VGAP   = 85;    // espacio vertical entre niveles
    private static final double PAD    = 44;    // margen exterior del canvas

    // Ancho dinamico: mide el texto real y agrega padding adecuado
    private double nw(TreeItem<String> n) {
        String lbl  = n.getValue();
        boolean term = isTerminal(lbl);
        String txt  = term ? extractLexema(lbl) : shortLabel(lbl);
        javafx.scene.text.Text m = new javafx.scene.text.Text(txt);
        m.setFont(term ? Font.font("Monospaced", FontWeight.NORMAL, FONT_PT)
                       : Font.font("System",     FontWeight.BOLD,   FONT_PT));
        double tw = m.getLayoutBounds().getWidth();
        // Terminal: padding de 20px c/lado; no-terminal: se expresa como diametro del ovalo
        return term ? Math.max(75,  tw + 28)
                    : Math.max(110, tw + 48);
    }
    private double nh(TreeItem<String> n) { return isTerminal(n.getValue()) ? TH_N : RY_N * 2; }
    private boolean isTerminal(String label) { return label.contains("→"); }

    private void mostrarArbolDerivacion() {
        Tab pestañaActiva = panelPestanas.getSelectionModel().getSelectedItem();
        if (pestañaActiva == null) {
            txtConsola.setText("No hay archivo abierto para generar el arbol.");
            return;
        }

        TextArea editor = getEditorDePestana(pestañaActiva);
        if (editor == null) {
            txtConsola.setText("ERROR: Selecciona un archivo .dac para generar el arbol.");
            return;
        }
        String codigo = editor.getText();

        ManejadorErrores errores = new ManejadorErrores();
        LexerBase lexer = new LexerBase(codigo, errores);
        List<Token> tokens = lexer.tokenizar();

        if (errores.tieneErrores()) {
            txtConsola.setText("El archivo tiene errores. Corrígelos antes de generar el árbol.");
            return;
        }

        // Detectar módulo
        String modulo = "desconocido";
        for (int i = 0; i < tokens.size() - 1; i++) {
            if (tokens.get(i).getTipo() == Token.Tipo.PR_DIAGRAMA) {
                modulo = tokens.get(i + 1).getLexema().toLowerCase();
                break;
            }
        }

        // Construir árbol lógico alineado con la gramática formal EBNF
        final TreeItem<String> raiz = new TreeItem<>("programa_" + modulo);
        raiz.setExpanded(true);

        // Agrupar tokens por instruccion:
        //   - se divide en ';' cuando prof == 0  (instrucciones lineales)
        //   - se divide en '}' cuando prof pasa a 0  (bloques BD/UML sin ';' final)
        List<List<Token>> instrucciones = new ArrayList<>();
        List<Token> actual = new ArrayList<>();
        int prof = 0;
        for (Token t : tokens) {
            if (t.getTipo() == Token.Tipo.EOF) break;
            if (t.getTipo() == Token.Tipo.LLAVE_IZQ) prof++;
            actual.add(t);
            if (t.getTipo() == Token.Tipo.LLAVE_DER) {
                prof--;
                if (prof == 0) {
                    instrucciones.add(new ArrayList<>(actual));
                    actual.clear();
                }
            } else if (t.getTipo() == Token.Tipo.PUNTO_Y_COMA && prof == 0) {
                if (!actual.isEmpty()) {
                    instrucciones.add(new ArrayList<>(actual));
                    actual.clear();
                }
            }
        }
        if (!actual.isEmpty()) instrucciones.add(actual);

        // Construir árbol: meta-instrucciones y cabecera van directo a raiz,
        // las instrucciones del módulo van dentro de cuerpo_<modulo>
        TreeItem<String> cuerpoNodo = new TreeItem<>("cuerpo_" + modulo);
        cuerpoNodo.setExpanded(true);

        for (List<Token> grupo : instrucciones) {
            if (grupo.isEmpty()) continue;
            Token primero = grupo.get(0);
            String lexPrimero = primero.getLexema();
            boolean esMeta = lexPrimero.equals("autor") || lexPrimero.equals("version") ||
                             lexPrimero.equals("tema");
            boolean esCabecera = primero.getTipo() == Token.Tipo.PR_DIAGRAMA;
            TreeItem<String> n = construirNodoRegla(grupo, modulo);
            if (n == null) continue;
            if (esMeta || esCabecera) raiz.getChildren().add(n);
            else                      cuerpoNodo.getChildren().add(n);
        }
        raiz.getChildren().add(cuerpoNodo);

        // ── Layout natural (solo una vez) ──────────────────────────────────
        final Map<TreeItem<String>, double[]> natPos = new HashMap<>();
        layoutTree(raiz, PAD + subtreeW(raiz) / 2.0, PAD, natPos);
        final double rawW = maxX(raiz, natPos) + PAD;
        final double rawH = maxY(raiz, natPos) + PAD;
        final double autoS = Math.min(1.0, Math.min(920.0 / rawW, 500.0 / rawH));

        // ── Estado de zoom compartido ───────────────────────────────────────
        final double[] zoomS = { autoS };

        // ── Canvas y scroll inicial ─────────────────────────────────────────
        final ScrollPane scroll = buildScroll(renderCanvas(raiz, natPos, rawW, rawH, zoomS[0]));

        // ── Helper: refresca el canvas dentro del scroll ────────────────────
        Runnable refrescar = () -> scroll.setContent(
            renderCanvas(raiz, natPos, rawW, rawH, zoomS[0]));

        // ── Controles de zoom ───────────────────────────────────────────────
        Label lblZoom = new Label(pct(zoomS[0], autoS));
        lblZoom.setStyle("-fx-font-weight: bold; -fx-min-width: 48; -fx-alignment: center;");

        Button btnZoomOut = new Button("  −  ");
        Button btnZoomIn  = new Button("  +  ");
        Button btnFit     = new Button("Ajustar");

        String zoomStyle = "-fx-font-weight: bold; -fx-font-size: 13;";
        btnZoomOut.setStyle(zoomStyle);
        btnZoomIn .setStyle(zoomStyle);
        btnFit.setStyle("-fx-font-size: 11;");

        btnZoomIn.setOnAction(ev -> {
            zoomS[0] = Math.min(zoomS[0] * 1.25, autoS * 8.0);
            lblZoom.setText(pct(zoomS[0], autoS));
            refrescar.run();
        });
        btnZoomOut.setOnAction(ev -> {
            zoomS[0] = Math.max(zoomS[0] * 0.8, autoS * 0.3);
            lblZoom.setText(pct(zoomS[0], autoS));
            refrescar.run();
        });
        btnFit.setOnAction(ev -> {
            zoomS[0] = autoS;
            lblZoom.setText(pct(zoomS[0], autoS));
            refrescar.run();
        });

        // ── Botón pantalla completa ─────────────────────────────────────────
        Button btnPantallaCompleta = new Button("⛶  Pantalla Completa");
        btnPantallaCompleta.setStyle(
            "-fx-background-color: #16a085; -fx-text-fill: white; -fx-font-weight: bold;");
        btnPantallaCompleta.setOnAction(ev -> {
            double fsAutoS = Math.min(2.0, Math.min(1900.0 / rawW, 1000.0 / rawH));
            double[] fsZoom = { fsAutoS };

            final ScrollPane fsScroll = buildScroll(renderCanvas(raiz, natPos, rawW, rawH, fsZoom[0]));

            // Controles de zoom de la ventana completa
            Label fsLblZoom = new Label(pct(fsZoom[0], fsAutoS));
            fsLblZoom.setStyle("-fx-font-weight: bold; -fx-min-width: 48; -fx-alignment: center;");

            Button fsBtnOut = new Button("  −  ");
            Button fsBtnIn  = new Button("  +  ");
            Button fsBtnFit = new Button("Ajustar");
            String zs = "-fx-font-weight: bold; -fx-font-size: 13;";
            fsBtnOut.setStyle(zs); fsBtnIn.setStyle(zs);
            fsBtnFit.setStyle("-fx-font-size: 11;");

            Runnable fsRefresh = () -> fsScroll.setContent(
                renderCanvas(raiz, natPos, rawW, rawH, fsZoom[0]));

            fsBtnIn.setOnAction(e2 -> {
                fsZoom[0] = Math.min(fsZoom[0] * 1.25, fsAutoS * 8.0);
                fsLblZoom.setText(pct(fsZoom[0], fsAutoS));
                fsRefresh.run();
            });
            fsBtnOut.setOnAction(e2 -> {
                fsZoom[0] = Math.max(fsZoom[0] * 0.8, fsAutoS * 0.2);
                fsLblZoom.setText(pct(fsZoom[0], fsAutoS));
                fsRefresh.run();
            });
            fsBtnFit.setOnAction(e2 -> {
                fsZoom[0] = fsAutoS;
                fsLblZoom.setText(pct(fsZoom[0], fsAutoS));
                fsRefresh.run();
            });

            Label fsLbl = new Label("Árbol de Derivación — " + pestañaActiva.getText());
            fsLbl.setFont(Font.font("System", FontWeight.BOLD, 15));
            fsLbl.setStyle("-fx-text-fill: #16a085;");

            HBox fsBarraZoom = new HBox(4, fsBtnOut, fsLblZoom, fsBtnIn, fsBtnFit);
            fsBarraZoom.setAlignment(Pos.CENTER_LEFT);

            HBox fsBarraTop = new HBox(12, fsLbl, fsBarraZoom);
            fsBarraTop.setAlignment(Pos.CENTER_LEFT);
            fsBarraTop.setStyle("-fx-padding: 8 10 4 10; -fx-background-color: #f8f9fa;");

            VBox root2 = new VBox(fsBarraTop, fsScroll);
            VBox.setVgrow(fsScroll, Priority.ALWAYS);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Árbol de Derivación — Pantalla Completa");
            stage.setScene(new javafx.scene.Scene(root2, 1200, 800));
            stage.setMaximized(true);
            stage.show();
        });

        // ── Títulos y barra superior ────────────────────────────────────────
        Label titulo = new Label("Árbol de Derivación — " + pestañaActiva.getText());
        titulo.setFont(Font.font("System", FontWeight.BOLD, 13));
        titulo.setStyle("-fx-text-fill: #16a085;");

        HBox barraZoom = new HBox(4, btnZoomOut, lblZoom, btnZoomIn, btnFit);
        barraZoom.setAlignment(Pos.CENTER_LEFT);
        barraZoom.setStyle("-fx-padding: 0 8 0 0;");

        HBox barraTop = new HBox(10, titulo, barraZoom, btnPantallaCompleta);
        barraTop.setAlignment(Pos.CENTER_LEFT);

        Label leyenda = new Label("Óvalo = no-terminal (regla)     Rectángulo = terminal (token)");
        leyenda.setStyle("-fx-font-size: 10; -fx-text-fill: #888888;");

        VBox contenido = new VBox(5, barraTop, leyenda, scroll);
        contenido.setPadding(new Insets(10));
        VBox.setVgrow(scroll, Priority.ALWAYS);

        String tabName = "Árbol — " + pestañaActiva.getText();
        for (Tab t : panelPestanas.getTabs()) {
            if (tabName.equals(t.getText())) {
                t.setContent(contenido);
                panelPestanas.getSelectionModel().select(t);
                return;
            }
        }
        Tab tab = new Tab(tabName, contenido);
        panelPestanas.getTabs().add(tab);
        panelPestanas.getSelectionModel().select(tab);
    }

    // ── Porcentaje de zoom para el label ─────────────────────────────────────
    private String pct(double current, double base) {
        return Math.round(current / base * 100) + "%";
    }

    // ── Renderiza el árbol a una escala exacta (posiciones pre-multiplicadas) ─
    private Pane renderCanvas(TreeItem<String> raiz, Map<TreeItem<String>, double[]> nat,
                              double rawW, double rawH, double s) {
        Map<TreeItem<String>, double[]> pos = new HashMap<>();
        for (var e : nat.entrySet())
            pos.put(e.getKey(), new double[]{ e.getValue()[0] * s, e.getValue()[1] * s });

        Pane canvas = new Pane();
        canvas.setStyle("-fx-background-color: white;");
        canvas.setPrefSize(rawW * s, rawH * s);
        canvas.setMaxSize(rawW * s, rawH * s);
        paintEdges(raiz, pos, canvas, s);
        paintNodes(raiz, pos, canvas, s);
        return canvas;
    }

    // ── buildCanvas usado por código antiguo / pruebas ────────────────────────
    private Pane buildCanvas(TreeItem<String> raiz) {
        return buildCanvas(raiz, 920, 500);
    }

    private Pane buildCanvas(TreeItem<String> raiz, double targetW, double targetH) {
        Map<TreeItem<String>, double[]> nat = new HashMap<>();
        layoutTree(raiz, PAD + subtreeW(raiz) / 2.0, PAD, nat);
        double rawW = maxX(raiz, nat) + PAD;
        double rawH = maxY(raiz, nat) + PAD;
        double s = Math.min(1.0, Math.min(targetW / rawW, targetH / rawH));
        return renderCanvas(raiz, nat, rawW, rawH, s);
    }

    private ScrollPane buildScroll(Pane canvas) {
        ScrollPane sc = new ScrollPane(canvas);
        sc.setPannable(true);
        sc.setFitToWidth(false);   // NO estirar: respetar el tamaño calculado
        sc.setFitToHeight(false);
        sc.setStyle("-fx-background: white; -fx-background-color: white;");
        return sc;
    }

    // ── Ancho mínimo del subárbol ─────────────────────────────────────────────
    private double subtreeW(TreeItem<String> n) {
        if (n.getChildren().isEmpty()) return nw(n) + HGAP;
        double sum = n.getChildren().stream().mapToDouble(this::subtreeW).sum();
        return Math.max(nw(n) + HGAP, sum);
    }

    // ── Layout top-down: pos = CENTRO (cx, cy) ────────────────────────────────
    private void layoutTree(TreeItem<String> n, double cx, double topY,
                            Map<TreeItem<String>, double[]> pos) {
        pos.put(n, new double[]{ cx, topY + nh(n) / 2.0 });
        if (n.getChildren().isEmpty()) return;
        double startX    = cx - subtreeW(n) / 2.0;
        double childTopY = topY + nh(n) + VGAP;
        for (TreeItem<String> h : n.getChildren()) {
            double w = subtreeW(h);
            layoutTree(h, startX + w / 2.0, childTopY, pos);
            startX += w;
        }
    }

    // ── Bounds recursivos del subárbol ────────────────────────────────────────
    private double maxX(TreeItem<String> n, Map<TreeItem<String>, double[]> pos) {
        double[] p = pos.get(n);
        double m = p[0] + nw(n) / 2.0;
        for (var c : n.getChildren()) m = Math.max(m, maxX(c, pos));
        return m;
    }
    private double maxY(TreeItem<String> n, Map<TreeItem<String>, double[]> pos) {
        double[] p = pos.get(n);
        double m = p[1] + nh(n) / 2.0;
        for (var c : n.getChildren()) m = Math.max(m, maxY(c, pos));
        return m;
    }

    // ── Color de relleno segun tipo de nodo (no-terminal) ────────────────────
    private Color getNodeColor(String lbl) {
        if (lbl.startsWith("programa_"))           return Color.web("#1a252f");
        if (lbl.startsWith("cuerpo_"))             return Color.web("#2c3e50");
        if (lbl.equals("cabecera"))                return Color.web("#d35400");
        if (lbl.equals("meta_instruccion"))        return Color.web("#7d3c98");
        if (lbl.equals("decl_nodo_simple")
         || lbl.equals("decl_nodo_texto"))         return Color.web("#1a6ea8");
        if (lbl.startsWith("conexion_")
         || lbl.startsWith("relacion_")
         || lbl.startsWith("enlace_"))             return Color.web("#b03a2e");
        if (lbl.startsWith("bloque_")
         || lbl.startsWith("componente_"))         return Color.web("#117a65");
        if (lbl.equals("atributo")
         || lbl.startsWith("propiedad_")
         || lbl.equals("miembro_clase"))           return Color.web("#1e8449");
        if (lbl.startsWith("decl_"))               return Color.web("#1a6ea8");
        return Color.web("#566573");
    }

    // ── Conexiones curvas padre → hijo ────────────────────────────────────────
    private void paintEdges(TreeItem<String> n, Map<TreeItem<String>, double[]> pos,
                            Pane c, double s) {
        double[] pp = pos.get(n);
        double   px = pp[0], pyBot = pp[1] + nh(n) * s / 2.0;
        for (TreeItem<String> h : n.getChildren()) {
            double[] ph   = pos.get(h);
            double   phTop = ph[1] - nh(h) * s / 2.0;
            double   midY  = (pyBot + phTop) / 2.0;
            CubicCurve curve = new CubicCurve(px, pyBot, px, midY, ph[0], midY, ph[0], phTop);
            curve.setStroke(Color.web("#95a5a6"));
            curve.setFill(Color.TRANSPARENT);
            curve.setStrokeWidth(Math.max(0.8, 1.2 * s));
            c.getChildren().add(curve);
            paintEdges(h, pos, c, s);
        }
    }

    // ── Nodos: ovalo coloreado (no-terminal) | rectangulo redondeado (terminal)
    private void paintNodes(TreeItem<String> n, Map<TreeItem<String>, double[]> pos,
                            Pane c, double s) {
        double[] p    = pos.get(n);
        String   lbl  = n.getValue();
        boolean  term = isTerminal(lbl);
        double   fs   = Math.max(8.0, FONT_PT * s);
        double   nodW = nw(n) * s;  // ancho dinamico escalado
        double   nodH = nh(n) * s;  // alto escalado

        if (term) {
            double tw = nodW, th = nodH;
            Rectangle r = new Rectangle(p[0] - tw / 2.0, p[1] - th / 2.0, tw, th);
            r.setFill(Color.web("#fdfefe"));
            r.setStroke(Color.web("#aab7b8"));
            r.setStrokeWidth(Math.max(0.5, 0.8 * s));
            r.setArcWidth(8 * s);
            r.setArcHeight(8 * s);
            c.getChildren().add(r);
            // Texto medido y centrado exactamente dentro del rectangulo
            javafx.scene.text.Text txt = new javafx.scene.text.Text(extractLexema(lbl));
            txt.setFont(Font.font("Monospaced", FontWeight.NORMAL, fs));
            txt.setFill(Color.web("#1a252f"));
            txt.setWrappingWidth(0);
            double tW = txt.getLayoutBounds().getWidth();
            double tH = txt.getLayoutBounds().getHeight();
            txt.setX(p[0] - tW / 2.0);
            txt.setY(p[1] + tH * 0.35);
            c.getChildren().add(txt);
        } else {
            double rx = nodW / 2.0, ry = nodH / 2.0;
            Color  bg = getNodeColor(lbl);
            Ellipse oval = new Ellipse(p[0], p[1], rx, ry);
            oval.setFill(bg);
            oval.setStroke(bg.darker());
            oval.setStrokeWidth(Math.max(0.7, 1.2 * s));
            c.getChildren().add(oval);
            // Texto medido y centrado dentro del ovalo
            javafx.scene.text.Text txt = new javafx.scene.text.Text(shortLabel(lbl));
            txt.setFont(Font.font("System", FontWeight.BOLD, fs));
            txt.setFill(Color.WHITE);
            txt.setWrappingWidth(0);
            double tW = txt.getLayoutBounds().getWidth();
            double tH = txt.getLayoutBounds().getHeight();
            txt.setX(p[0] - tW / 2.0);
            txt.setY(p[1] + tH * 0.35);
            c.getChildren().add(txt);
        }
        for (TreeItem<String> h : n.getChildren()) paintNodes(h, pos, c, s);
    }

    private javafx.scene.text.Text mkText(String txt, double x, double y,
                                          double w, Font f, Color col) {
        javafx.scene.text.Text t = new javafx.scene.text.Text(txt);
        t.setFont(f); t.setFill(col);
        t.setTextAlignment(TextAlignment.CENTER);
        t.setWrappingWidth(w - 2);
        t.setX(x + 1); t.setY(y);
        return t;
    }

    private String shortLabel(String lbl) {
        return lbl
            .replace("programa_",          "prog_")
            .replace("cuerpo_",            "body_")
            .replace("meta_instruccion",   "meta")
            .replace("decl_nodo_simple",   "nodo_simple")
            .replace("decl_nodo_texto",    "nodo_texto")
            .replace("conexion_flujo",     "conexion")
            .replace("bloque_bd",          "bloque_bd")
            .replace("componente_lineal_bd","comp_lineal")
            .replace("componente_red",     "comp_red")
            .replace("propiedad_red",      "prop_red")
            .replace("enlace_red",         "enlace")
            .replace("relacion_conceptual","rel_concept")
            .replace("relacion_uml",       "rel_uml")
            .replace("relacion_bd",        "rel_bd")
            .replace("decl_concepto",      "concepto")
            .replace("decl_lineal_uml",    "lineal_uml")
            .replace("decl_clase",         "clase")
            .replace("miembro_clase",      "miembro")
            .replace("instruccion_no_reconocida", "desconocida");
    }

    private String extractLexema(String label) {
        int a = label.indexOf("'"), b = label.lastIndexOf("'");
        if (a >= 0 && b > a) {
            String lexema = label.substring(a + 1, b);
            if (lexema.length() > 15) lexema = lexema.substring(0, 14) + "…";
            return "'" + lexema + "'";
        }
        return label;
    }

    // ── Helper: crea un nodo no-terminal (óvalo en el árbol) ─────────────────
    private TreeItem<String> ntItem(String nombre) {
        TreeItem<String> n = new TreeItem<>(nombre);
        n.setExpanded(true);
        return n;
    }

    private TreeItem<String> construirNodoRegla(List<Token> grupo, String modulo) {
        if (grupo.isEmpty()) return null;
        Token primero = grupo.get(0);
        String lex = primero.getLexema();

        // ── Cabecera: 'diagrama' tipo_diagrama ';' ────────────────────────────
        if (primero.getTipo() == Token.Tipo.PR_DIAGRAMA) {
            TreeItem<String> nodo = ntItem("cabecera");
            for (Token t : grupo) nodo.getChildren().add(tokenItem(t));
            return nodo;
        }

        // ── Meta-instruccion: kw_meta TEXTO_LITERAL ';' ───────────────────────
        if (lex.equals("autor") || lex.equals("version") || lex.equals("tema")) {
            TreeItem<String> nodo = ntItem("meta_instruccion");
            for (Token t : grupo) nodo.getChildren().add(tokenItem(t));
            return nodo;
        }

        // ── Módulo FLUJO ──────────────────────────────────────────────────────
        if (modulo.equals("flujo")) {
            // decl_nodo_simple ::= ('inicio'|'fin') IDENTIFICADOR ';'
            if (lex.equals("inicio") || lex.equals("fin")) {
                TreeItem<String> nodo = ntItem("decl_nodo_simple");
                for (Token t : grupo) nodo.getChildren().add(tokenItem(t));
                return nodo;
            }
            // decl_nodo_texto ::= kw_nodo_texto IDENTIFICADOR TEXTO_LITERAL ';'
            if (lex.equals("nodo") || lex.equals("condicion") || lex.equals("bucle") ||
                lex.equals("subproceso") || lex.equals("entrada") || lex.equals("salida") ||
                lex.equals("parada")) {
                TreeItem<String> nodo = ntItem("decl_nodo_texto");
                for (Token t : grupo) nodo.getChildren().add(tokenItem(t));
                return nodo;
            }
            // conexion_flujo ::= IDENTIFICADOR 'conecta' IDENTIFICADOR ';'
            for (Token t : grupo) {
                if (t.getLexema().equals("conecta")) {
                    TreeItem<String> nodo = ntItem("conexion_flujo");
                    for (Token tk : grupo) nodo.getChildren().add(tokenItem(tk));
                    return nodo;
                }
            }
        }

        // ── Módulo BD ─────────────────────────────────────────────────────────
        if (modulo.equals("bd")) {
            // bloque_bd ::= kw_bloque_bd IDENTIFICADOR '{' atributo* '}'
            if (lex.equals("tabla") || lex.equals("vista") || lex.equals("esquema") || lex.equals("paquete")) {
                TreeItem<String> nodo = ntItem("bloque_bd");
                boolean dentro = false;
                List<Token> atributoActual = new ArrayList<>();
                for (Token t : grupo) {
                    if (t.getTipo() == Token.Tipo.LLAVE_IZQ) {
                        nodo.getChildren().add(tokenItem(t));
                        dentro = true;
                    } else if (t.getTipo() == Token.Tipo.LLAVE_DER) {
                        dentro = false;
                        nodo.getChildren().add(tokenItem(t));
                    } else if (!dentro) {
                        nodo.getChildren().add(tokenItem(t));
                    } else {
                        atributoActual.add(t);
                        if (t.getTipo() == Token.Tipo.PUNTO_Y_COMA) {
                            TreeItem<String> atrib = ntItem("atributo");
                            for (Token at : atributoActual) atrib.getChildren().add(tokenItem(at));
                            nodo.getChildren().add(atrib);
                            atributoActual.clear();
                        }
                    }
                }
                return nodo;
            }
            // componente_lineal_bd ::= kw_lineal_bd IDENTIFICADOR ';'
            if (lex.equals("procedimiento") || lex.equals("funcion") || lex.equals("indice") ||
                lex.equals("disparador") || lex.equals("secuencia")) {
                TreeItem<String> nodo = ntItem("componente_lineal_bd");
                for (Token t : grupo) nodo.getChildren().add(tokenItem(t));
                return nodo;
            }
            // relacion_bd ::= IDENTIFICADOR 'relaciona' IDENTIFICADOR ';'
            for (Token t : grupo) {
                if (t.getLexema().equals("relaciona")) {
                    TreeItem<String> nodo = ntItem("relacion_bd");
                    for (Token tk : grupo) nodo.getChildren().add(tokenItem(tk));
                    return nodo;
                }
            }
        }

        // ── Módulo REDES ──────────────────────────────────────────────────────
        if (modulo.equals("redes")) {
            // componente_red ::= kw_componente_red IDENTIFICADOR IDENTIFICADOR ['{' propiedad_red* '}'] ';'
            if (lex.equals("dispositivo") || lex.equals("nube") || lex.equals("vlan") ||
                lex.equals("subred") || lex.equals("cluster") || lex.equals("tunel") ||
                lex.equals("zona") || lex.equals("puerto") || lex.equals("politica")) {
                TreeItem<String> nodo = ntItem("componente_red");
                boolean dentro = false;
                List<Token> propActual = new ArrayList<>();
                for (Token t : grupo) {
                    if (t.getTipo() == Token.Tipo.LLAVE_IZQ) {
                        nodo.getChildren().add(tokenItem(t));
                        dentro = true;
                    } else if (t.getTipo() == Token.Tipo.LLAVE_DER) {
                        dentro = false;
                        nodo.getChildren().add(tokenItem(t));
                    } else if (!dentro) {
                        nodo.getChildren().add(tokenItem(t));
                    } else {
                        propActual.add(t);
                        if (t.getTipo() == Token.Tipo.PUNTO_Y_COMA) {
                            TreeItem<String> prop = ntItem("propiedad_red");
                            for (Token pt : propActual) prop.getChildren().add(tokenItem(pt));
                            nodo.getChildren().add(prop);
                            propActual.clear();
                        }
                    }
                }
                return nodo;
            }
            // enlace_red ::= IDENTIFICADOR 'enlaza' IDENTIFICADOR ';'
            for (Token t : grupo) {
                if (t.getLexema().equals("enlaza")) {
                    TreeItem<String> nodo = ntItem("enlace_red");
                    for (Token tk : grupo) nodo.getChildren().add(tokenItem(tk));
                    return nodo;
                }
            }
        }

        // ── Módulo CONCEPTUAL ─────────────────────────────────────────────────
        if (modulo.equals("conceptual")) {
            // decl_concepto ::= ('concepto'|'categoria'|'propiedad') IDENTIFICADOR TEXTO_LITERAL ';'
            if (lex.equals("concepto") || lex.equals("categoria") || lex.equals("propiedad")) {
                TreeItem<String> nodo = ntItem("decl_concepto");
                for (Token t : grupo) nodo.getChildren().add(tokenItem(t));
                return nodo;
            }
            // relacion_conceptual ::= IDENTIFICADOR ('agrupa'|'asocia'|'depende') IDENTIFICADOR ';'
            for (Token t : grupo) {
                if (t.getLexema().equals("agrupa") || t.getLexema().equals("asocia") || t.getLexema().equals("depende")) {
                    TreeItem<String> nodo = ntItem("relacion_conceptual");
                    for (Token tk : grupo) nodo.getChildren().add(tokenItem(tk));
                    return nodo;
                }
            }
        }

        // ── Módulo UML ────────────────────────────────────────────────────────
        if (modulo.equals("uml")) {
            // decl_clase ::= 'clase' IDENTIFICADOR '{' miembro_clase* '}'
            if (lex.equals("clase")) {
                TreeItem<String> nodo = ntItem("decl_clase");
                boolean dentro = false;
                List<Token> miembroActual = new ArrayList<>();
                for (Token t : grupo) {
                    if (t.getTipo() == Token.Tipo.LLAVE_IZQ) {
                        nodo.getChildren().add(tokenItem(t));
                        dentro = true;
                    } else if (t.getTipo() == Token.Tipo.LLAVE_DER) {
                        dentro = false;
                        nodo.getChildren().add(tokenItem(t));
                    } else if (!dentro) {
                        nodo.getChildren().add(tokenItem(t));
                    } else {
                        miembroActual.add(t);
                        if (t.getTipo() == Token.Tipo.PUNTO_Y_COMA) {
                            TreeItem<String> miembro = ntItem("miembro_clase");
                            for (Token m : miembroActual) miembro.getChildren().add(tokenItem(m));
                            nodo.getChildren().add(miembro);
                            miembroActual.clear();
                        }
                    }
                }
                return nodo;
            }
            // decl_lineal_uml ::= ('interfaz'|'enum') IDENTIFICADOR ';'
            if (lex.equals("interfaz") || lex.equals("enum")) {
                TreeItem<String> nodo = ntItem("decl_lineal_uml");
                for (Token t : grupo) nodo.getChildren().add(tokenItem(t));
                return nodo;
            }
            // relacion_uml ::= IDENTIFICADOR ('extiende'|'implementa'|'usa') IDENTIFICADOR ';'
            for (Token t : grupo) {
                if (t.getLexema().equals("extiende") || t.getLexema().equals("implementa") || t.getLexema().equals("usa")) {
                    TreeItem<String> nodo = ntItem("relacion_uml");
                    for (Token tk : grupo) nodo.getChildren().add(tokenItem(tk));
                    return nodo;
                }
            }
        }

        // Grupo no reconocido por ninguna regla
        TreeItem<String> nodo = ntItem("instruccion_no_reconocida");
        for (Token t : grupo) nodo.getChildren().add(tokenItem(t));
        return nodo;
    }

    private TreeItem<String> tokenItem(Token t) {
        String pos = "[L" + t.getLinea() + ":C" + t.getColumna() + "]";
        String etiqueta = t.getTipo().name() + "  →  '" + t.getLexema() + "'  " + pos;
        return new TreeItem<>(etiqueta);
    }

    private void mostrarTablaErroresLexicos() {
        // Si la pestaña ya está abierta, solo la selecciona
        for (Tab t : panelPestanas.getTabs()) {
            if ("Catálogo de Errores".equals(t.getText())) {
                panelPestanas.getSelectionModel().select(t);
                return;
            }
        }

        // --- Errores extraídos literalmente del código fuente del compilador ---
        ObservableList<ErrorLexico> todos = FXCollections.observableArrayList(
            // ── LÉXICOS — LexerBase.java ──────────────────────────────────────
            new ErrorLexico("EL01", "Léxico",
                "Carácter inválido — el símbolo no pertenece al alfabeto del lenguaje DAC"),
            new ErrorLexico("EL02", "Léxico",
                "Identificador inválido — no puede iniciar con un dígito o contener caracteres especiales (ej: '2nodo', 'mi@nodo' son inválidos)"),
            new ErrorLexico("EL03", "Léxico",
                "Palabra reservada mal escrita — se detectó una palabra clave con mayúsculas incorrectas (ej: 'Nodo', 'TABLA'). El lenguaje es sensible a mayúsculas/minúsculas"),

            // ── SINTÁCTICOS — ParserBase.java ────────────────────────────────
            new ErrorLexico("ES01", "Sintáctico",
                "[Sintáctico Núcleo] Falta ';' al final de la instrucción (autor, version, tema)"),
            new ErrorLexico("ES02", "Sintáctico",
                "[Sintáctico Núcleo] Se esperaba un texto entre comillas después de la meta-instrucción"),
            new ErrorLexico("ES03", "Sintáctico",
                "[Sintáctico] Falta ';' en la cabecera 'diagrama <Tipo>'"),
            new ErrorLexico("ES04", "Sintáctico",
                "[Sintáctico] Falta el tipo de diagrama después de 'diagrama'"),
            new ErrorLexico("ES05", "Sintáctico",
                "[Sintáctico] No se encontró la cabecera principal 'diagrama <Tipo>;'"),
            new ErrorLexico("ES55", "Sintáctico",
                "[Sintáctico] Cadena de texto sin cerrar — falta la comilla doble de cierre (\")"),
            new ErrorLexico("ES56", "Sintáctico",
                "[Sintáctico] Palabra reservada usada como nombre de identificador — no se puede usar una palabra clave del lenguaje como nombre definido por el usuario"),

            // ── SINTÁCTICOS — FlujoParser.java ───────────────────────────────
            new ErrorLexico("ES06", "Sintáctico",
                "[Sintáctico Flujo] Token inesperado al inicio de instrucción"),
            new ErrorLexico("ES07", "Sintáctico",
                "[Sintáctico Flujo] Falta el nombre del identificador después de la palabra clave"),
            new ErrorLexico("ES08", "Sintáctico",
                "[Sintáctico Flujo] Falta ';' al final de la declaración del nodo simple"),
            new ErrorLexico("ES09", "Sintáctico",
                "[Sintáctico Flujo] Falta la descripción entre comillas (nodo, condicion, entrada, salida, etc.)"),
            new ErrorLexico("ES10", "Sintáctico",
                "[Sintáctico Flujo] Falta ';' al final de la instrucción con texto"),
            new ErrorLexico("ES11", "Sintáctico",
                "[Sintáctico Flujo] Falta el identificador destino en instrucción 'conecta'"),
            new ErrorLexico("ES12", "Sintáctico",
                "[Sintáctico Flujo] Falta ';' al finalizar la instrucción de conexión"),
            new ErrorLexico("ES13", "Sintáctico",
                "[Sintáctico Flujo] Instrucción o verbo inválido — se esperaba 'conecta'"),

            // ── SINTÁCTICOS — BDParser.java ──────────────────────────────────
            new ErrorLexico("ES14", "Sintáctico",
                "[Sintáctico BD] Token inesperado al inicio de instrucción"),
            new ErrorLexico("ES15", "Sintáctico",
                "[Sintáctico BD] Falta el nombre del elemento (tabla, vista, esquema, paquete)"),
            new ErrorLexico("ES16", "Sintáctico",
                "[Sintáctico BD] Falta '{' para abrir el bloque de definición"),
            new ErrorLexico("ES17", "Sintáctico",
                "[Sintáctico BD] Falta el nombre del atributo dentro del bloque"),
            new ErrorLexico("ES18", "Sintáctico",
                "[Sintáctico BD] Falta ':' en la definición del atributo"),
            new ErrorLexico("ES19", "Sintáctico",
                "[Sintáctico BD] Falta el tipo de dato del atributo"),
            new ErrorLexico("ES20", "Sintáctico",
                "[Sintáctico BD] Falta ';' al final del atributo"),
            new ErrorLexico("ES21", "Sintáctico",
                "[Sintáctico BD] Falta '}' para cerrar el bloque de definición"),
            new ErrorLexico("ES22", "Sintáctico",
                "[Sintáctico BD] Falta el identificador del componente lineal (procedimiento, funcion, indice, disparador, secuencia)"),
            new ErrorLexico("ES23", "Sintáctico",
                "[Sintáctico BD] Falta ';' al final del componente lineal"),
            new ErrorLexico("ES24", "Sintáctico",
                "[Sintáctico BD] Falta el identificador destino en instrucción 'relaciona'"),
            new ErrorLexico("ES25", "Sintáctico",
                "[Sintáctico BD] Falta ';' al finalizar la instrucción 'relaciona'"),
            new ErrorLexico("ES26", "Sintáctico",
                "[Sintáctico BD] Verbo inválido — se esperaba 'relaciona'"),

            // ── SINTÁCTICOS — RedesParser.java ───────────────────────────────
            new ErrorLexico("ES27", "Sintáctico",
                "[Sintáctico Redes] Token inesperado al inicio de instrucción"),
            new ErrorLexico("ES28", "Sintáctico",
                "[Sintáctico Redes] Falta el nombre del dispositivo"),
            new ErrorLexico("ES29", "Sintáctico",
                "[Sintáctico Redes] Falta el tipo del dispositivo (ej. Router, Switch)"),
            new ErrorLexico("ES30", "Sintáctico",
                "[Sintáctico Redes] Falta '}' para cerrar el bloque de propiedades"),
            new ErrorLexico("ES31", "Sintáctico",
                "[Sintáctico Redes] Falta ';' al final de la declaración del dispositivo"),
            new ErrorLexico("ES32", "Sintáctico",
                "[Sintáctico Redes] Falta el identificador destino en instrucción 'enlaza'"),
            new ErrorLexico("ES33", "Sintáctico",
                "[Sintáctico Redes] Falta ';' al finalizar la instrucción 'enlaza'"),
            new ErrorLexico("ES34", "Sintáctico",
                "[Sintáctico Redes] Verbo incorrecto — se esperaba 'enlaza'"),

            // ── SINTÁCTICOS — ConceptualParser.java ──────────────────────────
            new ErrorLexico("ES35", "Sintáctico",
                "[Sintáctico Conceptual] Token inesperado al inicio de instrucción"),
            new ErrorLexico("ES36", "Sintáctico",
                "[Sintáctico Conceptual] Falta el nombre del nodo (concepto, categoria o propiedad)"),
            new ErrorLexico("ES37", "Sintáctico",
                "[Sintáctico Conceptual] Falta la descripción entre comillas"),
            new ErrorLexico("ES38", "Sintáctico",
                "[Sintáctico Conceptual] Falta ';' al final de la declaración del nodo"),
            new ErrorLexico("ES39", "Sintáctico",
                "[Sintáctico Conceptual] Falta el identificador destino en la instrucción de relación"),
            new ErrorLexico("ES40", "Sintáctico",
                "[Sintáctico Conceptual] Falta ';' al finalizar la instrucción de relación"),
            new ErrorLexico("ES41", "Sintáctico",
                "[Sintáctico Conceptual] Instrucción inválida — identificador sin verbo de relación reconocido"),
            new ErrorLexico("ES42", "Sintáctico",
                "[Sintáctico Conceptual] Verbo inválido — se esperaba 'agrupa', 'asocia' o 'depende'"),

            // ── SINTÁCTICOS — UMLParser.java ─────────────────────────────────
            new ErrorLexico("ES43", "Sintáctico",
                "[Sintáctico UML] Token inesperado al inicio de instrucción"),
            new ErrorLexico("ES44", "Sintáctico",
                "[Sintáctico UML] Falta el nombre de la clase, interfaz o enum"),
            new ErrorLexico("ES45", "Sintáctico",
                "[Sintáctico UML] Falta '{' para abrir el cuerpo de la clase"),
            new ErrorLexico("ES46", "Sintáctico",
                "[Sintáctico UML] Se esperaba 'atributo' o 'metodo' dentro de la clase"),
            new ErrorLexico("ES47", "Sintáctico",
                "[Sintáctico UML] Falta el nombre del miembro (atributo o metodo)"),
            new ErrorLexico("ES48", "Sintáctico",
                "[Sintáctico UML] Falta ':' en la definición del miembro"),
            new ErrorLexico("ES49", "Sintáctico",
                "[Sintáctico UML] Falta el tipo del miembro"),
            new ErrorLexico("ES50", "Sintáctico",
                "[Sintáctico UML] Falta ';' al final del miembro o componente lineal"),
            new ErrorLexico("ES51", "Sintáctico",
                "[Sintáctico UML] Falta '}' para cerrar el cuerpo de la clase"),
            new ErrorLexico("ES52", "Sintáctico",
                "[Sintáctico UML] Falta el identificador destino en la instrucción de relación"),
            new ErrorLexico("ES53", "Sintáctico",
                "[Sintáctico UML] Falta ';' al finalizar la instrucción de relación"),
            new ErrorLexico("ES54", "Sintáctico",
                "[Sintáctico UML] Verbo inválido — se esperaba 'extiende', 'implementa' o 'usa'")
        );

        // --- Lista filtrada conectada al ComboBox ---
        FilteredList<ErrorLexico> datosFiltrados = new FilteredList<>(todos, e -> true);

        // --- ComboBox de categoría ---
        ComboBox<String> comboCategoria = new ComboBox<>();
        comboCategoria.getItems().addAll("Todos", "Errores Léxicos", "Errores Sintácticos");
        comboCategoria.setValue("Todos");
        comboCategoria.setStyle("-fx-font-size: 13px;");
        comboCategoria.setOnAction(e -> {
            String seleccion = comboCategoria.getValue();
            if ("Errores Léxicos".equals(seleccion)) {
                datosFiltrados.setPredicate(err -> "Léxico".equals(err.getCategoria()));
            } else if ("Errores Sintácticos".equals(seleccion)) {
                datosFiltrados.setPredicate(err -> "Sintáctico".equals(err.getCategoria()));
            } else {
                datosFiltrados.setPredicate(err -> true);
            }
        });

        Label lblCombo = new Label("Filtrar por tipo:");
        lblCombo.setStyle("-fx-font-weight: bold;");
        HBox barraFiltro = new HBox(8, lblCombo, comboCategoria);
        barraFiltro.setAlignment(Pos.CENTER_LEFT);

        // --- Columnas ---
        TableColumn<ErrorLexico, String> colCodigo = new TableColumn<>("Tipo de Error");
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colCodigo.setPrefWidth(100);

        TableColumn<ErrorLexico, String> colDescripcion = new TableColumn<>("Descripción");
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colDescripcion.setPrefWidth(500);

        TableView<ErrorLexico> tabla = new TableView<>(datosFiltrados);
        tabla.getColumns().addAll(colCodigo, colDescripcion);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setEditable(false);

        // --- Título ---
        Label lblTitulo = new Label("Catálogo de Errores — Diagrams As Code");
        lblTitulo.setFont(Font.font("System", FontWeight.BOLD, 14));
        lblTitulo.setStyle("-fx-text-fill: #c0392b;");

        VBox contenido = new VBox(10, lblTitulo, barraFiltro, tabla);
        contenido.setPadding(new Insets(12));
        VBox.setVgrow(tabla, Priority.ALWAYS);

        Tab tab = new Tab("Catálogo de Errores", contenido);
        panelPestanas.getTabs().add(tab);
        panelPestanas.getSelectionModel().select(tab);
    }

    // Modelo para la tabla de errores
    public static class ErrorLexico {
        private final String codigo;
        private final String categoria;
        private final String descripcion;

        public ErrorLexico(String codigo, String categoria, String descripcion) {
            this.codigo      = codigo;
            this.categoria   = categoria;
            this.descripcion = descripcion;
        }

        public String getCodigo()      { return codigo; }
        public String getCategoria()   { return categoria; }
        public String getDescripcion() { return descripcion; }
    }
}