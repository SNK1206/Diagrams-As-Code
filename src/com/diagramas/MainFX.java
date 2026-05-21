package com.diagramas;

import com.diagramas.core.*;
import com.diagramas.modulos.flujo.ast.*;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainFX extends Application {

    private TextArea txtEditor;
    private TextArea txtConsola;
    private Pane lienzoDibujo;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("⚡ IDE Pedagógico: Diagrams as Code");

        // --- 1. BARRA DE HERRAMIENTAS (TOP) ---
        HBox toolbar = new HBox(15);
        toolbar.setPadding(new Insets(10));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: #2c3e50;");

        Label lblTitulo = new Label("Diagrams As Code");
        lblTitulo.setTextFill(Color.WHITE);
        lblTitulo.setFont(Font.font("System", FontWeight.BOLD, 16));

        Button btnCompilar = new Button("⚡ Compilar y Renderizar");
        btnCompilar.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        btnCompilar.setOnAction(e -> ejecutarCompiladorYRenderizado());

        toolbar.getChildren().addAll(lblTitulo, btnCompilar);

        // --- 2. EDITOR DE CÓDIGO (CENTRO - IZQUIERDA) ---
        VBox panelEditor = new VBox(5);
        panelEditor.setPadding(new Insets(10));
        Label lblEditor = new Label("Editor de Código (.dac):");
        lblEditor.setStyle("-fx-font-weight: bold;");
        txtEditor = new TextArea();
        txtEditor.setFont(Font.font("Monospaced", 14));
        // Código inicial de ejemplo para facilitar las pruebas del estudiante
        txtEditor.setText("diagrama Flujo;\ninicio Comenzar;\nnodo LeerDatos \"Leer credenciales\";\nnodo Validar \"Verificar base de datos\";\nComenzar conecta LeerDatos;\nLeerDatos conecta Validar;");
        VBox.setVgrow(txtEditor, Priority.ALWAYS);
        panelEditor.getChildren().addAll(lblEditor, txtEditor);

        // --- 3. LIENZO GRÁFICO (CENTRO - DERECHA) ---
        VBox panelLienzo = new VBox(5);
        panelLienzo.setPadding(new Insets(10));
        Label lblLienzo = new Label("Renderizado del Diagrama:");
        lblLienzo.setStyle("-fx-font-weight: bold;");

        lienzoDibujo = new Pane();
        lienzoDibujo.setStyle("-fx-background-color: #ffffff; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");

        ScrollPane scrollLienzo = new ScrollPane(lienzoDibujo);
        scrollLienzo.setFitToWidth(true);
        scrollLienzo.setFitToHeight(true);
        VBox.setVgrow(scrollLienzo, Priority.ALWAYS);
        panelLienzo.getChildren().addAll(lblLienzo, scrollLienzo);

        // Dividir editor y lienzo simétricamente
        SplitPane splitCentro = new SplitPane(panelEditor, panelLienzo);
        splitCentro.setDividerPositions(0.4f);

        // --- 4. CONSOLA PEDAGÓGICA (BOTTOM) ---
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

        Scene scene = new Scene(root, 1000, 650);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void ejecutarCompiladorYRenderizado() {
        // Limpiar pantallas previas
        lienzoDibujo.getChildren().clear();
        txtConsola.clear();

        String codigoFuente = txtEditor.getText();

        ManejadorErrores manejadorErrores = new ManejadorErrores();
        TablaSimbolos tablaSimbolos = new TablaSimbolos();

        // 1. FASE LÉXICA
        LexerBase lexer = new LexerBase(codigoFuente, manejadorErrores);
        List<Token> tokens = lexer.tokenizar();

        if (manejadorErrores.tieneErrores()) {
            txtConsola.setText("🛑 PROCESO DETENIDO POR ERRORES LÉXICOS:\n\n" + obtenerErroresString(manejadorErrores));
            return;
        }

        // 2. FASE SINTÁCTICA Y SEMÁNTICA (Core + Módulo)
        ParserBase parserBase = new ParserBase(tokens, tablaSimbolos, manejadorErrores);
        Object astRoot = parserBase.analizarCabecera();

        if (manejadorErrores.tieneErrores() || astRoot == null) {
            txtConsola.setText("❌ COMPILACIÓN FALLIDA:\n\n" + obtenerErroresString(manejadorErrores));
            return;
        }

        // 3. GENERACIÓN GRÁFICA DESDE EL AST
        txtConsola.setText("✅ COMPILACIÓN EXITOSA: Estructura base y semántica validadas sin anomalías.\nGenerando vista gráfica desde el AST...");

        if (astRoot instanceof RaizFlujoAST) {
            graficarDiagramaFlujo((RaizFlujoAST) astRoot);
        }
    }

    private void graficarDiagramaFlujo(RaizFlujoAST ast) {
        // Mapa para guardar las coordenadas del centro de cada componente dibujado
        Map<String, double[]> posicionesComponentes = new HashMap<>();

        double xInicial = 200;
        double yInicial = 50;
        double espaciadoVertical = 100;

        // Primer pasada: Dibujar las figuras geométricas (Entidades del AST)
        for (NodeAST elemento : ast.getElementos()) {
            if (elemento instanceof NodoInicio) {
                NodoInicio nodo = (NodoInicio) elemento;

                // Un inicio se representa con una elipse estilizada
                Circle circulo = new Circle(xInicial, yInicial, 25);
                circulo.setFill(Color.web("#e74c3c"));
                circulo.setStroke(Color.web("#c0392b"));
                circulo.setStrokeWidth(2);

                Text texto = new Text(nodo.getId());
                texto.setFont(Font.font("System", FontWeight.BOLD, 12));
                texto.setFill(Color.WHITE);
                // Centrar texto
                texto.setX(xInicial - (texto.getLayoutBounds().getWidth() / 2));
                texto.setY(yInicial + 4);

                lienzoDibujo.getChildren().addAll(circulo, texto);
                posicionesComponentes.put(nodo.getId(), new double[]{xInicial, yInicial});
                yInicial += espaciadoVertical;

            } else if (elemento instanceof NodoProceso) {
                NodoProceso nodo = (NodoProceso) elemento;

                // Un proceso se representa con un rectángulo estándar de flujo
                double ancho = 160;
                double alto = 45;
                double rx = xInicial - (ancho / 2);
                double ry = yInicial - (alto / 2);

                Rectangle rect = new Rectangle(rx, ry, ancho, alto);
                rect.setFill(Color.web("#3498db"));
                rect.setStroke(Color.web("#2980b9"));
                rect.setStrokeWidth(2);
                rect.setArcWidth(10);
                rect.setArcHeight(10);

                Text tId = new Text(nodo.getId());
                tId.setFont(Font.font("System", FontWeight.BOLD, 11));
                tId.setX(xInicial - (tId.getLayoutBounds().getWidth() / 2));
                tId.setY(yInicial - 4);

                Text tDesc = new Text("\"" + nodo.getDescripcion() + "\"");
                tDesc.setFont(Font.font("System", 10));
                tDesc.setX(xInicial - (tDesc.getLayoutBounds().getWidth() / 2));
                tDesc.setY(yInicial + 12);

                lienzoDibujo.getChildren().addAll(rect, tId, tDesc);
                posicionesComponentes.put(nodo.getId(), new double[]{xInicial, yInicial});
                yInicial += espaciadoVertical;
            }
        }

        // Segunda pasada: Dibujar los enlaces y flechas conectores desde el AST
        for (NodeAST elemento : ast.getElementos()) {
            if (elemento instanceof NodoConexion) {
                NodoConexion conexion = (NodoConexion) elemento;

                double[] origenCoord = posicionesComponentes.get(conexion.getOrigen());
                double[] destinoCoord = posicionesComponentes.get(conexion.getDestino());

                if (origenCoord != null && destinoCoord != null) {
                    // Dibujar línea de interconexión (Ajustando los límites de los componentes)
                    double x1 = origenCoord[0];
                    double y1 = origenCoord[1] + 25; // Sale del fondo del nodo anterior
                    double x2 = destinoCoord[0];
                    double y2 = destinoCoord[1] - 22; // Entra por el tope del siguiente

                    Line linea = new Line(x1, y1, x2, y2);
                    linea.setStrokeWidth(2);
                    linea.setStroke(Color.web("#7f8c8d"));

                    // Punta de la flecha (Triángulo apuntando hacia abajo)
                    Polygon flecha = new Polygon();
                    flecha.getPoints().addAll(new Double[]{
                            x2, y2,
                            x2 - 6, y2 - 8,
                            x2 + 6, y2 - 8
                    });
                    flecha.setFill(Color.web("#7f8c8d"));

                    lienzoDibujo.getChildren().addAll(linea, flecha);
                }
            }
        }
    }

    // Metodo auxiliar para transformar los errores impresos en texto de interfaz
    private String obtenerErroresString(ManejadorErrores manejador) {
        // Redirigimos momentáneamente la salida de impresión a un capturador
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.PrintStream ps = new java.io.PrintStream(baos);
        java.io.PrintStream viejoErr = System.err;
        System.setErr(ps);

        manejador.imprimirErrores();

        System.setErr(viejoErr);
        return baos.toString();
    }
}