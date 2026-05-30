# Reporte de Integración — Diagrams As Code

**Rama destino:** Javier  
**Fecha:** 2026-05-30  
**Integrado por:** Javier Arias  
**Fuentes:** rama `Diego` + rama `Omar`

---

## Resumen ejecutivo

Se integraron los aportes de Diego y Omar a la rama Javier siguiendo una estrategia quirúrgica: solo se copiaron los archivos `.java` y `.md` relevantes, **sin importar los SDKs binarios de JavaFX** que Omar tenía comprometidos en su rama. Las funcionalidades previas de Javier (Catálogo de Errores, Árbol de Derivación) se conservaron íntegramente.

---

## Cambios de Diego

### Nuevos módulos del compilador

Diego implementó dos módulos completos de análisis sintáctico que faltaban en el compilador:

#### Módulo Conceptual (`src/com/diagramas/modulos/conceptual/`)

| Archivo | Descripción |
|---|---|
| `ConceptualParser.java` | Parser para el tipo de diagrama `conceptual`. Procesa nodos (`concepto`, `categoria`, `propiedad`) y relaciones (`agrupa`, `asocia`, `depende`). Errores ES35–ES42. |
| `ast/NodeAST.java` | Clase base abstracta del AST Conceptual. |
| `ast/NodoConcepto.java` | Nodo AST para un elemento conceptual (nombre, rol, descripción). |
| `ast/NodoRelacionConceptual.java` | Nodo AST para relaciones entre conceptos (origen, verbo, destino). |
| `ast/RaizConceptualAST.java` | Raíz del árbol de sintaxis abstracta del mapa conceptual. |

**Sintaxis habilitada:**
```
diagrama Conceptual;
concepto Compilador "Traduce código fuente a código objeto";
categoria Lenguajes "Grupo de lenguajes soportados";
Compilador agrupa Lenguajes;
Compilador asocia Lenguajes;
```

#### Módulo UML (`src/com/diagramas/modulos/uml/`)

| Archivo | Descripción |
|---|---|
| `UMLParser.java` | Parser para el tipo de diagrama `uml`. Soporta `clase` (con bloque de miembros), `interfaz`, `enum` y relaciones (`extiende`, `implementa`, `usa`). Errores ES43–ES54. |
| `ast/NodeAST.java` | Clase base abstracta del AST UML. |
| `ast/NodoClase.java` | Nodo AST para clases, interfaces y enums UML. |
| `ast/NodoMiembro.java` | Nodo AST para atributos y métodos de una clase. |
| `ast/NodoRelacionUML.java` | Nodo AST para relaciones UML (herencia, implementación, dependencia). |
| `ast/RaizUMLAST.java` | Raíz del árbol de sintaxis abstracta del diagrama UML. |

**Sintaxis habilitada:**
```
diagrama UML;
clase Vehiculo {
    atributo velocidad : INT;
    metodo acelerar : VOID;
}
interfaz Conducible;
Auto extiende Vehiculo;
Auto implementa Conducible;
```

#### RedesParser reubicado en paquete correcto

El archivo `src/com/diagramas/modulos/RedesParser.java` ya tenía declarado `package com.diagramas.modulos.redes;` pero se encontraba en la carpeta equivocada. Se copió al paquete físicamente correcto:

- **Nuevo:** `src/com/diagramas/modulos/redes/RedesParser.java`

El archivo original `modulos/RedesParser.java` permanece como referencia mientras no se elimine manualmente.

---

### Modificaciones a archivos existentes (Diego)

#### `src/com/diagramas/core/ParserBase.java`

- Se añadieron dos casos al método `delegarAlModulo()`:
  - `case "conceptual"` → instancia y ejecuta `ConceptualParser`
  - `case "uml"` → instancia y ejecuta `UMLParser`
- Se actualizó el mensaje de error ES05 para mencionar los 5 módulos válidos: Flujo, BD, Redes, Conceptual, UML.

#### `src/com/diagramas/core/ManejadorErrores.java`

- Se añadió un método sobrecargado:  
  `reportarError(String codigo, int linea, String contexto, String mensaje, String sugerencia)`  
  Esto permite a los nuevos parsers (ConceptualParser, UMLParser) reportar errores con código de identificación (ES35, ES43, etc.) sin romper la firma existente que usan FlujoParser, BDParser y RedesParser.

---

## Cambios de Omar

### Nuevos archivos

#### `src/com/diagramas/core/TablaSimbologiaEstatica.java`

Clase nueva con un catálogo estático de todos los símbolos del lenguaje Diagrams As Code. Contiene:

- `EntradaSimbolo`: modelo interno (lexema, tipoToken, categoria, descripcion).
- `TABLA`: lista inmutable con todos los símbolos registrados.
- `filtrar(String modulo)`: devuelve entradas filtradas por módulo (`Flujo`, `BD`, `Redes`, `Conceptual`, `UML`, `Global`, `Puntuacion`, `Todos`).
- `imprimir()`: vuelca la tabla formateada en consola.

La tabla de Omar fue **extendida** durante la integración para incluir los símbolos de los módulos Conceptual y UML de Diego (20 entradas adicionales: `concepto`, `categoria`, `propiedad`, `agrupa`, `asocia`, `depende`, `clase`, `interfaz`, `enum`, `atributo`, `metodo`, `extiende`, `implementa`, `usa`).

**Total de símbolos registrados: 64**

#### `documentacion_tecnica.md`

Documentación técnica completa del compilador elaborada por Omar. Incluye descripción de la arquitectura, flujo de compilación, descripción de cada clase del núcleo y los módulos BD y Redes.

---

### Modificaciones a archivos existentes (Omar)

#### `src/com/diagramas/MainFX.java`

Omar añadía tres botones de referencia y los métodos asociados, pero su versión de `MainFX.java` **no tenía** el Catálogo de Errores ni el Árbol de Derivación implementados por Javier. Por eso **no se hizo checkout directo** de su archivo. En cambio, se modificó el `MainFX.java` de Javier para incorporar únicamente lo nuevo de Omar:

**Imports añadidos:**
- `javafx.stage.Modality`
- `javafx.beans.property.SimpleStringProperty`

**Botones añadidos a la toolbar** (empujados al extremo derecho con un `Region` espaciador):

| Botón | Color | Acción |
|---|---|---|
| Simbología del Lenguaje | Verde (`#16a085`) | Abre ventana con tabla interactiva de simbología, filtrable por módulo |
| Manual de Usuario | Azul oscuro (`#1a5276`) | Abre y muestra `manual_diagrams_as_code.md` en ventana de solo lectura |
| Documentación Técnica | Morado (`#6c3483`) | Abre y muestra `documentacion_tecnica.md` en ventana de solo lectura |

**Botones conservados de Javier:**
- Compilar e Inspeccionar
- Errores Léxicos (Catálogo de Errores con filtro por categoría)
- Árbol de Derivación (visualización gráfica top-down con zoom y pantalla completa)

**Métodos añadidos a `MainFX`:**

| Método | Origen | Descripción |
|---|---|---|
| `mostrarTablaSimbologia(Stage)` | Omar | Abre ventana con `TableView` de `TablaSimbologiaEstatica`, con leyenda de colores por módulo y ComboBox de filtro |
| `mostrarManual(Stage, String, String)` | Omar | Abre una ventana de solo lectura que carga y muestra cualquier archivo `.md` del proyecto |
| `etiquetaColor(String, String)` | Omar | Helper que construye un `HBox` con cuadro de color y etiqueta para la leyenda |

**Catálogo de Errores actualizado:**  
Se añadieron 20 errores nuevos al catálogo estático (ES35–ES54) correspondientes a los módulos Conceptual y UML de Diego.

---

## Archivos excluidos de la integración

Los siguientes archivos de la rama Omar **no se importaron** para evitar contaminar el repositorio con binarios pesados:

- `javafx-sdk-21.0.9/` (cientos de archivos binarios `.jar`, `.dll`, `.so`)
- `javafx-sdk-26.0.1/` (ídem)
- `openjfx-21.0.9_windows-x64_bin-sdk.zip`
- `openjfx-26.0.1_windows-x64_bin-sdk.zip`

El SDK de JavaFX debe mantenerse fuera del repositorio y configurarse localmente en cada entorno de desarrollo.

---

## Resumen de archivos creados/modificados

| Archivo | Operación | Autor |
|---|---|---|
| `src/com/diagramas/core/ManejadorErrores.java` | Modificado | Diego (adaptación Javier) |
| `src/com/diagramas/core/ParserBase.java` | Modificado | Diego (adaptación Javier) |
| `src/com/diagramas/core/TablaSimbologiaEstatica.java` | Creado | Omar (extendido con Conceptual+UML) |
| `src/com/diagramas/modulos/conceptual/ConceptualParser.java` | Creado | Diego |
| `src/com/diagramas/modulos/conceptual/ast/NodeAST.java` | Creado | Diego |
| `src/com/diagramas/modulos/conceptual/ast/NodoConcepto.java` | Creado | Diego |
| `src/com/diagramas/modulos/conceptual/ast/NodoRelacionConceptual.java` | Creado | Diego |
| `src/com/diagramas/modulos/conceptual/ast/RaizConceptualAST.java` | Creado | Diego |
| `src/com/diagramas/modulos/uml/UMLParser.java` | Creado | Diego |
| `src/com/diagramas/modulos/uml/ast/NodeAST.java` | Creado | Diego |
| `src/com/diagramas/modulos/uml/ast/NodoClase.java` | Creado | Diego |
| `src/com/diagramas/modulos/uml/ast/NodoMiembro.java` | Creado | Diego |
| `src/com/diagramas/modulos/uml/ast/NodoRelacionUML.java` | Creado | Diego |
| `src/com/diagramas/modulos/uml/ast/RaizUMLAST.java` | Creado | Diego |
| `src/com/diagramas/modulos/redes/RedesParser.java` | Creado | Javier (reubicación de Diego) |
| `src/com/diagramas/MainFX.java` | Modificado | Omar (adaptación Javier) |
| `documentacion_tecnica.md` | Creado | Omar |
| `integracion.md` | Creado | Javier |

---

## Estado del compilador tras la integración

El compilador ahora soporta **5 módulos completos**:

| Módulo | Palabra clave | Parser | Autor |
|---|---|---|---|
| Flujo | `diagrama Flujo;` | `FlujoParser` | Equipo base |
| Base de Datos | `diagrama BD;` | `BDParser` | Equipo base |
| Redes | `diagrama Redes;` | `RedesParser` | Javier |
| Mapa Conceptual | `diagrama Conceptual;` | `ConceptualParser` | Diego |
| UML | `diagrama UML;` | `UMLParser` | Diego |

Y la interfaz gráfica cuenta con **8 acciones** en la barra de herramientas:

1. Nuevo archivo
2. Abrir archivo
3. Guardar archivo
4. Compilar e Inspeccionar
5. Errores Léxicos (Catálogo filtrable — Javier)
6. Árbol de Derivación (visualización gráfica — Javier)
7. Simbología del Lenguaje (tabla filtrable — Omar)
8. Manual de Usuario (visor de `.md` — Omar)
9. Documentación Técnica (visor de `.md` — Omar)
