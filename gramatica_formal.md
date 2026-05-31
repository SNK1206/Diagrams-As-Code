# Gramática Formal — Diagrams As Code (DAC)

**Versión:** 1.0  
**Notación:** EBNF (Extended Backus-Naur Form)  
**Derivada de:** LexerBase.java, ParserBase.java y los cinco parsers de módulo

---

## Convenciones de Notación

| Símbolo | Significado |
|---------|-------------|
| `::=` | Define una regla de producción |
| `\|` | Alternativa (OR) |
| `( ... )` | Agrupación |
| `[ ... ]` | Elemento opcional — aparece 0 ó 1 vez |
| `{ ... }` | Repetición — aparece 0 ó más veces |
| `'palabra'` | Terminal literal (keyword del lenguaje) |
| `MAYUSCULAS` | Clase de token producida por el Lexer |
| `minusculas` | Símbolo no terminal (regla de la gramática) |

---

## Sección 1 — Tokens (Análisis Léxico)

Definidos e implementados en `LexerBase.java`.

```
IDENTIFICADOR  ::= ( letra | '_' ) { letra | digito | '_' }
TEXTO_LITERAL  ::= '"' { caracter_no_comilla } '"'
PR_DIAGRAMA    ::= 'diagrama'
PUNTO_Y_COMA   ::= ';'
DOS_PUNTOS     ::= ':'
LLAVE_IZQ      ::= '{'
LLAVE_DER      ::= '}'
EOF            ::= <fin de archivo>

letra          ::= 'a'..'z' | 'A'..'Z'
digito         ::= '0'..'9'
```

**Observaciones:**
- Solo `diagrama` tiene su propio tipo de token (`PR_DIAGRAMA`). Todas las demás
  palabras reservadas del lenguaje son reconocidas como `IDENTIFICADOR` por el
  Lexer y clasificadas semánticamente por el Parser según su posición en la gramática.
- Los comentarios (`//` y `#`) son absorbidos por el Lexer y no generan ningún token.
- Un `TEXTO_LITERAL` no puede abarcar más de una línea.

---

## Sección 2 — Gramática Global (Cabecera)

Implementada en `ParserBase.java`.

```
programa         ::= { meta_instruccion } cabecera cuerpo_modulo

meta_instruccion ::= kw_meta TEXTO_LITERAL ';'
kw_meta          ::= 'autor'
                   | 'version'
                   | 'tema'
                   | 'exportar'
                   | 'importar'

cabecera         ::= 'diagrama' tipo_diagrama ';'
tipo_diagrama    ::= 'Flujo'
                   | 'BD'
                   | 'Redes'
                   | 'Conceptual'
                   | 'UML'

cuerpo_modulo    ::= cuerpo_flujo
                   | cuerpo_bd
                   | cuerpo_redes
                   | cuerpo_conceptual
                   | cuerpo_uml
```

**Observaciones:**
- Las meta-instrucciones son opcionales y deben aparecer **antes** de la cabecera.
- La cabecera `diagrama <Tipo>;` es **obligatoria**. Sin ella el compilador rechaza
  el archivo completo.
- `tipo_diagrama` determina cuál de los cinco módulos toma el control del análisis
  para el resto del archivo.

---

## Sección 3 — Módulo Flujo

Implementado en `FlujoParser.java`.  
Activado con: `diagrama Flujo;`

```
cuerpo_flujo      ::= { instruccion_flujo }

instruccion_flujo ::= decl_nodo_simple
                    | decl_nodo_texto
                    | conexion_flujo

decl_nodo_simple  ::= kw_nodo_simple IDENTIFICADOR ';'
kw_nodo_simple    ::= 'inicio'
                    | 'fin'

decl_nodo_texto   ::= kw_nodo_texto IDENTIFICADOR TEXTO_LITERAL ';'
kw_nodo_texto     ::= 'nodo'
                    | 'condicion'
                    | 'bucle'
                    | 'subproceso'
                    | 'entrada'
                    | 'salida'
                    | 'parada'

conexion_flujo    ::= IDENTIFICADOR 'conecta' IDENTIFICADOR ';'
```

**Observaciones:**
- `decl_nodo_simple` — nodos sin descripción textual: `inicio` y `fin`.
- `decl_nodo_texto` — nodos que llevan descripción entre comillas dobles.
- `conexion_flujo` — establece una arista dirigida entre dos nodos ya declarados.
- El parser distingue las tres formas mirando el primer token de cada instrucción:
  si es `inicio` o `fin` → `decl_nodo_simple`; si es cualquier otra keyword de nodo
  → `decl_nodo_texto`; si es un `IDENTIFICADOR` libre → `conexion_flujo`.

**Nodos del AST generados:**
- `decl_nodo_simple` → `NodoProceso`
- `decl_nodo_texto` → `NodoProceso`
- `conexion_flujo` → `NodoConexion`

---

## Sección 4 — Módulo BD (Base de Datos)

Implementado en `BDParser.java`.  
Activado con: `diagrama BD;`

```
cuerpo_bd            ::= { instruccion_bd }

instruccion_bd       ::= bloque_bd
                       | componente_lineal_bd
                       | relacion_bd

bloque_bd            ::= kw_bloque_bd IDENTIFICADOR '{' { atributo } '}'
kw_bloque_bd         ::= 'tabla'
                       | 'vista'
                       | 'esquema'
                       | 'paquete'

atributo             ::= IDENTIFICADOR ':' IDENTIFICADOR [ IDENTIFICADOR ] ';'

componente_lineal_bd ::= kw_lineal_bd IDENTIFICADOR ';'
kw_lineal_bd         ::= 'procedimiento'
                       | 'indice'
                       | 'disparador'
                       | 'secuencia'
                       | 'funcion'

relacion_bd          ::= IDENTIFICADOR 'relaciona' IDENTIFICADOR ';'
```

**Observaciones:**
- `bloque_bd` — componentes con estructura interna (columnas/campos).
- El tercer `IDENTIFICADOR` en `atributo` es el modificador opcional (`PK`, `FK`).
  El Lexer lo trata igual que cualquier identificador; el Parser lo acepta si está
  presente en la tercera posición de la línea.
- `componente_lineal_bd` — objetos de BD sin estructura interna en el diagrama.
- `relacion_bd` — establece una relación entre dos entidades ya declaradas.

**Nodos del AST generados:**
- `bloque_bd` → `NodoTabla` (con lista de `Atributo`)
- `componente_lineal_bd` → `NodoTabla` (sin atributos, nombre incluye el rol)
- `relacion_bd` → `NodoRelacion`

---

## Sección 5 — Módulo Redes

Implementado en `RedesParser.java` (`modulos/redes/`).  
Activado con: `diagrama Redes;`

```
cuerpo_redes      ::= { instruccion_redes }

instruccion_redes ::= componente_red
                    | enlace_red

componente_red    ::= kw_componente_red IDENTIFICADOR IDENTIFICADOR
                      [ '{' { propiedad_red } '}' ] ';'

kw_componente_red ::= 'dispositivo'
                    | 'nube'
                    | 'vlan'
                    | 'subred'
                    | 'cluster'
                    | 'tunel'
                    | 'zona'
                    | 'puerto'
                    | 'politica'

propiedad_red     ::= IDENTIFICADOR ':' TEXTO_LITERAL ';'

enlace_red        ::= IDENTIFICADOR 'enlaza' IDENTIFICADOR ';'
```

**Observaciones:**
- `componente_red` recibe tres partes: la keyword de tipo de componente, el nombre
  del dispositivo y el tipo específico (ej. `Router`, `Firewall`, `LoadBalancer`).
- El bloque `{ propiedad_red* }` es completamente **opcional**. Un componente puede
  declararse con o sin propiedades de configuración.
- Las propiedades dentro del bloque usan `TEXTO_LITERAL` para el valor (a diferencia
  de los atributos de BD que usan `IDENTIFICADOR`).
- `enlace_red` — establece una conexión entre dos componentes ya declarados.

**Nodos del AST generados:**
- `componente_red` → `NodoDispositivo`
- `enlace_red` → `NodoEnlace`

---

## Sección 6 — Módulo Conceptual

Implementado en `ConceptualParser.java`.  
Activado con: `diagrama Conceptual;`

```
cuerpo_conceptual      ::= { instruccion_conceptual }

instruccion_conceptual ::= decl_concepto
                         | relacion_conceptual

decl_concepto          ::= kw_concepto IDENTIFICADOR TEXTO_LITERAL ';'
kw_concepto            ::= 'concepto'
                         | 'categoria'
                         | 'propiedad'

relacion_conceptual    ::= IDENTIFICADOR kw_verbo_conceptual IDENTIFICADOR ';'
kw_verbo_conceptual    ::= 'agrupa'
                         | 'asocia'
                         | 'depende'
```

**Observaciones:**
- Todos los nodos conceptuales llevan una descripción textual obligatoria
  (`TEXTO_LITERAL`). No existe un nodo simple sin descripción en este módulo.
- Las relaciones usan verbos semánticos (`agrupa`, `asocia`, `depende`) que
  expresan el tipo de vínculo entre dos conceptos.
- El Parser distingue `decl_concepto` de `relacion_conceptual` por el segundo token:
  si es una keyword de concepto → declaración; si es un `IDENTIFICADOR` libre
  seguido de un verbo de relación → relación.

**Nodos del AST generados:**
- `decl_concepto` → `NodoConcepto`
- `relacion_conceptual` → `NodoRelacionConceptual`

---

## Sección 7 — Módulo UML

Implementado en `UMLParser.java`.  
Activado con: `diagrama UML;`

```
cuerpo_uml      ::= { instruccion_uml }

instruccion_uml ::= decl_clase
                  | decl_lineal_uml
                  | relacion_uml

decl_clase      ::= 'clase' IDENTIFICADOR '{' { miembro_clase } '}'
miembro_clase   ::= kw_miembro IDENTIFICADOR ':' IDENTIFICADOR ';'
kw_miembro      ::= 'atributo'
                  | 'metodo'

decl_lineal_uml ::= kw_lineal_uml IDENTIFICADOR ';'
kw_lineal_uml   ::= 'interfaz'
                  | 'enum'

relacion_uml    ::= IDENTIFICADOR kw_verbo_uml IDENTIFICADOR ';'
kw_verbo_uml    ::= 'extiende'
                  | 'implementa'
                  | 'usa'
```

**Observaciones:**
- `decl_clase` — define una clase con cero o más miembros internos.
  Un bloque `clase Nombre { }` sin miembros es sintácticamente válido.
- `miembro_clase` — cada miembro lleva su rol (`atributo` o `metodo`), nombre y tipo.
- `decl_lineal_uml` — interfaces y enumeraciones no tienen cuerpo interno.
- `relacion_uml` — expresa herencia (`extiende`), implementación (`implementa`) o
  dependencia (`usa`) entre dos identificadores declarados.

**Nodos del AST generados:**
- `decl_clase` → `NodoClase` (con lista de `NodoMiembro`)
- `decl_lineal_uml` → `NodoClase` (sin miembros, nombre incluye el rol)
- `relacion_uml` → `NodoRelacionUML`

---

## Sección 8 — Gramática Completa Consolidada

Vista unificada de todas las reglas en un solo bloque de referencia.

```
(* ── NIVEL GLOBAL ─────────────────────────────────────────── *)
programa             ::= { meta_instruccion } cabecera cuerpo_modulo

meta_instruccion     ::= ( 'autor' | 'version' | 'tema'
                         | 'exportar' | 'importar' ) TEXTO_LITERAL ';'

cabecera             ::= 'diagrama' ( 'Flujo' | 'BD' | 'Redes'
                                    | 'Conceptual' | 'UML' ) ';'

cuerpo_modulo        ::= cuerpo_flujo | cuerpo_bd | cuerpo_redes
                       | cuerpo_conceptual | cuerpo_uml

(* ── FLUJO ─────────────────────────────────────────────────── *)
cuerpo_flujo         ::= { instruccion_flujo }
instruccion_flujo    ::= ( 'inicio' | 'fin' ) IDENTIFICADOR ';'
                       | ( 'nodo' | 'condicion' | 'bucle' | 'subproceso'
                         | 'entrada' | 'salida' | 'parada' )
                           IDENTIFICADOR TEXTO_LITERAL ';'
                       | IDENTIFICADOR 'conecta' IDENTIFICADOR ';'

(* ── BD ────────────────────────────────────────────────────── *)
cuerpo_bd            ::= { instruccion_bd }
instruccion_bd       ::= ( 'tabla' | 'vista' | 'esquema' | 'paquete' )
                           IDENTIFICADOR '{' { atributo } '}'
                       | ( 'procedimiento' | 'indice' | 'disparador'
                         | 'secuencia' | 'funcion' ) IDENTIFICADOR ';'
                       | IDENTIFICADOR 'relaciona' IDENTIFICADOR ';'
atributo             ::= IDENTIFICADOR ':' IDENTIFICADOR [ IDENTIFICADOR ] ';'

(* ── REDES ─────────────────────────────────────────────────── *)
cuerpo_redes         ::= { instruccion_redes }
instruccion_redes    ::= ( 'dispositivo' | 'nube' | 'vlan' | 'subred'
                         | 'cluster' | 'tunel' | 'zona' | 'puerto'
                         | 'politica' ) IDENTIFICADOR IDENTIFICADOR
                           [ '{' { IDENTIFICADOR ':' TEXTO_LITERAL ';' } '}' ] ';'
                       | IDENTIFICADOR 'enlaza' IDENTIFICADOR ';'

(* ── CONCEPTUAL ─────────────────────────────────────────────── *)
cuerpo_conceptual    ::= { instruccion_conceptual }
instruccion_conceptual ::= ( 'concepto' | 'categoria' | 'propiedad' )
                             IDENTIFICADOR TEXTO_LITERAL ';'
                          | IDENTIFICADOR ( 'agrupa' | 'asocia' | 'depende' )
                            IDENTIFICADOR ';'

(* ── UML ────────────────────────────────────────────────────── *)
cuerpo_uml           ::= { instruccion_uml }
instruccion_uml      ::= 'clase' IDENTIFICADOR
                           '{' { ( 'atributo' | 'metodo' )
                                   IDENTIFICADOR ':' IDENTIFICADOR ';' } '}'
                       | ( 'interfaz' | 'enum' ) IDENTIFICADOR ';'
                       | IDENTIFICADOR ( 'extiende' | 'implementa' | 'usa' )
                           IDENTIFICADOR ';'

(* ── TOKENS PRIMITIVOS ──────────────────────────────────────── *)
IDENTIFICADOR        ::= ( letra | '_' ) { letra | digito | '_' }
TEXTO_LITERAL        ::= '"' { caracter_no_comilla } '"'
letra                ::= 'a'..'z' | 'A'..'Z'
digito               ::= '0'..'9'
```

---

## Sección 9 — Propiedades de la Gramática

### Tipo de gramática

La gramática es **LL(1)** — analizable de izquierda a derecha con un lookahead
de exactamente 1 token — bajo las condiciones normales de cada módulo. El parser
implementado es un **analizador descendente recursivo** (Recursive Descent Parser).

### Conjuntos FIRST de las instrucciones principales

| Instrucción | FIRST |
|-------------|-------|
| `meta_instruccion` | { `autor`, `version`, `tema`, `exportar`, `importar` } |
| `cabecera` | { `diagrama` } |
| `decl_nodo_simple` | { `inicio`, `fin` } |
| `decl_nodo_texto` | { `nodo`, `condicion`, `bucle`, `subproceso`, `entrada`, `salida`, `parada` } |
| `conexion_flujo` | { IDENTIFICADOR } |
| `bloque_bd` | { `tabla`, `vista`, `esquema`, `paquete` } |
| `componente_lineal_bd` | { `procedimiento`, `indice`, `disparador`, `secuencia`, `funcion` } |
| `relacion_bd` | { IDENTIFICADOR } |
| `componente_red` | { `dispositivo`, `nube`, `vlan`, `subred`, `cluster`, `tunel`, `zona`, `puerto`, `politica` } |
| `enlace_red` | { IDENTIFICADOR } |
| `decl_concepto` | { `concepto`, `categoria`, `propiedad` } |
| `relacion_conceptual` | { IDENTIFICADOR } |
| `decl_clase` | { `clase` } |
| `decl_lineal_uml` | { `interfaz`, `enum` } |
| `relacion_uml` | { IDENTIFICADOR } |

### Resolución de ambigüedad IDENTIFICADOR

En los módulos Flujo, BD, Redes, Conceptual y UML existe una ambigüedad aparente
cuando el primer token de una instrucción es `IDENTIFICADOR`: puede ser el inicio
de una conexión/relación/enlace **o** un nombre propio que no fue declarado.

El parser resuelve esto mirando el **segundo token** (lookahead de 1):
- Si el segundo token es una keyword de verbo del módulo (`conecta`, `relaciona`,
  `enlaza`, `agrupa`, `asocia`, `depende`, `extiende`, `implementa`, `usa`)
  → es una instrucción de relación/conexión.
- Si el segundo token es otra cosa → error sintáctico.

Esto hace que la gramática sea efectivamente LL(1) en la práctica.

---

*Gramática extraída directamente del código fuente de los parsers.*  
*Archivo: `gramatica_formal.md` — Diagrams As Code v2.0*
