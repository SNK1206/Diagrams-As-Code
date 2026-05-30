# Documento de Pruebas — Diagrams As Code (DAC)

**Compilador:** Diagrams As Code v1.0  
**JDK usado:** Liberica Full JDK 21.0.7  
**Fecha:** 2026-05-29  
**Total de pruebas:** 25  
**Resultado esperado vs real:** Todas las pruebas coincidieron con lo esperado.

---

## Convenciones

| Símbolo | Significado |
|---------|-------------|
| ✅ PASS | Compilación exitosa (sin errores) |
| ❌ FAIL | Compilación con errores (comportamiento esperado en pruebas de error) |
| LÉXICO | Error detectado en fase de Análisis Léxico |
| SINTÁCTICO | Error detectado en fase de Análisis Sintáctico |
| SEMÁNTICO | Error detectado en fase de Análisis Semántico |

---

## Categoría 1 — Pruebas Léxicas

### PR01 — Cadena de texto sin cerrar

**Tipo:** Léxico | **Resultado esperado:** ❌ ERROR LÉXICO

**Código de entrada:**
```
diagrama Flujo;
nodo Proceso "texto sin cerrar;
inicio Arrancar;
```

**Qué se prueba:** El Lexer debe detectar que la cadena abierta con `"` nunca se cierra antes del fin de línea/archivo.

**Resultado real:** ❌ COMPILACIÓN FALLIDA
```
ERROR DE COMPILACIÓN [Línea 2] [Contexto: Léxico]
Detalle: Cadena de texto sin cerrar.
Sugerencia: Añade comillas dobles (") al final del texto.
```

---

### PR02 — Carácter inválido `@`

**Tipo:** Léxico | **Resultado esperado:** ❌ ERROR LÉXICO

**Código de entrada:**
```
diagrama Flujo;
@nodo Proceso "texto";
inicio Arrancar;
```

**Qué se prueba:** El carácter `@` no pertenece al alfabeto del lenguaje DAC.

**Resultado real:** ❌ COMPILACIÓN FALLIDA
```
ERROR DE COMPILACIÓN [Línea 2] [Contexto: Análisis Léxico]
Detalle: El carácter '@' no pertenece al alfabeto del lenguaje.
Sugerencia: Elimina el carácter o verifica si querías escribir un identificador o una cadena entre comillas ".
```

> **Nota:** Después del error, el Lexer continúa y tokeniza `nodo` como IDENTIFICADOR. El error léxico detiene el pipeline antes de parsear.

---

### PR03 — Número como primer carácter de identificador

**Tipo:** Léxico | **Resultado esperado:** ❌ ERROR LÉXICO

**Código de entrada:**
```
diagrama Flujo;
nodo 1Proceso "texto valido";
inicio Arrancar;
```

**Qué se prueba:** Un dígito (`1`) no puede iniciar un identificador. El Lexer lo reporta como carácter inválido.

**Resultado real:** ❌ COMPILACIÓN FALLIDA
```
ERROR DE COMPILACIÓN [Línea 2] [Contexto: Análisis Léxico]
Detalle: El carácter '1' no pertenece al alfabeto del lenguaje.
Sugerencia: Elimina el carácter o verifica si querías escribir un identificador o una cadena entre comillas ".
```

> **Nota:** El dígito `1` genera error; `Proceso` se tokeniza correctamente como IDENTIFICADOR separado.

---

### PR04 — Comentarios válidos (`#` y `//`)

**Tipo:** Léxico | **Resultado esperado:** ✅ COMPILACIÓN EXITOSA

**Código de entrada:**
```
# Este es un comentario estilo script
// Este es un comentario estilo Java
diagrama Flujo;
inicio Arrancar;
nodo Proceso "Procesar datos";
Arrancar conecta Proceso;
```

**Qué se prueba:** El Lexer debe ignorar correctamente ambos estilos de comentario sin generar tokens ni errores.

**Resultado real:** ✅ COMPILACIÓN EXITOSA
```
[Lexer] Análisis de caracteres completado.
[ParserBase] Evaluando Regla de Cabecera...
Contexto bloqueado para: Flujo
Registrado: Arrancar con rol [inicio]
Registrado: Proceso con rol [nodo]
COMPILACIÓN EXITOSA: Estructura base validada sin anomalías.
```

---

### PR22 — Carácter inválido `!`

**Tipo:** Léxico | **Resultado esperado:** ❌ ERROR LÉXICO

**Código de entrada:**
```
diagrama Flujo;
inicio !A;
```

**Qué se prueba:** El carácter `!` no forma parte del alfabeto del lenguaje DAC.

**Resultado real:** ❌ COMPILACIÓN FALLIDA
```
ERROR DE COMPILACIÓN [Línea 2] [Contexto: Análisis Léxico]
Detalle: El carácter '!' no pertenece al alfabeto del lenguaje.
Sugerencia: Elimina el carácter o verifica si querías escribir un identificador o una cadena entre comillas ".
```

---

### Tabla de errores léxicos encontrados en las pruebas

Errores léxicos que se activaron durante la ejecución de la Categoría 1:

| Prueba | Línea | Carácter / Situación | Ejemplo de código que lo genera | Contexto reportado | Mensaje de error | Detiene pipeline |
|--------|-------|---------------------|--------------------------------|--------------------|-----------------|-----------------|
| PR01 | 2 | Cadena `"` sin cerrar al llegar a EOF | `nodo Proceso "texto sin cerrar;` | `Léxico` | Cadena de texto sin cerrar. | ✅ Sí |
| PR02 | 2 | Carácter `@` | `@nodo Proceso "texto";` | `Análisis Léxico` | El carácter '@' no pertenece al alfabeto del lenguaje. | ✅ Sí |
| PR03 | 2 | Carácter `1` (dígito inicia identificador) | `nodo 1Proceso "texto valido";` | `Análisis Léxico` | El carácter '1' no pertenece al alfabeto del lenguaje. | ✅ Sí |
| PR22 | 2 | Carácter `!` | `inicio !A;` | `Análisis Léxico` | El carácter '!' no pertenece al alfabeto del lenguaje. | ✅ Sí |

**Observaciones:**
- Los errores léxicos del tipo EL01 (`@`, `!`, `1`) usan el mismo mensaje genérico `reportarErrorLéxico()` del `ManejadorErrores`.
- EL02 (cadena sin cerrar) usa un mensaje específico definido en el bloque de cadenas del `LexerBase`.
- En todos los casos el `ManejadorErrores` acumula el error y el compilador detiene el pipeline **antes de llegar al análisis sintáctico**.
- PR04 no generó ningún error léxico: los comentarios `#` y `//` son absorbidos correctamente sin producir tokens.

---

## Categoría 2 — Pruebas Sintácticas (Generales)

### PR05 — Archivo sin cabecera `diagrama`

**Tipo:** Sintáctico | **Resultado esperado:** ❌ ERROR SINTÁCTICO

**Código de entrada:**
```
inicio A;
nodo B "texto";
A conecta B;
```

**Qué se prueba:** Todo archivo `.dac` debe comenzar con `diagrama <Tipo>;`. Sin cabecera el compilador rechaza el archivo.

**Resultado real:** ❌ COMPILACIÓN FALLIDA
```
ERROR DE COMPILACIÓN [Línea 1] [Contexto: Sintáctico]
Detalle: No se encontró la cabecera principal.
Sugerencia: Asegúrate de incluir 'diagrama [Tipo];' en el archivo.
```

---

### PR06 — Tipo de diagrama desconocido

**Tipo:** Semántico | **Resultado esperado:** ❌ ERROR SEMÁNTICO

**Código de entrada:**
```
diagrama Mapa;
inicio A;
```

**Qué se prueba:** Solo existen tres módulos registrados: `Flujo`, `BD`, `Redes`. Cualquier otro tipo genera error.

**Resultado real:** ❌ COMPILACIÓN FALLIDA
```
ERROR DE COMPILACIÓN [Línea 1] [Contexto: Semántico]
Detalle: Módulo 'mapa' no reconocido.
Sugerencia: Los módulos válidos son: Flujo, BD, Redes.
```

---

### PR07 — Cabecera sin punto y coma

**Tipo:** Sintáctico | **Resultado esperado:** ❌ ERROR SINTÁCTICO

**Código de entrada:**
```
diagrama Flujo
inicio A;
nodo B "texto";
```

**Qué se prueba:** El `;` es obligatorio al final de la cabecera. Si falta, el parser detecta el error.

**Resultado real:** ❌ COMPILACIÓN FALLIDA
```
ERROR DE COMPILACIÓN [Línea 1] [Contexto: Sintáctico]
Detalle: Falta ';' en la cabecera.
Sugerencia: Termina la declaración con punto y coma.
```

---

### PR20 — Archivo vacío

**Tipo:** Sintáctico | **Resultado esperado:** ❌ ERROR SINTÁCTICO

**Código de entrada:** *(archivo vacío)*

**Qué se prueba:** Un archivo `.dac` vacío debe ser rechazado por ausencia de cabecera.

**Resultado real:** ❌ COMPILACIÓN FALLIDA
```
ERROR DE COMPILACIÓN [Línea 1] [Contexto: Sintáctico]
Detalle: No se encontró la cabecera principal.
Sugerencia: Asegúrate de incluir 'diagrama [Tipo];' en el archivo.
```

---

### PR21 — Solo meta-instrucciones, sin cabecera de diagrama

**Tipo:** Sintáctico | **Resultado esperado:** ❌ ERROR SINTÁCTICO

**Código de entrada:**
```
autor "Juan Perez";
version "1.0";
```

**Qué se prueba:** Las meta-instrucciones (`autor`, `version`) son válidas pero no sustituyen a la cabecera `diagrama <Tipo>;`.

**Resultado real:** ❌ COMPILACIÓN FALLIDA
```
ERROR DE COMPILACIÓN [Línea 1] [Contexto: Sintáctico]
Detalle: No se encontró la cabecera principal.
Sugerencia: Asegúrate de incluir 'diagrama [Tipo];' en el archivo.
```

---

## Categoría 3 — Pruebas del Módulo Flujo

### PR08 — Identificador duplicado en Flujo

**Tipo:** Semántico Flujo | **Resultado esperado:** ❌ ERROR SEMÁNTICO

**Código de entrada:**
```
diagrama Flujo;
inicio A;
inicio A;
nodo B "texto";
```

**Qué se prueba:** La tabla de símbolos no permite registrar dos veces el mismo nombre.

**Resultado real:** ❌ COMPILACIÓN FALLIDA
```
ERROR DE COMPILACIÓN [Línea 3] [Contexto: Semántico Flujo]
Detalle: El identificador 'A' ya existe.
Sugerencia: Usa un nombre diferente.
```

---

### PR09 — Conexión a destino no declarado

**Tipo:** Semántico Flujo | **Resultado esperado:** ❌ ERROR SEMÁNTICO

**Código de entrada:**
```
diagrama Flujo;
inicio A;
nodo B "Proceso B";
A conecta Fantasma;
```

**Qué se prueba:** Ambos extremos de una conexión deben estar registrados en la tabla de símbolos.

**Resultado real:** ❌ COMPILACIÓN FALLIDA
```
ERROR DE COMPILACIÓN [Línea 4] [Contexto: Semántico Flujo]
Detalle: El destino 'Fantasma' no ha sido declarado.
Sugerencia: Declara el elemento antes de conectarlo.
```

---

### PR10 — Verbo incorrecto en Flujo (`une` en lugar de `conecta`)

**Tipo:** Sintáctico Flujo | **Resultado esperado:** ❌ ERROR SINTÁCTICO

**Código de entrada:**
```
diagrama Flujo;
inicio A;
nodo B "texto";
A une B;
```

**Qué se prueba:** El único verbo de conexión válido en Flujo es `conecta`.

**Resultado real:** ❌ COMPILACIÓN FALLIDA
```
ERROR DE COMPILACIÓN [Línea 4] [Contexto: Sintáctico Flujo]
Detalle: Instrucción o verbo inválido en 'A'.
Sugerencia: Si deseas conectar elementos utiliza el verbo exclusivo 'conecta'.
```

---

### PR11 — Falta descripción en declaración de nodo

**Tipo:** Sintáctico Flujo | **Resultado esperado:** ❌ ERROR SINTÁCTICO

**Código de entrada:**
```
diagrama Flujo;
nodo A;
inicio B;
```

**Qué se prueba:** La instrucción `nodo` requiere obligatoriamente un identificador y una descripción entre comillas.

**Resultado real:** ❌ COMPILACIÓN FALLIDA
```
ERROR DE COMPILACIÓN [Línea 2] [Contexto: Sintáctico Flujo]
Detalle: Falta la descripción entre comillas para la instrucción 'nodo'.
Sugerencia: Revisa la sintaxis del diagrama.
```

---

### PR25 — Origen de conexión no declarado

**Tipo:** Semántico Flujo | **Resultado esperado:** ❌ ERROR SEMÁNTICO

**Código de entrada:**
```
diagrama Flujo;
nodo B "Proceso B";
OrigenInexistente conecta B;
```

**Qué se prueba:** El nodo origen también debe estar declarado antes de usarlo en una conexión.

**Resultado real:** ❌ COMPILACIÓN FALLIDA
```
ERROR DE COMPILACIÓN [Línea 3] [Contexto: Semántico Flujo]
Detalle: El origen 'OrigenInexistente' no ha sido declarado.
Sugerencia: Declara el elemento antes de conectarlo.
```

---

### PR23 — Flujo completo con todos los tipos de nodo

**Tipo:** Integración Flujo | **Resultado esperado:** ✅ COMPILACIÓN EXITOSA

**Código de entrada:**
```
diagrama Flujo;
inicio Arrancar;
entrada LeerDatos "Ingresar credenciales";
condicion Validar "Usuario existe?";
nodo Procesar "Generar token";
bucle Repetir "Mientras hay intentos";
subproceso Calcular "Calcular permisos";
salida MostrarResultado "Mostrar bienvenida";
parada Error "Bloquear cuenta";
fin Terminar;
Arrancar conecta LeerDatos;
LeerDatos conecta Validar;
Validar conecta Procesar;
Procesar conecta MostrarResultado;
MostrarResultado conecta Terminar;
```

**Qué se prueba:** Los 9 tipos de nodo del módulo Flujo (`inicio`, `entrada`, `condicion`, `nodo`, `bucle`, `subproceso`, `salida`, `parada`, `fin`) funcionan correctamente juntos.

**Resultado real:** ✅ COMPILACIÓN EXITOSA
```
Registrado: Arrancar   con rol [inicio]
Registrado: LeerDatos  con rol [entrada]
Registrado: Validar    con rol [condicion]
Registrado: Procesar   con rol [nodo]
Registrado: Repetir    con rol [bucle]
Registrado: Calcular   con rol [subproceso]
Registrado: MostrarResultado con rol [salida]
Registrado: Error      con rol [parada]
Registrado: Terminar   con rol [fin]
COMPILACIÓN EXITOSA: Estructura base validada sin anomalías.
```

---

## Categoría 4 — Pruebas del Módulo BD

### PR12 — BD válido completo

**Tipo:** Integración BD | **Resultado esperado:** ✅ COMPILACIÓN EXITOSA

**Código de entrada:**
```
diagrama BD;
tabla Productos {
    id: INT PK;
    nombre: VARCHAR;
    precio: DECIMAL;
}
tabla Categorias {
    id: INT PK;
    nombre: VARCHAR;
}
Productos relaciona Categorias;
```

**Qué se prueba:** Declaración de dos tablas con atributos y modificadores, más una relación entre ellas.

**Resultado real:** ✅ COMPILACIÓN EXITOSA
```
Registrado: Productos  con rol [tabla]
Registrado: Categorias con rol [tabla]
COMPILACIÓN EXITOSA: Estructura base validada sin anomalías.
```

---

### PR13 — Tabla duplicada en BD

**Tipo:** Semántico BD | **Resultado esperado:** ❌ ERROR SEMÁNTICO

**Código de entrada:**
```
diagrama BD;
tabla Usuarios {
    id: INT PK;
}
tabla Usuarios {
    nombre: VARCHAR;
}
```

**Qué se prueba:** No se puede declarar dos tablas con el mismo nombre en el mismo archivo.

**Resultado real:** ❌ COMPILACIÓN FALLIDA
```
ERROR DE COMPILACIÓN [Línea 5] [Contexto: Semántico BD]
Detalle: El elemento 'Usuarios' ya existe.
Sugerencia: Elige otro identificador.
```

---

### PR14 — Verbo incorrecto en BD (`conecta` en lugar de `relaciona`)

**Tipo:** Sintáctico BD | **Resultado esperado:** ❌ ERROR SINTÁCTICO

**Código de entrada:**
```
diagrama BD;
tabla A { id: INT PK; }
tabla B { id: INT PK; }
A conecta B;
```

**Qué se prueba:** En el módulo BD el verbo de unión es `relaciona`, no `conecta` (que pertenece a Flujo).

**Resultado real:** ❌ COMPILACIÓN FALLIDA
```
ERROR DE COMPILACIÓN [Línea 7] [Contexto: Sintáctico BD]
Detalle: Verbo inválido en 'A'.
Sugerencia: Usa el verbo 'relaciona' para bases de datos.
```

---

### PR15 — Relación a tabla no declarada

**Tipo:** Semántico BD | **Resultado esperado:** ❌ ERROR SEMÁNTICO

**Código de entrada:**
```
diagrama BD;
tabla A {
    id: INT PK;
}
A relaciona TablaInexistente;
```

**Qué se prueba:** Ambas tablas referenciadas en `relaciona` deben estar declaradas en el archivo.

**Resultado real:** ❌ COMPILACIÓN FALLIDA
```
ERROR DE COMPILACIÓN [Línea 5] [Contexto: Semántico BD]
Detalle: La entidad destino 'TablaInexistente' no existe.
Sugerencia: Declárala primero.
```

---

### PR16 — Falta llave de apertura `{` en tabla BD

**Tipo:** Sintáctico BD | **Resultado esperado:** ❌ ERROR SINTÁCTICO

**Código de entrada:**
```
diagrama BD;
tabla A
    id: INT PK;
}
```

**Qué se prueba:** La declaración de tabla requiere obligatoriamente abrir con `{` antes de los atributos.

**Resultado real:** ❌ COMPILACIÓN FALLIDA
```
ERROR DE COMPILACIÓN [Línea 3] [Contexto: Sintáctico BD]
Detalle: Falta abrir '{' para la definición.
Sugerencia: Verifica las reglas de sintaxis.
```

---

### PR24 — BD con todos los tipos de componentes

**Tipo:** Integración BD | **Resultado esperado:** ✅ COMPILACIÓN EXITOSA

**Código de entrada:**
```
diagrama BD;
tabla Ventas { id: INT PK; monto: DECIMAL; }
vista ResumenVentas { total: DECIMAL; }
esquema Reportes { region: VARCHAR; }
procedimiento GenerarReporte;
funcion CalcularIVA;
indice IdxVentas;
disparador AuditarVenta;
secuencia SeqVentas;
Ventas relaciona ResumenVentas;
```

**Qué se prueba:** Los 8 tipos de componente BD (`tabla`, `vista`, `esquema`, `paquete`, `procedimiento`, `funcion`, `indice`, `disparador`, `secuencia`) se registran correctamente.

**Resultado real:** ✅ COMPILACIÓN EXITOSA
```
Registrado: Ventas            con rol [tabla]
Registrado: ResumenVentas     con rol [vista]
Registrado: Reportes          con rol [esquema]
Registrado: GenerarReporte    con rol [procedimiento]
Registrado: CalcularIVA       con rol [funcion]
Registrado: IdxVentas         con rol [indice]
Registrado: AuditarVenta      con rol [disparador]
Registrado: SeqVentas         con rol [secuencia]
COMPILACIÓN EXITOSA: Estructura base validada sin anomalías.
```

---

## Categoría 5 — Pruebas del Módulo Redes

### PR17 — Redes válido completo

**Tipo:** Integración Redes | **Resultado esperado:** ✅ COMPILACIÓN EXITOSA

**Código de entrada:**
```
diagrama Redes;
dispositivo Router1 Router { IP: "192.168.1.1"; };
dispositivo Switch1 Switch { Puertos: "24"; };
dispositivo PC1 Servidor { SO: "Linux"; };
Router1 enlaza Switch1;
Switch1 enlaza PC1;
```

**Qué se prueba:** Declaración de dispositivos con propiedades y enlaces entre ellos.

**Resultado real:** ✅ COMPILACIÓN EXITOSA
```
Registrado: Router1 con rol [dispositivo_Router]
Registrado: Switch1 con rol [dispositivo_Switch]
Registrado: PC1     con rol [dispositivo_Servidor]
COMPILACIÓN EXITOSA: Estructura base validada sin anomalías.
```

---

### PR18 — Dispositivo duplicado en Redes

**Tipo:** Semántico Redes | **Resultado esperado:** ❌ ERROR SEMÁNTICO

**Código de entrada:**
```
diagrama Redes;
dispositivo R1 Router { IP: "10.0.0.1"; };
dispositivo R1 Switch { Puertos: "8"; };
```

**Qué se prueba:** Dos dispositivos no pueden tener el mismo nombre en el mismo diagrama.

**Resultado real:** ❌ COMPILACIÓN FALLIDA
```
ERROR DE COMPILACIÓN [Línea 3] [Contexto: Semántico Redes]
Detalle: El dispositivo 'R1' ya existe.
Sugerencia: Asigna un nombre único.
```

---

### PR19 — Verbo incorrecto en Redes (`conecta` en lugar de `enlaza`)

**Tipo:** Sintáctico Redes | **Resultado esperado:** ❌ ERROR SINTÁCTICO

**Código de entrada:**
```
diagrama Redes;
dispositivo A Router { IP: "1.1.1.1"; };
dispositivo B Switch { IP: "2.2.2.2"; };
A conecta B;
```

**Qué se prueba:** En Redes el verbo de unión es `enlaza`. Usar `conecta` (de Flujo) genera error sintáctico.

**Resultado real:** ❌ COMPILACIÓN FALLIDA
```
ERROR DE COMPILACIÓN [Línea 4] [Contexto: Sintáctico Redes]
Detalle: Verbo incorrecto en 'A'.
Sugerencia: Para redes usa el verbo exclusivo 'enlaza'.
```

---

## Resumen General de Resultados

| ID | Categoría | Descripción | Esperado | Obtenido | Estado |
|----|-----------|-------------|----------|----------|--------|
| PR01 | Léxico | Cadena sin cerrar | ❌ Error | ❌ Error | ✅ OK |
| PR02 | Léxico | Carácter inválido `@` | ❌ Error | ❌ Error | ✅ OK |
| PR03 | Léxico | Número inicia identificador | ❌ Error | ❌ Error | ✅ OK |
| PR04 | Léxico | Comentarios `#` y `//` | ✅ Éxito | ✅ Éxito | ✅ OK |
| PR05 | Sintáctico | Sin cabecera | ❌ Error | ❌ Error | ✅ OK |
| PR06 | Semántico | Módulo desconocido | ❌ Error | ❌ Error | ✅ OK |
| PR07 | Sintáctico | Cabecera sin `;` | ❌ Error | ❌ Error | ✅ OK |
| PR08 | Semántico Flujo | Identificador duplicado | ❌ Error | ❌ Error | ✅ OK |
| PR09 | Semántico Flujo | Destino de conexión no declarado | ❌ Error | ❌ Error | ✅ OK |
| PR10 | Sintáctico Flujo | Verbo incorrecto (`une`) | ❌ Error | ❌ Error | ✅ OK |
| PR11 | Sintáctico Flujo | Falta descripción en `nodo` | ❌ Error | ❌ Error | ✅ OK |
| PR12 | Integración BD | BD válida completa | ✅ Éxito | ✅ Éxito | ✅ OK |
| PR13 | Semántico BD | Tabla duplicada | ❌ Error | ❌ Error | ✅ OK |
| PR14 | Sintáctico BD | Verbo `conecta` en BD | ❌ Error | ❌ Error | ✅ OK |
| PR15 | Semántico BD | Relación a tabla no declarada | ❌ Error | ❌ Error | ✅ OK |
| PR16 | Sintáctico BD | Falta `{` en tabla | ❌ Error | ❌ Error | ✅ OK |
| PR17 | Integración Redes | Redes válido completo | ✅ Éxito | ✅ Éxito | ✅ OK |
| PR18 | Semántico Redes | Dispositivo duplicado | ❌ Error | ❌ Error | ✅ OK |
| PR19 | Sintáctico Redes | Verbo `conecta` en Redes | ❌ Error | ❌ Error | ✅ OK |
| PR20 | Sintáctico | Archivo vacío | ❌ Error | ❌ Error | ✅ OK |
| PR21 | Sintáctico | Meta-instrucciones sin diagrama | ❌ Error | ❌ Error | ✅ OK |
| PR22 | Léxico | Carácter inválido `!` | ❌ Error | ❌ Error | ✅ OK |
| PR23 | Integración Flujo | Flujo con todos los tipos de nodo | ✅ Éxito | ✅ Éxito | ✅ OK |
| PR24 | Integración BD | BD con todos los componentes | ✅ Éxito | ✅ Éxito | ✅ OK |
| PR25 | Semántico Flujo | Origen de conexión no declarado | ❌ Error | ❌ Error | ✅ OK |

**Pruebas ejecutadas:** 25 / 25  
**Pruebas correctas:** 25 / 25  
**Pruebas fallidas:** 0 / 25

---

## Catálogo Completo de Errores del Compilador

Errores extraídos directamente del código fuente (`LexerBase`, `ParserBase`, `FlujoParser`, `BDParser`, `RedesParser`).  
**Total: 2 léxicos + 29 sintácticos = 31 errores.**

---

### Errores Léxicos — `LexerBase.java`

El compilador tiene **exactamente 2 llamadas reales** a funciones de error léxico en el código fuente:

| Código | Contexto reportado | Función en el código | Descripción | Mensaje exacto del compilador |
|--------|--------------------|---------------------|-------------|-------------------------------|
| EL01 | `Análisis Léxico` | `reportarErrorLéxico(linea, char)` | Cualquier carácter que no pertenece al alfabeto del lenguaje (ej. `@`, `!`, `-`, `$`) | `El carácter 'X' no pertenece al alfabeto del lenguaje.` |
| EL02 | `Léxico` | `reportarError(linea, "Léxico", ...)` | Cadena de texto abierta con `"` sin cerrar antes del fin de línea o del archivo | `Cadena de texto sin cerrar.` |

> **Nota:** EL01 es una sola función genérica que aplica a cualquier carácter inválido. No existe una función separada por carácter en el código.

---

### Errores Sintácticos — `ParserBase.java` (General / Núcleo)

| Código | Contexto reportado | Descripción | Ejemplo que lo dispara | Mensaje exacto del compilador |
|--------|--------------------|-------------|------------------------|-------------------------------|
| ES01 | `Sintáctico Núcleo` | Falta `;` al final de una meta-instrucción | `autor "Juan"` *(sin `;`)* | `Falta ';' al final de la instrucción 'autor'.` |
| ES02 | `Sintáctico Núcleo` | Meta-instrucción sin valor entre comillas | `autor 1.0;` | `Se esperaba un texto entre comillas después de 'autor'.` |
| ES03 | `Sintáctico` | Falta `;` al final de la cabecera `diagrama <Tipo>` | `diagrama Flujo` *(sin `;`)* | `Falta ';' en la cabecera.` |
| ES04 | `Sintáctico` | Falta el tipo de diagrama después de `diagrama` | `diagrama;` | `Falta el tipo de diagrama.` |
| ES05 | `Sintáctico` | No se encontró la cabecera principal en el archivo | Archivo vacío o sin `diagrama` | `No se encontró la cabecera principal.` |

---

### Errores Sintácticos — `FlujoParser.java` (Módulo Flujo)

| Código | Contexto reportado | Descripción | Ejemplo que lo dispara | Mensaje exacto del compilador |
|--------|--------------------|-------------|------------------------|-------------------------------|
| ES06 | `Sintáctico Flujo` | Token inesperado al inicio de instrucción | `123 inicio A;` | `Token inesperado '123'.` |
| ES07 | `Sintáctico Flujo` | Falta el identificador después de palabra clave (`inicio`, `fin`, `nodo`, etc.) | `inicio;` | `Falta el nombre del identificador después de 'inicio'.` |
| ES08 | `Sintáctico Flujo` | Falta `;` al final de declaración simple (`inicio`, `fin`) | `inicio A` *(sin `;`)* | `Falta ';' al final de la declaración de 'A'.` |
| ES09 | `Sintáctico Flujo` | Falta descripción entre comillas en instrucción con texto (`nodo`, `condicion`, `entrada`, etc.) | `nodo A;` | `Falta la descripción entre comillas para la instrucción 'nodo'.` |
| ES10 | `Sintáctico Flujo` | Falta `;` al final de instrucción con texto | `nodo A "desc"` *(sin `;`)* | `Falta ';' al final de la línea.` |
| ES11 | `Sintáctico Flujo` | Falta el identificador destino en instrucción `conecta` | `A conecta;` | `Falta el elemento destino para la conexión.` |
| ES12 | `Sintáctico Flujo` | Falta `;` al finalizar instrucción de conexión | `A conecta B` *(sin `;`)* | `Falta ';' al finalizar la instrucción de conexión.` |
| ES13 | `Sintáctico Flujo` | Verbo inválido — se esperaba `conecta` | `A une B;` / `A -> B;` | `Instrucción o verbo inválido en 'A'.` |

---

### Errores Sintácticos — `BDParser.java` (Módulo BD)

| Código | Contexto reportado | Descripción | Ejemplo que lo dispara | Mensaje exacto del compilador |
|--------|--------------------|-------------|------------------------|-------------------------------|
| ES14 | `Sintáctico BD` | Token inesperado al inicio de instrucción | `{ id: INT; }` *(sin nombre de tabla)* | `Token inesperado '{'.` |
| ES15 | `Sintáctico BD` | Falta el nombre de la entidad (tabla, vista, esquema, paquete) | `tabla { id: INT; }` | `Falta el nombre de la tabla.` |
| ES16 | `Sintáctico BD` | Falta `{` para abrir bloque de definición | `tabla A id: INT; }` | `Falta abrir '{' para la definición.` |
| ES17 | `Sintáctico BD` | Falta el nombre del atributo dentro del bloque | `tabla A { : INT; }` | `Falta el nombre del atributo.` |
| ES18 | `Sintáctico BD` | Falta `:` entre nombre y tipo de dato del atributo | `tabla A { id INT; }` | `Falta ':'.` |
| ES19 | `Sintáctico BD` | Falta el tipo de dato del atributo | `tabla A { id: ; }` | `Falta el tipo de dato.` |
| ES20 | `Sintáctico BD` | Falta `;` al final del atributo | `tabla A { id: INT }` | `Falta ';' al final del atributo.` |
| ES21 | `Sintáctico BD` | Falta `}` para cerrar el bloque de definición | `tabla A { id: INT;` *(sin `}`)* | `Falta cerrar llaves '}'.` |
| ES22 | `Sintáctico BD` | Falta el identificador del componente lineal (procedimiento, funcion, etc.) | `procedimiento;` | `Falta el identificador para el procedimiento.` |
| ES23 | `Sintáctico BD` | Falta `;` al final de componente lineal | `procedimiento MiProc` *(sin `;`)* | `Falta ';' al final.` |
| ES24 | `Sintáctico BD` | Falta el identificador destino en instrucción `relaciona` | `A relaciona;` | `Falta el destino de la relación.` |
| ES25 | `Sintáctico BD` | Falta `;` en instrucción `relaciona` | `A relaciona B` *(sin `;`)* | `Falta ';'.` |
| ES26 | `Sintáctico BD` | Verbo inválido — se esperaba `relaciona` | `A conecta B;` | `Verbo inválido en 'A'.` |

---

### Errores Sintácticos — `RedesParser.java` (Módulo Redes)

| Código | Contexto reportado | Descripción | Ejemplo que lo dispara | Mensaje exacto del compilador |
|--------|--------------------|-------------|------------------------|-------------------------------|
| ES27 | `Sintáctico Redes` | Token inesperado al inicio de instrucción | `{ IP: "1.1.1.1"; };` | `Token inesperado '{'.` |
| ES28 | `Sintáctico Redes` | Falta el nombre del dispositivo | `dispositivo Router { IP: "1.1"; };` | `Falta el nombre del dispositivo.` |
| ES29 | `Sintáctico Redes` | Falta el tipo del dispositivo | `dispositivo R1 { IP: "1.1"; };` | `Falta el tipo de dispositivo (ej. Router, Switch).` |
| ES30 | `Sintáctico Redes` | Falta `}` para cerrar bloque de propiedades | `dispositivo R1 Router { IP: "1.1";` *(sin `}`)* | `Falta cerrar llaves '}'.` |
| ES31 | `Sintáctico Redes` | Falta `;` al final de declaración de dispositivo | `dispositivo R1 Router { IP: "1.1"; }` *(sin `;` final)* | `Falta ';' al final del dispositivo.` |
| ES32 | `Sintáctico Redes` | Falta el identificador destino en instrucción `enlaza` | `R1 enlaza;` | `Falta el dispositivo destino.` |
| ES33 | `Sintáctico Redes` | Falta `;` en instrucción `enlaza` | `R1 enlaza R2` *(sin `;`)* | `Falta ';'.` |
| ES34 | `Sintáctico Redes` | Verbo incorrecto — se esperaba `enlaza` | `R1 conecta R2;` | `Verbo incorrecto en 'R1'.` |

---

### Resumen por categoría

| Categoría | Archivo fuente | Cantidad | Códigos |
|-----------|---------------|----------|---------|
| Léxicos | `LexerBase.java` | 2 | EL01 – EL02 |
| Sintácticos Generales | `ParserBase.java` | 5 | ES01 – ES05 |
| Sintácticos Flujo | `FlujoParser.java` | 8 | ES06 – ES13 |
| Sintácticos BD | `BDParser.java` | 13 | ES14 – ES26 |
| Sintácticos Redes | `RedesParser.java` | 8 | ES27 – ES34 |
| **Total** | | **36** | |

---

*Documento generado para el proyecto Diagrams As Code — Pruebas de compilación léxica, sintáctica y semántica.*
