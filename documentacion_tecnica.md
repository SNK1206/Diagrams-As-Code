# Documentacion Tecnica — Diagrams As Code (DAC)

**Proyecto:** Compilador Pedagogico de Lenguaje de Dominio Especifico (DSL)  
**Lenguaje de implementacion:** Java (JDK compatible con JavaFX 26)  
**Interfaz grafica:** JavaFX 26  
**Extension de archivos fuente:** `.dac`  
**Version del documento:** 2.0

---

## Tabla de Contenidos

1. [Proposito del Proyecto](#1-proposito-del-proyecto)
2. [Arquitectura General](#2-arquitectura-general)
3. [Nucleo del Compilador](#3-nucleo-del-compilador)
4. [Modulos de Diagrama](#4-modulos-de-diagrama)
5. [Sistema de Errores](#5-sistema-de-errores)
6. [Interfaz Grafica (JavaFX)](#6-interfaz-grafica-javafx)
7. [Tabla de Simbologia Estatica](#7-tabla-de-simbologia-estatica)
8. [Catalogo de Codigos de Error](#8-catalogo-de-codigos-de-error)
9. [Decisiones de Diseno](#9-decisiones-de-diseno)
10. [Estructura de Archivos del Proyecto](#10-estructura-de-archivos-del-proyecto)
11. [Cambios Relevantes de Esta Version](#11-cambios-relevantes-de-esta-version)

---

## 1. Proposito del Proyecto

**Diagrams As Code (DAC)** es un compilador pedagogico disenado para ilustrar de forma practica las fases clasicas del analisis de lenguajes formales:

1. **Analisis Lexico** (LexerBase) — Convierte el texto en tokens.
2. **Analisis Sintactico** (ParserBase + parsers de modulo) — Verifica la estructura gramatical y construye el AST.
3. **Tabla de Simbolos** (TablaSimbolos) — Registra identificadores y sus roles.

La salida del sistema es:
- Lista de tokens clasificados con numero de linea.
- Tabla de simbolos con todos los identificadores registrados.
- Mensajes de error detallados con codigo, linea, descripcion y consejo de correccion.

El objetivo **no** es generar imagenes de diagramas, sino demostrar el proceso interno de un compilador de forma visible y educativa.

---

## 2. Arquitectura General

### Pipeline de compilacion

```
Archivo .dac (texto plano)
         |
         v
  [ LexerBase ]
  - Lee caracter por caracter
  - Produce List<Token>
  - Reporta errores lexicos (EL01-EL04)
  - Continua aunque haya errores lexicos
         |
         v (List<Token>)
  [ ParserBase.analizarCabecera() ]
  - Procesa meta-instrucciones (autor, version, etc.)
  - Detecta "diagrama <Tipo>;"
  - Bloquea contexto en TablaSimbolos
  - Delega tokens restantes al modulo correcto
         |
         v (tokens restantes)
  [ FlujoParser | BDParser | RedesParser | ConceptualParser | UMLParser ]
  - Implementan la gramatica especifica del modulo
  - Construyen el AST del diagrama
  - Registran identificadores en TablaSimbolos
  - Reportan errores sintacticos (ES06-ES54)
  - Recuperacion de panico: sincronizan en ';' o '}' tras cada error
         |
         v
  [ AST + TablaSimbolos + ManejadorErrores ]
  - Resultado consultado por MainFX para mostrar en la UI
```

### Comportamiento ante errores

| Situacion | Comportamiento |
|-----------|---------------|
| Errores lexicos (EL01-EL04) | El parser sigue ejecutandose. Los tokens validos se procesan normalmente. |
| Error en declaracion `diagrama` (ES03-ES05) | El modulo parser no corre. La tabla solo muestra `autor` y `version`. |
| Errores en instrucciones del modulo (ES06-ES54) | La instruccion con error se omite. Las siguientes instrucciones validas se procesan con normalidad (anti-cascada). |

---

## 3. Nucleo del Compilador

### 3.1 LexerBase.java

**Responsabilidad:** Convertir el codigo fuente (String) en una lista de objetos Token.

**Tokens producidos:**

| Token | Cuando se genera |
|-------|-----------------|
| `PR_DIAGRAMA` | Lexema exactamente igual a `"diagrama"` |
| `IDENTIFICADOR` | Cualquier palabra valida (letras, digitos, `_`, empieza con letra) |
| `TEXTO_LITERAL` | Cadena entre comillas dobles `"..."` |
| `PUNTO_Y_COMA` | Caracter `;` |
| `DOS_PUNTOS` | Caracter `:` |
| `LLAVE_IZQ` | Caracter `{` |
| `LLAVE_DER` | Caracter `}` |
| `EOF` | Siempre al final de la lista |

**Errores lexicos detectados:**

| Codigo | Condicion | Accion del lexer |
|--------|-----------|-----------------|
| EL01 | Caracter no pertenece al alfabeto DAC | Reporta error, avanza un caracter, continua |
| EL02 | Cadena sin comilla de cierre (EOF o salto de linea) | Reporta error, agrega token sintetico TEXTO_LITERAL + PUNTO_Y_COMA para evitar cascada sintactica |
| EL03 | Identificador que empieza con digito | Reporta error, consume todos los caracteres alfanumericos del token invalido |
| EL04 | Caracter invalido dentro de un identificador | Reporta error, omite el caracter invalido y continua construyendo el identificador |

**Decision de diseno — tokens sinteticos para EL02:**  
Cuando se detecta una cadena sin cerrar (EL02), el lexer agrega dos tokens sinteticos (`TEXTO_LITERAL` con el contenido parcial y `PUNTO_Y_COMA`). Esto permite que el parser pueda continuar analizando el resto del archivo sin generar errores sintacticos en cascada por la falta del texto y el `;`.

**Decision de diseno — lexer generico:**  
Solo `diagrama` tiene su propio tipo de token (`PR_DIAGRAMA`). Todas las palabras reservadas de modulo (`inicio`, `tabla`, `clase`, `enlaza`, etc.) son tokenizadas como `IDENTIFICADOR`. El parser las reconoce por su posicion en la gramatica, no por su tipo de token. Esto mantiene el lexer estable: nunca necesita modificarse al agregar nuevos modulos.

---

### 3.2 Token.java

**Responsabilidad:** Modelo de datos que representa la unidad minima del lenguaje.

**Campos:**
- `tipo` : `enum Token.Tipo` — clasificacion del token.
- `lexema` : `String` — texto original tal como aparece en el codigo fuente.
- `linea` : `int` — numero de linea donde aparece (para mensajes de error precisos).

---

### 3.3 ParserBase.java

**Responsabilidad:** Analizar la cabecera del archivo y delegar al modulo correcto.

**Flujo de `analizarCabecera()`:**

1. Recorre los tokens buscando meta-instrucciones (`autor`, `version`, `tema`, `exportar`, `importar`).
2. Para cada meta-instruccion valida: registra el identificador y su valor en `TablaSimbolos`.
3. Detecta la instruccion `diagrama <Tipo>;`:
   - Registra `diagrama` con el tipo como valor en `TablaSimbolos`.
   - Registra el tipo (`Flujo`, `BD`, etc.) con rol `tipo_diagrama`.
   - Llama a `bloquearContexto()` en `TablaSimbolos`.
   - Crea el parser del modulo correspondiente con los tokens restantes.
4. Si no encuentra `diagrama` y no hubo errores previos: reporta ES05.

**Delegacion de modulos:**

```java
switch (tipoDiagrama) {
    case "flujo"      --> new FlujoParser(...)
    case "bd"         --> new BDParser(...)
    case "redes"      --> new RedesParser(...)
    case "conceptual" --> new ConceptualParser(...)
    case "uml"        --> new UMLParser(...)
    default           --> error ES05
}
```

**Errores de cabecera:**

| Codigo | Condicion | Comportamiento |
|--------|-----------|----------------|
| ES01 | Falta `;` despues del valor de meta-instruccion | Reporta error, `continue` (busca mas cabecera) |
| ES02 | Falta texto entre comillas en meta-instruccion | Reporta error, `pos++`, `continue` |
| ES03 | Falta `;` en `diagrama <Tipo>` | Reporta error, `break` (no busca mas) |
| ES04 | Falta tipo despues de `diagrama` | Reporta error, `break` |
| ES05 | No se encontro `diagrama <Tipo>;` | Reporta error (solo si no hubo errores previos) |

**Anti-cascada ES03/ES04 → ES05:**  
Despues de ES03 o ES04 se hace `break` para salir del bucle. La variable `diagramaEncontrado` queda en `false`, pero como `manejadorErrores.tieneErrores()` ya es `true`, la condicion `!diagramaEncontrado && !tieneErrores()` no se cumple y ES05 no se reporta. Un solo error por falla de cabecera.

---

### 3.4 TablaSimbolos.java

**Responsabilidad:** Memoria del compilador durante el analisis.

**Estructura interna:**
```java
Map<String, String> simbolos;  // nombre -> rol/tipo
String contextoActivo;         // tipo de diagrama activo
```

**Metodos:**
- `registrar(nombre, rol)` : `boolean` — agrega el identificador si no existe. Retorna `false` si ya estaba registrado (sin reportar error; los errores semanticos fueron eliminados del diseno).
- `existe(nombre)` : `boolean` — consulta si el identificador fue declarado.
- `bloquearContexto(contexto)` — registra el modulo activo.
- `getSimbolos()` : `Map<String, String>` — para visualizacion en la UI.
- `limpiar()` — reinicia entre compilaciones.

**Por que se usa HashMap:**  
Acceso en O(1) para la consulta de existencia de identificadores durante el parseo. El orden de los elementos en el `Map` es arbitrario (HashMap no garantiza orden), por lo que los simbolos en la UI pueden aparecer en cualquier orden.

---

### 3.5 ManejadorErrores.java

**Responsabilidad:** Acumular y formatear todos los errores de compilacion.

**Formato de cada error:**
```
==================================================
[ERROR] [EL01] LEXICO [Linea N]
Detalle: descripcion del problema
Consejo: como corregirlo
==================================================
```

**Constante MAX_ERRORES = 1000:**  
Limita el numero de errores que se acumulan para evitar degradacion de rendimiento en archivos con errores masivos. Con el valor 1000, practicamente nunca se alcanza el limite en uso normal.

**Por que acumular en vez de lanzar excepcion:**  
Lanzar una excepcion detiene la compilacion al primer error. Acumular permite reportar multiples errores en una sola pasada, lo que es mas util pedagogicamente: el usuario ve todos sus errores de una vez.

**Recuperacion de panico:**  
Los parsers de modulo implementan recuperacion de panico: al detectar un error, avanzan hasta el siguiente punto de sincronizacion (`;` o `}`) y continuan. Esto evita que un error genere decenas de errores falsos en cascada.

---

## 4. Modulos de Diagrama

### 4.1 FlujoParser.java

**Activado con:** `diagrama Flujo;`  
**Paquete:** `com.diagramas.modulos.flujo`

**Instrucciones reconocidas:**

| Instruccion | Forma |
|-------------|-------|
| Nodo simple | `<keyword> <ID>;` donde keyword es `inicio`, `fin`, `parada` |
| Nodo con texto | `<keyword> <ID> "<texto>";` donde keyword es `nodo`, `condicion`, `bucle`, `subproceso`, `entrada`, `salida` |
| Conexion | `<ID> conecta <ID>;` |

**Errores (ES06-ES13):**
- ES06: Token inesperado → `pos++` (no recuperacion de panico, el token se salta)
- ES07: Falta ID despues de keyword → `recuperarPanico()` (salta a `;`)
- ES08: Falta `;` despues de nodo simple → `recuperarPanico()`
- ES09: Falta texto entre comillas → `recuperarPanico()`
- ES10: Falta `;` despues de nodo con texto → `recuperarPanico()`
- ES11: Falta destino en `conecta` → `recuperarPanico()`
- ES12: Falta `;` despues de conexion → `recuperarPanico()`
- ES13: Verbo invalido (no es `conecta`) → `recuperarPanico()`

**Nodos del AST:**

| Clase | Cuando se crea |
|-------|---------------|
| `NodoProceso` | Para todos los nodos (simple y con texto) |
| `NodoConexion` | Para cada instruccion `conecta` |
| `RaizFlujoAST` | Contenedor raiz, lista de todos los nodos |

---

### 4.2 BDParser.java

**Activado con:** `diagrama BD;`  
**Paquete:** `com.diagramas.modulos.bd`

**Instrucciones reconocidas:**

| Instruccion | Forma |
|-------------|-------|
| Bloque con atributos | `<keyword> <ID> { <atributos> }` donde keyword es `tabla`, `vista`, `esquema`, `paquete` |
| Componente lineal | `<keyword> <ID>;` donde keyword es `procedimiento`, `funcion`, `disparador`, `secuencia`, `indice`, `tipo`, `sinonimo`, `dominio` |
| Relacion | `<ID> relaciona <ID>;` |

**Atributos dentro del bloque:**
```
<nombre> : <Tipo>;
```

**Errores (ES14-ES26):**  
ES14 cubre tanto tokens inesperados como identificadores seguidos de `{` sin palabra clave. ES17-ES20 se reportan individualmente por cada atributo malformado dentro de un bloque (cada uno es independiente, sin cascada entre ellos).

**Nodos del AST:**

| Clase | Cuando se crea |
|-------|---------------|
| `NodoTabla` | Para cada bloque (tabla/vista/esquema/paquete) |
| `NodoRelacion` | Para cada `relaciona` |
| `Atributo` | Por cada campo dentro de un bloque |
| `RaizBDAST` | Contenedor raiz |

---

### 4.3 RedesParser.java

**Activado con:** `diagrama Redes;`  
**Paquete:** `com.diagramas.modulos.redes`

**Instrucciones reconocidas:**

| Instruccion | Forma |
|-------------|-------|
| Dispositivo simple | `dispositivo <Nombre> <Tipo>;` |
| Dispositivo con config | `dispositivo <Nombre> <Tipo> { <config> }` |
| Enlace | `<ID> enlaza <ID>;` |

**Decision de diseno — pre-registrar dispositivos:**  
Antes de procesar el bloque de configuracion (si existe), el nombre del dispositivo ya se registra en `TablaSimbolos`. Esto permite que instrucciones `enlaza` que referencian ese dispositivo no fallen aunque el bloque no haya terminado de procesarse.

**Nodos del AST:**

| Clase | Cuando se crea |
|-------|---------------|
| `NodoDispositivo` | Para cada componente de red |
| `NodoEnlace` | Para cada `enlaza` |
| `RaizRedesAST` | Contenedor raiz |

---

### 4.4 ConceptualParser.java

**Activado con:** `diagrama Conceptual;`  
**Paquete:** `com.diagramas.modulos.conceptual`

**Instrucciones reconocidas:**

| Instruccion | Forma |
|-------------|-------|
| Nodo conceptual | `<keyword> <ID> "<descripcion>";` donde keyword es `concepto`, `categoria`, `propiedad` |
| Relacion | `<ID> <verbo> <ID>;` donde verbo es `agrupa`, `asocia`, `depende` |

**Distincion ES41 vs ES42:**
- ES41: El token despues del identificador origen NO es un IDENTIFICADOR (por ejemplo, es `;` u otro simbolo). Significa que se escribio el identificador pero no hay verbo.
- ES42: El token despues del identificador origen ES un IDENTIFICADOR, pero no es uno de los verbos validos (`agrupa`, `asocia`, `depende`).

**Nodos del AST:**

| Clase | Cuando se crea |
|-------|---------------|
| `NodoConcepto` | Para cada nodo conceptual |
| `NodoRelacionConceptual` | Para cada relacion |
| `RaizConceptualAST` | Contenedor raiz |

---

### 4.5 UMLParser.java

**Activado con:** `diagrama UML;`  
**Paquete:** `com.diagramas.modulos.uml`

**Instrucciones reconocidas:**

| Instruccion | Forma |
|-------------|-------|
| Clase con miembros | `clase <ID> { <miembros> }` |
| Componente lineal | `<keyword> <ID>;` donde keyword es `interfaz`, `enum` |
| Relacion | `<ID> <verbo> <ID>;` donde verbo es `extiende`, `implementa`, `usa` |

**Miembros dentro de una clase:**
```
atributo <nombre> : <Tipo>;
metodo <nombre> : <TipoRetorno>;
```

**Decision de diseno — pre-registrar clases:**  
Antes de procesar el bloque `{ ... }` de una clase, el nombre ya se registra en `TablaSimbolos`. Esto permite que instrucciones de relacion que referencian esa clase (ej: `Auto extiende Vehiculo;`) funcionen aunque la clase no haya terminado de procesarse.

**Nodos del AST:**

| Clase | Cuando se crea |
|-------|---------------|
| `NodoClase` | Para cada clase con bloque |
| `NodoLinealUML` | Para `interfaz` y `enum` |
| `NodoRelacionUML` | Para cada relacion |
| `RaizUMLAST` | Contenedor raiz |

---

## 5. Sistema de Errores

### 5.1 Clasificacion

Todos los errores tienen un codigo unico con prefijo:
- `EL` — Error Lexico (generado por LexerBase)
- `ES` — Error Sintactico (generado por parsers)

No existen errores semanticos en el sistema actual. La tabla de simbolos registra sin validar unicidad ni existencia de referencias (el metodo `registrar` retorna `false` si el nombre ya existe pero no reporta error).

### 5.2 Recuperacion de panico

Cada parser de modulo implementa metodos de recuperacion:

| Metodo | Avanza hasta | Usado cuando |
|--------|-------------|--------------|
| `recuperarPanico()` | Siguiente `;` | Error en instruccion simple |
| `recuperarPanicoBloque()` | Siguiente `}` | Error en instruccion con bloque |
| `recuperarPanicoMiembro()` | Siguiente `;` o `}` | Error dentro de un bloque (atributo/miembro) |

El objetivo es que cada instruccion incorrecta genere **exactamente un error** y que la instruccion siguiente se procese correctamente.

### 5.3 Patron de validacion

Los parsers usan el metodo `validarSiguienteTipo()` para verificar el proximo token:

```java
private boolean validarSiguienteTipo(Token.Tipo esperado, String codigoError, String mensaje) {
    if (pos >= tokens.size() || tokens.get(pos).getTipo() != esperado) {
        int linea = (pos < tokens.size()) ? tokens.get(pos).getLinea()
                  : (pos > 0 ? tokens.get(pos - 1).getLinea() : 1);
        errores.reportarError(codigoError, linea, "Sintactico", mensaje, "...");
        return false;
    }
    return true;
}
```

La expresion para obtener la linea tiene tres casos: token disponible, fin de lista con tokens previos, o inicio absoluto (linea 1). Esto evita `IndexOutOfBoundsException` cuando el error ocurre al final del archivo.

---

## 6. Interfaz Grafica (JavaFX)

### 6.1 Estructura de la ventana principal

```
BorderPane (root)
|
+-- TOP: HBox toolbar
|    +-- Label "Diagrams As Code"
|    +-- Button Nuevo       (naranja)
|    +-- Button Abrir       (azul)
|    +-- Button Guardar     (morado)
|    +-- Button Compilar    (verde)
|    +-- Button Errores Lexicos (rojo)  --> ventana modal catalogo de errores
|    +-- Button Arbol de Derivacion (verde azulado) --> genera y muestra AST
|    +-- Region espaciador  (Priority.ALWAYS)
|    +-- Button Simbologia  (verde oscuro) --> ventana modal tabla de simbologia
|    +-- Button Manual      (azul oscuro)  --> ventana modal manual usuario
|
+-- CENTER: SplitPane vertical
     +-- SplitPane horizontal (0.45 editor / 0.55 analisis)
     |    +-- VBox panelEditor
     |    |    +-- Label "Archivos Abiertos:"
     |    |    +-- TabPane (una Tab por archivo)
     |    |         +-- HBox (contenedor editor)
     |    |              +-- TextArea lineaNums (numeros, 48px, solo lectura)
     |    |              +-- TextArea editor    (id="dac-editor", editable)
     |    |
     |    +-- VBox panelAnalisis
     |         +-- Label "1. Tokens:"
     |         +-- TextArea txtTokens (solo lectura)
     |         +-- Label "2. Tabla de Simbolos:"
     |         +-- TextArea txtSimbolos (solo lectura)
     |
     +-- VBox panelConsola (arrastrable, posicion inicial 0.72)
          +-- Label "Consola:"
          +-- TextArea txtConsola (solo lectura, crece con el panel)
```

### 6.2 Flujo de ejecutarCompilador()

```java
ejecutarCompilador() {
    1. Limpiar txtTokens, txtSimbolos, txtConsola
    2. Obtener TextArea del editor mediante getEditorDePestana(pestanaActiva)
    3. Crear ManejadorErrores y TablaSimbolos limpios
    4. LexerBase.tokenizar() --> poblar txtTokens
    5. Registrar si hay errores lexicos (boolean hayErroresLexicos)
    6. ParserBase.analizarCabecera() --> corre SIEMPRE (incluso con errores lexicos)
    7. Poblar txtSimbolos con TablaSimbolos.getSimbolos()
    8. Si hay errores --> txtConsola con prefijo LEXICOS+SINTACTICOS o solo FALLIDA
    9. Si no hay errores --> "COMPILACION EXITOSA de [nombre_archivo]"
}
```

### 6.3 Acceso al editor en las pestanas

Las pestanas contienen un `HBox` (no un `TextArea` directo) para soportar numeros de linea. El metodo `getEditorDePestana(Tab tab)` extrae el `TextArea` correcto buscando el hijo con `id="dac-editor"`:

```java
private TextArea getEditorDePestana(Tab tab) {
    if (tab == null) return null;
    Object content = tab.getContent();
    if (content instanceof TextArea) return (TextArea) content; // compatibilidad
    if (content instanceof HBox) {
        for (Node node : ((HBox) content).getChildren()) {
            if (node instanceof TextArea && "dac-editor".equals(node.getId())) {
                return (TextArea) node;
            }
        }
    }
    return null;
}
```

Retorna `null` cuando la pestana activa es una ventana de catalogo (VBox) en vez de un editor. El llamador verifica el `null` y muestra un mensaje al usuario.

### 6.4 Numeros de linea

Cada pestana de editor contiene un `HBox` con dos `TextArea`:

- **lineaNums** (48px fijo, no editable, fondo gris): muestra `1\n2\n3\n...`
- **editor** (crece con el espacio disponible, editable): el codigo fuente

El scroll se sincroniza mediante listener:
```java
editor.scrollTopProperty().addListener((obs, old, nv) ->
    lineaNums.setScrollTop(nv.doubleValue()));
```

Los numeros se actualizan con listener de texto:
```java
editor.textProperty().addListener((obs, old, nv) -> actualizarNumeros.run());
```

### 6.5 Consola arrastrable

La consola se integra en un `SplitPane` vertical con orientacion `VERTICAL`. La posicion inicial del divisor es `0.72` (72% para editor+analisis, 28% para consola). El usuario puede arrastrar el separador para ver mas o menos de la consola.

### 6.6 Ventana modal de Errores Lexicos (catalogo estatico)

Muestra una `TableView` con todos los codigos de error EL y ES, sus descripciones y ejemplos. Los datos provienen de una lista `ObservableList<ErrorLexico>` definida en `MainFX` con todos los 58 codigos (EL01-EL04 + ES01-ES54).

---

## 7. Tabla de Simbologia Estatica

### Archivo: TablaSimbologiaEstatica.java

**Que es:** Catalogo estatico de todos los simbolos del lenguaje DAC.

**Por que existe:** El LexerBase solo distingue entre `PR_DIAGRAMA` e `IDENTIFICADOR` para la mayoria de las palabras. La tabla de simbologia es el catalogo oficial que documenta el significado de cada simbolo, independientemente de como lo clasifique internamente el lexer.

**Estructura de cada entrada:**

| Campo | Tipo | Descripcion |
|-------|------|-------------|
| `lexema` | String | Texto exacto como aparece en el .dac |
| `tipoToken` | String | Clasificacion del token segun LexerBase |
| `categoria` | String | Grupo semantico (Flujo-Nodo, BD-Bloque, etc.) |
| `descripcion` | String | Explicacion del proposito del simbolo |

**Metodo filtrar(String modulo):**

| Opcion | Incluye |
|--------|---------|
| "Todos" | Todos los simbolos |
| "Flujo" | Global + Flujo-Nodo + Flujo-Verbo + Puntuacion |
| "BD" | Global + BD-Bloque + BD-Lineal + BD-Verbo + Puntuacion |
| "Redes" | Global + Redes-Componente + Redes-Verbo + Puntuacion |
| "Conceptual" | Global + Conceptual-Nodo + Conceptual-Verbo + Puntuacion |
| "UML" | Global + UML-Clase + UML-Lineal + UML-Verbo + Puntuacion |

---

## 8. Catalogo de Codigos de Error

### Errores Lexicos

| Codigo | Descripcion | Condicion |
|--------|-------------|-----------|
| EL01 | Caracter invalido en el alfabeto | Cualquier caracter no reconocido por el lexer |
| EL02 | Cadena de texto sin cerrar | EOF o salto de linea antes de la comilla de cierre |
| EL03 | Identificador que comienza con digito | Primer caracter es un digito |
| EL04 | Caracter invalido dentro de identificador | Caracter no alfanumerico ni `_` dentro de una palabra |

### Errores Sintacticos — Cabecera

| Codigo | Descripcion | Accion |
|--------|-------------|--------|
| ES01 | Falta `;` al final de meta-instruccion | Reporta, `continue` |
| ES02 | Falta texto entre comillas en meta-instruccion | Reporta, `pos++`, `continue` |
| ES03 | Falta `;` en `diagrama <Tipo>` | Reporta, `break` |
| ES04 | Falta tipo despues de `diagrama` | Reporta, `break` |
| ES05 | No se encontro `diagrama <Tipo>;` | Reporta (solo si !tieneErrores()) |

### Errores Sintacticos — Flujo (ES06-ES13)

| Codigo | Descripcion |
|--------|-------------|
| ES06 | Token inesperado en contexto Flujo |
| ES07 | Falta nombre del nodo despues de la palabra clave |
| ES08 | Falta `;` al final de nodo simple |
| ES09 | Falta descripcion entre comillas en nodo con texto |
| ES10 | Falta `;` al final de nodo con texto |
| ES11 | Falta identificador destino en `conecta` |
| ES12 | Falta `;` al final de conexion |
| ES13 | Verbo invalido en Flujo |

### Errores Sintacticos — BD (ES14-ES26)

| Codigo | Descripcion |
|--------|-------------|
| ES14 | Token inesperado o identificador sin palabra clave |
| ES15 | Falta nombre de tabla/bloque |
| ES16 | Falta `{` para abrir bloque |
| ES17 | Falta nombre de atributo |
| ES18 | Falta `:` en atributo |
| ES19 | Falta tipo de dato del atributo |
| ES20 | Falta `;` al final del atributo |
| ES21 | Falta `}` de cierre de bloque (detectado en EOF) |
| ES22 | Falta nombre de componente lineal |
| ES23 | Falta `;` al final de componente lineal |
| ES24 | Falta identificador destino en `relaciona` |
| ES25 | Falta `;` al final de relacion |
| ES26 | Verbo invalido en BD |

### Errores Sintacticos — Redes (ES27-ES34)

| Codigo | Descripcion |
|--------|-------------|
| ES27 | Token inesperado en contexto Redes |
| ES28 | Falta nombre del dispositivo |
| ES29 | Falta tipo del dispositivo |
| ES30 | Falta `}` de cierre de bloque de configuracion (detectado en EOF) |
| ES31 | Falta `;` al final de declaracion de dispositivo |
| ES32 | Falta identificador destino en `enlaza` |
| ES33 | Falta `;` al final de enlace |
| ES34 | Verbo invalido en Redes |

### Errores Sintacticos — Conceptual (ES35-ES42)

| Codigo | Descripcion |
|--------|-------------|
| ES35 | Token inesperado en contexto Conceptual |
| ES36 | Falta nombre del concepto/categoria/propiedad |
| ES37 | Falta descripcion entre comillas |
| ES38 | Falta `;` al final de declaracion |
| ES39 | Falta identificador destino en relacion conceptual |
| ES40 | Falta `;` al final de relacion |
| ES41 | Identificador origen sin verbo (siguiente token no es IDENTIFICADOR) |
| ES42 | Verbo invalido en Conceptual |

### Errores Sintacticos — UML (ES43-ES54)

| Codigo | Descripcion |
|--------|-------------|
| ES43 | Token inesperado en contexto UML |
| ES44 | Falta nombre de clase, interfaz o enum |
| ES45 | Falta `{` para abrir cuerpo de clase |
| ES46 | Keyword invalido dentro de clase |
| ES47 | Falta nombre del miembro (atributo/metodo) |
| ES48 | Falta `:` en definicion del miembro |
| ES49 | Falta tipo del miembro |
| ES50 | Falta `;` al final del miembro o componente lineal |
| ES51 | Falta `}` de cierre de clase (detectado en EOF) |
| ES52 | Falta identificador destino en relacion UML |
| ES53 | Falta `;` al final de relacion UML |
| ES54 | Verbo invalido en UML |

---

## 9. Decisiones de Diseno

### Por que no hay errores semanticos

La version actual no reporta errores semanticos (identificadores duplicados, referencias no declaradas). Esta decision es intencional para el alcance pedagogico actual: el foco esta en demostrar las fases lexica y sintactica. La tabla de simbolos existe y registra, pero `registrar()` retorna `false` silenciosamente si el nombre ya existe en vez de disparar un error.

### Por que las palabras reservadas de modulo son IDENTIFICADOR

Si se crearan tokens PR_INICIO, PR_NODO, PR_TABLA, etc., habria que modificar el lexer cada vez que se agrega un nuevo modulo. El lexer generico es estable: solo reconoce `diagrama` como palabra reservada. Todos los demas son IDENTIFICADOR y el parser decide por posicion si son palabras clave o nombres de usuario.

### Por que tres (o cinco) AST independientes

Un AST generico requiere generics complejos o casting frecuente. Los cinco modulos tienen estructuras de datos genuinamente diferentes. Cinco AST independientes son mas simples de entender y mas seguros de tipo.

### Por que recuperar panico en `;` y no en la siguiente instruccion

El punto y coma es el terminador universal en DAC. Avanzar hasta `;` siempre deja el parser en un estado limpio listo para la siguiente instruccion, sin importar cuantos tokens malformados habia antes del `;`. Es el enfoque mas simple y el mas predecible para el usuario.

### Por que el parser corre incluso con errores lexicos

En la version anterior, el parser se detenia si habia errores lexicos. El problema es que errores lexicos como EL04 (caracter invalido dentro de identificador) no destruyen la estructura del token stream: el lexer omite el caracter invalido y produce un token parcialmente valido. El parser puede usar esos tokens. Detener el parser ante cualquier error lexico era demasiado restrictivo; ahora el parser siempre corre y la tabla de simbolos muestra todo lo que fue posible registrar.

---

## 10. Estructura de Archivos del Proyecto

```
DiagramsAsCode/
|
+-- src/com/diagramas/
|   +-- MainFX.java                   IDE grafico (JavaFX)
|   +-- Main.java                     Punto de entrada CLI (sin JavaFX)
|   |
|   +-- core/
|   |   +-- LexerBase.java            Analizador lexico
|   |   +-- Token.java                Modelo de token
|   |   +-- ParserBase.java           Parser de cabecera + delegador
|   |   +-- TablaSimbolos.java        Registro de identificadores
|   |   +-- ManejadorErrores.java     Acumulador de errores (MAX=1000)
|   |   +-- TablaSimbologiaEstatica.java  Catalogo oficial de simbolos
|   |
|   +-- modulos/
|       +-- flujo/
|       |   +-- FlujoParser.java
|       |   +-- ast/
|       |       +-- NodeAST.java
|       |       +-- NodoProceso.java
|       |       +-- NodoConexion.java
|       |       +-- NodoInicio.java
|       |       +-- RaizFlujoAST.java
|       |
|       +-- bd/
|       |   +-- BDParser.java
|       |   +-- ast/
|       |       +-- NodeAST.java
|       |       +-- NodoTabla.java
|       |       +-- NodoRelacion.java
|       |       +-- Atributo.java
|       |       +-- RaizBDAST.java
|       |
|       +-- redes/
|       |   +-- RedesParser.java
|       |   +-- ast/
|       |       +-- NodoDispositivo.java
|       |       +-- NodoEnlace.java
|       |       +-- RaizRedesAST.java
|       |
|       +-- conceptual/
|       |   +-- ConceptualParser.java
|       |   +-- ast/
|       |       +-- NodoConcepto.java
|       |       +-- NodoRelacionConceptual.java
|       |       +-- RaizConceptualAST.java
|       |
|       +-- uml/
|           +-- UMLParser.java
|           +-- ast/
|               +-- NodoClase.java
|               +-- NodoLinealUML.java
|               +-- NodoRelacionUML.java
|               +-- RaizUMLAST.java
|
+-- out/                              Clases compiladas (.class)
+-- test_EL01.dac                     Prueba: EL01 caracter invalido
+-- test_EL02.dac                     Prueba: EL02 cadena sin cerrar
+-- test_EL03.dac                     Prueba: EL03 identificador con digito inicial
+-- test_EL04.dac                     Prueba: EL04 caracter invalido en identificador
+-- test_ES01_ES02.dac                Prueba: ES01 y ES02 en cabecera
+-- test_ES03.dac                     Prueba: ES03 falta ; en diagrama
+-- test_ES04.dac                     Prueba: ES04 falta tipo en diagrama
+-- test_ES05.dac                     Prueba: ES05 sin declaracion diagrama
+-- test_ES_flujo.dac                 Prueba: ES06-ES13 modulo Flujo
+-- test_ES_bd.dac                    Prueba: ES14-ES26 modulo BD
+-- test_ES_redes.dac                 Prueba: ES27-ES34 modulo Redes
+-- test_ES_conceptual.dac            Prueba: ES35-ES42 modulo Conceptual
+-- test_ES_uml.dac                   Prueba: ES43-ES54 modulo UML
+-- manual_diagrams_as_code.md        Manual de usuario
+-- documentacion_tecnica.md          Este documento
```

---

## 11. Cambios Relevantes de Esta Version

Esta seccion documenta las decisiones y cambios implementados durante el desarrollo de la version 2.0 del IDE.

### Adicion de modulos Conceptual y UML

Se agregaron `ConceptualParser` y `UMLParser` como modulos completos. Cada uno tiene su propia jerarquia de nodos AST en paquetes separados (`com.diagramas.modulos.conceptual` y `com.diagramas.modulos.uml`). El `ParserBase.delegarAlModulo()` fue actualizado para incluir los casos `"conceptual"` y `"uml"`.

### Eliminacion de errores semanticos

Todos los errores semanticos (identificador duplicado, referencia no declarada) fueron eliminados del sistema. Los parsers simplemente llaman `tabla.registrar(nombre, rol)` sin verificar el valor de retorno. El filtro en `ManejadorErrores` que ignoraba contextos semanticos tambien fue eliminado.

### Implementacion de EL03 y EL04

Los errores EL03 (identificador con digito inicial) y EL04 (caracter invalido dentro de identificador) se implementaron en `LexerBase`. EL03 consume el token invalido completo. EL04 omite el caracter invalido y fusiona el resto del identificador.

### El parser siempre corre (incluso con errores lexicos)

Se elimino el `return` anticipado que detenia la compilacion al primer error lexico. Ahora el parser sintactico siempre corre despues del lexer, permitiendo que la tabla de simbolos muestre los identificadores que si se pudieron registrar.

### Registro de cabecera en tabla de simbolos

`ParserBase` ahora llama a `tabla.registrar()` para `autor`, `version`, `diagrama` y el tipo de diagrama. La tabla de simbolos refleja toda la informacion del archivo, no solo los nodos del modulo.

### Numeros de linea en el editor

Las pestanas del editor ahora contienen un `HBox` con dos `TextArea`: uno de numeros de linea (48px, no editable, fondo gris, sincronizado en scroll) y uno de edicion. El metodo `getEditorDePestana(Tab)` permite acceder al `TextArea` correcto mediante su `id="dac-editor"`.

### Consola arrastrable

El `BorderPane` ya no tiene el panel de consola en `setBottom()`. En su lugar, `splitCentro` y `panelConsola` se combinan en un `SplitPane` vertical con posicion inicial `0.72`. El usuario puede arrastrar el divisor para agrandar la consola.

### Eliminacion de emojis

Todos los emojis y caracteres Unicode especiales (emojis de error, check, lock, etc.) fueron eliminados de los mensajes del compilador. El formato de error ahora usa texto ASCII puro: `[ERROR]`, `Detalle:`, `Consejo:`, `[!]`.

### MAX_ERRORES elevado a 1000

El limite de errores en `ManejadorErrores` se subio de 10 a 1000 para que el compilador nunca suprima errores durante demostraciones con archivos de prueba complejos.

### Correccion de ClassCastException en pestanas de catalogo

Cuando la pestana activa es una ventana de catalogo (VBox) y no un editor, los metodos `guardarArchivoActual()`, `ejecutarCompilador()` y `mostrarArbolDerivacion()` obtenian `null` de `getEditorDePestana()` y mostraban un mensaje de error en lugar de lanzar una excepcion.

### Correccion de IndexOutOfBoundsException en validarSiguienteTipo

Todos los parsers de modulo tenian un bug donde `tokens.get(pos - 1)` podia lanzar `IndexOutOfBoundsException` cuando `pos = 0`. La expresion correcta es:

```java
int linea = (pos < tokens.size()) ? tokens.get(pos).getLinea()
          : (pos > 0 ? tokens.get(pos - 1).getLinea() : 1);
```

---

*Documentacion Tecnica — Diagrams As Code v2.0*  
*Compilador pedagogico de DSL para cinco tipos de diagrama*
