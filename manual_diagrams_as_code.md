# Manual de Usuario — Diagrams As Code (DAC)

**Versión:** 1.0  
**Lenguaje fuente:** `.dac`  
**Plataforma:** IDE Pedagógico (JavaFX) + Compilador de Consola (CLI)

---

## Tabla de Contenidos

1. [Introducción](#1-introducción)
2. [Arquitectura del Compilador](#2-arquitectura-del-compilador)
3. [Uso del IDE Pedagógico (GUI)](#3-uso-del-ide-pedagógico-gui)
4. [Uso desde la Terminal (CLI)](#4-uso-desde-la-terminal-cli)
5. [Estructura del Lenguaje DAC](#5-estructura-del-lenguaje-dac)
6. [Módulo: Diagramas de Flujo](#6-módulo-diagramas-de-flujo)
7. [Módulo: Diagramas de Base de Datos](#7-módulo-diagramas-de-base-de-datos)
8. [Sistema de Errores y Diagnósticos](#8-sistema-de-errores-y-diagnósticos)
9. [Tabla de Símbolos](#9-tabla-de-símbolos)
10. [Referencia Rápida de Sintaxis](#10-referencia-rápida-de-sintaxis)
11. [Ejemplos Completos](#11-ejemplos-completos)

---

## 1. Introducción

**Diagrams As Code (DAC)** es un compilador pedagógico que permite describir diagramas mediante un lenguaje textual propio con extensión `.dac`. El sistema valida el código fuente en tres fases clásicas de compilación:

- **Análisis Léxico** — Descompone el código en tokens.
- **Análisis Sintáctico** — Verifica que la estructura gramatical sea correcta y construye un AST.
- **Análisis Semántico** — Valida la coherencia lógica (referencias existentes, unicidad de identificadores, etc.).

El compilador soporta actualmente dos tipos de diagrama:

| Tipo de Diagrama | Palabra Clave | Módulo                              |
|------------------|---------------|-------------------------------------|
| Diagrama de Flujo | `Flujo`      | `com.diagramas.modulos.flujo`       |
| Diagrama de BD    | `BD`         | `com.diagramas.modulos.bd`          |

---

## 2. Arquitectura del Compilador

El pipeline de compilación sigue este orden estricto. Si una fase falla, las siguientes no se ejecutan.

```
Archivo .dac
     │
     ▼
┌─────────────┐
│  LexerBase  │  → Tokenización del código fuente
└──────┬──────┘
       │  Lista<Token>
       ▼
┌────────────────┐
│  ParserBase    │  → Valida la cabecera del archivo (diagrama <Tipo>;)
└──────┬─────────┘    y delega al módulo correspondiente
       │
       ├──── caso "Flujo" ──► FlujoParser → RaizFlujoAST
       │
       └──── caso "BD"    ──► BDParser   → RaizBDAST
                                │
                     TablaSimbolos (Análisis Semántico)
                     ManejadorErrores (Reporte de errores)
```

### Componentes principales

| Clase | Responsabilidad |
|-------|----------------|
| `LexerBase` | Convierte el código fuente en una secuencia de `Token` |
| `ParserBase` | Analiza la cabecera obligatoria y delega al módulo correcto |
| `FlujoParser` | Parser + análisis semántico del módulo Flujo |
| `BDParser` | Parser + análisis semántico del módulo BD |
| `TablaSimbolos` | Registro de identificadores y sus tipos en memoria |
| `ManejadorErrores` | Acumula y formatea todos los errores de compilación |

---

## 3. Uso del IDE Pedagógico (GUI)

Al lanzar `MainFX`, se abre una ventana dividida en tres paneles:

### 3.1 Barra de Herramientas

| Botón | Atajo Visual | Descripción |
|-------|-------------|-------------|
| ➕ Nuevo | Naranja | Crea una pestaña en blanco con plantilla `diagrama Flujo;` |
| 📂 Abrir .dac | Azul | Abre el explorador de archivos para cargar un `.dac` existente |
| 💾 Guardar | Morado | Guarda el archivo de la pestaña activa en disco |
| ⚡ Compilar e Inspeccionar | Verde | Ejecuta las tres fases del compilador sobre el código activo |

### 3.2 Panel Central

Se divide en dos columnas:

**Izquierda — Editor de Código con Pestañas**
- Cada archivo abierto ocupa su propia pestaña.
- El editor es de fuente monoespaciada y totalmente editable.
- Los archivos "Nuevo" sin guardar se nombran `sin_titulo_N.dac`.

**Derecha — Panel de Análisis Interno**
- **Flujo Léxico (Tokens Extracted):** Muestra todos los tokens generados por el Lexer, con tipo, lexema y número de línea.
- **Tabla de Símbolos:** Muestra el contexto activo y todos los identificadores registrados durante el análisis semántico, junto con su tipo de rol asignado.

### 3.3 Consola de Diagnóstico

Panel inferior que muestra:
- Confirmación de carga de archivo.
- Resultado final de compilación (éxito o fallo).
- Todos los errores detallados con línea, contexto, descripción y sugerencia de corrección.

---

## 4. Uso desde la Terminal (CLI)

El compilador puede ejecutarse sin interfaz gráfica desde la clase `Main`.

### Comando

```bash
java com.diagramas.Main <ruta_del_archivo>.dac
```

### Ejemplo

```bash
java com.diagramas.Main test_flujo_valido.dac
```

### Salida esperada (compilación exitosa)

```
====== COMPILADOR DE CONSOLA: DIAGRAMS AS CODE ======

📖 Leyendo archivo fuente: test_flujo_valido.dac
--------------------------------------------------
[Lexer] Análisis de caracteres completado. Tokens:
  Token [PR_DIAGRAMA | 'diagrama' | Línea: 1]
  Token [IDENTIFICADOR | 'Flujo' | Línea: 1]
  ...
🔒 [ParserBase] Contexto 'Flujo' bloqueado de manera estricta.
...
✅ COMPILACIÓN EXITOSA: Estructura base validada sin anomalías.
```

### Reglas de uso CLI

- El archivo de entrada **debe** tener extensión `.dac`.
- Si no se pasa ningún argumento, el programa termina con un mensaje de error pedagógico.
- Los errores se imprimen en `System.err` y el proceso termina después de la primera fase fallida.

---

## 5. Estructura del Lenguaje DAC

### 5.1 Regla de Cabecera (Obligatoria)

Todo archivo `.dac` **debe** comenzar con la declaración de cabecera. Sin ella, el compilador rechaza el archivo completo.

**Sintaxis:**

```
diagrama <TipoDiagrama>;
```

- `diagrama` es una **palabra reservada** del sistema (no puede usarse como identificador).
- `<TipoDiagrama>` es un identificador que determina el módulo a usar (`Flujo` o `BD`).
- El punto y coma `;` es **obligatorio**.

**Ejemplos válidos:**

```
diagrama Flujo;
diagrama BD;
```

**Ejemplos inválidos:**

```
# ❌ Sin cabecera
inicio MiNodo;

# ❌ Sin punto y coma
diagrama Flujo

# ❌ Tipo desconocido (módulo no registrado)
diagrama Redes;
```

### 5.2 Identificadores

Un identificador es cualquier secuencia de letras, dígitos y guiones bajos que **comienza con una letra o guión bajo**.

**Válidos:** `Usuarios`, `nodo1`, `Mi_Proceso`, `LeerDatos`  
**Inválidos:** `1nodo`, `mi-proceso`, `@tabla`

### 5.3 Literales de Texto

Las cadenas de texto se delimitan con comillas dobles (`"`). Se usan para las descripciones de nodos.

```
"Leer credenciales del sistema"
"Verificar base de datos"
```

**Regla:** La cadena debe cerrarse en la misma línea. Una cadena sin cerrar genera un error léxico.

### 5.4 Signos de Puntuación Reconocidos

| Carácter | Token | Uso |
|----------|-------|-----|
| `;` | `PUNTO_Y_COMA` | Cierra toda instrucción |
| `:` | `DOS_PUNTOS` | Separa nombre de tipo en atributos BD |
| `{` | `LLAVE_IZQ` | Abre el cuerpo de una tabla BD |
| `}` | `LLAVE_DER` | Cierra el cuerpo de una tabla BD |

### 5.5 Comentarios

El Lexer no define un token de comentario formal. La línea `# ...` generará errores léxicos por el carácter `#`. **Evita usar comentarios en archivos `.dac`.**

---

## 6. Módulo: Diagramas de Flujo

Activado con `diagrama Flujo;`. Permite modelar procesos secuenciales con nodos y conexiones dirigidas.

### 6.1 Declaración de Inicio

Define el **punto de entrada** del flujo. Solo puede existir un punto de inicio con un nombre dado (unicidad semántica).

**Sintaxis:**

```
inicio <Identificador>;
```

**Ejemplo:**

```
inicio Comenzar;
```

**AST generado:** `NodoAST_Inicio [ID: Comenzar]`

**Reglas:**
- El identificador no puede estar previamente registrado en la tabla de símbolos.
- Termina obligatoriamente con `;`.

---

### 6.2 Declaración de Nodo de Proceso

Define un **paso o proceso** dentro del flujo. Lleva un identificador único y una descripción textual.

**Sintaxis:**

```
nodo <Identificador> "<Descripción>";
```

**Ejemplo:**

```
nodo LeerDatos "Leer credenciales del sistema";
nodo Validar "Verificar base de datos";
```

**AST generado:** `NodoAST_Proceso [ID: LeerDatos | Descripción: "Leer credenciales del sistema"]`

**Reglas:**
- El identificador debe ser único en el archivo.
- La descripción va **entre comillas dobles** y es obligatoria.
- Termina con `;`.

---

### 6.3 Instrucción de Conexión

Establece una **arista dirigida** entre dos nodos ya declarados.

**Sintaxis:**

```
<IdentificadorOrigen> conecta <IdentificadorDestino>;
```

**Ejemplo:**

```
Comenzar conecta LeerDatos;
LeerDatos conecta Validar;
```

**AST generado:** `NodoAST_Conexion [Comenzar ──conecta──► LeerDatos]`

**Reglas semánticas (críticas):**
- Tanto el origen como el destino **deben estar previamente declarados** con `inicio` o `nodo`.
- Una conexión hacia un identificador inexistente genera un **error semántico**, no sintáctico.
- El verbo conector exacto es `conecta` (en minúsculas). Cualquier otro verbo genera error sintáctico.

**Error típico:**

```
# ❌ ProcesoInexistente nunca fue declarado
Comenzar conecta ProcesoInexistente;
```

```
ERROR DE COMPILACIÓN [Línea 4] [Contexto: Semántico Flujo]
💡 Detalle: El destino 'ProcesoInexistente' no está registrado.
🔍 Sugerencia: Verifica errores de dedo o declara el nodo de destino.
```

---

### 6.4 Gramática Completa del Módulo Flujo

```
programa_flujo  ::= cabecera instruccion_flujo*
cabecera        ::= "diagrama" "Flujo" ";"
instruccion_flujo ::= decl_inicio | decl_nodo | instruccion_conexion

decl_inicio     ::= "inicio" IDENTIFICADOR ";"
decl_nodo       ::= "nodo" IDENTIFICADOR TEXTO_LITERAL ";"
instruccion_conexion ::= IDENTIFICADOR "conecta" IDENTIFICADOR ";"
```

---

## 7. Módulo: Diagramas de Base de Datos

Activado con `diagrama BD;`. Permite modelar esquemas de base de datos con tablas, atributos y relaciones.

### 7.1 Declaración de Tabla

Define una **tabla** con su nombre y la lista de sus atributos.

**Sintaxis:**

```
tabla <NombreTabla> {
    <nombre_atributo>: <TipoDato> [Modificador];
    ...
}
```

**Ejemplo:**

```
tabla Usuarios {
    id: INT PK;
    nombre: VARCHAR;
    correo: VARCHAR;
}
```

**AST generado:**

```
NodoAST_Tabla [Usuarios]
      ├─ id: INT <PK>
      ├─ nombre: VARCHAR
      └─ correo: VARCHAR
```

**Reglas:**
- El nombre de la tabla debe ser único (control semántico).
- El bloque debe abrirse con `{` y cerrarse con `}`.
- Cada atributo termina con `;`.

---

### 7.2 Definición de Atributos

Cada línea dentro del bloque `{}` de una tabla define un atributo.

**Sintaxis:**

```
<nombre>: <TipoDato>;
<nombre>: <TipoDato> <Modificador>;
```

**Tipos de dato comunes (identificadores libres):**

| Tipo | Descripción |
|------|-------------|
| `INT` | Número entero |
| `VARCHAR` | Cadena de caracteres variable |
| `TEXT` | Texto largo |
| `FLOAT` | Número decimal |
| `BOOLEAN` | Valor lógico |
| `DATE` | Fecha |

> El Lexer trata los tipos de dato como identificadores ordinarios, por lo que puedes usar cualquier identificador válido como tipo.

**Modificadores opcionales:**

| Modificador | Significado |
|-------------|-------------|
| `PK` | Llave primaria (Primary Key) |
| `FK` | Llave foránea (Foreign Key) |

**Ejemplos:**

```
id: INT PK;
usuario_id: INT FK;
nombre: VARCHAR;
contenido: TEXT;
```

**Reglas:**
- El modificador es **opcional**. Si se omite, el atributo se registra sin modificador.
- Solo se puede declarar **un modificador** por atributo.

---

### 7.3 Instrucción de Relación

Establece una **relación semántica** entre dos tablas ya declaradas.

**Sintaxis:**

```
<TablaOrigen> relaciona <TablaDestino>;
```

**Ejemplo:**

```
Usuarios relaciona Posts;
```

**AST generado:** `NodoAST_Relacion [Usuarios ──relaciona──► Posts]`

**Reglas semánticas (críticas):**
- Ambas tablas deben haber sido **declaradas previamente** en el mismo archivo.
- El verbo conector exacto es `relaciona`. El uso de `conecta` u otro verbo genera error sintáctico.
- Una referencia a una tabla inexistente genera un **error semántico**.

**Error típico (verbo incorrecto):**

```
# ❌ 'conecta' no es el verbo del módulo BD
Usuarios conecta Posts;
```

```
ERROR DE COMPILACIÓN [Línea 7] [Contexto: Sintáctico BD]
💡 Detalle: Instrucción desconocida o verbo inválido en 'Usuarios'.
🔍 Sugerencia: Para unir tablas usa el verbo exclusivo 'relaciona' (Ej: TablaA relaciona TablaB;)
```

---

### 7.4 Gramática Completa del Módulo BD

```
programa_bd     ::= cabecera instruccion_bd*
cabecera        ::= "diagrama" "BD" ";"
instruccion_bd  ::= decl_tabla | instruccion_relacion

decl_tabla      ::= "tabla" IDENTIFICADOR "{" atributo* "}"
atributo        ::= IDENTIFICADOR ":" IDENTIFICADOR [IDENTIFICADOR] ";"
instruccion_relacion ::= IDENTIFICADOR "relaciona" IDENTIFICADOR ";"
```

---

## 8. Sistema de Errores y Diagnósticos

El `ManejadorErrores` acumula todos los errores encontrados durante las tres fases. Cada error sigue el formato:

```
==================================================
❌ ERROR DE COMPILACIÓN [Línea N] [Contexto: <fase>]
💡 Detalle: <descripción del problema>
🔍 Sugerencia: <indicación de corrección>
==================================================
```

### 8.1 Contextos de Error

| Contexto | Fase | Qué detecta |
|----------|------|-------------|
| `Análisis Léxico` | Léxica | Caracteres no permitidos en el alfabeto del lenguaje |
| `Léxico` | Léxica | Cadenas sin cerrar |
| `Parser Base` | Sintáctica | Cabecera ausente o malformada |
| `Sintáctico Flujo` | Sintáctica | Estructura incorrecta en el módulo Flujo |
| `Semántico Flujo` | Semántica | Identificadores duplicados o referencias no declaradas (Flujo) |
| `Sintáctico BD` | Sintáctica | Estructura incorrecta en el módulo BD |
| `Semántico BD` | Semántica | Tablas duplicadas o referencias no declaradas (BD) |
| `Infraestructura` | — | Tipo de diagrama no registrado en el sistema |

### 8.2 Comportamiento de Detención

El compilador **detiene el pipeline** si detecta errores en una fase antes de pasar a la siguiente:

- Errores léxicos → se detiene antes del análisis sintáctico.
- Errores sintácticos/semánticos → se acumulan y se reportan al final.

Dentro del análisis sintáctico, el parser utiliza **recuperación de pánico** para sincronizar tras un error y seguir reportando errores subsecuentes sin "encadenar" falsas alarmas.

### 8.3 Errores Léxicos Comunes

| Causa | Ejemplo | Error generado |
|-------|---------|---------------|
| Carácter no permitido | `#`, `@`, `!` | `El carácter '#' no pertenece al alfabeto del lenguaje.` |
| Cadena sin cerrar | `"texto sin cerrar` | `Cadena de texto sin cerrar.` |
| Número como primer carácter | `1nodo` | El `1` genera error léxico; `nodo` se tokeniza como IDENTIFICADOR. |

---

## 9. Tabla de Símbolos

La `TablaSimbolos` es el componente de memoria del compilador. Se reinicia en cada compilación.

### Comportamiento

- Registra cada identificador declarado con su **tipo de rol** (`inicio`, `nodo`, `tabla`).
- Valida **unicidad**: si un identificador ya fue registrado, retorna `false` y el parser emite error semántico.
- Mantiene el **contexto activo** (el tipo de diagrama en proceso).

### Visualización en el IDE

En el panel derecho del IDE, la tabla se muestra así:

```
🔒 Contexto Bloqueado: Flujo

IDENTIFICADOR ENCONTRADO  | TIPO/ROL ASIGNADO
----------------------------------------------------
Comenzar                  | inicio
LeerDatos                 | nodo
Validar                   | nodo
```

### Roles registrados

| Rol | Registrado por | Módulo |
|-----|---------------|--------|
| `inicio` | `procesarInicio()` | Flujo |
| `nodo` | `procesarNodoProceso()` | Flujo |
| `tabla` | `procesarTabla()` | BD |

> Las **conexiones** (`conecta`) y **relaciones** (`relaciona`) **no registran** nuevos identificadores; solo validan que los referenciados ya existan.

---

## 10. Referencia Rápida de Sintaxis

### Módulo Flujo

```
diagrama Flujo;

inicio <ID>;
nodo <ID> "<descripción>";
<IDOrigen> conecta <IDDestino>;
```

### Módulo BD

```
diagrama BD;

tabla <NombreTabla> {
    <campo>: <Tipo>;
    <campo>: <Tipo> PK;
    <campo>: <Tipo> FK;
}

<TablaOrigen> relaciona <TablaDestino>;
```

### Palabras reservadas del sistema

| Palabra | Tipo Token | Contexto |
|---------|-----------|----------|
| `diagrama` | `PR_DIAGRAMA` | Global (cabecera) |
| `inicio` | `IDENTIFICADOR` (semántico) | Flujo |
| `nodo` | `IDENTIFICADOR` (semántico) | Flujo |
| `conecta` | `IDENTIFICADOR` (semántico) | Flujo |
| `tabla` | `IDENTIFICADOR` (semántico) | BD |
| `relaciona` | `IDENTIFICADOR` (semántico) | BD |

> Nota: Solo `diagrama` tiene su propio tipo de token (`PR_DIAGRAMA`). El resto son identificadores que el parser reconoce por su lexema en posiciones específicas de la gramática.

---

## 11. Ejemplos Completos

### Ejemplo 1: Diagrama de Flujo válido

**Archivo:** `test_flujo_valido.dac`

```
diagrama Flujo;
inicio Comenzar;
nodo LeerDatos "Leer credenciales";
nodo Validar "Verificar base de datos";
Comenzar conecta LeerDatos;
LeerDatos conecta Validar;
```

**AST generado:**

```
==================================================
🌳 ÁRBOL DE SINTAXIS ABSTRACTA (AST) GENERADO:
==================================================
  NodoAST_Inicio [ID: Comenzar]
  NodoAST_Proceso [ID: LeerDatos | Descripción: "Leer credenciales"]
  NodoAST_Proceso [ID: Validar | Descripción: "Verificar base de datos"]
  NodoAST_Conexion [Comenzar ──conecta──► LeerDatos]
  NodoAST_Conexion [LeerDatos ──conecta──► Validar]
==================================================
```

---

### Ejemplo 2: Diagrama de Flujo con error semántico

**Archivo:** `test_flujo_error.dac`

```
diagrama Flujo;
inicio Comenzar;
nodo LeerDatos "Leer credenciales";
Comenzar conecta ProcesoInexistente;
```

**Error generado:**

```
==================================================
❌ ERROR DE COMPILACIÓN [Línea 4] [Contexto: Semántico Flujo]
💡 Detalle: El destino 'ProcesoInexistente' no está registrado.
🔍 Sugerencia: Verifica errores de dedo o declara el nodo de destino.
==================================================
```

---

### Ejemplo 3: Diagrama de Base de Datos válido

**Archivo:** `test_bd_valido.dac`

```
diagrama BD;
tabla Usuarios {
    id: INT PK;
    nombre: VARCHAR;
    correo: VARCHAR;
}
tabla Posts {
    id: INT PK;
    usuario_id: INT FK;
    contenido: TEXT;
}
Usuarios relaciona Posts;
```

**AST generado:**

```
==================================================
🗄️ ÁRBOL DE SINTAXIS ABSTRACTA (AST) - BASE DE DATOS:
==================================================
  NodoAST_Tabla [Usuarios]
      ├─ id: INT <PK>
      ├─ nombre: VARCHAR
      └─ correo: VARCHAR

  NodoAST_Tabla [Posts]
      ├─ id: INT <PK>
      ├─ usuario_id: INT <FK>
      └─ contenido: TEXT

  NodoAST_Relacion [Usuarios ──relaciona──► Posts]
==================================================
```

---

### Ejemplo 4: Diagrama BD con error sintáctico (verbo incorrecto)

**Archivo:** `test_bd_error.dac`

```
diagrama BD;
tabla Usuarios {
    id: INT PK;
    nombre: VARCHAR;
}
Usuarios conecta Posts;
```

**Error generado:**

```
==================================================
❌ ERROR DE COMPILACIÓN [Línea 7] [Contexto: Sintáctico BD]
💡 Detalle: Instrucción desconocida o verbo inválido en 'Usuarios'.
🔍 Sugerencia: Para unir tablas usa el verbo exclusivo 'relaciona' (Ej: TablaA relaciona TablaB;)
==================================================
```

---

*Manual generado para el proyecto Diagrams As Code — compilador pedagógico de análisis léxico, sintáctico y semántico.*
