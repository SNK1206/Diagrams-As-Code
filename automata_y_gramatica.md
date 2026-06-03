# Autómata y Gramática — Diagrams As Code (DAC)

**Propósito:** Explicación formal del autómata utilizado en el analizador léxico
y de la gramática empleada en el analizador sintáctico del compilador DAC.

---

## Tabla de Contenidos

1. [El Analizador Léxico y su Autómata](#1-el-analizador-léxico-y-su-autómata)
2. [Definición Formal del AFD](#2-definición-formal-del-afd)
3. [Tabla de Transiciones](#3-tabla-de-transiciones)
4. [Diagrama de Transiciones](#4-diagrama-de-transiciones)
5. [El Analizador Sintáctico y su Gramática](#5-el-analizador-sintáctico-y-su-gramática)
6. [Tipo de Gramática — Jerarquía de Chomsky](#6-tipo-de-gramática---jerarquía-de-chomsky)
7. [Propiedades LL(1)](#7-propiedades-ll1)
8. [Conjuntos FIRST y FOLLOW](#8-conjuntos-first-y-follow)
9. [Ejemplo de Derivación Completa](#9-ejemplo-de-derivación-completa)
10. [Relación Lexer ↔ Parser](#10-relación-lexer--parser)

---

## 1. El Analizador Léxico y su Autómata

El analizador léxico (`LexerBase.java`) convierte el código fuente (texto plano)
en una secuencia de **tokens**. Para ello implementa un
**Autómata Finito Determinista (AFD)**.

### ¿Qué es un AFD?

Un Autómata Finito Determinista es una máquina abstracta que:
- Lee la cadena de entrada **carácter por carácter**
- En cada paso, **transita** a exactamente un nuevo estado (sin ambigüedad)
- Al terminar la lectura, si está en un **estado de aceptación**, reconoce el token

A diferencia de un Autómata No Determinista (AFND), el AFD nunca tiene dos
opciones posibles para un mismo símbolo en un mismo estado. Esto lo hace
eficiente y predecible para implementar en código.

### Alfabeto del lenguaje DAC

El conjunto de símbolos válidos que puede leer el lexer es:

```
Σ = {  a–z, A–Z          (letras)
       0–9               (dígitos)
       _                 (guion bajo)
       "                 (comilla doble)
       ;  :  {  }        (puntuación)
       /  #              (inicio de comentario)
       espacio, \t, \n   (espacios en blanco, ignorados)
    }
```

Cualquier carácter fuera de Σ genera un **error léxico**.

---

## 2. Definición Formal del AFD

Un AFD se define como la quíntupla **M = (Q, Σ, δ, q₀, F)** donde:

```
Q  = { q0, q1, q2, q3, q4, qErr }     Conjunto de estados

Σ  = (definido en la sección anterior)  Alfabeto de entrada

q₀ = q0                                 Estado inicial

F  = { qACC_ID, qACC_STR,
       qACC_KW, qACC_PUNCT }            Estados de aceptación (tokens aceptados)

δ  = función de transición (ver tabla)  δ: Q × Σ → Q
```

### Descripción de los estados

| Estado | Nombre | Descripción |
|--------|--------|-------------|
| `q0` | Inicial | Estado de arranque; esperando el primer carácter de un token |
| `q1` | Identificador | Leyendo un identificador o palabra reservada (letra o `_` leído) |
| `q2` | CadenaLiteral | Dentro de una cadena de texto (después de abrir `"`) |
| `q3` | Comentario | Dentro de un comentario de línea (`#` o `//` leídos) |
| `q4` | Slash | Leído un `/`; se espera otro `/` para confirmar comentario |
| `qACC_ID` | AceptarID | **Estado final** — se acepta `IDENTIFICADOR` o `PR_DIAGRAMA` |
| `qACC_STR` | AceptarSTR | **Estado final** — se acepta `TEXTO_LITERAL` |
| `qACC_PUNCT` | AceptarPUNCT | **Estado final** — se acepta uno de `;` `:` `{` `}` |
| `qErr` | Error | Carácter inválido detectado — se reporta error léxico |

---

## 3. Tabla de Transiciones

La función de transición `δ(estado, símbolo) → estado_siguiente`:

```
Estado  │ letra/_  │ dígito  │   "    │  ;:{}  │   #    │   /    │  espacio/\n │   otro
────────┼──────────┼─────────┼────────┼────────┼────────┼────────┼─────────────┼────────
  q0    │   q1     │  qErr   │   q2   │ qACC_PUNCT│  q3  │   q4   │    q0       │  qErr
  q1    │   q1     │   q1    │ qACC_ID│ qACC_ID│ qACC_ID│ qACC_ID│  qACC_ID    │ qACC_ID
  q2    │   q2     │   q2    │ qACC_STR│  q2   │   q2   │   q2   │    q2       │  q2 (*)
  q3    │   q3     │   q3    │   q3   │   q3   │   q3   │   q3   │   q0 (\n)   │  q3
  q4    │  qErr    │  qErr   │  qErr  │  qErr  │  qErr  │   q3   │   qErr      │  qErr
```

**Notas:**
- En `q1`, cualquier carácter que no sea letra/dígito/`_` genera aceptación del
  identificador (el carácter se devuelve para el siguiente token — "retroceso").
- En `q2`, `\n` o `EOF` sin haber encontrado `"` de cierre genera **error léxico**
  (cadena sin cerrar).
- En `q4`, solo `//` produce comentario; un solo `/` genera **error léxico**.
- `qACC_ID` verifica si el lexema es exactamente `"diagrama"`:
  - Si sí → emite token `PR_DIAGRAMA`
  - Si no → emite token `IDENTIFICADOR`

---

## 4. Diagrama de Transiciones

Representación visual del AFD (cada flecha indica la transición de un estado a otro):

```
                     ┌──────────────────────────────────────────┐
                     │  letra/_: q1          "letra/_/dígito"   │
                     │                             ↙             │
              espacio/\n                         q1 ────────────► qACC_ID
                  ↙                            ↗    ↖           (IDENTIFICADOR
    ─────► q0 ──────────────────── letra/_                        o PR_DIAGRAMA)
            │                                
            │─── " ──────────────► q2 ──── " ──────────────────► qACC_STR
            │                      │↑                            (TEXTO_LITERAL)
            │                      ││ (cualquier otro carácter)
            │                      └┘ 
            │
            │─── ; : { } ─────────────────────────────────────► qACC_PUNCT
            │                                                   (PUNTO_Y_COMA,
            │                                                    DOS_PUNTOS,
            │                                                    LLAVE_IZQ, LLAVE_DER)
            │
            │─── # ───────────────► q3 ◄──── (cualquier char)
            │                       │
            │                       └──── \n ──────────► q0
            │
            │─── / ───────────────► q4 ──── / ──────────► q3
            │                       │
            │                       └──── (otro) ────────► qErr
            │
            └─── (otro) ──────────────────────────────────► qErr
                                                             (ERROR LÉXICO)
```

---

## 5. El Analizador Sintáctico y su Gramática

El analizador sintáctico está implementado como un **analizador descendente
recursivo** (`ParserBase`, `FlujoParser`, `BDParser`, `RedesParser`,
`ConceptualParser`, `UMLParser`).

Cada método de los parsers corresponde directamente a **una regla de producción**
de la gramática formal. El árbol de llamadas recursivas genera implícitamente
el **Árbol de Sintaxis Abstracta (AST)**.

La gramática completa en notación EBNF se encuentra en `gramatica_formal.md`.

---

## 6. Tipo de Gramática — Jerarquía de Chomsky

### La Jerarquía de Chomsky clasifica las gramáticas en 4 tipos:

```
┌────────────────────────────────────────────────────────┐
│  Tipo 0: Gramáticas Sin Restricción (Máquinas de Turing)│
│  ┌──────────────────────────────────────────────────┐   │
│  │  Tipo 1: Sensibles al Contexto                   │   │
│  │  ┌────────────────────────────────────────────┐  │   │
│  │  │  Tipo 2: Libres de Contexto (CFG)          │  │   │
│  │  │  ┌──────────────────────────────────────┐  │  │   │
│  │  │  │  Tipo 3: Regulares (AFD/AFN)         │  │  │   │
│  │  │  │  ← El Lexer de DAC está aquí         │  │  │   │
│  │  │  └──────────────────────────────────────┘  │  │   │
│  │  │  ← El Parser de DAC está aquí              │  │   │
│  │  └────────────────────────────────────────────┘  │   │
│  └──────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────┘
```

### El compilador DAC usa DOS niveles:

**Nivel 1 — Analizador Léxico → Gramática Regular (Tipo 3)**
- Implementada con un **AFD** (Autómata Finito Determinista)
- Las gramáticas regulares son las más simples: sin recursión, sin memoria de profundidad
- Reconoce los tokens: identificadores, literales, puntuación
- Los autómatas finitos son suficientes porque los tokens son patrones simples

**Nivel 2 — Analizador Sintáctico → Gramática Libre de Contexto (Tipo 2)**
- Implementada con un **Parser Descendente Recursivo**
- Las CFG pueden expresar estructuras anidadas (bloques `{ }`, recursión)
- Se procesan con un **Autómata de Pila (Pushdown Automaton — PDA)**
  aunque en DAC el apilamiento es implícito en la pila de llamadas de Java
- Reconoce: instrucciones, bloques, relaciones entre elementos

### ¿Por qué Tipo 2 y no Tipo 3 para el parser?

Porque el lenguaje DAC tiene construcciones que **no pueden describirse con
gramáticas regulares**:

```
tabla Clientes {
    id: INT PK;          ← bloque anidado
    nombre: VARCHAR;     ← bloque anidado
}                        ← cierre de bloque
```

Las gramáticas regulares no pueden contar ni anidar. Se necesita la
potencia de un PDA (pila) para recordar que el `{` debe cerrarse con `}`.

---

## 7. Propiedades LL(1)

La gramática del parser DAC es de tipo **LL(1)**:

```
LL(1)
│││
││└── k=1: Se necesita exactamente 1 token de lookahead para decidir qué regla aplicar
│└─── L: Leftmost derivation — siempre se expande el no-terminal más a la izquierda
└──── L: Left-to-right — la cadena se lee de izquierda a derecha
```

### Condiciones que cumple la gramática para ser LL(1)

**1. Sin recursión por la izquierda:**
Ninguna regla tiene la forma `A ::= A ...`. Si la hubiera, el parser entraría
en un bucle infinito al intentar expandir el primer símbolo.

```
❌ Recursion izquierda (NO existe en DAC):
   instruccion_flujo ::= instruccion_flujo 'conecta' IDENTIFICADOR ';'

✅ Sin recursion izquierda (lo que usa DAC):
   cuerpo_flujo ::= { instruccion_flujo }   ← repetición iterativa
```

**2. Determinismo con 1 token de lookahead:**
Para cualquier estado del parser, el primer token de la entrada siguiente
identifica unívocamente cuál regla aplicar.

```
Viendo 'inicio'     → solo puede ser decl_nodo_simple
Viendo 'nodo'       → solo puede ser decl_nodo_texto
Viendo IDENTIFICADOR → solo puede ser conexion_flujo (si el siguiente es 'conecta')
```

**3. Sin ambigüedad:**
Cada cadena de entrada tiene **exactamente un árbol de derivación** posible.
No existen dos formas distintas de parsear la misma instrucción.

**4. Conjuntos FIRST disjuntos:**
Para reglas alternativas `A ::= α | β`, se cumple que:
`FIRST(α) ∩ FIRST(β) = ∅`

Esto garantiza que el parser sabe cuál alternativa elegir con 1 token.

---

## 8. Conjuntos FIRST y FOLLOW

### ¿Qué es FIRST?

`FIRST(α)` es el conjunto de terminales que pueden aparecer como **primer
símbolo** de cualquier cadena derivada desde `α`. El parser usa este conjunto
para decidir qué regla aplicar.

### ¿Qué es FOLLOW?

`FOLLOW(A)` es el conjunto de terminales que pueden aparecer **inmediatamente
después** del no-terminal `A` en alguna forma sentencial. Útil para manejar
reglas que pueden derivar la cadena vacía (ε).

### Conjuntos FIRST de las reglas principales

```
FIRST(programa)          = { 'autor', 'version', 'tema', 'diagrama' }

FIRST(meta_instruccion)  = { 'autor', 'version', 'tema' }

FIRST(cabecera)          = { 'diagrama' }

─────────────── Módulo FLUJO ────────────────────────────────────
FIRST(decl_nodo_simple)  = { 'inicio', 'fin' }

FIRST(decl_nodo_texto)   = { 'nodo', 'condicion', 'bucle', 'subproceso',
                              'entrada', 'salida', 'parada' }

FIRST(conexion_flujo)    = { IDENTIFICADOR }

─────────────── Módulo BD ───────────────────────────────────────
FIRST(bloque_bd)         = { 'tabla', 'vista', 'esquema', 'paquete' }

FIRST(atributo)          = { IDENTIFICADOR }

FIRST(componente_lineal_bd) = { 'procedimiento', 'indice', 'disparador',
                                 'secuencia', 'funcion' }

FIRST(relacion_bd)       = { IDENTIFICADOR }

─────────────── Módulo REDES ────────────────────────────────────
FIRST(componente_red)    = { 'dispositivo', 'nube', 'vlan', 'subred',
                              'cluster', 'tunel', 'zona', 'puerto', 'politica' }

FIRST(enlace_red)        = { IDENTIFICADOR }

─────────────── Módulo CONCEPTUAL ───────────────────────────────
FIRST(decl_concepto)     = { 'concepto', 'categoria', 'propiedad' }

FIRST(relacion_conceptual) = { IDENTIFICADOR }

─────────────── Módulo UML ──────────────────────────────────────
FIRST(decl_clase)        = { 'clase' }

FIRST(miembro_clase)     = { 'atributo', 'metodo' }

FIRST(decl_lineal_uml)   = { 'interfaz', 'enum' }

FIRST(relacion_uml)      = { IDENTIFICADOR }
```

### Resolución de conflictos IDENTIFICADOR

Los conjuntos `FIRST(conexion_flujo)`, `FIRST(relacion_bd)`, `FIRST(enlace_red)`,
`FIRST(relacion_conceptual)` y `FIRST(relacion_uml)` todos incluyen `IDENTIFICADOR`.

Esto crea un **conflicto aparente**. El parser lo resuelve con **lookahead extendido**:
al ver un `IDENTIFICADOR`, examina el **segundo token** (la posición siguiente):

```
Si token[0] = IDENTIFICADOR y token[1] = 'conecta'   → es conexion_flujo
Si token[0] = IDENTIFICADOR y token[1] = 'relaciona' → es relacion_bd
Si token[0] = IDENTIFICADOR y token[1] = 'enlaza'    → es enlace_red
Si token[0] = IDENTIFICADOR y token[1] = 'agrupa' | 'asocia' | 'depende' → es relacion_conceptual
Si token[0] = IDENTIFICADOR y token[1] = 'extiende' | 'implementa' | 'usa' → es relacion_uml
```

En la práctica, la gramática sigue siendo LL(1) porque el **contexto del módulo**
activo (Flujo, BD, Redes, etc.) reduce las alternativas posibles a 1.

---

## 9. Ejemplo de Derivación Completa

### Archivo de entrada (Flujo)

```
autor "Javier";
diagrama Flujo;
inicio A;
A conecta B;
```

### Paso 1 — Análisis Léxico (AFD)

El AFD lee carácter a carácter y produce:

```
Carácter(es)   Estado          Token emitido
─────────────────────────────────────────────────────────
'a','u','t','o','r'   q0→q1→q1→q1→q1→q1  IDENTIFICADOR 'autor'
' '                   q0                  (ignorado)
'"','J','a','v'...'"' q0→q2→q2→...→qACC_STR  TEXTO_LITERAL 'Javier'
';'                   q0→qACC_PUNCT       PUNTO_Y_COMA ';'
'\n'                  q0                  (ignorado)
'd','i','a','g'...    q0→q1→q1→...→qACC_ID → es "diagrama" → PR_DIAGRAMA
' '                   q0                  (ignorado)
'F','l','u','j','o'   q0→q1→...→qACC_ID  IDENTIFICADOR 'Flujo'
';'                   qACC_PUNCT          PUNTO_Y_COMA ';'
'\n'                  q0                  (ignorado)
'i','n','i','c','i','o'  q1...qACC_ID   IDENTIFICADOR 'inicio'
...
```

**Lista de tokens resultante:**
```
[ IDENTIFICADOR:'autor', TEXTO_LITERAL:'Javier', PUNTO_Y_COMA,
  PR_DIAGRAMA, IDENTIFICADOR:'Flujo', PUNTO_Y_COMA,
  IDENTIFICADOR:'inicio', IDENTIFICADOR:'A', PUNTO_Y_COMA,
  IDENTIFICADOR:'A', IDENTIFICADOR:'conecta', IDENTIFICADOR:'B', PUNTO_Y_COMA,
  EOF ]
```

### Paso 2 — Análisis Sintáctico (Parser LL(1))

El parser aplica las reglas de la gramática de izquierda a derecha:

```
programa
├── meta_instruccion           ← FIRST = { 'autor' } ✓ (ve 'autor')
│   ├── kw_meta → 'autor'
│   ├── TEXTO_LITERAL → 'Javier'
│   └── PUNTO_Y_COMA → ';'
│
├── cabecera                   ← FIRST = { 'diagrama' } ✓ (ve PR_DIAGRAMA)
│   ├── PR_DIAGRAMA → 'diagrama'
│   ├── IDENTIFICADOR → 'Flujo'
│   └── PUNTO_Y_COMA → ';'
│
└── cuerpo_flujo
    │
    ├── instruccion_flujo      ← FIRST = { 'inicio' } → decl_nodo_simple
    │   └── decl_nodo_simple
    │       ├── kw_nodo_simple → 'inicio'
    │       ├── IDENTIFICADOR → 'A'
    │       └── PUNTO_Y_COMA → ';'
    │
    └── instruccion_flujo      ← FIRST = { IDENTIFICADOR } + token[1]='conecta' → conexion_flujo
        └── conexion_flujo
            ├── IDENTIFICADOR → 'A'
            ├── 'conecta' → 'conecta'
            ├── IDENTIFICADOR → 'B'
            └── PUNTO_Y_COMA → ';'
```

### Paso 3 — Análisis Semántico (TablaSimbolos)

```
Acción              │ TablaSimbolos después
────────────────────┼──────────────────────────────
registrar('A','inicio') │ { A → inicio }
verificar('A','conecta','B') │ tabla.existe('A') = true ✓
                    │ tabla.existe('B') = false ✗ → ERROR SEMÁNTICO
```

---

## 10. Relación Lexer ↔ Parser

```
┌─────────────────────────────────────────────────────────────────┐
│                      PIPELINE DE COMPILACIÓN                     │
│                                                                   │
│   Código fuente (.dac)                                           │
│          │                                                        │
│          ▼                                                        │
│   ┌─────────────┐    Lee char a char         Lista<Token>        │
│   │  LexerBase  │ ─────────────────────────► [tok1, tok2, ...]   │
│   │   (AFD)     │    Emite tokens                                 │
│   └─────────────┘    (Gramática Regular)          │              │
│                                                    │              │
│                                                    ▼              │
│   ┌────────────────┐  Consume tokens  ┌──────────────────────┐   │
│   │  ParserBase    │ ◄──────────────  │   Lista de Tokens    │   │
│   │  (LL(1) CFG)   │                  └──────────────────────┘   │
│   └───────┬────────┘                                             │
│           │ delega según tipo_diagrama                           │
│           ├──► FlujoParser     ┐                                 │
│           ├──► BDParser        │  Cada parser implementa         │
│           ├──► RedesParser     │  las reglas de producción       │
│           ├──► ConceptualParser│  de la gramática CFG            │
│           └──► UMLParser       ┘                                 │
│                    │                                              │
│                    ▼                                              │
│            AST + TablaSimbolos + ManejadorErrores                │
└─────────────────────────────────────────────────────────────────┘
```

### Separación de responsabilidades

| Componente | Gramática | Autómata | Función |
|-----------|-----------|----------|---------|
| `LexerBase` | Regular (Tipo 3) | AFD | Tokenización |
| `ParserBase` + parsers | Libre de Contexto (Tipo 2) | PDA implícito | Análisis sintáctico y semántico |

Esta separación en dos niveles es el diseño estándar de todos los compiladores
modernos (GCC, LLVM, Java, Python). El lexer hace el trabajo pesado de caracter
por carácter; el parser trabaja con tokens de alto nivel.

---

## Glosario

| Término | Definición |
|---------|-----------|
| **AFD** | Autómata Finito Determinista — máquina de estados sin ambigüedad |
| **AFND** | Autómata Finito No Determinista — puede tener múltiples transiciones posibles |
| **CFG** | Context-Free Grammar — Gramática Libre de Contexto (Tipo 2 de Chomsky) |
| **PDA** | Pushdown Automaton — Autómata de Pila, procesa CFGs |
| **LL(1)** | Parser descendente: izquierda→derecha, derivación más a la izquierda, 1 lookahead |
| **FIRST(α)** | Conjunto de terminales que inician alguna cadena derivada desde α |
| **FOLLOW(A)** | Conjunto de terminales que pueden seguir al no-terminal A |
| **Token** | Unidad léxica mínima: tipo + lexema + línea |
| **Lexema** | El texto exacto reconocido por el lexer (e.g., "diagrama", "Flujo") |
| **AST** | Abstract Syntax Tree — Árbol de Sintaxis Abstracta |
| **Lookahead** | Tokens que el parser examina adelante para decidir qué regla aplicar |

---

*Autómata y Gramática — Diagrams As Code v2.0*
*Compilador pedagógico para la Primera Fase de Compiladores*
