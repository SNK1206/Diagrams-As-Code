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
8. [Modulo Conceptual](#8-modulo-conceptual)
9. [Modulo UML](#9-modulo-uml)
10. [Gramatica Formal del Lenguaje](#10-gramatica-formal-del-lenguaje)
11. [Tabla de Simbologia Estatica](#11-tabla-de-simbologia-estatica)
12. [Interfaz Grafica (JavaFX)](#12-interfaz-grafica-javafx)
13. [Decisiones de Disenio](#13-decisiones-de-disenio)
14. [Estructura de Archivos del Proyecto](#14-estructura-de-archivos-del-proyecto)
15. [Historial de Cambios](#15-historial-de-cambios)

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

Los diagramas (flujo, BD, redes, conceptual, UML) tienen una gramatica natural sencilla:
declarar elementos y luego conectarlos. Esto resulta en:

  - Gramaticas de complejidad baja-media (ideales para ensenar)
  - Vocabulario familiar para estudiantes de ingenieria
  - Cinco modulos con suficiente diferencia entre si para mostrar extension del DSL

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
  [ FlujoParser | BDParser | RedesParser | ConceptualParser | UMLParser ]
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

**Campos de estado:**
  - int indice        : posicion actual dentro del String de codigo fuente
  - int lineaActual   : linea del caracter que se esta leyendo (base 1)
  - int columnaActual : columna del caracter actual dentro de la linea (base 1)

**Decisiones de implementacion:**
- Se usa un indice entero (int indice) en vez de un iterador para
  permitir retroceder o hacer lookahead sin complejidad.
- El tracking de columna se reinicia a 1 en cada salto de linea (\n),
  permitiendo reportar errores con linea Y columna exactas.
- Los comentarios se absorben con un bucle simple hasta encontrar '\n'.
  No generan token porque no aportan informacion al compilador.
- Los identificadores incluyen letras, digitos y guion bajo (_) pero
  deben comenzar con letra o guion bajo, siguiendo la convencion de Java/C.

**Codigos de error lexicos generados:**

  Codigo | Condicion
  -------|---------------------------------------------------------------
  EL01   | Caracter invalido que no pertenece al alfabeto del lenguaje
  EL02   | Identificador invalido: inicia con digito (ej: "2nodo") o
         | contiene caracter especial (ej: "mi@nodo")
  EL03   | Palabra reservada mal escrita por mayusculas/minusculas
         | (ej: "Nodo", "TABLA", "Extiende"). El lexer emite el token
         | corregido para que el parser pueda continuar sin errores en cascada.

**Codigos de error sintacticos detectados por el Lexer:**

  Codigo | Condicion
  -------|---------------------------------------------------------------
  ES55   | Cadena de texto sin cerrar — falta la comilla doble de cierre.
         | El lexer lo detecta pero se clasifica como error sintactico
         | porque refleja una violacion de la gramatica (produccion
         | texto_literal nunca se cierra), no del alfabeto.
  ES56   | Palabra reservada usada como nombre de identificador de usuario.
         | Se genera en validarSiguienteTipo() de cada parser modulo cuando
         | se espera IDENTIFICADOR pero se recibe PALABRA_RESERVADA o PR_DIAGRAMA.

**Tabla de tokens producidos:**

  Token            | Cuando se genera
  -----------------|--------------------------------------
  PR_DIAGRAMA      | Cuando el lexema es exactamente "diagrama"
  PALABRA_RESERVADA| Cuando el lexema coincide con alguna de las ~40 palabras
                   | reservadas de modulo (inicio, nodo, tabla, dispositivo,
                   | clase, extiende, etc.). Lista definida en PALABRAS_RESERVADAS.
  IDENTIFICADOR    | Palabra formada por letras/digitos/_ que NO es "diagrama"
                   | ni ninguna palabra reservada de modulo
  TEXTO_LITERAL    | Cadena entre comillas dobles
  PUNTO_Y_COMA     | Caracter ;
  DOS_PUNTOS       | Caracter :
  LLAVE_IZQ        | Caracter {
  LLAVE_DER        | Caracter }
  EOF              | Final del archivo (siempre se agrega al final)

---

### 4.2 Token.java

**Que hace:**
Modelo de datos que representa la unidad minima del lenguaje.

**Campos:**
  - tipo    : enum Token.Tipo  (clasificacion del token)
  - lexema  : String           (texto original del codigo fuente)
  - linea   : int              (numero de linea donde aparece el token, base 1)
  - columna : int              (columna donde inicia el token en la linea, base 1)

**Constructores:**
  - Token(tipo, lexema, linea, columna) : constructor completo
  - Token(tipo, lexema, linea)          : compatibilidad — columna queda en 0

**Por que guardar linea y columna:**
Para reportar errores con precision. Cuando el parser o el analizador semantico
detectan un problema, pueden indicar exactamente en que linea y columna ocurrio.
La columna permite al estudiante localizar el error dentro de la linea.

---

### 4.3 ParserBase.java

**Que hace:**
Analiza la cabecera obligatoria del archivo y delega al submodulo.

**Responsabilidades:**
  1. Procesar meta-instrucciones (autor, version, tema)
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
      case "flujo"      --> new FlujoParser(...)
      case "bd"         --> new BDParser(...)
      case "redes"      --> new RedesParser(...)
      case "conceptual" --> new ConceptualParser(...)
      case "uml"        --> new UMLParser(...)
      default           --> error semantico "modulo no reconocido"
  }

---

### 4.4 TablaSimbolos.java

**Que hace:**
Actua como la memoria del compilador durante el analisis semantico.
Registra cada identificador junto a su rol, la linea donde aparece y
genera una descripcion legible automaticamente.

**Estructura interna:**
  LinkedHashMap<String, Entrada>  nombre --> Entrada{rol, linea}

  La clase interna Entrada almacena:
    - String rol   : tipo asignado al identificador (ej. "nodo", "tabla", "inicio")
    - int    linea : linea del codigo fuente donde fue declarado

  Se usa LinkedHashMap (en lugar de HashMap) para preservar el orden de
  insercion, de forma que la tabla se muestre en el mismo orden en que
  aparecen los identificadores en el archivo fuente.

**Operaciones:**
  - registrar(nombre, rol, linea) : boolean
    Intenta agregar el identificador con su rol y linea de declaracion.
    Retorna false si ya existe (error semantico: identificador duplicado).
  - registrar(nombre, rol) : boolean   [sobrecarga de compatibilidad]
    Llama al anterior con linea = 0 (sin tracking de linea).
  - existe(nombre) : boolean
    Consulta si un identificador fue declarado (valida referencias hacia adelante).
  - bloquearContexto(contexto)
    Registra el modulo activo (ej. "Flujo", "BD"). Util para la GUI.
  - limpiar()
    Reinicia la tabla entre compilaciones.
  - descripcionPara(nombre, rol) : String   [metodo estatico]
    Genera una descripcion legible basada en el nombre o rol del simbolo.
    Casos cubiertos: meta-instrucciones, tipo_diagrama, todos los nodos
    de Flujo, BD, Redes, Conceptual y UML.

**Ejemplo de salida en la GUI:**

  IDENTIFICADOR          | TIPO / ROL         | LINEA | DESCRIPCION
  ───────────────────────────────────────────────────────────────────────────
  autor                  | Sin Punto Y Coma   |     2 | Metadato: autor del documento
  diagrama               | Flujo              |     7 | Encabezado del diagrama (Flujo)
  Flujo                  | tipo_diagrama      |     7 | Modulo activo del compilador
  Arranque               | inicio             |     8 | Nodo de inicio del flujo

---

### 4.5 ManejadorErrores.java

**Que hace:**
Acumula todos los errores de compilacion en una lista con formato pedagogico.

**Formato de cada error:**
  ==================================================
  [ERROR] [ELxx / ESxx] LEXICO | SINTACTICO [Linea N]
  Detalle: descripcion del problema
  Consejo: como corregirlo
  ==================================================

**Dos sobrecargas de reportarError:**
  - reportarError(int linea, String contexto, String mensaje, String consejo)
    Para errores sin codigo categorizado (semanticos simples).
  - reportarError(String codigo, int linea, String contexto, String mensaje, String consejo)
    Para errores con codigo EL/ES. Detecta automaticamente si es lexico o
    sintactico segun si el contexto contiene "Lexico".

**Metodo tieneErroresLexicos():**
  Recorre la lista de errores acumulados y retorna true solo si alguno
  contiene la cadena "LEXICO" en su texto formateado. Esto distingue
  errores EL01-EL03 (verdaderamente lexicos) de ES55 (cadena sin cerrar,
  detectada por el lexer pero clasificada como sintactica). La GUI usa
  este metodo para determinar el prefijo de la consola correctamente.

**Limite de errores (MAX_ERRORES = 1000):**
  Si se superan 1000 errores, los adicionales se suprimen con un aviso.
  Previene que un solo error en cascada sature la salida con miles de
  mensajes inutil para el estudiante.

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
  NodoInicio      | (definido, sin uso activo) | id

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

### Archivo: RedesParser.java (modulos/redes/)

**Que hace:**
Parsea archivos con `diagrama Redes;` y construye el AST de una topologia de red.

**Por que los componentes llevan tipo:**
Un dispositivo de red no es solo un nombre; su tipo (Router, Switch, Firewall,
LoadBalancer) define su rol visual y funcional en el diagrama. Por eso la
sintaxis es: `dispositivo <Nombre> <Tipo> { ... };`

El tipo se registra en la TablaSimbolos como parte del rol:
  "dispositivo_Router"  "dispositivo_Firewall"  etc.

**Bloque de propiedades opcional:**
A diferencia del modulo BD donde los bloques son obligatorios para tablas,
en Redes el bloque { } es completamente opcional. Un dispositivo puede
declararse sin propiedades o con varias propiedades clave-valor.

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

---

## 8. Modulo Conceptual

### Archivo: ConceptualParser.java (modulos/conceptual/)

**Que hace:**
Parsea archivos con `diagrama Conceptual;` y construye el AST de un mapa conceptual.

**Tipos de nodo:**
  - concepto   : idea o entidad principal del mapa
  - categoria  : agrupacion logica de conceptos
  - propiedad  : caracteristica o atributo de un concepto

Todos llevan un TEXTO_LITERAL con su descripcion. No existen nodos sin descripcion
en este modulo — la descripcion es parte obligatoria de la declaracion.

**Verbos de relacion:**
  - agrupa   : una categoria contiene o agrupa un concepto
  - asocia   : dos conceptos estan relacionados entre si
  - depende  : un concepto depende de otro

**Codigos de error:**
Este modulo usa la sobrecarga extendida de reportarError con codigos (ES35–ES42)
para categorizar los errores de forma mas precisa que los modulos anteriores.

### Nodos del AST del modulo Conceptual

  Clase                  | Cuando se crea         | Informacion que guarda
  -----------------------|------------------------|----------------------------------
  NodoConcepto           | Para concepto/categoria/propiedad | nombre + rol + descripcion
  NodoRelacionConceptual | Para cada relacion     | origen + verbo + destino

  Ambas heredan de NodeAST (com.diagramas.modulos.conceptual.ast).
  RaizConceptualAST agrupa todos los nodos.

---

## 9. Modulo UML

### Archivo: UMLParser.java (modulos/uml/)

**Que hace:**
Parsea archivos con `diagrama UML;` y construye el AST de un diagrama de clases UML.

**Tipos de declaracion:**
  - clase     : bloque con atributos y metodos internos (requiere { })
  - interfaz  : declaracion lineal sin cuerpo
  - enum      : declaracion lineal sin cuerpo

**Miembros de clase:**
Dentro de un bloque `clase { }` se declaran miembros con la forma:
  (atributo | metodo) nombre : tipo ;

Un bloque de clase vacio `clase Nombre { }` es sintacticamente valido.

**Verbos de relacion:**
  - extiende   : herencia entre clases
  - implementa : una clase implementa una interfaz
  - usa        : dependencia entre dos clases

**Codigos de error:**
Este modulo usa la sobrecarga extendida de reportarError con codigos (ES43–ES54).

**Recuperacion de panico extendida:**
El modulo UML implementa tres estrategias de recuperacion:
  - recuperarPanico()         : sincroniza en ';'
  - recuperarPanicoBloque()   : sincroniza en '}'
  - recuperarPanicoMiembro()  : sincroniza en ';' o '}'

### Nodos del AST del modulo UML

  Clase          | Cuando se crea              | Informacion que guarda
  ---------------|-----------------------------|----------------------------------
  NodoClase      | Para clase/interfaz/enum    | nombre + lista de NodoMiembro
  NodoMiembro    | Para atributo/metodo        | rol + nombre + tipo
  NodoRelacionUML| Para cada relacion          | origen + verbo + destino

  NodoClase y NodoRelacionUML heredan de NodeAST (com.diagramas.modulos.uml.ast).
  NodoMiembro NO hereda de NodeAST (es un componente interno de NodoClase).
  RaizUMLAST agrupa todos los nodos.

---

## 10. Gramatica Formal del Lenguaje

### Descripcion

La gramatica completa del lenguaje DAC esta documentada en formato EBNF en el
archivo dedicado:

  gramatica_formal.md

Ese archivo contiene:
  - Definicion de todos los tokens producidos por el Lexer
  - Reglas de produccion de la cabecera global (ParserBase)
  - Gramatica de cada uno de los cinco modulos
  - Vista consolidada de toda la gramatica en un solo bloque
  - Tabla de conjuntos FIRST de las instrucciones principales
  - Analisis de las propiedades de la gramatica (LL(1), resolucion de ambiguedad)

### Tipo de gramatica

La gramatica es LL(1) — analizable de izquierda a derecha con un lookahead de
exactamente 1 token. El analizador implementado en todos los parsers es un
Recursive Descent Parser (analizador descendente recursivo).

### Punto de entrada de cada modulo

  Modulo      | No-terminal raiz    | Parser responsable
  ------------|---------------------|-----------------------
  Global      | programa            | ParserBase
  Flujo       | cuerpo_flujo        | FlujoParser
  BD          | cuerpo_bd           | BDParser
  Redes       | cuerpo_redes        | RedesParser
  Conceptual  | cuerpo_conceptual   | ConceptualParser
  UML         | cuerpo_uml          | UMLParser

### Palabras reservadas por modulo

"diagrama" recibe el token PR_DIAGRAMA. Todas las demas palabras reservadas
de modulo reciben el token PALABRA_RESERVADA. El Lexer las reconoce por su
lexema exacto (sensible a mayusculas/minusculas). Si se escriben con mayusculas
incorrectas se genera EL03 y el Lexer emite el token corregido en minusculas.

  Modulo      | Palabras reservadas (token: PALABRA_RESERVADA)
  ------------|----------------------------------------------------------
  Global      | autor, version, tema
  Flujo       | inicio, fin, nodo, condicion, bucle, subproceso,
              | entrada, salida, parada, conecta
  BD          | tabla, vista, esquema, paquete, procedimiento, indice,
              | disparador, secuencia, funcion, relaciona
  Redes       | dispositivo, nube, vlan, subred, cluster, tunel,
              | zona, puerto, politica, enlaza
  Conceptual  | concepto, categoria, propiedad, agrupa, asocia, depende
  UML         | clase, interfaz, enum, atributo, metodo,
              | extiende, implementa, usa

---

## 11. Tabla de Simbologia Estatica

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

  Campo       | Tipo   | Descripcion
  ------------|--------|---------------------------------------------
  lexema      | String | El texto exacto tal como aparece en el .dac
  tipoToken   | String | Clasificacion del token segun LexerBase
  categoria   | String | Grupo semantico (Flujo-Nodo, BD-Bloque, etc.)
  descripcion | String | Explicacion del proposito del simbolo

**Total de simbolos registrados: 57**
(6 globales + 10 Flujo + 11 BD + 10 Redes + 6 Conceptual + 8 UML + 4 Puntuacion + 1 Literal + 2 Comentario)

El conteo en la GUI es dinamico: se toma de TablaSimbologiaEstatica.TABLA.size()
y se actualiza al aplicar cada filtro.

**Metodo filtrar(String modulo):**
Permite obtener un subconjunto de la tabla segun el modulo activo.

  Opcion       | Que incluye
  -------------|------------------------------------------------------
  "Todos"      | Los 57 simbolos completos
  "Flujo"      | Global + Flujo + Puntuacion
  "BD"         | Global + BD + Puntuacion
  "Redes"      | Global + Redes + Puntuacion
  "Conceptual" | Global + Conceptual + Puntuacion
  "UML"        | Global + UML + Puntuacion
  "Global"     | Solo Cabecera y Meta-instrucciones
  "Puntuacion" | Solo Puntuacion + Literal + Comentarios

**Uso en consola (Main.java):**
TablaSimbologiaEstatica.imprimir() se llama al inicio de cada ejecucion
para mostrar el catalogo completo antes del analisis del archivo.

**Uso en interfaz (MainFX.java):**
El metodo filtrar() alimenta el TableView de la ventana modal de simbologia.
El ComboBox de la ventana ofrece 8 opciones de filtro (incluye Conceptual y UML).

---

## 12. Interfaz Grafica (JavaFX)

### Archivo: MainFX.java

**Que es:**
La interfaz grafica del IDE pedagogico. Implementa javafx.application.Application
y construye toda la UI de forma programatica (sin FXML).

**Por que JavaFX y no Swing:**
JavaFX es el framework moderno de Java para UI de escritorio. Provee:
  - Layout managers mas flexibles (HBox, VBox, BorderPane, SplitPane)
  - Estilos CSS via setStyle()
  - TabPane para edicion multi-archivo
  - TextArea nativa con scroll
  - TableView con columnas y cell factories para datos tabulares

### Estructura de la ventana principal

  BorderPane (root)
  |-- TOP: HBox toolbar
  |    |-- Label "Diagrams As Code"
  |    |-- Button Nuevo          (naranja)
  |    |-- Button Abrir          (azul)
  |    |-- Button Guardar        (morado)
  |    |-- Button Compilar       (verde)
  |    |-- Button Errores Lexicos (rojo)     --> catalogo de 58 codigos EL/ES
  |    |-- Button Arbol Derivacion (teal)    --> arbol de derivacion grafico
  |    |-- Region espaciador     (crece para llenar el espacio)
  |    |-- Button Simbologia     (verde oscuro) --> tabla estatica de simbolos
  |    |-- Button Manual         (azul oscuro)  --> manual de usuario
  |    |-- Button Gramatica DAC  (verde oscuro) --> gramatica_dac.md
  |
  |-- CENTER: SplitPane
  |    |-- VBox panelEditor
  |    |    |-- TabPane (una Tab por archivo abierto)
  |    |-- VBox panelAnalisis
  |         |-- Label "1. Tokens:"
  |         |-- TextArea txtTokens   (solo lectura)
  |         |-- Label "2. Tabla de Simbolos:"
  |         |-- TextArea txtSimbolos (solo lectura)
  |
  |-- BOTTOM: VBox panelConsola
       |-- Label "Consola:"
       |-- TextArea txtConsola (solo lectura)

### Acceso al editor activo

El metodo getEditorDePestana(Tab) recupera el TextArea de codigo de la
pestana activa usando un Map<Tab, TextArea> llamado "editores".
Al crear cada pestana, el editor se registra en el mapa; al cerrarla,
se elimina. Esto evita hacer cast de nodos de la escena (mas fragil).

### Como funciona el compilador en la GUI

  ejecutarCompilador() {
      1. Obtener el TextArea activo via getEditorDePestana(pestanaActiva)
      2. Crear ManejadorErrores y TablaSimbolos limpios
      3. LexerBase.tokenizar()  --> llenar txtTokens (con linea y columna)
      4. Si hay errores lexicos --> mostrar en txtConsola pero continuar
      5. ParserBase.analizarCabecera()  --> delega al modulo
      6. Llenar txtSimbolos con 4 columnas:
            IDENTIFICADOR | TIPO/ROL | LINEA | DESCRIPCION
      7. Si hay errores --> mostrar en txtConsola
      8. Si no hay errores --> "COMPILACION EXITOSA"
  }

**Tabla de Simbolos dinamica (4 columnas):**
La GUI muestra los simbolos registrados durante la compilacion.
Las descripciones se generan automaticamente via TablaSimbolos.descripcionPara().

### Ventana modal de Simbologia

  - Stage con initModality(APPLICATION_MODAL) e initOwner(primaryStage)
  - TableView con 4 columnas usando SimpleStringProperty como CellValueFactory
  - TableRow factory para colorear filas por modulo
  - ComboBox con 8 opciones que llama a TablaSimbologiaEstatica.filtrar()
  - Conteo dinamico de simbolos al cambiar el filtro

### Ventana modal del Manual

  - Abre manual_diagrams_as_code.md desde el directorio de trabajo
  - TextArea monoespaciado, solo lectura, con scroll
  - Header coloreado por tipo de documento

### Arbol de Derivacion

  - Alineado con la gramatica formal EBNF definida en gramatica_formal.md
  - Agrupacion de tokens por instruccion: divide en ';' a profundidad 0 y en '}'
    cuando la profundidad pasa de 1 a 0 (necesario para bloques BD y UML sin ';' final)
  - Estructura del arbol:
      programa_<modulo>
      |-- meta_instruccion (0 o mas)
      |-- cabecera
      |-- cuerpo_<modulo>
          |-- <regla_gramatical> por cada instruccion del modulo
  - Reglas reconocidas por modulo:
      Flujo:      decl_nodo_simple, decl_nodo_texto, conexion_flujo
      BD:         bloque_bd (con atributo*), componente_lineal_bd, relacion_bd
      Redes:      componente_red (con propiedad_red*), enlace_red
      Conceptual: decl_concepto, relacion_conceptual
      UML:        decl_clase (con miembro_clase*), decl_lineal_uml, relacion_uml
  - Nodos: ovalo = no-terminal (regla gramatical) | rectangulo = terminal (token)
  - Soporta zoom (+/-/ajustar) y pantalla completa
  - Se muestra como pestana adicional dentro del editor

---

## 13. Decisiones de Disenio

### Por que las palabras reservadas de modulo son PALABRA_RESERVADA y no PR_* individuales

Alternativa descartada: crear un token PR_INICIO, PR_NODO, PR_TABLA, etc.

Razon: tener decenas de tipos de token individuales (uno por cada palabra reservada)
habria complicado el lexer y el parser sin ningun beneficio real. En su lugar,
todas las palabras reservadas de modulo comparten el tipo PALABRA_RESERVADA y el
parser las distingue por su lexema (su texto exacto).

Ventaja: agregar una nueva palabra reservada solo requiere incluirla en el Set
PALABRAS_RESERVADAS del LexerBase. No es necesario modificar el enum Token.Tipo
ni los parsers existentes.

**Efecto en la deteccion de ES56:**
Al cambiar las palabras reservadas de IDENTIFICADOR a PALABRA_RESERVADA, los
parsers deben aceptar ambos tipos cuando buscan verbos (conecta, relaciona, etc.)
o palabras clave (inicio, nodo, clase, etc.). validarSiguienteTipo() detecta
automaticamente el caso erroneo: si se esperaba IDENTIFICADOR pero llega
PALABRA_RESERVADA o PR_DIAGRAMA, genera ES56 en lugar del error generico.

### Por que cinco modulos con sus propios AST

Alternativa descartada: un solo AST generico con nodos tipados.

Razon: los cinco modulos tienen gramaticas genuinamente diferentes.
Un AST generico habria requerido mucho casting o generics complejos.
Cinco AST independientes son mas simples de entender y mas seguros de tipo.

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

### Por que el espaciador (Region con HGrow.ALWAYS) en la toolbar

Los botones de accion principal (Nuevo, Abrir, Guardar, Compilar, Errores, Arbol)
estan al lado izquierdo. Los botones de referencia (Simbologia, Manual) estan al
extremo derecho. El Region con Priority.ALWAYS consume todo el espacio disponible
entre ambos grupos, empujando los botones de referencia al extremo sin importar
el ancho de la ventana.

---

## 14. Estructura de Archivos del Proyecto

  DiagramsAsCode/
  |
  |-- src/com/diagramas/
  |   |-- Main.java              Punto de entrada CLI (sin JavaFX)
  |   |-- MainFX.java            Punto de entrada GUI (con JavaFX)
  |   |
  |   |-- core/
  |   |   |-- LexerBase.java              Analizador lexico
  |   |   |-- Token.java                  Modelo de token (tipo + lexema + linea)
  |   |   |-- TipoToken.java              (Archivo vacio — tipos en Token.java)
  |   |   |-- ParserBase.java             Parser de cabecera + delegador de modulos
  |   |   |-- TablaSimbolos.java          Registro de identificadores
  |   |   |-- ManejadorErrores.java       Acumulador de errores de compilacion
  |   |   |-- TablaSimbologiaEstatica.java  Catalogo estatico de 55 simbolos del DSL
  |   |
  |   |-- modulos/
  |       |-- flujo/
  |       |   |-- FlujoParser.java        Parser del modulo Flujo
  |       |   |-- ast/
  |       |       |-- NodeAST.java        Clase base abstracta de nodos Flujo
  |       |       |-- NodoProceso.java    Nodo generico (nodo/condicion/entrada/etc.)
  |       |       |-- NodoConexion.java   Arista dirigida entre dos nodos
  |       |       |-- NodoInicio.java     Nodo de inicio (definido, sin instanciar)
  |       |       |-- RaizFlujoAST.java   Contenedor raiz del AST de Flujo
  |       |
  |       |-- bd/
  |       |   |-- BDParser.java           Parser del modulo Base de Datos
  |       |   |-- ast/
  |       |       |-- NodeAST.java        Clase base abstracta de nodos BD
  |       |       |-- NodoTabla.java      Tabla/Vista/Esquema/Paquete con atributos
  |       |       |-- NodoRelacion.java   Relacion entre dos entidades
  |       |       |-- Atributo.java       Campo de una tabla (nombre + tipo + modificador)
  |       |       |-- RaizBDAST.java      Contenedor raiz del AST de BD
  |       |
  |       |-- redes/
  |       |   |-- RedesParser.java        Parser del modulo Redes
  |       |   |-- ast/
  |       |       |-- NodoDispositivo.java  Componente de red (nombre + tipo + config)
  |       |       |-- NodoEnlace.java       Enlace entre dos dispositivos
  |       |       |-- RaizRedesAST.java     Contenedor raiz del AST de Redes
  |       |
  |       |-- conceptual/
  |       |   |-- ConceptualParser.java   Parser del modulo Conceptual
  |       |   |-- ast/
  |       |       |-- NodeAST.java        Clase base abstracta de nodos Conceptual
  |       |       |-- NodoConcepto.java   Nodo de concepto/categoria/propiedad
  |       |       |-- NodoRelacionConceptual.java  Relacion entre conceptos
  |       |       |-- RaizConceptualAST.java       Contenedor raiz del AST Conceptual
  |       |
  |       |-- uml/
  |           |-- UMLParser.java          Parser del modulo UML
  |           |-- ast/
  |               |-- NodeAST.java        Clase base abstracta de nodos UML
  |               |-- NodoClase.java      Clase/Interfaz/Enum UML
  |               |-- NodoMiembro.java    Atributo o metodo dentro de una clase
  |               |-- NodoRelacionUML.java  Relacion entre clases
  |               |-- RaizUMLAST.java     Contenedor raiz del AST UML
  |
  |-- out/production/DiagramsAsCode/   Clases compiladas (generado automaticamente)
  |-- javafx-sdk-21.0.9/               JavaFX SDK para ejecucion desde linea de comandos
  |
  |-- test_flujo_completo.dac          Prueba del modulo Flujo (valido)
  |-- test_bd_avanzado.dac             Prueba del modulo BD (valido)
  |-- test_redes_nube.dac              Prueba del modulo Redes (valido)
  |-- test_conceptual_completo.dac     Prueba del modulo Conceptual (valido)
  |-- test_UML_completo.dac            Prueba del modulo UML (valido)
  |
  |-- manual_diagrams_as_code.md       Manual de usuario del lenguaje DAC
  |-- gramatica_formal.md              Gramatica EBNF formal de los cinco modulos
  |-- documentacion_tecnica.md         Este archivo
  |-- documentacion_Javier.md          Documentacion complementaria del equipo
  |-- integracion.md                   Notas de integracion entre ramas
  |-- pruebas.md                       Registro de pruebas realizadas
  |-- ejecutar.ps1                     Script PowerShell para ejecutar la aplicacion
  |-- DiagramsAsCode.iml               Configuracion del modulo IntelliJ IDEA

---

## Resumen de Componentes por Funcion

  Componente                   | Funcion
  -----------------------------|---------------------------------------------------
  LexerBase                    | Texto --> Tokens
  Token                        | Modelo de unidad lexica
  ParserBase                   | Cabecera + delegacion a los cinco modulos
  FlujoParser                  | Gramatica + AST del modulo Flujo
  BDParser                     | Gramatica + AST del modulo Base de Datos
  RedesParser                  | Gramatica + AST del modulo Redes
  ConceptualParser             | Gramatica + AST del modulo Conceptual
  UMLParser                    | Gramatica + AST del modulo UML
  TablaSimbolos                | Memoria semantica (identificadores y sus roles)
  ManejadorErrores             | Acumulador de errores con formato pedagogico
  TablaSimbologiaEstatica      | Catalogo oficial de 55 simbolos del DSL
  Main                         | Punto de entrada CLI (sin JavaFX)
  MainFX                       | IDE grafico con editor, compilador y visualizadores

---

## 15. Historial de Cambios

  Version | Fecha      | Descripcion
  --------|------------|-----------------------------------------------------------
  1.0     | 2026-05-30 | Version inicial: nucleo, modulos Flujo y BD
  1.1     | 2026-05-30 | Se agrego el modulo Redes
  1.2     | 2026-05-30 | Se agrego TablaSimbologiaEstatica (57 simbolos)
  1.3     | 2026-05-30 | Se agrego interfaz JavaFX (MainFX) con boton de simbologia
  1.4     | 2026-05-30 | Se agrego manual de usuario y boton de manual en la GUI
  1.5     | 2026-05-30 | Merge rama Javier: modulos Conceptual y UML, arbol de
          |            | derivacion grafico, boton de errores lexicos, RedesParser
          |            | reubicado en modulos/redes/
  1.6     | 2026-05-30 | Gramatica formal EBNF en gramatica_formal.md.
          |            | TablaSimbologiaEstatica verificada con 57 simbolos.
          |            | Documentacion tecnica actualizada con secciones 8, 9, 10.
  1.7     | 2026-05-30 | Arbol de derivacion alineado con la gramatica formal.
          |            | Agrupacion corregida para bloques BD/UML (split en '}').
          |            | Modulos Conceptual y UML incluidos. shortLabel() actualizado.
  1.8     | 2026-05-30 | automata_y_gramatica.md: AFD del lexer, gramatica LL(1),
          |            | jerarquia de Chomsky, FIRST/FOLLOW y derivacion completa.
          |            | Nodos del arbol con tamano dinamico (getLayoutBounds()).
  2.0     | 2026-05-30 | Merge rama Diego: codigos EL02/EL03/EL04 en LexerBase,
          |            | MAX_ERRORES=1000 en ManejadorErrores, getEditorDePestana
          |            | via Map<Tab,TextArea>, 13 archivos de prueba EL/ES.
          |            | Merge rama Javier: integracion de codigos ES en parsers.
          |            | Token.java: campo columna (int) con constructor de 4 args.
          |            | LexerBase: tracking de columna, recuperacion en EL02.
  2.1     | 2026-05-30 | TablaSimbolos: clase interna Entrada{rol,linea},
          |            | LinkedHashMap para orden de insercion, registrar con linea,
          |            | descripcionPara(nombre,rol) para descripciones automaticas.
          |            | GUI: tabla de simbolos dinamica con 4 columnas
          |            | (IDENTIFICADOR | TIPO/ROL | LINEA | DESCRIPCION).
          |            | Contador de simbologia dinamico (TABLA.size()).
          |            | Boton Gramatica DAC en la toolbar.
  2.2     | 2026-06-03 | NUEVA clasificacion de tokens: palabras reservadas de modulo
          |            | ahora producen token PALABRA_RESERVADA (antes IDENTIFICADOR).
          |            | Set PALABRAS_RESERVADAS en LexerBase con las ~40 palabras.
          |            | Todos los parsers actualizados: checks de verbo y keyword
          |            | aceptan IDENTIFICADOR || PALABRA_RESERVADA para no romper
          |            | con el nuevo tipo de token.
          |            | Reestructuracion de codigos de error lexicos:
          |            |   EL02 unifica digito-inicio y caracter-invalido-en-id.
          |            |   EL03 (NUEVO): palabra reservada mal escrita en mayusculas;
          |            |     el lexer emite el token corregido para evitar cascadas.
          |            |   ES55 (NUEVO/reclasificado): cadena sin cerrar pasa de
          |            |     lexico (EL02 anterior) a sintactico.
          |            |   ES56 (NUEVO): palabra reservada usada como identificador
          |            |     de usuario; generado en validarSiguienteTipo() de los
          |            |     5 parsers cuando se esperaba IDENTIFICADOR y llego
          |            |     PALABRA_RESERVADA o PR_DIAGRAMA.
          |            | ManejadorErrores: metodo tieneErroresLexicos() para
          |            |   distinguir EL01-EL03 de ES55 en la consola de la GUI.
          |            | TablaSimbolos.descripcionPara(): corregidas descripciones
          |            |   faltantes para nodo, condicion, bucle, subproceso,
          |            |   entrada, salida, parada, procedimiento, indice, disparador,
          |            |   secuencia, funcion, concepto, categoria, propiedad.

---

*Documentacion Tecnica — Diagrams As Code v2.2*
*Compilador pedagogico de DSL para diagramas*
