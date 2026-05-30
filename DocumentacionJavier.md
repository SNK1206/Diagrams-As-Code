# Documentación de Cambios al Código — Diagrams As Code
### Qué se modificó y qué se integró en `MainFX.java`

**Proyecto:** Diagrams As Code — IDE Pedagógico  
**Archivo principal modificado:** `src/com/diagramas/MainFX.java`  
**Archivos nuevos creados:** `ejecutar.ps1`  
**Autor:** Javier Chávez Arias  
**Fecha:** 2026-05-30

---

## Índice

1. [Módulo 1 — Catálogo de Errores](#módulo-1--catálogo-de-errores-botón-errores-léxicos)
2. [Módulo 2 — Árbol de Derivación](#módulo-2--árbol-de-derivación-botón-árbol-de-derivación)

---

## Módulo 1 — Catálogo de Errores (Botón "Errores Léxicos")

### 1.1 Imports integrados

Se agregaron las siguientes líneas al bloque de imports de `MainFX.java`:

```java
// INTEGRADO — necesarios para la TableView filtrable
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.cell.PropertyValueFactory;
```

**`FXCollections`** — crea la `ObservableList` que alimenta la tabla.  
**`ObservableList`** — lista que notifica automáticamente a la `TableView` cuando cambia.  
**`FilteredList`** — envuelve la `ObservableList` y aplica un predicado de filtrado cuando el usuario cambia el ComboBox, sin recargar los datos.  
**`PropertyValueFactory`** — enlaza cada columna de la `TableView` con el getter correspondiente de la clase `ErrorLexico` usando reflexión (por eso los métodos se llaman `getCodigo()`, `getCategoria()`, `getDescripcion()`).

---

### 1.2 Modificación en `start()` — barra de herramientas

**Código original (antes de la integración):**
```java
toolbar.getChildren().addAll(lblTitulo, btnNuevo, btnAbrir, btnGuardar, btnCompilar);
```

**Código modificado (después de la integración):**
```java
// --- INTEGRADO: botón rojo "Errores Léxicos" ---
Button btnErroresLexicos = new Button("Errores Léxicos");
btnErroresLexicos.setStyle(
    "-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold;");
btnErroresLexicos.setOnAction(e -> mostrarTablaErroresLexicos());

toolbar.getChildren().addAll(
    lblTitulo, btnNuevo, btnAbrir, btnGuardar, btnCompilar, btnErroresLexicos);
```

Se creó un `Button` nuevo con color rojo `#c0392b` y se agregó al `HBox` de la barra de herramientas. El evento llama a `mostrarTablaErroresLexicos()`.

---

### 1.3 Método integrado: `mostrarTablaErroresLexicos()`

Método **completamente nuevo**, agregado al final de la clase antes del cierre `}`.

```java
private void mostrarTablaErroresLexicos() {

    // BLOQUE 1: Evita abrir la pestaña duplicada
    for (Tab t : panelPestanas.getTabs()) {
        if ("Catálogo de Errores".equals(t.getText())) {
            panelPestanas.getSelectionModel().select(t);
            return;
        }
    }

    // BLOQUE 2: Lista observable con los 36 errores del compilador
    ObservableList<ErrorLexico> todos = FXCollections.observableArrayList(
        new ErrorLexico("EL01", "Léxico",
            "El carácter encontrado no pertenece al alfabeto del lenguaje DAC"),
        new ErrorLexico("EL02", "Léxico",
            "Cadena de texto sin cerrar — falta comilla doble de cierre"),
        new ErrorLexico("ES01", "Sintáctico",
            "[Sintáctico Núcleo] Falta ';' al final de la instrucción..."),
        // ... 33 errores más (ES02 a ES34)
    );

    // BLOQUE 3: Lista filtrada enlazada al ComboBox
    FilteredList<ErrorLexico> datosFiltrados = new FilteredList<>(todos, e -> true);

    // BLOQUE 4: ComboBox con las tres categorías de filtro
    ComboBox<String> comboCategoria = new ComboBox<>();
    comboCategoria.getItems().addAll("Todos", "Errores Léxicos", "Errores Sintácticos");
    comboCategoria.setValue("Todos");
    comboCategoria.setOnAction(e -> {
        String sel = comboCategoria.getValue();
        if ("Errores Léxicos".equals(sel))
            datosFiltrados.setPredicate(err -> "Léxico".equals(err.getCategoria()));
        else if ("Errores Sintácticos".equals(sel))
            datosFiltrados.setPredicate(err -> "Sintáctico".equals(err.getCategoria()));
        else
            datosFiltrados.setPredicate(err -> true);
    });

    // BLOQUE 5: Columnas de la tabla
    TableColumn<ErrorLexico, String> colCodigo = new TableColumn<>("Tipo de Error");
    colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
    colCodigo.setPrefWidth(120);

    TableColumn<ErrorLexico, String> colDescripcion = new TableColumn<>("Descripción");
    colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
    colDescripcion.setPrefWidth(500);

    // BLOQUE 6: TableView enlazada a la lista filtrada
    TableView<ErrorLexico> tabla = new TableView<>(datosFiltrados);
    tabla.getColumns().addAll(colCodigo, colDescripcion);
    tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    tabla.setEditable(false);

    // BLOQUE 7: Barra de filtro (label + ComboBox)
    Label lblCombo = new Label("Filtrar por tipo:");
    lblCombo.setStyle("-fx-font-weight: bold;");
    HBox barraFiltro = new HBox(8, lblCombo, comboCategoria);
    barraFiltro.setAlignment(Pos.CENTER_LEFT);

    // BLOQUE 8: Título de la pestaña
    Label lblTitulo = new Label("Catálogo de Errores — Diagrams As Code");
    lblTitulo.setFont(Font.font("System", FontWeight.BOLD, 14));
    lblTitulo.setStyle("-fx-text-fill: #c0392b;");

    // BLOQUE 9: Composición del contenido de la pestaña
    VBox contenido = new VBox(10, lblTitulo, barraFiltro, tabla);
    contenido.setPadding(new Insets(12));
    VBox.setVgrow(tabla, Priority.ALWAYS);

    // BLOQUE 10: Crear y seleccionar la pestaña
    Tab tab = new Tab("Catálogo de Errores", contenido);
    panelPestanas.getTabs().add(tab);
    panelPestanas.getSelectionModel().select(tab);
}
```

---

### 1.4 Clase interna integrada: `ErrorLexico`

Clase **completamente nueva**, agregada al final de `MainFX.java` (antes del `}` de cierre de la clase).

```java
// INTEGRADO: modelo de datos para la TableView del catálogo
public static class ErrorLexico {
    private final String codigo;
    private final String categoria;
    private final String descripcion;

    public ErrorLexico(String codigo, String categoria, String descripcion) {
        this.codigo      = codigo;
        this.categoria   = categoria;
        this.descripcion = descripcion;
    }

    // Getters requeridos por PropertyValueFactory (reflexión de JavaFX)
    public String getCodigo()      { return codigo; }
    public String getCategoria()   { return categoria; }
    public String getDescripcion() { return descripcion; }
}
```

---

## Módulo 2 — Árbol de Derivación (Botón "Árbol de Derivación")

### 2.1 Imports integrados

```java
// INTEGRADO — shapes para dibujar el árbol visualmente
import javafx.scene.shape.CubicCurve;   // reservado (no usado en versión final)
import javafx.scene.shape.Ellipse;      // óvalos para nodos no-terminales
import javafx.scene.shape.Line;         // líneas entre nodos padre e hijo
import javafx.scene.shape.Rectangle;    // rectángulos para nodos terminales
import javafx.scene.text.TextAlignment; // centrado de texto dentro de los nodos

// INTEGRADO — colecciones necesarias para el algoritmo de layout
import java.util.ArrayList;
import java.util.HashMap;
```

---

### 2.2 Modificación en `start()` — barra de herramientas

**Código antes:**
```java
toolbar.getChildren().addAll(
    lblTitulo, btnNuevo, btnAbrir, btnGuardar, btnCompilar, btnErroresLexicos);
```

**Código después:**
```java
// INTEGRADO: botón verde-azul "Árbol de Derivación"
Button btnArbol = new Button("Árbol de Derivación");
btnArbol.setStyle(
    "-fx-background-color: #16a085; -fx-text-fill: white; -fx-font-weight: bold;");
btnArbol.setOnAction(e -> mostrarArbolDerivacion());

toolbar.getChildren().addAll(
    lblTitulo, btnNuevo, btnAbrir, btnGuardar,
    btnCompilar, btnErroresLexicos, btnArbol);  // <-- btnArbol agregado al final
```

---

### 2.3 Constantes integradas para el layout visual

Agregadas como campos `private static final` en la clase `MainFX`, antes de los métodos:

```java
// INTEGRADO — dimensiones naturales de los nodos (antes de aplicar escala)
private static final double RX_N  = 52;  // radio X del óvalo (no-terminal)
private static final double RY_N  = 14;  // radio Y del óvalo (no-terminal)
private static final double TW_N  = 50;  // ancho del rectángulo (terminal)
private static final double TH_N  = 17;  // alto del rectángulo (terminal)
private static final double HGAP  = 6;   // espacio horizontal mínimo entre hermanos
private static final double VGAP  = 58;  // espacio vertical entre niveles
private static final double PAD   = 36;  // margen exterior del canvas
```

---

### 2.4 Métodos helpers integrados (acceso a tamaños por nodo)

```java
// INTEGRADO — devuelven el ancho/alto natural de un nodo según si es terminal
private double nw(TreeItem<String> n) {
    return isTerminal(n.getValue()) ? TW_N : RX_N * 2;
}
private double nh(TreeItem<String> n) {
    return isTerminal(n.getValue()) ? TH_N : RY_N * 2;
}
// Detecta si un nodo es terminal por la presencia del símbolo '→' en su etiqueta
private boolean isTerminal(String label) {
    return label.contains("→");
}
```

---

### 2.5 Método integrado: `mostrarArbolDerivacion()`

Método **completamente nuevo**. Orquesta el proceso completo: obtiene el código, ejecuta el lexer, construye el árbol lógico, calcula el layout y monta la UI con controles de zoom.

```java
private void mostrarArbolDerivacion() {

    // BLOQUE 1: Validar que hay una pestaña activa con contenido
    Tab pestañaActiva = panelPestanas.getSelectionModel().getSelectedItem();
    if (pestañaActiva == null) { ... return; }
    TextArea editor = (TextArea) pestañaActiva.getContent();
    String codigo = editor.getText();

    // BLOQUE 2: Ejecutar el Lexer del compilador sobre el código activo
    ManejadorErrores errores = new ManejadorErrores();
    LexerBase lexer = new LexerBase(codigo, errores);
    List<Token> tokens = lexer.tokenizar();
    if (errores.tieneErrores()) { ... return; }  // Detener si hay errores léxicos

    // BLOQUE 3: Detectar el módulo (flujo, bd, redes)
    String modulo = "desconocido";
    for (int i = 0; i < tokens.size() - 1; i++) {
        if (tokens.get(i).getTipo() == Token.Tipo.PR_DIAGRAMA) {
            modulo = tokens.get(i + 1).getLexema().toLowerCase();
            break;
        }
    }

    // BLOQUE 4: Construir el árbol lógico (TreeItem<String>)
    final TreeItem<String> raiz = new TreeItem<>("programa_" + modulo);
    // Separar tokens en grupos por instrucción (';') respetando profundidad de '{}'
    List<List<Token>> instrucciones = new ArrayList<>();
    List<Token> actual = new ArrayList<>();
    int prof = 0;
    for (Token t : tokens) {
        if (t.getTipo() == Token.Tipo.EOF) break;
        if (t.getTipo() == Token.Tipo.LLAVE_IZQ) prof++;
        if (t.getTipo() == Token.Tipo.LLAVE_DER) prof--;
        actual.add(t);
        if (t.getTipo() == Token.Tipo.PUNTO_Y_COMA && prof == 0) {
            instrucciones.add(new ArrayList<>(actual));
            actual.clear();
        }
    }
    for (List<Token> grupo : instrucciones) {
        TreeItem<String> n = construirNodoRegla(grupo, modulo);
        if (n != null) raiz.getChildren().add(n);
    }

    // BLOQUE 5: Calcular layout natural UNA SOLA VEZ
    final Map<TreeItem<String>, double[]> natPos = new HashMap<>();
    layoutTree(raiz, PAD + subtreeW(raiz) / 2.0, PAD, natPos);
    final double rawW = maxX(raiz, natPos) + PAD;
    final double rawH = maxY(raiz, natPos) + PAD;
    // Escala automática para que el árbol quepa en 920×500 px
    final double autoS = Math.min(1.0, Math.min(920.0 / rawW, 500.0 / rawH));

    // BLOQUE 6: Estado de zoom compartido entre los botones (array mutable en lambda)
    final double[] zoomS = { autoS };
    final ScrollPane scroll = buildScroll(renderCanvas(raiz, natPos, rawW, rawH, zoomS[0]));

    // BLOQUE 7: Controles de zoom
    Label  lblZoom    = new Label(pct(zoomS[0], autoS));
    Button btnZoomOut = new Button("  −  ");
    Button btnZoomIn  = new Button("  +  ");
    Button btnFit     = new Button("Ajustar");

    // Zoom in: aumenta 25% por clic, máximo 8× el autoS
    btnZoomIn.setOnAction(ev -> {
        zoomS[0] = Math.min(zoomS[0] * 1.25, autoS * 8.0);
        lblZoom.setText(pct(zoomS[0], autoS));
        scroll.setContent(renderCanvas(raiz, natPos, rawW, rawH, zoomS[0]));
    });
    // Zoom out: reduce 20% por clic, mínimo 30% del autoS
    btnZoomOut.setOnAction(ev -> {
        zoomS[0] = Math.max(zoomS[0] * 0.8, autoS * 0.3);
        lblZoom.setText(pct(zoomS[0], autoS));
        scroll.setContent(renderCanvas(raiz, natPos, rawW, rawH, zoomS[0]));
    });
    // Ajustar: vuelve al autoS inicial
    btnFit.setOnAction(ev -> {
        zoomS[0] = autoS;
        lblZoom.setText(pct(zoomS[0], autoS));
        scroll.setContent(renderCanvas(raiz, natPos, rawW, rawH, zoomS[0]));
    });

    // BLOQUE 8: Botón Pantalla Completa con sus propios controles de zoom
    Button btnPantallaCompleta = new Button("⛶  Pantalla Completa");
    btnPantallaCompleta.setOnAction(ev -> {
        double fsAutoS = Math.min(2.0, Math.min(1900.0 / rawW, 1000.0 / rawH));
        double[] fsZoom = { fsAutoS };
        final ScrollPane fsScroll = buildScroll(
            renderCanvas(raiz, natPos, rawW, rawH, fsZoom[0]));
        // Controles de zoom independientes para la ventana completa
        Label fsLblZoom = new Label(pct(fsZoom[0], fsAutoS));
        Button fsBtnOut = new Button("  −  ");
        Button fsBtnIn  = new Button("  +  ");
        Button fsBtnFit = new Button("Ajustar");
        // ... misma lógica de zoom pero con fsZoom y fsScroll
        Stage stage = new Stage();
        stage.setMaximized(true);
        stage.show();
    });

    // BLOQUE 9: Composición de la UI de la pestaña
    HBox barraZoom = new HBox(4, btnZoomOut, lblZoom, btnZoomIn, btnFit);
    HBox barraTop  = new HBox(10, titulo, barraZoom, btnPantallaCompleta);
    VBox contenido = new VBox(5, barraTop, leyenda, scroll);
    VBox.setVgrow(scroll, Priority.ALWAYS);

    // BLOQUE 10: Crear pestaña (o actualizarla si ya existe)
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
```

---

### 2.6 Método integrado: `construirNodoRegla()`

Método **completamente nuevo**. Recibe un grupo de tokens (una instrucción) y devuelve un `TreeItem<String>` etiquetado con el nombre de la regla gramatical que representa.

```java
private TreeItem<String> construirNodoRegla(List<Token> grupo, String modulo) {
    if (grupo.isEmpty()) return null;
    Token primero = grupo.get(0);
    String lex = primero.getLexema();

    // Cabecera: "diagrama Flujo ;"
    if (primero.getTipo() == Token.Tipo.PR_DIAGRAMA) {
        TreeItem<String> nodo = new TreeItem<>("cabecera");
        for (Token t : grupo) nodo.getChildren().add(tokenItem(t));
        return nodo;
    }

    // Meta-instrucciones: "autor '...'; version '...';"
    if (lex.equals("autor") || lex.equals("version") || lex.equals("tema")
        || lex.equals("exportar") || lex.equals("importar")) {
        TreeItem<String> nodo = new TreeItem<>("meta_instruccion");
        for (Token t : grupo) nodo.getChildren().add(tokenItem(t));
        return nodo;
    }

    // --- Módulo FLUJO ---
    if (modulo.equals("flujo")) {
        if (lex.equals("inicio") || lex.equals("fin")) {
            // "inicio Arrancar ;" → decl_inicio
            TreeItem<String> nodo = new TreeItem<>("decl_" + lex);
            for (Token t : grupo) nodo.getChildren().add(tokenItem(t));
            return nodo;
        }
        if (lex.equals("nodo") || lex.equals("condicion") || lex.equals("bucle")
            || lex.equals("subproceso") || lex.equals("entrada")
            || lex.equals("salida") || lex.equals("parada")) {
            // "nodo A 'desc' ;" → decl_nodo_nodo
            TreeItem<String> nodo = new TreeItem<>("decl_nodo_" + lex);
            for (Token t : grupo) nodo.getChildren().add(tokenItem(t));
            return nodo;
        }
        // Busca el verbo 'conecta' → instruccion_conexion
        for (Token t : grupo) {
            if (t.getLexema().equals("conecta")) {
                TreeItem<String> nodo = new TreeItem<>("instruccion_conexion");
                for (Token tk : grupo) nodo.getChildren().add(tokenItem(tk));
                return nodo;
            }
        }
    }

    // --- Módulo BD ---
    if (modulo.equals("bd")) {
        if (lex.equals("tabla") || lex.equals("vista")
            || lex.equals("esquema") || lex.equals("paquete")) {
            // Bloque con {} → agrupa nombre + nodo "atributos" con hijos decl_atributo
            TreeItem<String> nodo = new TreeItem<>("bloque_" + lex);
            // Agrega nombre de la tabla
            if (grupo.size() > 1) nodo.getChildren().add(tokenItem(grupo.get(1)));
            // Agrupa los atributos dentro de {}
            TreeItem<String> bloque = new TreeItem<>("atributos");
            boolean dentro = false;
            List<Token> atribActual = new ArrayList<>();
            for (int i = 2; i < grupo.size(); i++) {
                Token t = grupo.get(i);
                if (t.getTipo() == Token.Tipo.LLAVE_IZQ) { dentro = true; continue; }
                if (t.getTipo() == Token.Tipo.LLAVE_DER) { dentro = false; continue; }
                if (dentro) {
                    atribActual.add(t);
                    if (t.getTipo() == Token.Tipo.PUNTO_Y_COMA) {
                        TreeItem<String> atrib = new TreeItem<>("decl_atributo");
                        for (Token at : atribActual) atrib.getChildren().add(tokenItem(at));
                        bloque.getChildren().add(atrib);
                        atribActual.clear();
                    }
                }
            }
            nodo.getChildren().add(bloque);
            return nodo;
        }
        if (lex.equals("procedimiento") || lex.equals("funcion") || lex.equals("indice")
            || lex.equals("disparador") || lex.equals("secuencia")) {
            TreeItem<String> nodo = new TreeItem<>("decl_" + lex);
            for (Token t : grupo) nodo.getChildren().add(tokenItem(t));
            return nodo;
        }
        // Busca 'relaciona' → instruccion_relacion
        for (Token t : grupo) {
            if (t.getLexema().equals("relaciona")) {
                TreeItem<String> nodo = new TreeItem<>("instruccion_relacion");
                for (Token tk : grupo) nodo.getChildren().add(tokenItem(tk));
                return nodo;
            }
        }
    }

    // --- Módulo REDES ---
    if (modulo.equals("redes")) {
        if (lex.equals("dispositivo") || lex.equals("nube") || lex.equals("vlan")
            || lex.equals("subred") || lex.equals("cluster") || lex.equals("tunel")
            || lex.equals("zona") || lex.equals("puerto") || lex.equals("politica")) {
            // dispositivo Nombre Tipo { Prop: "val"; };
            TreeItem<String> nodo = new TreeItem<>("decl_dispositivo");
            // Agrega cabecera (nombre y tipo) y bloque de propiedades
            // ... lógica similar a BD para {} internos
            return nodo;
        }
        // Busca 'enlaza' → instruccion_enlace
        for (Token t : grupo) {
            if (t.getLexema().equals("enlaza")) {
                TreeItem<String> nodo = new TreeItem<>("instruccion_enlace");
                for (Token tk : grupo) nodo.getChildren().add(tokenItem(tk));
                return nodo;
            }
        }
    }
    // Si no reconoció la instrucción
    TreeItem<String> nodo = new TreeItem<>("instruccion_desconocida");
    for (Token t : grupo) nodo.getChildren().add(tokenItem(t));
    return nodo;
}
```

---

### 2.7 Método integrado: `tokenItem()`

Método **completamente nuevo**. Crea el nodo hoja del árbol para un token terminal.

```java
private TreeItem<String> tokenItem(Token t) {
    // Formato con '→' para que isTerminal() lo reconozca
    String etiqueta = t.getTipo().name() + "  →  '"
                    + t.getLexema() + "'  [Línea " + t.getLinea() + "]";
    return new TreeItem<>(etiqueta);
}
```

---

### 2.8 Método integrado: `pct()`

Método **completamente nuevo**. Calcula el porcentaje de zoom relativo al autoS.

```java
private String pct(double current, double base) {
    return Math.round(current / base * 100) + "%";
}
```

---

### 2.9 Método integrado: `renderCanvas()`

Método **completamente nuevo**. Dibuja el árbol en un `Pane` a una escala dada, multiplicando todas las posiciones por `s` antes de dibujar.

```java
private Pane renderCanvas(TreeItem<String> raiz,
                           Map<TreeItem<String>, double[]> nat,
                           double rawW, double rawH, double s) {
    // Crear mapa de posiciones escaladas
    Map<TreeItem<String>, double[]> pos = new HashMap<>();
    for (var e : nat.entrySet())
        pos.put(e.getKey(),
            new double[]{ e.getValue()[0] * s, e.getValue()[1] * s });

    Pane canvas = new Pane();
    canvas.setStyle("-fx-background-color: white;");
    canvas.setPrefSize(rawW * s, rawH * s);
    canvas.setMaxSize(rawW * s, rawH * s);

    paintEdges(raiz, pos, canvas, s);  // primero líneas (detrás)
    paintNodes(raiz, pos, canvas, s);  // luego nodos (encima)
    return canvas;
}
```

---

### 2.10 Métodos integrados: `buildCanvas()` y `buildScroll()`

Métodos **completamente nuevos**. Calculan el auto-escala y configuran el `ScrollPane`.

```java
// buildCanvas sin argumentos: usa 920×500 como área objetivo
private Pane buildCanvas(TreeItem<String> raiz) {
    return buildCanvas(raiz, 920, 500);
}

// buildCanvas con área objetivo: calcula scale y llama a renderCanvas
private Pane buildCanvas(TreeItem<String> raiz, double targetW, double targetH) {
    Map<TreeItem<String>, double[]> nat = new HashMap<>();
    layoutTree(raiz, PAD + subtreeW(raiz) / 2.0, PAD, nat);
    double rawW = maxX(raiz, nat) + PAD;
    double rawH = maxY(raiz, nat) + PAD;
    double s = Math.min(1.0, Math.min(targetW / rawW, targetH / rawH));
    return renderCanvas(raiz, nat, rawW, rawH, s);
}

// buildScroll: configura ScrollPane con pannable y sin estirar contenido
private ScrollPane buildScroll(Pane canvas) {
    ScrollPane sc = new ScrollPane(canvas);
    sc.setPannable(true);
    sc.setFitToWidth(false);   // NO estirar — respetar el tamaño calculado
    sc.setFitToHeight(false);
    sc.setStyle("-fx-background: white; -fx-background-color: white;");
    return sc;
}
```

---

### 2.11 Método integrado: `subtreeW()`

Método **completamente nuevo**. Calcula el ancho mínimo del subárbol centrado en el nodo `n`.

```java
private double subtreeW(TreeItem<String> n) {
    if (n.getChildren().isEmpty()) return nw(n) + HGAP;
    double sum = n.getChildren().stream()
                   .mapToDouble(this::subtreeW).sum();
    return Math.max(nw(n) + HGAP, sum);
}
```

---

### 2.12 Método integrado: `layoutTree()`

Método **completamente nuevo**. Algoritmo de posicionamiento top-down que asigna el centro `(cx, cy)` a cada nodo.

```java
private void layoutTree(TreeItem<String> n, double cx, double topY,
                         Map<TreeItem<String>, double[]> pos) {
    // Guarda el CENTRO del nodo
    pos.put(n, new double[]{ cx, topY + nh(n) / 2.0 });
    if (n.getChildren().isEmpty()) return;

    // Distribuye los hijos horizontalmente
    double startX    = cx - subtreeW(n) / 2.0;
    double childTopY = topY + nh(n) + VGAP;

    for (TreeItem<String> h : n.getChildren()) {
        double w = subtreeW(h);
        layoutTree(h, startX + w / 2.0, childTopY, pos);
        startX += w;
    }
}
```

---

### 2.13 Métodos integrados: `maxX()` y `maxY()`

Métodos **completamente nuevos**. Calculan los límites del árbol para dimensionar el canvas correctamente.

```java
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
```

---

### 2.14 Método integrado: `paintEdges()`

Método **completamente nuevo**. Dibuja `Line` entre el centro-inferior de cada padre y el centro-superior de cada hijo.

```java
private void paintEdges(TreeItem<String> n,
                         Map<TreeItem<String>, double[]> pos,
                         Pane c, double s) {
    double[] pp  = pos.get(n);
    double   px  = pp[0];
    double   pyBot = pp[1] + nh(n) * s / 2.0;  // pie del padre

    for (TreeItem<String> h : n.getChildren()) {
        double[] ph   = pos.get(h);
        double   hyTop = ph[1] - nh(h) * s / 2.0; // cabeza del hijo

        Line l = new Line(px, pyBot, ph[0], hyTop);
        l.setStroke(Color.web("#333333"));
        l.setStrokeWidth(Math.max(0.5, s));
        c.getChildren().add(l);

        paintEdges(h, pos, c, s); // llamada recursiva
    }
}
```

---

### 2.15 Método integrado: `paintNodes()`

Método **completamente nuevo**. Dibuja `Ellipse` (no-terminal) o `Rectangle` (terminal) con texto centrado dentro de cada nodo.

```java
private void paintNodes(TreeItem<String> n,
                         Map<TreeItem<String>, double[]> pos,
                         Pane c, double s) {
    double[] p    = pos.get(n);
    String   lbl  = n.getValue();
    boolean  term = isTerminal(lbl);
    double   fs   = Math.max(7.5, 10.0 * s);  // fuente mínima legible: 7.5px

    if (term) {
        // TERMINAL → Rectángulo blanco con borde oscuro
        double tw = TW_N * s, th = TH_N * s;
        Rectangle r = new Rectangle(p[0] - tw/2.0, p[1] - th/2.0, tw, th);
        r.setFill(Color.WHITE);
        r.setStroke(Color.web("#333333"));
        r.setStrokeWidth(Math.max(0.5, s));
        c.getChildren().add(r);
        // Solo el lexema: 'inicio', ';', 'Flujo', etc.
        c.getChildren().add(mkText(extractLexema(lbl),
            p[0] - tw/2.0, p[1] + fs * 0.4, tw,
            Font.font("Monospaced", FontWeight.NORMAL, fs),
            Color.web("#111111")));
    } else {
        // NO-TERMINAL → Óvalo (Ellipse)
        double rx = RX_N * s, ry = RY_N * s;
        boolean root = lbl.startsWith("programa_");

        Ellipse oval = new Ellipse(p[0], p[1], rx, ry);
        oval.setFill(root ? Color.web("#1a252f") : Color.WHITE);
        oval.setStroke(root ? Color.BLACK : Color.web("#222222"));
        oval.setStrokeWidth(Math.max(0.6, 1.3 * s));
        c.getChildren().add(oval);
        // Nombre de la regla gramatical (abreviado)
        c.getChildren().add(mkText(shortLabel(lbl),
            p[0] - rx, p[1] + fs * 0.4, rx * 2,
            Font.font("System", FontWeight.BOLD, fs),
            root ? Color.WHITE : Color.web("#111111")));
    }
    for (TreeItem<String> h : n.getChildren()) paintNodes(h, pos, c, s);
}
```

---

### 2.16 Métodos auxiliares integrados: `mkText()`, `shortLabel()`, `extractLexema()`

Métodos **completamente nuevos**.

```java
// Crea un objeto Text centrado dentro de un nodo
private javafx.scene.text.Text mkText(String txt, double x, double y,
                                       double w, Font f, Color col) {
    javafx.scene.text.Text t = new javafx.scene.text.Text(txt);
    t.setFont(f);
    t.setFill(col);
    t.setTextAlignment(TextAlignment.CENTER);
    t.setWrappingWidth(w - 2);
    t.setX(x + 1);
    t.setY(y);
    return t;
}

// Abrevia etiquetas largas para que quepan dentro del óvalo
private String shortLabel(String lbl) {
    return lbl
        .replace("instruccion_", "inst_")
        .replace("programa_",    "prog_")
        .replace("declaracion_", "decl_")
        .replace("bloque_",      "blq_")
        .replace("meta_instruccion", "meta")
        .replace("propiedades",  "props");
}

// Extrae el lexema del formato interno "TIPO  →  'lexema'  [Línea N]"
private String extractLexema(String label) {
    int a = label.indexOf("'"), b = label.lastIndexOf("'");
    if (a >= 0 && b > a) return label.substring(a, b + 1);
    return label;
}
```

---

## Tabla Resumen de Cambios

### `MainFX.java` — líneas modificadas vs líneas originales

| Sección | Tipo de cambio | Descripción |
|---------|---------------|-------------|
| Imports (líneas 5–7, 12, 20–24, 30–31) | **INTEGRADO** | 9 imports nuevos para colecciones, shapes y texto |
| `start()` línea 82–90 | **MODIFICADO** | Se agregaron 2 botones nuevos y se actualizó `toolbar.getChildren().addAll()` |
| `mostrarTablaErroresLexicos()` | **INTEGRADO** | Método nuevo — ~80 líneas |
| `ErrorLexico` (clase interna) | **INTEGRADO** | Clase nueva — 15 líneas |
| `mostrarArbolDerivacion()` | **INTEGRADO** | Método nuevo — ~130 líneas |
| `construirNodoRegla()` | **INTEGRADO** | Método nuevo — ~95 líneas |
| `tokenItem()` | **INTEGRADO** | Método nuevo — 4 líneas |
| `pct()` | **INTEGRADO** | Método nuevo — 3 líneas |
| `renderCanvas()` | **INTEGRADO** | Método nuevo — 15 líneas |
| `buildCanvas()` x2 | **INTEGRADO** | Métodos nuevos — 12 líneas |
| `buildScroll()` | **INTEGRADO** | Método nuevo — 8 líneas |
| `subtreeW()` | **INTEGRADO** | Método nuevo — 5 líneas |
| `layoutTree()` | **INTEGRADO** | Método nuevo — 12 líneas |
| `maxX()` / `maxY()` | **INTEGRADO** | Métodos nuevos — 8 líneas |
| `paintEdges()` | **INTEGRADO** | Método nuevo — 14 líneas |
| `paintNodes()` | **INTEGRADO** | Método nuevo — 35 líneas |
| `mkText()` | **INTEGRADO** | Método nuevo — 9 líneas |
| `shortLabel()` | **INTEGRADO** | Método nuevo — 6 líneas |
| `extractLexema()` | **INTEGRADO** | Método nuevo — 4 líneas |
| `nw()` / `nh()` / `isTerminal()` | **INTEGRADO** | 3 one-liners auxiliares |
| Constantes `RX_N`, `RY_N`, etc. | **INTEGRADO** | 7 constantes `static final` |

### Archivos nuevos creados

| Archivo | Descripción |
|---------|-------------|
| `pruebas.md` | Documento con 25 casos de prueba y catálogo de 36 errores |
| `DocumentacionJavier.md` | Este documento |

---

*Documentación técnica del proyecto Diagrams As Code — Compilador pedagógico DAC.*
