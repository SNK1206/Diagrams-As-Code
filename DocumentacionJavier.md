# Documentación de Cambios al Código — Diagrams As Code
### Qué se modificó y qué se integró en `MainFX.java`

**Proyecto:** Diagrams As Code — IDE Pedagógico  
**Archivo principal modificado:** `src/com/diagramas/MainFX.java`  
**Archivos nuevos creados:** `ejecutar.ps1`, `pruebas.md`, `DocumentacionJavier.md`  
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
**`PropertyValueFactory`** — enlaza cada columna de la `TableView` con el getter correspondiente de la clase `ErrorLexico` usando reflexión.

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

Clase **completamente nueva**, agregada al final de `MainFX.java`.

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
    btnCompilar, btnErroresLexicos, btnArbol);
```

---

### 2.3 Constantes integradas para el layout visual

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

### 2.4 Métodos helpers integrados

```java
// Devuelven el ancho/alto natural de un nodo según si es terminal o no
private double nw(TreeItem<String> n) {
    return isTerminal(n.getValue()) ? TW_N : RX_N * 2;
}
private double nh(TreeItem<String> n) {
    return isTerminal(n.getValue()) ? TH_N : RY_N * 2;
}
// Detecta nodos terminales (tokens) por la presencia de '→' en su etiqueta
private boolean isTerminal(String label) {
    return label.contains("→");
}
```

---

### 2.5 Método integrado: `mostrarArbolDerivacion()`

Método **completamente nuevo**. Orquesta el proceso completo desde obtener el código hasta mostrar la pestaña.

```java
private void mostrarArbolDerivacion() {

    // BLOQUE 1: Validar que hay una pestaña activa
    Tab pestañaActiva = panelPestanas.getSelectionModel().getSelectedItem();
    if (pestañaActiva == null) { return; }
    TextArea editor = (TextArea) pestañaActiva.getContent();
    String codigo = editor.getText();

    // BLOQUE 2: Ejecutar el Lexer del compilador sobre el código activo
    ManejadorErrores errores = new ManejadorErrores();
    LexerBase lexer = new LexerBase(codigo, errores);
    List<Token> tokens = lexer.tokenizar();
    if (errores.tieneErrores()) { return; }

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
    // Agrupar tokens por instrucción (';') respetando profundidad de '{}'
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

    // BLOQUE 5: Calcular layout natural UNA SOLA VEZ (reutilizado por todos los zooms)
    final Map<TreeItem<String>, double[]> natPos = new HashMap<>();
    layoutTree(raiz, PAD + subtreeW(raiz) / 2.0, PAD, natPos);
    final double rawW = maxX(raiz, natPos) + PAD;
    final double rawH = maxY(raiz, natPos) + PAD;
    // Auto-escala: ajusta el árbol para que quepa en 920×500 px
    final double autoS = Math.min(1.0, Math.min(920.0 / rawW, 500.0 / rawH));

    // BLOQUE 6: Renderizar el canvas inicial y crear el ScrollPane
    final double[] zoomS = { autoS };
    final ScrollPane scroll = buildScroll(renderCanvas(raiz, natPos, rawW, rawH, zoomS[0]));

    // BLOQUE 7: Controles de zoom (ver sección 2.6)
    // BLOQUE 8: Botón Pantalla Completa (ver sección 2.7)
    // BLOQUE 9 y 10: Composición de la UI y creación de la pestaña
}
```

---

### 2.6 Cómo se crearon los botones de Zoom

Los botones de zoom se crean dentro de `mostrarArbolDerivacion()` (BLOQUE 7) después de calcular el layout. Son cuatro elementos visuales colocados en un `HBox`:

| Elemento | Tipo JavaFX | Función |
|----------|-------------|---------|
| `btnZoomOut` | `Button` "−" | Reduce escala 20% por clic |
| `lblZoom` | `Label` | Muestra el porcentaje actual |
| `btnZoomIn` | `Button` "+" | Aumenta escala 25% por clic |
| `btnFit` | `Button` "Ajustar" | Restablece al auto-ajuste inicial |

**Código integrado:**
```java
// Estado de zoom: array mutable para ser capturado por las lambdas
final double[] zoomS = { autoS };

Label  lblZoom    = new Label(pct(zoomS[0], autoS));
Button btnZoomOut = new Button("  −  ");
Button btnZoomIn  = new Button("  +  ");
Button btnFit     = new Button("Ajustar");

// Zoom In: multiplica escala × 1.25, límite máximo = autoS × 8
btnZoomIn.setOnAction(ev -> {
    zoomS[0] = Math.min(zoomS[0] * 1.25, autoS * 8.0);
    lblZoom.setText(pct(zoomS[0], autoS));
    scroll.setContent(renderCanvas(raiz, natPos, rawW, rawH, zoomS[0]));
});

// Zoom Out: multiplica escala × 0.8, límite mínimo = autoS × 0.3
btnZoomOut.setOnAction(ev -> {
    zoomS[0] = Math.max(zoomS[0] * 0.8, autoS * 0.3);
    lblZoom.setText(pct(zoomS[0], autoS));
    scroll.setContent(renderCanvas(raiz, natPos, rawW, rawH, zoomS[0]));
});

// Ajustar: vuelve al autoS calculado al abrir la pestaña
btnFit.setOnAction(ev -> {
    zoomS[0] = autoS;
    lblZoom.setText(pct(zoomS[0], autoS));
    scroll.setContent(renderCanvas(raiz, natPos, rawW, rawH, zoomS[0]));
});

// Los cuatro elementos se colocan en una HBox integrada en la barra superior
HBox barraZoom = new HBox(4, btnZoomOut, lblZoom, btnZoomIn, btnFit);
barraZoom.setAlignment(Pos.CENTER_LEFT);
HBox barraTop = new HBox(10, titulo, barraZoom, btnPantallaCompleta);
```

**Cómo funciona el zoom:** Cada botón modifica `zoomS[0]` (el factor de escala actual) y llama a `renderCanvas()` con el nuevo factor. Esto reconstruye el `Pane` con todas las posiciones y tamaños multiplicados por el nuevo factor y lo coloca en el mismo `ScrollPane` con `scroll.setContent(nuevoCanvas)`, sin crear una nueva pestaña.

**Por qué `double[]` y no `double`:** Las lambdas de Java solo pueden capturar variables `effectively final`. Un array es `final` (siempre apunta al mismo objeto) pero su contenido sí puede modificarse, lo que permite que los tres botones compartan y modifiquen el mismo estado de zoom.

---

### 2.7 Cómo se creó el botón Pantalla Completa y su zoom

El botón **"⛶ Pantalla Completa"** abre una nueva `Stage` (ventana separada) de JavaFX maximizada con sus **propios controles de zoom independientes** (BLOQUE 8 de `mostrarArbolDerivacion()`).

**Código integrado:**
```java
Button btnPantallaCompleta = new Button("⛶  Pantalla Completa");
btnPantallaCompleta.setStyle(
    "-fx-background-color: #16a085; -fx-text-fill: white; -fx-font-weight: bold;");

btnPantallaCompleta.setOnAction(ev -> {

    // Auto-escala para pantalla grande (1900×1000 px), distinta a la de la pestaña
    double fsAutoS = Math.min(2.0, Math.min(1900.0 / rawW, 1000.0 / rawH));
    double[] fsZoom = { fsAutoS };  // estado de zoom propio de esta ventana

    // ScrollPane propio de la ventana completa
    final ScrollPane fsScroll = buildScroll(
        renderCanvas(raiz, natPos, rawW, rawH, fsZoom[0]));

    // Función de refresco local
    Runnable fsRefresh = () -> fsScroll.setContent(
        renderCanvas(raiz, natPos, rawW, rawH, fsZoom[0]));

    // Controles de zoom independientes (misma lógica que en la pestaña)
    Label  fsLblZoom = new Label(pct(fsZoom[0], fsAutoS));
    Button fsBtnOut  = new Button("  −  ");
    Button fsBtnIn   = new Button("  +  ");
    Button fsBtnFit  = new Button("Ajustar");

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

    // Barra superior con título y controles de zoom
    Label fsLbl = new Label("Árbol de Derivación — " + pestañaActiva.getText());
    HBox fsBarraZoom = new HBox(4, fsBtnOut, fsLblZoom, fsBtnIn, fsBtnFit);
    HBox fsBarraTop  = new HBox(12, fsLbl, fsBarraZoom);
    fsBarraTop.setStyle("-fx-padding: 8 10 4 10; -fx-background-color: #f8f9fa;");

    // Composición y apertura de la nueva ventana
    VBox root2 = new VBox(fsBarraTop, fsScroll);
    VBox.setVgrow(fsScroll, Priority.ALWAYS);

    Stage stage = new Stage();
    stage.setTitle("Árbol de Derivación — Pantalla Completa");
    stage.setScene(new javafx.scene.Scene(root2, 1200, 800));
    stage.setMaximized(true);
    stage.show();
});
```

**Por qué el zoom de la ventana completa es independiente:** Cada apertura crea nuevos objetos `fsZoom`, `fsScroll` y botones que no comparten estado con la pestaña principal. Esto permite que el usuario tenga el árbol a 50% en la pestaña y a 200% en la ventana completa al mismo tiempo.

**Por qué `fsAutoS` es diferente a `autoS`:** La pestaña tiene ~920×500 px, la ventana completa tiene ~1900×1000 px. Reutilizar el mismo `autoS` haría el árbol pequeño con mucho espacio vacío. Cada contexto calcula su propia escala óptima.

---

### 2.8 Cómo se genera el árbol visualmente

La generación visual del árbol ocurre en tres fases secuenciales:

#### Fase 1 — Construcción del árbol lógico: `construirNodoRegla()`

Se agrupan los tokens por instrucción (separados por `;`, respetando profundidad de `{}`) y a cada grupo se le asigna una etiqueta de regla gramatical:

```
diagrama Flujo ;          →  cabecera
inicio Arrancar ;         →  decl_inicio
nodo A "desc" ;           →  decl_nodo_nodo
Arrancar conecta A ;      →  instruccion_conexion
tabla T { id: INT PK; }   →  bloque_tabla  →  atributos  →  decl_atributo
```

Los tokens individuales se convierten en hojas con `tokenItem()`, que los etiqueta con el formato `TIPO → 'lexema' [Línea N]`. La presencia de `→` en la etiqueta es la señal que usa `isTerminal()` para distinguir hojas de nodos internos.

#### Fase 2 — Algoritmo de layout top-down: `layoutTree()` + `subtreeW()`

Se calculan las posiciones `(cx, cy)` de cada nodo en coordenadas naturales (sin escala):

```java
// Ancho mínimo del subárbol
subtreeW(n) = max(nw(n) + HGAP, suma de subtreeW de todos los hijos)

// Posicionamiento recursivo top-down
layoutTree(n, cx, topY):
    pos[n] = (cx, topY + nh(n)/2)        // guarda el CENTRO del nodo
    startX = cx - subtreeW(n) / 2        // inicio del primer hijo
    para cada hijo h:
        layoutTree(h, startX + subtreeW(h)/2, topY + nh(n) + VGAP)
        startX += subtreeW(h)
```

#### Fase 3 — Renderizado escalado: `renderCanvas()` → `paintEdges()` + `paintNodes()`

Se multiplican todas las posiciones por el factor `s` y se dibuja sobre un `Pane`:

```java
// 1. Escalar posiciones
pos_escalada[n] = { natPos[n][0] * s, natPos[n][1] * s }

// 2. Dibujar aristas (paintEdges)
Line(pie_del_padre, cabeza_del_hijo)  // línea recta vertical

// 3. Dibujar nodos (paintNodes)
Ellipse(cx, cy, RX_N*s, RY_N*s)   // nodo no-terminal (regla gramatical)
Rectangle(x, y, TW_N*s, TH_N*s)   // nodo terminal (token)
```

**Convención visual aplicada:**

| Tipo de nodo | Forma | Color | Texto mostrado |
|---|---|---|---|
| Raíz (`prog_flujo`) | Óvalo oscuro (#1a252f) | Texto blanco | Nombre del programa |
| No-terminal (regla) | Óvalo blanco con borde | Texto oscuro | Nombre de la regla |
| Terminal (token) | Rectángulo blanco con borde | Texto oscuro | Solo el lexema: `'inicio'`, `';'` |

El canvas resultante se coloca dentro del `ScrollPane`. Cada vez que cambia el zoom, `renderCanvas` se ejecuta con el nuevo `s` y su resultado reemplaza el contenido con `scroll.setContent(nuevoCanvas)`.

---

### 2.9 Método integrado: `construirNodoRegla()`

Método **completamente nuevo**. Recibe un grupo de tokens y devuelve un `TreeItem<String>` con la regla gramatical correspondiente.

```java
private TreeItem<String> construirNodoRegla(List<Token> grupo, String modulo) {
    Token primero = grupo.get(0);
    String lex = primero.getLexema();

    // Cabecera: "diagrama Flujo ;"
    if (primero.getTipo() == Token.Tipo.PR_DIAGRAMA) {
        TreeItem<String> nodo = new TreeItem<>("cabecera");
        for (Token t : grupo) nodo.getChildren().add(tokenItem(t));
        return nodo;
    }
    // Meta-instrucciones: autor, version, tema
    if (lex.equals("autor") || lex.equals("version") || ...) {
        TreeItem<String> nodo = new TreeItem<>("meta_instruccion");
        for (Token t : grupo) nodo.getChildren().add(tokenItem(t));
        return nodo;
    }
    // Módulo FLUJO
    if (modulo.equals("flujo")) {
        if (lex.equals("inicio") || lex.equals("fin"))
            return new TreeItem<>("decl_" + lex);  // + hijos
        if (lex.equals("nodo") || lex.equals("condicion") || ...)
            return new TreeItem<>("decl_nodo_" + lex);
        // Si contiene 'conecta' → instruccion_conexion
    }
    // Módulo BD: tabla/vista/esquema → bloque con atributos agrupados
    // Módulo REDES: dispositivo → decl_dispositivo con propiedades
}
```

---

### 2.10 Método integrado: `tokenItem()`

Método **completamente nuevo**. Crea el nodo hoja del árbol para un token terminal.

```java
private TreeItem<String> tokenItem(Token t) {
    // El símbolo '→' distingue terminales de no-terminales (isTerminal lo detecta)
    String etiqueta = t.getTipo().name() + "  →  '"
                    + t.getLexema() + "'  [Línea " + t.getLinea() + "]";
    return new TreeItem<>(etiqueta);
}
```

---

### 2.11 Método integrado: `pct()`

Método **completamente nuevo**. Calcula el porcentaje de zoom relativo al `autoS`.

```java
private String pct(double current, double base) {
    return Math.round(current / base * 100) + "%";
}
```

---

### 2.12 Método integrado: `renderCanvas()`

Método **completamente nuevo**. Dibuja el árbol en un `Pane` a una escala exacta.

```java
private Pane renderCanvas(TreeItem<String> raiz,
                           Map<TreeItem<String>, double[]> nat,
                           double rawW, double rawH, double s) {
    // Multiplicar todas las posiciones por s antes de dibujar
    Map<TreeItem<String>, double[]> pos = new HashMap<>();
    for (var e : nat.entrySet())
        pos.put(e.getKey(),
            new double[]{ e.getValue()[0] * s, e.getValue()[1] * s });

    Pane canvas = new Pane();
    canvas.setStyle("-fx-background-color: white;");
    canvas.setPrefSize(rawW * s, rawH * s);
    canvas.setMaxSize(rawW * s, rawH * s);
    paintEdges(raiz, pos, canvas, s);
    paintNodes(raiz, pos, canvas, s);
    return canvas;
}
```

---

### 2.13 Métodos integrados: `buildCanvas()` y `buildScroll()`

Métodos **completamente nuevos**.

```java
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
    sc.setFitToWidth(false);   // NO estirar — respetar tamaño calculado
    sc.setFitToHeight(false);
    sc.setStyle("-fx-background: white; -fx-background-color: white;");
    return sc;
}
```

---

### 2.14 Método integrado: `subtreeW()`

Método **completamente nuevo**.

```java
private double subtreeW(TreeItem<String> n) {
    if (n.getChildren().isEmpty()) return nw(n) + HGAP;
    double sum = n.getChildren().stream()
                   .mapToDouble(this::subtreeW).sum();
    return Math.max(nw(n) + HGAP, sum);
}
```

---

### 2.15 Método integrado: `layoutTree()`

Método **completamente nuevo**. Asigna posiciones top-down a cada nodo.

```java
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
```

---

### 2.16 Métodos integrados: `maxX()` y `maxY()`

Métodos **completamente nuevos**.

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

### 2.17 Método integrado: `paintEdges()`

Método **completamente nuevo**. Dibuja `Line` entre el pie de cada padre y la cabeza de cada hijo.

```java
private void paintEdges(TreeItem<String> n,
                         Map<TreeItem<String>, double[]> pos,
                         Pane c, double s) {
    double[] pp  = pos.get(n);
    double   px  = pp[0];
    double   pyBot = pp[1] + nh(n) * s / 2.0;

    for (TreeItem<String> h : n.getChildren()) {
        double[] ph    = pos.get(h);
        double   hyTop = ph[1] - nh(h) * s / 2.0;

        Line l = new Line(px, pyBot, ph[0], hyTop);
        l.setStroke(Color.web("#333333"));
        l.setStrokeWidth(Math.max(0.5, s));
        c.getChildren().add(l);
        paintEdges(h, pos, c, s);
    }
}
```

---

### 2.18 Método integrado: `paintNodes()`

Método **completamente nuevo**. Dibuja `Ellipse` (no-terminal) o `Rectangle` (terminal) con texto.

```java
private void paintNodes(TreeItem<String> n,
                         Map<TreeItem<String>, double[]> pos,
                         Pane c, double s) {
    double[] p    = pos.get(n);
    String   lbl  = n.getValue();
    boolean  term = isTerminal(lbl);
    double   fs   = Math.max(7.5, 10.0 * s);  // fuente mínima: 7.5px

    if (term) {
        // TERMINAL → Rectángulo blanco con borde oscuro + lexema
        double tw = TW_N * s, th = TH_N * s;
        Rectangle r = new Rectangle(p[0]-tw/2.0, p[1]-th/2.0, tw, th);
        r.setFill(Color.WHITE);
        r.setStroke(Color.web("#333333"));
        r.setStrokeWidth(Math.max(0.5, s));
        c.getChildren().add(r);
        c.getChildren().add(mkText(extractLexema(lbl), ...));
    } else {
        // NO-TERMINAL → Óvalo blanco (raíz: oscuro) + nombre de la regla
        double rx = RX_N * s, ry = RY_N * s;
        boolean root = lbl.startsWith("programa_");
        Ellipse oval = new Ellipse(p[0], p[1], rx, ry);
        oval.setFill(root ? Color.web("#1a252f") : Color.WHITE);
        oval.setStroke(root ? Color.BLACK : Color.web("#222222"));
        c.getChildren().add(oval);
        c.getChildren().add(mkText(shortLabel(lbl), ...));
    }
    for (TreeItem<String> h : n.getChildren()) paintNodes(h, pos, c, s);
}
```

---

### 2.19 Métodos auxiliares integrados: `mkText()`, `shortLabel()`, `extractLexema()`

Métodos **completamente nuevos**.

```java
// Crea un Text centrado dentro de un nodo
private javafx.scene.text.Text mkText(String txt, double x, double y,
                                       double w, Font f, Color col) {
    javafx.scene.text.Text t = new javafx.scene.text.Text(txt);
    t.setFont(f); t.setFill(col);
    t.setTextAlignment(TextAlignment.CENTER);
    t.setWrappingWidth(w - 2);
    t.setX(x + 1); t.setY(y);
    return t;
}

// Abrevia etiquetas largas para que quepan en el óvalo (104px de ancho)
private String shortLabel(String lbl) {
    return lbl.replace("instruccion_","inst_").replace("programa_","prog_")
              .replace("declaracion_","decl_").replace("bloque_","blq_")
              .replace("meta_instruccion","meta").replace("propiedades","props");
}

// Extrae el lexema del formato "TIPO  →  'lexema'  [Línea N]"
private String extractLexema(String label) {
    int a = label.indexOf("'"), b = label.lastIndexOf("'");
    if (a >= 0 && b > a) return label.substring(a, b + 1);
    return label;
}
```

---

## Tabla Resumen de Cambios

### `MainFX.java` — líneas modificadas vs integradas

| Elemento | Tipo | Descripción |
|----------|------|-------------|
| Imports (9 nuevos) | **INTEGRADO** | Colecciones, shapes, TextAlignment, ArrayList, HashMap |
| `start()` — barra de herramientas | **MODIFICADO** | 2 botones nuevos agregados al `toolbar` |
| `mostrarTablaErroresLexicos()` | **INTEGRADO** | Método nuevo — Catálogo de Errores con TableView |
| `ErrorLexico` (clase interna) | **INTEGRADO** | Modelo de datos para la TableView |
| `mostrarArbolDerivacion()` | **INTEGRADO** | Método nuevo — orquesta todo el árbol |
| `construirNodoRegla()` | **INTEGRADO** | Identifica reglas gramaticales por grupo de tokens |
| `tokenItem()` | **INTEGRADO** | Crea nodo hoja para un token terminal |
| `pct()` | **INTEGRADO** | Formatea porcentaje de zoom |
| `renderCanvas()` | **INTEGRADO** | Dibuja el árbol a escala dada |
| `buildCanvas()` x2 | **INTEGRADO** | Calcula auto-escala y llama a renderCanvas |
| `buildScroll()` | **INTEGRADO** | Crea ScrollPane configurado |
| `subtreeW()` | **INTEGRADO** | Calcula ancho mínimo del subárbol |
| `layoutTree()` | **INTEGRADO** | Asigna posiciones top-down a cada nodo |
| `maxX()` / `maxY()` | **INTEGRADO** | Calculan límites del árbol para el canvas |
| `paintEdges()` | **INTEGRADO** | Dibuja líneas padre → hijo |
| `paintNodes()` | **INTEGRADO** | Dibuja óvalos y rectángulos con texto |
| `mkText()` | **INTEGRADO** | Texto centrado dentro de un nodo |
| `shortLabel()` | **INTEGRADO** | Abrevia etiquetas largas |
| `extractLexema()` | **INTEGRADO** | Extrae el lexema del formato interno del token |
| `nw()` / `nh()` / `isTerminal()` | **INTEGRADO** | Helpers de tamaño y tipo de nodo |
| 7 constantes `static final` | **INTEGRADO** | Dimensiones del layout visual |

### Archivos nuevos creados

| Archivo | Descripción |
|---------|-------------|
| `pruebas.md` | 25 casos de prueba con resultados y catálogo de 36 errores |
| `DocumentacionJavier.md` | Este documento |

---

*Documentación técnica del proyecto Diagrams As Code — Compilador pedagógico DAC.*
