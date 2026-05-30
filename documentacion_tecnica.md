# Documentacion Tecnica — Diagrams As Code

**Proyecto:** Compilador Pedagogico de Lenguaje de Dominio Especifico (DSL)
**Lenguaje de implementacion:** Java 21 (Liberica JDK Full / Oracle JDK 21)
**Interfaz grafica:** JavaFX 21
**Extension de archivos fuente:** `.dac`

---

## Tabla de Contenidos

1. [Proposito del Proyecto](#1-proposito-del-proyecto)
2. [Problema que Resuelve](#2-problema-que-resuelve)
3. [Arquitectura General](#3-arquitectura-general)
4. [Nucleo del Compilador](#4-nucleo-del-compilador)
5. [Modulo de Flujo](#5-modulo-de-flujo)
6. [Modulo de Base de Datos](#6-modulo-de-base-de-datos)
7. [Modulo de Redes](#7-modulo-de-redes)
8. [Tabla de Simbologia Estatica](#8-tabla-de-simbologia-estatica)
9. [Interfaz Grafica (JavaFX)](#9-interfaz-grafica-javafx)
10. [Decisiones de Disenio](#10-decisiones-de-disenio)
11. [Estructura de Archivos del Proyecto](#11-estructura-de-archivos-del-proyecto)

---

## 1. Proposito del Proyecto

**Diagrams As Code (DAC)** es un compilador pedagogico disenado para ilustrar de forma
practica las tres fases clasicas del analisis de lenguajes formales:

  1. Analisis Lexico   (Lexer)
  2. Analisis Sintactico (Parser / Gramatica)
  3. Analisis Semantico  (Tabla de Simbolos / Validacion de contexto)

El proyecto toma como entrada un archivo de texto plano con extension `.dac` escrito
en un lenguaje propio disenado especificamente para el proyecto. La salida es:

  - Una lista de tokens clasificados (fase lexica)
  - Un Arbol de Sintaxis Abstracta (AST) por modulo (fase sintactica)
  - Un registro de identificadores con sus roles (fase semantica)
  - Mensajes de error detallados con sugerencia de correccion

El objetivo NO es generar una imagen de diagrama final, sino demostrar
el proceso interno de compilacion de forma visible y educativa.

---

## 2. Problema que Resuelve

### Por que se creo este proyecto

Aprender teoria de compiladores (automatas, gramaticas formales, AST) suele ser
abstracto. Este proyecto materializa esos conceptos con un caso real:

  - El estudiante escribe codigo DAC
  - El sistema lo analiza fase por fase
  - Cada fase produce una salida visible (tokens, AST, tabla de simbolos)
  - Los errores se reportan con contexto pedagogico (que fallo y como corregirlo)

### Por que se eligio el dominio de "diagramas"

Los diagramas (flujo, BD, redes) tienen una gramatica natural sencilla:
declarar elementos y luego conectarlos. Esto resulta en:

  - Gramaticas de complejidad baja-media (ideales para ensenar)
  - Vocabulario familiar para estudiantes de ingenieria
  - Tres modulos con suficiente diferencia entre si para mostrar extension del DSL

---

## 3. Arquitectura General

### Patron de Disenio: Pipeline de Compilacion

El proyecto implementa el patron clasico de pipeline donde cada etapa
produce la entrada para la siguiente y puede detener la cadena ante errores.

  [ Archivo .dac ]
        |
        v
  [ LexerBase ]
  - Lee el codigo caracter por caracter
  - Agrupa secuencias en tokens con tipo y numero de linea
  - Absorbe comentarios (// y #) sin generar tokens
  - Reporta caracteres invalidos al ManejadorErrores
        |
        v (List<Token>)
  [ ParserBase ]
  - Consume tokens de la cabecera (meta-instrucciones + diagrama <Tipo>;)
  - Determina el modulo activo
  - Bloquea el contexto en la TablaSimbolos
  - Delega el resto de tokens al parser del modulo correcto
        |
        v (tokens restantes)
  [ FlujoParser | BDParser | RedesParser ]
  - Implementan la gramatica especifica del modulo
  - Construyen el AST del diagrama
  - Validan semantica (unicidad, existencia de referencias)
  - Registran identificadores en la TablaSimbolos
        |
        v
  [ AST + TablaSimbolos + ManejadorErrores ]
  - Resultado final consultable por la interfaz

### Por que este patron

La separacion en fases permite:
  - Detener el proceso al primer error lexico (no tiene sentido parsear tokens invalidos)
  - Acumular multiples errores sintacticos/semanticos en una sola pasada
  - Agregar nuevos modulos sin tocar el nucleo
  - Probar cada fase de forma aislada

---

## 4. Nucleo del Compilador

### 4.1 LexerBase.java

**Que hace:**
Convierte el codigo fuente (String) en una lista de objetos Token.

**Por que es una clase base separada:**
El lexer es identico para todos los modulos. El vocabulario base del lenguaje
(puntuacion, literales, identificadores) es compartido. Solo la palabra reservada
`diagrama` recibe un tipo de token propio (PR_DIAGRAMA); el resto de palabras
reservadas son reconocidas semanticamente por el parser, no lexicamente.

**Decisiones de implementacion:**
- Se usa un indice entero (int indice) en vez de un iterador para
  permitir retroceder o hacer lookahead sin complejidad.
- Los comentarios se absorben con un bucle simple hasta encontrar '\n'.
  No generan token porque no aportan informacion al compilador.
- Los identificadores incluyen letras, digitos y guion bajo (_) pero
  deben comenzar con letra o guion bajo, siguiendo la convencion de Java/C.

**Tabla de tokens producidos:**

  Token           | Cuando se genera
  ----------------|--------------------------------------
  PR_DIAGRAMA     | Cuando el lexema es exactamente "diagrama"
  IDENTIFICADOR   | Cualquier otra palabra (incluyendo palabras reservadas de modulo)
  TEXTO_LITERAL   | Cadena entre comillas dobles
  PUNTO_Y_COMA    | Caracter ;
  DOS_PUNTOS      | Caracter :
  LLAVE_IZQ       | Caracter {
  LLAVE_DER       | Caracter }
  EOF             | Final del archivo (siempre se agrega al final)

---

### 4.2 Token.java

**Que hace:**
Modelo de datos que representa la unidad minima del lenguaje.

**Campos:**
  - tipo    : enum Token.Tipo  (clasificacion del token)
  - lexema  : String           (texto original del codigo fuente)
  - linea   : int              (numero de linea donde aparece)

**Por que guardar la linea:**
Para reportar errores con precision. Cuando el parser o el analizador semantico
detectan un problema, pueden indicar exactamente en que linea ocurrio.

---

### 4.3 ParserBase.java

**Que hace:**
Analiza la cabecera obligatoria del archivo y delega al submodulo.

**Responsabilidades:**
  1. Procesar meta-instrucciones (autor, version, tema, exportar, importar)
  2. Detectar la instruccion "diagrama <Tipo>;"
  3. Bloquear el contexto en la TablaSimbolos
  4. Recortar la lista de tokens y pasarsela al parser del modulo

**Por que la cabecera es obligatoria:**
Sin ella el compilador no sabe a que modulo delegar. Es el punto de
entrada semantico del lenguaje: define el contexto de todo lo que sigue.

**Como funciona la delegacion:**
Se usa un switch sobre el tipo de diagrama (String) que instancia el
parser correspondiente. Agregar un nuevo modulo solo requiere agregar
un nuevo caso al switch.

  switch (tipoDiagrama) {
      case "flujo"  --> new FlujoParser(...)
      case "bd"     --> new BDParser(...)
      case "redes"  --> new RedesParser(...)
      default       --> error semantico "modulo no reconocido"
  }

---

### 4.4 TablaSimbolos.java

**Que hace:**
Actua como la memoria del compilador durante el analisis semantico.

**Estructura interna:**
  Map<String, String>  nombre --> rol/tipo

**Operaciones:**
  - registrar(nombre, rol) : boolean
    Intenta agregar el identificador. Retorna false si ya existe (duplicado).
  - existe(nombre) : boolean
    Consulta si un identificador fue declarado (para validar referencias).
  - bloquearContexto(contexto)
    Registra el modulo activo. Util para diagnosticos y para la interfaz.
  - limpiar()
    Reinicia la tabla entre compilaciones.

**Por que es un HashMap:**
El acceso a nombre en O(1) es ideal para la validacion semantica, que
consulta la existencia de identificadores con frecuencia durante el parseo.

**Por que el contexto se "bloquea":**
Una vez que el ParserBase decide el tipo de diagrama, ese contexto es
exclusivo para todo el archivo. No puede cambiarse a mitad del archivo.
El bloqueo hace explicito este invariante.

---

### 4.5 ManejadorErrores.java

**Que hace:**
Acumula todos los errores de compilacion en una lista con formato pedagogico.

**Formato de cada error:**
  ERROR DE COMPILACION [Linea N] [Contexto: fase]
  Detalle: descripcion del problema
  Sugerencia: como corregirlo

**Por que acumular en vez de lanzar excepcion:**
Lanzar una excepcion detiene la compilacion al primer error. Acumular
permite reportar multiples errores en una sola pasada, lo que es mas
util pedagogicamente (el estudiante ve todos sus errores de una vez).

**Recuperacion de panico:**
Los parsers de modulo implementan recuperacion de panico: cuando detectan
un error, avanzan hasta el siguiente punto de sincronizacion (;) y
continuan parseando. Esto evita que un solo error genere decenas de
errores en cascada.

---

## 5. Modulo de Flujo

### Archivo: FlujoParser.java

**Que hace:**
Parsea archivos con `diagrama Flujo;` y construye el AST de un diagrama de flujo.

**Por que dos tipos de instruccion:**
  - Nodos simples (inicio, fin): solo necesitan un identificador. Su forma visual
    en un diagrama es fija (ovalo) y no lleva descripcion.
  - Nodos con texto (nodo, condicion, bucle, etc.): llevan una descripcion que
    indica lo que hace ese paso. Son la mayoria de los nodos de un diagrama real.

**Verbo de conexion: "conecta"**
Se eligio un verbo en espanol para diferenciarlo claramente de palabras reservadas
de otros lenguajes y para reforzar el caracter pedagogico en espanol.

**Validacion semantica de conexiones:**
Antes de agregar una NodoConexion al AST, el parser verifica que tanto
el origen como el destino existan en la TablaSimbolos. Si alguno no
existe, reporta error semantico con el nombre del identificador faltante.

### Nodos del AST del modulo Flujo

  Clase           | Cuando se crea          | Informacion que guarda
  ----------------|-------------------------|----------------------------
  NodoProceso     | Para todos los nodos    | id + descripcion (o rol)
  NodoConexion    | Para cada "conecta"     | id origen + id destino
  NodoInicio      | (reservado, sin uso activo) | id

  Todos heredan de NodeAST (clase abstracta base de polimorfismo).
  RaizFlujoAST agrupa todos los nodos en una lista ordenada.

---

## 6. Modulo de Base de Datos

### Archivo: BDParser.java

**Que hace:**
Parsea archivos con `diagrama BD;` y construye el AST de un esquema de BD.

**Por que dos tipos de componente:**
  - Bloques con atributos (tabla, vista, esquema, paquete): modelan objetos
    que tienen estructura interna (columnas, campos). Requieren { }.
  - Componentes lineales (procedimiento, indice, disparador, etc.): son
    objetos de BD que no tienen atributos en el contexto del diagrama.
    Solo necesitan un nombre.

**Lectura de atributos:**
Cada atributo dentro de un bloque sigue la forma:
  nombre : TipoDato [Modificador] ;

El modificador (PK, FK) es opcional. El lexer lo tokeniza como IDENTIFICADOR
igual que el tipo de dato. El parser decide si es modificador segun su posicion
(tercer identificador de la linea).

**Verbo de relacion: "relaciona"**
Distinto de "conecta" (Flujo) para reforzar que cada modulo tiene su propio
vocabulario. Intentar usar "conecta" en un diagrama BD genera un error que
indica exactamente cual es el verbo correcto.

### Nodos del AST del modulo BD

  Clase           | Cuando se crea              | Informacion que guarda
  ----------------|-----------------------------|--------------------------
  NodoTabla       | Para tabla/vista/esquema/paquete | nombre + lista de Atributo
  NodoRelacion    | Para cada "relaciona"       | nombre origen + nombre destino
  Atributo        | Por cada campo dentro de {} | nombre + tipoDato + modificador

  Todos (excepto Atributo) heredan de NodeAST.
  RaizBDAST agrupa todos los nodos.

---

## 7. Modulo de Redes

### Archivo: RedesParser.java

**Que hace:**
Parsea archivos con `diagrama Redes;` y construye el AST de una topologia de red.

**Por que los componentes llevan tipo:**
Un dispositivo de red no es solo un nombre; su tipo (Router, Switch, Firewall,
LoadBalancer) define su rol visual y funcional en el diagrama. Por eso la
sintaxis es: `dispositivo <Nombre> <Tipo> { ... };`

El tipo se registra en la TablaSimbolos como parte del rol:
  "dispositivo_Router"  "dispositivo_Firewall"  etc.

Esto permite al IDE (o a un generador futuro) saber que icono o forma
usar para representar cada nodo.

**Bloque de propiedades opcional:**
A diferencia del modulo BD donde los bloques son obligatorios para tablas,
en Redes el bloque { } es completamente opcional. Un dispositivo puede
declararse sin propiedades o con varias propiedades clave-valor.

Esto refleja la realidad: en un diagrama de redes a veces interesa mostrar
la IP, la region, el numero de nodos, etc., pero no siempre es necesario.

**Verbo de enlace: "enlaza"**
Distinto de "conecta" y "relaciona" para mantener el vocabulario especifico
de cada dominio. En redes, los dispositivos se "enlazan", no se "conectan"
ni se "relacionan".

### Nodos del AST del modulo Redes

  Clase             | Cuando se crea            | Informacion que guarda
  ------------------|---------------------------|----------------------------
  NodoDispositivo   | Para cada componente      | nombre + tipo + configuracion
  NodoEnlace        | Para cada "enlaza"        | nombre origen + nombre destino

  RaizRedesAST agrupa todos los nodos.
  Nota: RaizRedesAST usa List<Object> en vez de List<NodeAST> porque Redes
  tiene su propia jerarquia de nodos independiente del modulo Flujo y BD.

---

## 8. Tabla de Simbologia Estatica

### Archivo: TablaSimbologiaEstatica.java

**Que es:**
Un catalogo estatico (cargado una sola vez al iniciar la JVM via bloque static {})
que registra todos los simbolos del lenguaje DAC con su clasificacion completa.

**Por que se creo:**
El LexerBase solo distingue entre `diagrama` (PR_DIAGRAMA) e IDENTIFICADOR para
el resto de palabras. Las palabras reservadas de modulo (inicio, tabla, dispositivo,
etc.) son tecnicamente identificadores; su caracter reservado lo decide el parser
por posicion, no el lexer por tipo de token.

La TablaSimbologiaEstatica resuelve esta ambiguedad: es el catalogo oficial de
referencia que dice "estos son los simbolos del lenguaje, sus categorias y lo
que hacen", independientemente de como los clasifique el lexer internamente.

**Estructura de cada entrada:**

  Campo     | Tipo   | Descripcion
  ----------|--------|---------------------------------------------
  lexema    | String | El texto exacto tal como aparece en el .dac
  tipoToken | String | Clasificacion del token segun LexerBase
  categoria | String | Grupo semantico (Flujo-Nodo, BD-Bloque, etc.)
  descripcion | String | Explicacion del proposito del simbolo

**Metodo filtrar(String modulo):**
Permite obtener un subconjunto de la tabla segun el modulo activo.
Los filtros de modulo (Flujo, BD, Redes) incluyen automaticamente los
simbolos globales (Cabecera, Meta-Instruccion) y de puntuacion porque
son comunes a todos los diagramas.

  Opcion      | Que incluye
  ------------|-----------------------------------------------------
  "Todos"     | Los 42 simbolos completos
  "Flujo"     | Global + Flujo-Nodo + Flujo-Verbo + Puntuacion
  "BD"        | Global + BD-Bloque + BD-Lineal + BD-Verbo + Puntuacion
  "Redes"     | Global + Redes-Componente + Redes-Verbo + Puntuacion
  "Global"    | Solo Cabecera y Meta-Instruccion (6 simbolos)
  "Puntuacion"| Solo Puntuacion + Literal + Comentario (7 simbolos)

**Uso en consola (Main.java):**
`TablaSimbologiaEstatica.imprimir()` se llama al inicio de cada ejecucion
para mostrar el catalogo completo antes del analisis del archivo.

**Uso en interfaz (MainFX.java):**
El metodo `filtrar()` alimenta el TableView de la ventana modal de simbologia.

---

## 9. Interfaz Grafica (JavaFX)

### Archivo: MainFX.java

**Que es:**
La interfaz grafica del IDE pedagogico. Implementa `javafx.application.Application`
y construye toda la UI de forma programatica (sin FXML).

**Por que JavaFX y no Swing:**
JavaFX es el framework moderno de Java para UI de escritorio. Provee:
  - Layout managers mas flexibles (HBox, VBox, BorderPane, SplitPane)
  - Estilos CSS via setStyle()
  - TabPane para edicion multi-archivo
  - TextArea nativa con scroll
  - TableView con columnas y cell factories para datos tabulares

**Por que sin FXML:**
Para mantener toda la logica de UI en un solo archivo Java y facilitar
la lectura y modificacion pedagogica sin depender de un editor visual.

### Estructura de la ventana principal

  BorderPane (root)
  ├── TOP: HBox toolbar
  │    ├── Label "Diagrams As Code"
  │    ├── Button Nuevo       (naranja)
  │    ├── Button Abrir       (azul)
  │    ├── Button Guardar     (morado)
  │    ├── Button Compilar    (verde)
  │    ├── Region espaciador  (crece para llenar el espacio)
  │    ├── Button Simbologia  (verde oscuro) --> ventana modal tabla
  │    └── Button Manual      (azul oscuro)  --> ventana modal manual
  │
  ├── CENTER: SplitPane
  │    ├── VBox panelEditor
  │    │    └── TabPane (una Tab por archivo abierto)
  │    └── VBox panelAnalisis
  │         ├── Label "1. Tokens:"
  │         ├── TextArea txtTokens   (solo lectura)
  │         ├── Label "2. Tabla de Simbolos:"
  │         └── TextArea txtSimbolos (solo lectura)
  │
  └── BOTTOM: VBox panelConsola
       ├── Label "Consola:"
       └── TextArea txtConsola (solo lectura)

### Como funciona el compilador en la GUI

  ejecutarCompilador() {
      1. Obtener el texto del TextArea de la pestana activa
      2. Crear ManejadorErrores y TablaSimbolos limpios
      3. LexerBase.tokenizar()  --> llenar txtTokens
      4. Si hay errores lexicos --> mostrar en txtConsola y detener
      5. ParserBase.analizarCabecera()  --> delega al modulo
      6. Llenar txtSimbolos con el contenido de TablaSimbolos
      7. Si hay errores --> mostrar en txtConsola
      8. Si no hay errores --> "COMPILACION EXITOSA"
  }

### Por que el espaciador (Region con HGrow.ALWAYS)

Los botones de accion principal (Nuevo, Abrir, Guardar, Compilar) estan
al lado izquierdo del titulo. Los botones de referencia (Simbologia, Manual)
estan al extremo derecho. El Region con Priority.ALWAYS consume todo el
espacio disponible entre ambos grupos, empujando los botones de referencia
al extremo sin importar el ancho de la ventana.

### Ventana modal de Simbologia

  - Stage con initModality(APPLICATION_MODAL) e initOwner(primaryStage)
  - Bloquea la ventana principal mientras esta abierta
  - TableView con 4 columnas usando SimpleStringProperty como CellValueFactory
  - TableRow factory para colorear filas por categoria
  - ComboBox que llama a TablaSimbologiaEstatica.filtrar() y actualiza
    el ObservableList de la tabla dinamicamente

### Ventana modal del Manual

  - Stage con initModality(APPLICATION_MODAL) e initOwner(primaryStage)
  - Lee `manual_diagrams_as_code.md` desde el directorio de trabajo
  - Muestra el contenido en un TextArea monoespaciado de solo lectura
  - Si el archivo no se encuentra, muestra un mensaje de error descriptivo

---

## 10. Decisiones de Disenio

### Por que las palabras reservadas de modulo son IDENTIFICADOR y no PR_*

Alternativa descartada: crear un token PR_INICIO, PR_NODO, PR_TABLA, etc.

Razon: el lexer seria especifico de cada modulo. Si se agrega un nuevo modulo,
hay que modificar el lexer. La decision de disenio fue mantener el lexer
completamente generico y dejar que cada parser reconozca sus palabras
reservadas por contexto (posicion en la gramatica).

Ventaja: el lexer es un componente estable que nunca necesita cambios.
La extension del lenguaje se hace agregando parsers, no modificando el nucleo.

### Por que tres modulos independientes con sus propios AST

Alternativa descartada: un solo AST generico con nodos tipados.

Razon: los tres modulos tienen gramaticas genuinamente diferentes:
  - Flujo: nodos unarios con conexiones dirigidas
  - BD: nodos con estructura interna (atributos) y relaciones
  - Redes: nodos con tipo y configuracion clave-valor

Un AST generico habria requerido mucho casting o generics complejos.
Tres AST independientes son mas simples de entender y mas seguros de tipo.

### Por que el RedesParser esta en modulos/ y no en modulos/redes/

El RedesParser.java esta ubicado en com.diagramas.modulos.RedesParser
en vez de com.diagramas.modulos.redes.RedesParser como seria lo ideal.
Esto es un residuo de como se agrego el modulo durante el desarrollo.
Los nodos AST de Redes si estan correctamente en com.diagramas.modulos.redes.ast.

### Por que la recuperacion de panico sincroniza en ';'

El punto y coma es el terminador universal de toda instruccion en DAC.
Cuando el parser encuentra una instruccion malformada, avanzar hasta ';'
garantiza quedar en un estado limpio listo para la siguiente instruccion.
Esto minimiza los errores en cascada sin necesidad de una gramatica de
recuperacion mas compleja.

### Por que la TablaSimbologiaEstatica usa Collections.unmodifiableList

La lista es un catalogo de definicion del lenguaje. No debe modificarse
en tiempo de ejecucion. El wrapper unmodifiable lanza UnsupportedOperationException
si algo intenta agregar o eliminar entradas, protegiendola de modificaciones
accidentales desde cualquier parte del codigo.

---

## 11. Estructura de Archivos del Proyecto

  DiagramsAsCode/
  |
  |-- src/com/diagramas/
  |   |-- Main.java              Punto de entrada CLI (sin JavaFX)
  |   |-- MainFX.java            Punto de entrada GUI (con JavaFX)
  |   |
  |   |-- core/
  |   |   |-- LexerBase.java         Analizador lexico
  |   |   |-- Token.java             Modelo de token (tipo + lexema + linea)
  |   |   |-- TipoToken.java         (Archivo vacio, tipos definidos en Token.java)
  |   |   |-- ParserBase.java        Parser de cabecera + delegador de modulos
  |   |   |-- TablaSimbolos.java     Registro de identificadores en tiempo de analisis
  |   |   |-- ManejadorErrores.java  Acumulador de errores de compilacion
  |   |   |-- TablaSimbologiaEstatica.java  Catalogo estatico de simbolos del lenguaje
  |   |
  |   |-- modulos/
  |       |-- RedesParser.java       Parser del modulo Redes (deberia estar en redes/)
  |       |
  |       |-- flujo/
  |       |   |-- FlujoParser.java   Parser del modulo Flujo
  |       |   |-- ast/
  |       |       |-- NodeAST.java        Clase base abstracta de nodos Flujo
  |       |       |-- NodoProceso.java    Nodo generico (nodo/condicion/entrada/etc.)
  |       |       |-- NodoConexion.java   Arista dirigida entre dos nodos
  |       |       |-- NodoInicio.java     Nodo de inicio (reservado)
  |       |       |-- RaizFlujoAST.java   Contenedor raiz del AST de Flujo
  |       |
  |       |-- bd/
  |       |   |-- BDParser.java      Parser del modulo Base de Datos
  |       |   |-- ast/
  |       |       |-- NodeAST.java        Clase base abstracta de nodos BD
  |       |       |-- NodoTabla.java      Tabla/Vista/Esquema/Paquete con atributos
  |       |       |-- NodoRelacion.java   Relacion entre dos entidades
  |       |       |-- Atributo.java       Campo de una tabla (nombre + tipo + modificador)
  |       |       |-- RaizBDAST.java      Contenedor raiz del AST de BD
  |       |
  |       |-- redes/
  |           |-- ast/
  |               |-- NodoDispositivo.java  Componente de red (nombre + tipo + config)
  |               |-- NodoEnlace.java       Enlace entre dos dispositivos
  |               |-- RaizRedesAST.java     Contenedor raiz del AST de Redes
  |
  |-- out/production/DiagramsAsCode/   Clases compiladas (generado automaticamente)
  |
  |-- javafx-sdk-21.0.9/               JavaFX SDK para ejecucion desde linea de comandos
  |
  |-- test_flujo_completo.dac          Archivo de prueba del modulo Flujo
  |-- test_bd_avanzado.dac             Archivo de prueba del modulo BD
  |-- test_redes_nube.dac              Archivo de prueba del modulo Redes
  |
  |-- manual_diagrams_as_code.md       Manual de usuario del lenguaje DAC
  |-- documentacion_tecnica.md         Este archivo
  |-- DiagramsAsCode.iml               Configuracion del modulo IntelliJ IDEA

---

## Resumen de Componentes por Funcion

  Componente                  | Funcion
  ----------------------------|---------------------------------------------------
  LexerBase                   | Texto --> Tokens
  Token                       | Modelo de unidad lexica
  ParserBase                  | Cabecera + delegacion de modulos
  FlujoParser                 | Gramatica + AST del modulo Flujo
  BDParser                    | Gramatica + AST del modulo Base de Datos
  RedesParser                 | Gramatica + AST del modulo Redes
  TablaSimbolos               | Memoria semantica (identificadores y sus roles)
  ManejadorErrores            | Acumulador de errores con formato pedagogico
  TablaSimbologiaEstatica     | Catalogo oficial de simbolos del DSL
  Main                        | Punto de entrada CLI (sin JavaFX)
  MainFX                      | IDE grafico con editor, compilador y visualizadores

---

*Documentacion Tecnica — Diagrams As Code*
*Compilador pedagogico de DSL para diagramas*
