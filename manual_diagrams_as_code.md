# Manual de Usuario — Diagrams As Code (DAC)

**Version:** 2.0  
**Lenguaje fuente:** `.dac`  
**Plataforma:** IDE Pedagogico (JavaFX)

---

## Tabla de Contenidos

1. [Que es Diagrams As Code](#1-que-es-diagrams-as-code)
2. [Como ejecutar el programa](#2-como-ejecutar-el-programa)
3. [La interfaz del IDE](#3-la-interfaz-del-ide)
4. [Estructura basica de un archivo DAC](#4-estructura-basica-de-un-archivo-dac)
5. [Modulo Flujo](#5-modulo-flujo)
6. [Modulo BD (Base de Datos)](#6-modulo-bd-base-de-datos)
7. [Modulo Redes](#7-modulo-redes)
8. [Modulo Conceptual](#8-modulo-conceptual)
9. [Modulo UML](#9-modulo-uml)
10. [Sistema de errores](#10-sistema-de-errores)
11. [Tabla de simbolos](#11-tabla-de-simbolos)
12. [Catalogo de errores lexicos y sintacticos](#12-catalogo-de-errores-lexicos-y-sintacticos)
13. [Ejemplos completos](#13-ejemplos-completos)
14. [Preguntas frecuentes](#14-preguntas-frecuentes)

---

## 1. Que es Diagrams As Code

**Diagrams As Code (DAC)** es un IDE pedagogico que te permite escribir diagramas usando texto plano con un lenguaje propio (extension `.dac`). El sistema analiza tu codigo en tres fases:

1. **Analisis Lexico** — Divide el codigo en tokens (palabras, simbolos, literales).
2. **Analisis Sintactico** — Verifica que la estructura del codigo sea correcta segun la gramatica del lenguaje.
3. **Tabla de Simbolos** — Registra todos los identificadores declarados y sus roles.

El programa no genera una imagen del diagrama. Su proposito es educativo: mostrar como funciona un compilador real paso a paso.

### Tipos de diagrama disponibles

| Tipo | Palabra clave | Para que sirve |
|------|--------------|----------------|
| Diagrama de Flujo | `Flujo` | Modelar procesos con nodos y conexiones |
| Base de Datos | `BD` | Modelar tablas, atributos y relaciones |
| Redes | `Redes` | Modelar topologias de red con dispositivos y enlaces |
| Conceptual | `Conceptual` | Modelar mapas conceptuales con conceptos y relaciones |
| UML | `UML` | Modelar diagramas de clases con clases, interfaces y relaciones |

---

## 2. Como ejecutar el programa

Desde la terminal (PowerShell), ubicate en la carpeta del proyecto y ejecuta:

**Compilar el proyecto:**
```powershell
javac --module-path "C:\Users\bobal\Desktop\openjfx-26.0.1_windows-x64_bin-sdk\javafx-sdk-26.0.1\lib" --add-modules javafx.controls,javafx.fxml -cp src -d out (Get-ChildItem -Recurse -Filter "*.java" src | Select-Object -ExpandProperty FullName)
```

**Ejecutar el IDE:**
```powershell
java --module-path "C:\Users\bobal\Desktop\openjfx-26.0.1_windows-x64_bin-sdk\javafx-sdk-26.0.1\lib" --add-modules javafx.controls,javafx.fxml -cp out com.diagramas.MainFX
```

---

## 3. La interfaz del IDE

Al abrir el programa verás una ventana dividida en cuatro areas principales.

### 3.1 Barra de herramientas (parte superior)

| Boton | Color | Que hace |
|-------|-------|----------|
| Nuevo | Naranja | Crea una pestaña nueva con plantilla `diagrama Flujo;` |
| Abrir | Azul | Abre el explorador para cargar un archivo `.dac` existente |
| Guardar | Morado | Guarda el archivo de la pestaña activa en disco |
| Compilar e Inspeccionar | Verde | Ejecuta el compilador sobre el codigo de la pestaña activa |
| Errores Lexicos | Rojo | Muestra el catalogo estatico de todos los codigos de error (EL y ES) |
| Arbol de Derivacion | Verde azulado | Genera y muestra el arbol sintactico del codigo actual |
| Simbologia del Lenguaje | Verde oscuro | Abre la tabla con todos los simbolos del lenguaje DAC |
| Manual de Usuario | Azul oscuro | Abre este manual dentro del IDE |

### 3.2 Panel izquierdo — Editor de codigo

- Cada archivo abierto ocupa su propia **pestana** en la parte superior.
- El editor muestra **numeros de linea** en un panel gris a la izquierda. Los numeros se actualizan automaticamente al escribir.
- La fuente es monoespaciada para facilitar la lectura del codigo.
- Los archivos nuevos sin guardar se llaman `sin_titulo_N.dac`.

### 3.3 Panel derecho — Analisis

Dividido en dos secciones:

**1. Tokens**  
Muestra todos los tokens generados por el analizador lexico, con su tipo, lexema y numero de linea. Ejemplo:
```
Token [IDENTIFICADOR | 'inicio' | Linea: 3]
Token [IDENTIFICADOR | 'Arranque' | Linea: 3]
Token [PUNTO_Y_COMA | ';' | Linea: 3]
```

**2. Tabla de Simbolos**  
Muestra el contexto activo y todos los identificadores registrados durante el analisis. Ejemplo:
```
Contexto Bloqueado: Flujo

IDENTIFICADOR ENCONTRADO  | TIPO/ROL ASIGNADO
----------------------------------------------------
autor                     | Mi Proyecto
version                   | 1.0
diagrama                  | Flujo
Flujo                     | tipo_diagrama
Arranque                  | inicio
Proceso1                  | nodo
```

### 3.4 Consola (parte inferior, arrastrable)

Muestra el resultado del compilador:
- **COMPILACION EXITOSA** si no hay errores.
- **COMPILACION FALLIDA** con la lista completa de errores si los hay.
- **ERRORES LEXICOS + SINTACTICOS** si hay errores en ambas fases.

La consola es un panel **arrastrable**: puedes hacer clic en el separador entre la consola y el panel principal y arrastrarlo hacia arriba para ver mas errores.

---

## 4. Estructura basica de un archivo DAC

Todo archivo `.dac` debe seguir esta estructura:

```
[meta-instrucciones opcionales]
diagrama <TipoDiagrama>;
[instrucciones del modulo]
```

### 4.1 Meta-instrucciones (opcionales)

Se escriben antes de la declaracion `diagrama`. Son informacion descriptiva del archivo.

```
autor "Nombre del autor";
version "1.0";
```

Estas instrucciones se registran en la tabla de simbolos automaticamente.

**Reglas:**
- El valor debe ir entre comillas dobles (`"`).
- Cada instruccion termina con punto y coma (`;`).

### 4.2 Declaracion de tipo de diagrama (obligatoria)

Esta es la unica instruccion que el compilador exige siempre.

```
diagrama Flujo;
diagrama BD;
diagrama Redes;
diagrama Conceptual;
diagrama UML;
```

**Reglas:**
- La palabra `diagrama` es una palabra reservada del sistema.
- El tipo (`Flujo`, `BD`, etc.) es sensible a mayusculas/minusculas.
- El punto y coma al final es obligatorio.
- Sin esta declaracion, el compilador reporta el error ES05 y no procesa nada mas.

### 4.3 Identificadores

Son los nombres que das a tus elementos (nodos, tablas, clases, etc.).

**Reglas:**
- Deben comenzar con una letra (`a-z`, `A-Z`) o guion bajo (`_`).
- Pueden contener letras, digitos y guiones bajos.
- **No** pueden comenzar con un numero.
- **No** pueden contener caracteres especiales (`$`, `@`, `%`, etc.).

**Validos:** `Proceso1`, `mi_tabla`, `ClaseVehiculo`, `_temp`  
**Invalidos:** `1proceso`, `mi-tabla`, `clase$Auto`

### 4.4 Literales de texto

Se usan para descripciones. Van entre comillas dobles en la misma linea.

```
nodo Validar "Verificar credenciales del usuario";
```

**Reglas:**
- La comilla de cierre es obligatoria en la misma linea.
- Una cadena sin cerrar genera el error lexico EL02.

### 4.5 Comentarios

Los comentarios se escriben con `//` y el compilador los ignora completamente.

```
// Esto es un comentario
inicio Arranque;   // esto tambien es comentario
```

### 4.6 Compatibilidad entre modulos

Si declaras `diagrama BD;` pero escribes instrucciones de otro modulo (por ejemplo, `clase MiClase {...}`), el compilador reportara errores sintacticos porque cada modulo solo reconoce su propio vocabulario.

---

## 5. Modulo Flujo

Activado con `diagrama Flujo;`. Modela procesos secuenciales con nodos y flechas.

### 5.1 Nodos simples

Representan puntos de inicio, fin u otros nodos sin descripcion.

```
inicio <Identificador>;
fin <Identificador>;
parada <Identificador>;
```

**Ejemplo:**
```
inicio Comenzar;
fin Terminar;
```

### 5.2 Nodos con descripcion

Representan pasos del proceso con un texto explicativo.

```
nodo <ID> "<descripcion>";
condicion <ID> "<descripcion>";
bucle <ID> "<descripcion>";
subproceso <ID> "<descripcion>";
entrada <ID> "<descripcion>";
salida <ID> "<descripcion>";
```

**Ejemplo:**
```
nodo LeerDatos "Leer credenciales del usuario";
condicion Valido "El usuario es valido?";
bucle Reintentar "Intentar de nuevo si fallo";
```

### 5.3 Conexiones

Establece flechas entre dos nodos.

```
<IDOrigen> conecta <IDDestino>;
```

**Ejemplo:**
```
Comenzar conecta LeerDatos;
LeerDatos conecta Valido;
```

### 5.4 Ejemplo completo Flujo

```
autor "Login de usuario";
version "1.0";
diagrama Flujo;

inicio Comenzar;
nodo LeerDatos "Leer credenciales";
condicion Valido "Las credenciales son correctas?";
nodo Acceso "Permitir acceso al sistema";
fin Terminar;

Comenzar conecta LeerDatos;
LeerDatos conecta Valido;
Valido conecta Acceso;
Acceso conecta Terminar;
```

### 5.5 Errores frecuentes en Flujo

| Error | Causa | Ejemplo |
|-------|-------|---------|
| ES07 | Falta nombre despues de `inicio` | `inicio;` |
| ES08 | Falta `;` al final | `inicio MiNodo` |
| ES09 | Falta descripcion entre comillas | `nodo MiNodo;` |
| ES11 | Falta destino en `conecta` | `A conecta;` |
| ES13 | Verbo incorrecto (usa `conecta`) | `A enlaza B;` |

---

## 6. Modulo BD (Base de Datos)

Activado con `diagrama BD;`. Modela esquemas de base de datos.

### 6.1 Tablas y bloques

```
tabla <NombreTabla> {
    <campo> : <Tipo>;
    <campo> : <Tipo>;
}
```

Otras palabras clave que funcionan igual que `tabla`: `vista`, `esquema`, `paquete`.

**Ejemplo:**
```
tabla Usuarios {
    id : INT;
    nombre : VARCHAR;
    correo : VARCHAR;
}
```

### 6.2 Componentes lineales (sin bloque)

Objetos de BD que solo necesitan un nombre:

```
procedimiento <Nombre>;
funcion <Nombre>;
disparador <Nombre>;
secuencia <Nombre>;
```

**Ejemplo:**
```
procedimiento ActualizarSaldo;
funcion CalcularImpuesto;
```

### 6.3 Relaciones

```
<TablaOrigen> relaciona <TablaDestino>;
```

**Ejemplo:**
```
Usuarios relaciona Posts;
Posts relaciona Comentarios;
```

### 6.4 Ejemplo completo BD

```
autor "Esquema de blog";
version "1.0";
diagrama BD;

tabla Usuarios {
    id : INT;
    nombre : VARCHAR;
    correo : VARCHAR;
}

tabla Posts {
    id : INT;
    titulo : VARCHAR;
    contenido : TEXT;
}

tabla Comentarios {
    id : INT;
    texto : VARCHAR;
}

funcion ObtenerPosts;
disparador AuditarCambios;

Usuarios relaciona Posts;
Posts relaciona Comentarios;
```

### 6.5 Errores frecuentes en BD

| Error | Causa |
|-------|-------|
| ES15 | Falta nombre de tabla: `tabla { ... }` |
| ES16 | Falta `{` para abrir bloque: `tabla MiTabla;` |
| ES17 | Falta nombre de atributo: `: INT;` |
| ES18 | Falta `:` en atributo: `campo INT;` |
| ES19 | Falta tipo de dato: `campo : ;` |
| ES26 | Verbo incorrecto (usa `relaciona`): `A conecta B;` |

---

## 7. Modulo Redes

Activado con `diagrama Redes;`. Modela topologias de red.

### 7.1 Declaracion de dispositivos

```
dispositivo <Nombre> <Tipo>;
dispositivo <Nombre> <Tipo> { configuracion };
```

**Tipos comunes:** `Router`, `Switch`, `Hub`, `Firewall`, `PC`, `Servidor`

**Ejemplo:**
```
dispositivo R1 Router;
dispositivo SW1 Switch { modelo Catalyst3750 };
dispositivo PC1 PC;
```

### 7.2 Enlace entre dispositivos

```
<IDOrigen> enlaza <IDDestino>;
```

**Ejemplo:**
```
R1 enlaza SW1;
SW1 enlaza PC1;
```

### 7.3 Ejemplo completo Redes

```
autor "Red corporativa";
version "1.0";
diagrama Redes;

dispositivo Router1 Router;
dispositivo Switch1 Switch;
dispositivo PC1 PC;
dispositivo PC2 PC;
dispositivo Servidor1 Servidor;

Router1 enlaza Switch1;
Switch1 enlaza PC1;
Switch1 enlaza PC2;
Switch1 enlaza Servidor1;
```

### 7.4 Errores frecuentes en Redes

| Error | Causa |
|-------|-------|
| ES28 | Falta nombre del dispositivo: `dispositivo;` |
| ES29 | Falta tipo del dispositivo: `dispositivo R1;` |
| ES32 | Falta destino en enlaza: `R1 enlaza;` |
| ES34 | Verbo incorrecto (usa `enlaza`): `R1 conecta R2;` |

---

## 8. Modulo Conceptual

Activado con `diagrama Conceptual;`. Modela mapas conceptuales.

### 8.1 Declaracion de nodos conceptuales

```
concepto <ID> "<descripcion>";
categoria <ID> "<descripcion>";
propiedad <ID> "<descripcion>";
```

**Ejemplo:**
```
concepto Compilador "Programa que transforma codigo fuente";
categoria Fases "Etapas del proceso de compilacion";
propiedad Eficiencia "Velocidad de procesamiento";
```

### 8.2 Relaciones conceptuales

```
<IDOrigen> agrupa <IDDestino>;
<IDOrigen> asocia <IDDestino>;
<IDOrigen> depende <IDDestino>;
```

**Verbos y su significado:**

| Verbo | Significado |
|-------|-------------|
| `agrupa` | Un concepto contiene o agrupa a otro |
| `asocia` | Asociacion general entre dos conceptos |
| `depende` | Un concepto depende de otro para existir |

**Ejemplo:**
```
Fases agrupa Lexico;
Compilador asocia Eficiencia;
Sintactico depende Lexico;
```

### 8.3 Ejemplo completo Conceptual

```
autor "Teoria de compiladores";
version "1.0";
diagrama Conceptual;

concepto Compilador "Transforma codigo fuente en ejecutable";
categoria Fases "Las tres fases principales";
concepto Lexico "Analiza caracteres y genera tokens";
concepto Sintactico "Verifica la estructura gramatical";
concepto Semantico "Valida la coherencia logica";
propiedad Eficiencia "Rapidez del proceso de compilacion";

Fases agrupa Lexico;
Fases agrupa Sintactico;
Fases agrupa Semantico;
Compilador asocia Eficiencia;
Sintactico depende Lexico;
Semantico depende Sintactico;
```

### 8.4 Errores frecuentes en Conceptual

| Error | Causa |
|-------|-------|
| ES36 | Falta nombre: `concepto;` |
| ES37 | Falta descripcion: `concepto MiConcepto;` |
| ES39 | Falta destino: `A agrupa;` |
| ES42 | Verbo incorrecto: `A conecta B;` |

---

## 9. Modulo UML

Activado con `diagrama UML;`. Modela diagramas de clases UML.

### 9.1 Declaracion de clases

```
clase <NombreClase> {
    atributo <nombre> : <Tipo>;
    metodo <nombre> : <TipoRetorno>;
}
```

**Tipos comunes:** `INT`, `STRING`, `BOOLEAN`, `VOID`, `FLOAT`, `DOUBLE`

**Ejemplo:**
```
clase Vehiculo {
    atributo velocidad : INT;
    atributo marca : STRING;
    metodo acelerar : VOID;
    metodo frenar : BOOLEAN;
}
```

### 9.2 Interfaces y enumeraciones

Son componentes sin bloque de miembros:

```
interfaz <ID>;
enum <ID>;
```

**Ejemplo:**
```
interfaz Serializable;
interfaz Comparable;
enum EstadoMotor;
```

### 9.3 Relaciones UML

```
<IDOrigen> extiende <IDDestino>;
<IDOrigen> implementa <IDDestino>;
<IDOrigen> usa <IDDestino>;
```

| Verbo | Equivalente UML |
|-------|-----------------|
| `extiende` | Herencia (`extends`) |
| `implementa` | Implementacion de interfaz (`implements`) |
| `usa` | Dependencia o uso |

**Ejemplo:**
```
Auto extiende Vehiculo;
Auto implementa Serializable;
Motor usa Combustible;
```

### 9.4 Ejemplo completo UML

```
autor "Diagrama de vehiculos";
version "1.0";
diagrama UML;

clase Vehiculo {
    atributo velocidad : INT;
    atributo marca : STRING;
    metodo acelerar : VOID;
    metodo frenar : BOOLEAN;
}

clase Auto {
    atributo puertas : INT;
    metodo abrirCofre : VOID;
}

clase Camion {
    atributo carga : FLOAT;
    metodo cargar : VOID;
}

interfaz Serializable;
interfaz Conducible;
enum EstadoMotor;

Auto extiende Vehiculo;
Camion extiende Vehiculo;
Auto implementa Serializable;
Auto implementa Conducible;
Auto usa EstadoMotor;
```

### 9.5 Errores frecuentes en UML

| Error | Causa |
|-------|-------|
| ES44 | Falta nombre de clase o interfaz: `clase { ... }` |
| ES45 | Falta `{` para abrir clase: `clase Auto;` |
| ES46 | Keyword incorrecto dentro de clase (solo `atributo` o `metodo`) |
| ES47 | Falta nombre del miembro: `atributo : INT;` |
| ES48 | Falta `:` en miembro: `atributo velocidad INT;` |
| ES49 | Falta tipo del miembro: `atributo velocidad : ;` |
| ES54 | Verbo incorrecto (usa `extiende`, `implementa` o `usa`) |

---

## 10. Sistema de errores

El compilador clasifica los errores en dos categorias y los muestra en la consola con este formato:

```
==================================================
[ERROR] [EL01] LEXICO [Linea 6]
Detalle: El caracter '%' no pertenece al alfabeto del lenguaje.
Consejo: Elimina el caracter o verifica si querias escribir un identificador.
==================================================
```

### 10.1 Comportamiento del compilador ante errores

- **Errores lexicos (EL):** El lexer reporta el error pero el parser **sigue ejecutandose** para mostrar mas informacion. Los simbolos validos que logre registrar apareceran en la tabla de simbolos.
- **Errores de cabecera (ES01-ES05):** Si la declaracion `diagrama` es incorrecta, el modulo parser **no corre** y la tabla de simbolos solo muestra `autor` y `version`.
- **Errores de modulo (ES06-ES54):** Las instrucciones con error se saltan y las instrucciones validas siguientes **si se procesan** (anti-cascada). Los simbolos validos aparecen en la tabla.

### 10.2 Recuperacion de panico (anti-cascada)

Cuando el parser encuentra un error en una instruccion, avanza automaticamente hasta el siguiente `;` o `}` y continua con la siguiente instruccion. Esto evita que un solo error genere docenas de errores falsos.

**Ejemplo:** Si tienes 10 instrucciones y la primera tiene un error, el compilador reporta **1 error** para esa instruccion y luego procesa correctamente las 9 restantes.

---

## 11. Tabla de simbolos

La tabla de simbolos registra todos los identificadores declarados durante el analisis. Se muestra en el panel derecho del IDE.

### Que aparece en la tabla

| Identificador | Tipo/Rol | Cuando se registra |
|--------------|----------|--------------------|
| `autor` | Valor del autor | Al procesar `autor "...";` |
| `version` | Valor de la version | Al procesar `version "...";` |
| `diagrama` | Tipo de diagrama | Al procesar `diagrama Flujo;` |
| `Flujo` (o el tipo) | `tipo_diagrama` | Al procesar `diagrama Flujo;` |
| Nombre de nodo | `inicio`, `fin`, `nodo`, etc. | Al procesar cada instruccion valida |
| Nombre de tabla | `tabla`, `vista`, etc. | Al procesar cada bloque valido |
| Nombre de clase | `clase` | Al procesar cada clase valida |

### Que NO aparece en la tabla

- Conexiones (`conecta`, `enlaza`, `relaciona`) — no declaran nuevos identificadores.
- Instrucciones con error — si una instruccion falla, su identificador no se registra.

---

## 12. Catalogo de errores lexicos y sintacticos

### Errores Lexicos (EL)

| Codigo | Descripcion | Ejemplo que lo genera |
|--------|-------------|----------------------|
| EL01 | Caracter no valido en el alfabeto DAC | `inicio Nodo %;` |
| EL02 | Cadena de texto sin cerrar | `nodo A "sin cerrar` |
| EL03 | Identificador que comienza con digito | `3inicio` |
| EL04 | Caracter invalido dentro de un identificador | `mi$nodo` |

### Errores Sintacticos — Cabecera (ES01-ES05)

| Codigo | Descripcion |
|--------|-------------|
| ES01 | Falta `;` al final de meta-instruccion (`autor`, `version`) |
| ES02 | Falta texto entre comillas en meta-instruccion |
| ES03 | Falta `;` en la declaracion `diagrama <Tipo>` |
| ES04 | Falta el tipo de diagrama despues de `diagrama` |
| ES05 | No se encontro la declaracion `diagrama <Tipo>;` |

### Errores Sintacticos — Modulo Flujo (ES06-ES13)

| Codigo | Descripcion |
|--------|-------------|
| ES06 | Token inesperado (no es una instruccion valida de Flujo) |
| ES07 | Falta el nombre del nodo despues de la palabra clave |
| ES08 | Falta `;` al final de nodo simple |
| ES09 | Falta descripcion entre comillas en nodo con texto |
| ES10 | Falta `;` al final de nodo con descripcion |
| ES11 | Falta el identificador destino en instruccion `conecta` |
| ES12 | Falta `;` al finalizar instruccion de conexion |
| ES13 | Verbo invalido en Flujo (usa exclusivamente `conecta`) |

### Errores Sintacticos — Modulo BD (ES14-ES26)

| Codigo | Descripcion |
|--------|-------------|
| ES14 | Token inesperado (no es una instruccion valida de BD) |
| ES15 | Falta el nombre de la tabla o bloque |
| ES16 | Falta `{` para abrir el bloque de atributos |
| ES17 | Falta el nombre del atributo dentro del bloque |
| ES18 | Falta `:` en la definicion del atributo |
| ES19 | Falta el tipo de dato del atributo |
| ES20 | Falta `;` al final del atributo |
| ES21 | Falta `}` para cerrar el bloque (se detecta al llegar a EOF) |
| ES22 | Falta el nombre del componente lineal |
| ES23 | Falta `;` al final del componente lineal |
| ES24 | Falta el identificador destino en instruccion `relaciona` |
| ES25 | Falta `;` al finalizar instruccion de relacion |
| ES26 | Verbo invalido en BD (usa exclusivamente `relaciona`) |

### Errores Sintacticos — Modulo Redes (ES27-ES34)

| Codigo | Descripcion |
|--------|-------------|
| ES27 | Token inesperado (no es una instruccion valida de Redes) |
| ES28 | Falta el nombre del dispositivo |
| ES29 | Falta el tipo del dispositivo |
| ES30 | Falta `}` para cerrar bloque de configuracion (se detecta al llegar a EOF) |
| ES31 | Falta `;` al final de la declaracion del dispositivo |
| ES32 | Falta el identificador destino en instruccion `enlaza` |
| ES33 | Falta `;` al finalizar instruccion de enlace |
| ES34 | Verbo invalido en Redes (usa exclusivamente `enlaza`) |

### Errores Sintacticos — Modulo Conceptual (ES35-ES42)

| Codigo | Descripcion |
|--------|-------------|
| ES35 | Token inesperado (no es una instruccion valida de Conceptual) |
| ES36 | Falta el nombre del concepto, categoria o propiedad |
| ES37 | Falta la descripcion entre comillas |
| ES38 | Falta `;` al final de la declaracion |
| ES39 | Falta el identificador destino en la relacion conceptual |
| ES40 | Falta `;` al finalizar la relacion conceptual |
| ES41 | Identificador sin verbo de relacion (el siguiente token no es un verbo) |
| ES42 | Verbo invalido en Conceptual (usa `agrupa`, `asocia` o `depende`) |

### Errores Sintacticos — Modulo UML (ES43-ES54)

| Codigo | Descripcion |
|--------|-------------|
| ES43 | Token inesperado (no es una instruccion valida de UML) |
| ES44 | Falta el nombre de la clase, interfaz o enum |
| ES45 | Falta `{` para abrir el cuerpo de la clase |
| ES46 | Keyword invalido dentro de clase (solo se permite `atributo` o `metodo`) |
| ES47 | Falta el nombre del miembro (atributo o metodo) |
| ES48 | Falta `:` en la definicion del miembro |
| ES49 | Falta el tipo del miembro |
| ES50 | Falta `;` al final del miembro o componente lineal |
| ES51 | Falta `}` para cerrar la clase (se detecta al llegar a EOF) |
| ES52 | Falta el identificador destino en la relacion UML |
| ES53 | Falta `;` al finalizar la relacion UML |
| ES54 | Verbo invalido en UML (usa `extiende`, `implementa` o `usa`) |

---

## 13. Ejemplos completos

### Ejemplo 1 — Flujo de inicio de sesion

```
autor "Sistema de Login";
version "1.0";
diagrama Flujo;

inicio Comenzar;
nodo LeerCredenciales "Solicitar usuario y contrasena";
condicion CredencialesValidas "Son correctas las credenciales?";
nodo PermitirAcceso "Abrir sesion del usuario";
nodo MostrarError "Mostrar mensaje de error";
fin FinSesion;

Comenzar conecta LeerCredenciales;
LeerCredenciales conecta CredencialesValidas;
CredencialesValidas conecta PermitirAcceso;
CredencialesValidas conecta MostrarError;
PermitirAcceso conecta FinSesion;
MostrarError conecta FinSesion;
```

---

### Ejemplo 2 — Base de datos de tienda

```
autor "Tienda en linea";
version "2.0";
diagrama BD;

tabla Clientes {
    id : INT;
    nombre : VARCHAR;
    correo : VARCHAR;
}

tabla Productos {
    id : INT;
    nombre : VARCHAR;
    precio : FLOAT;
    stock : INT;
}

tabla Pedidos {
    id : INT;
    fecha : DATE;
    total : FLOAT;
}

tabla DetallesPedido {
    id : INT;
    cantidad : INT;
}

procedimiento GenerarFactura;
funcion CalcularTotal;

Clientes relaciona Pedidos;
Pedidos relaciona DetallesPedido;
DetallesPedido relaciona Productos;
```

---

### Ejemplo 3 — Red de oficina

```
autor "Infraestructura oficina";
version "1.0";
diagrama Redes;

dispositivo RouterPrincipal Router;
dispositivo Switch1 Switch { puertos 24 };
dispositivo Switch2 Switch { puertos 24 };
dispositivo ServidorWeb Servidor;
dispositivo ServidorBD Servidor;
dispositivo PC1 PC;
dispositivo PC2 PC;
dispositivo PC3 PC;
dispositivo Impresora Impresora;

RouterPrincipal enlaza Switch1;
RouterPrincipal enlaza Switch2;
Switch1 enlaza ServidorWeb;
Switch1 enlaza ServidorBD;
Switch2 enlaza PC1;
Switch2 enlaza PC2;
Switch2 enlaza PC3;
Switch2 enlaza Impresora;
```

---

### Ejemplo 4 — Mapa conceptual de compiladores

```
autor "Teoria de compiladores";
version "1.0";
diagrama Conceptual;

concepto Compilador "Programa que transforma codigo fuente en codigo ejecutable";
categoria FasesCompilacion "Las etapas principales del proceso de compilacion";
concepto AnálisisLexico "Lee caracteres y produce tokens";
concepto AnalisisSintactico "Verifica la estructura gramatical";
concepto AnalisisSemantico "Valida la coherencia logica del codigo";
propiedad Eficiencia "Velocidad y calidad del proceso";
concepto Token "Unidad minima del lenguaje";
concepto AST "Arbol de Sintaxis Abstracta";

FasesCompilacion agrupa AnálisisLexico;
FasesCompilacion agrupa AnalisisSintactico;
FasesCompilacion agrupa AnalisisSemantico;
Compilador asocia FasesCompilacion;
Compilador asocia Eficiencia;
AnálisisLexico asocia Token;
AnalisisSintactico asocia AST;
AnalisisSintactico depende AnálisisLexico;
AnalisisSemantico depende AnalisisSintactico;
```

---

### Ejemplo 5 — Diagrama de clases UML

```
autor "Sistema de vehiculos";
version "1.0";
diagrama UML;

clase Vehiculo {
    atributo velocidadMaxima : INT;
    atributo marca : STRING;
    atributo modelo : STRING;
    metodo acelerar : VOID;
    metodo frenar : VOID;
    metodo obtenerInfo : STRING;
}

clase Auto {
    atributo numeroPuertas : INT;
    atributo tieneAireAcondicionado : BOOLEAN;
    metodo abrirMaletero : VOID;
}

clase Camion {
    atributo capacidadCarga : FLOAT;
    atributo ejes : INT;
    metodo cargar : VOID;
    metodo descargar : VOID;
}

clase Motor {
    atributo cilindros : INT;
    atributo potencia : INT;
    metodo encender : BOOLEAN;
    metodo apagar : VOID;
}

interfaz Serializable;
interfaz Mantenible;
enum EstadoMotor;
enum TipoCombustible;

Auto extiende Vehiculo;
Camion extiende Vehiculo;
Auto implementa Serializable;
Auto implementa Mantenible;
Camion implementa Mantenible;
Auto usa Motor;
Camion usa Motor;
Motor usa EstadoMotor;
Motor usa TipoCombustible;
```

---

## 14. Preguntas frecuentes

**P: El compilador dice "COMPILACION EXITOSA" pero no veo imagen del diagrama. Es normal?**  
R: Si. El proposito del IDE es pedagogico: muestra el proceso de compilacion (tokens, tabla de simbolos, errores), no genera imagenes. La salida esperada son los tokens y la tabla de simbolos.

**P: Por que algunos simbolos no aparecen en la tabla de simbolos?**  
R: Solo se registran los identificadores de nodos, tablas, clases, etc. Las conexiones (`conecta`, `enlaza`, `relaciona`) no registran identificadores porque no declaran elementos nuevos.

**P: Tengo un error en la linea 3 pero el compilador reporta error en la linea 5. Por que?**  
R: El numero de linea corresponde a donde el parser detecto el problema, no necesariamente donde esta el error de escritura. Por ejemplo, si olvidas el `;` al final de una instruccion, el error se detecta en la linea siguiente donde el parser encuentra un token inesperado.

**P: Escribi `diagrama Redes;` pero puse instrucciones de UML dentro. Que pasa?**  
R: El compilador reporta errores sintacticos porque cada modulo solo reconoce su propio vocabulario. Por ejemplo, `clase` en un diagrama de Redes genera ES34 (verbo invalido) porque el parser de Redes lo interpreta como un identificador sin el verbo `enlaza`.

**P: La consola muestra muchos errores. Como los veo todos?**  
R: Arrastra el separador entre la consola y el panel principal hacia arriba para agrandar la consola. El compilador puede mostrar hasta 1000 errores sin suprimir ninguno.

**P: Puedo tener comentarios en mi archivo .dac?**  
R: Si. Usa `//` al inicio de la linea o despues de una instruccion. El lexer ignora todo lo que siga despues de `//` hasta el final de la linea.

**P: Los nombres de los nodos distinguen mayusculas y minusculas?**  
R: Si. `MiNodo`, `minodo` y `MINODO` son tres identificadores diferentes.

**P: Puedo usar el mismo nombre en dos modulos diferentes?**  
R: No aplica, ya que cada archivo solo puede tener un tipo de diagrama. La tabla de simbolos se reinicia con cada compilacion.

---

*Manual de Usuario — Diagrams As Code v2.0*  
*Compilador pedagogico de lenguaje especifico de dominio (DSL)*
