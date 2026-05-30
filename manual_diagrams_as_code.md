# Manual de Usuario — Diagrams As Code (DAC)

**Version:** 2.0
**Lenguaje fuente:** `.dac`
**Plataforma:** IDE Pedagogico (JavaFX) + Compilador de Consola (CLI)

---

## Tabla de Contenidos

1. [Introduccion](#1-introduccion)
2. [Arquitectura del Compilador](#2-arquitectura-del-compilador)
3. [Uso del IDE Pedagogico (GUI)](#3-uso-del-ide-pedagogico-gui)
4. [Uso desde la Terminal (CLI)](#4-uso-desde-la-terminal-cli)
5. [Estructura del Lenguaje DAC](#5-estructura-del-lenguaje-dac)
6. [Meta-Instrucciones Globales](#6-meta-instrucciones-globales)
7. [Modulo: Diagramas de Flujo](#7-modulo-diagramas-de-flujo)
8. [Modulo: Diagramas de Base de Datos](#8-modulo-diagramas-de-base-de-datos)
9. [Modulo: Diagramas de Redes](#9-modulo-diagramas-de-redes)
10. [Sistema de Errores y Diagnosticos](#10-sistema-de-errores-y-diagnosticos)
11. [Tabla de Simbolos](#11-tabla-de-simbolos)
12. [Referencia Rapida de Sintaxis](#12-referencia-rapida-de-sintaxis)
13. [Ejemplos Completos](#13-ejemplos-completos)

---

## 1. Introduccion

**Diagrams As Code (DAC)** es un compilador pedagogico que permite describir diagramas mediante
un lenguaje textual propio con extension `.dac`. El sistema valida el codigo fuente en tres fases:

  - Analisis Lexico    -- Descompone el codigo en tokens.
  - Analisis Sintactico -- Verifica la estructura gramatical y construye un AST.
  - Analisis Semantico  -- Valida coherencia logica (referencias existentes, unicidad, etc.).

El compilador soporta tres tipos de diagrama:

  Tipo de Diagrama       | Palabra Clave | Modulo
  -----------------------|---------------|----------------------------------
  Diagrama de Flujo      | Flujo         | com.diagramas.modulos.flujo
  Diagrama de BD         | BD            | com.diagramas.modulos.bd
  Diagrama de Redes      | Redes         | com.diagramas.modulos.redes

---

## 2. Arquitectura del Compilador

El pipeline de compilacion sigue este orden. Si una fase falla, las siguientes no se ejecutan.

  Archivo .dac
       |
       v
  [ LexerBase ]          Tokenizacion del codigo fuente
       |
       v
  [ ParserBase ]         Valida la cabecera y delega al modulo correcto
       |
       |--- "Flujo"  --> [ FlujoParser ]  --> RaizFlujoAST
       |--- "BD"     --> [ BDParser    ]  --> RaizBDAST
       |--- "Redes"  --> [ RedesParser ]  --> RaizRedesAST
                                |
                     [ TablaSimbolos ]     Analisis semantico
                     [ ManejadorErrores ]  Reporte de errores

### Componentes principales

  Clase               | Responsabilidad
  --------------------|------------------------------------------------------
  LexerBase           | Convierte el codigo fuente en una secuencia de Token
  Token               | Unidad minima del lenguaje (tipo, lexema, linea)
  ParserBase          | Analiza la cabecera y delega al modulo correcto
  FlujoParser         | Parser + semantica del modulo Flujo
  BDParser            | Parser + semantica del modulo Base de Datos
  RedesParser         | Parser + semantica del modulo Redes
  TablaSimbolos       | Registro de identificadores y sus tipos en memoria
  ManejadorErrores    | Acumula y formatea todos los errores de compilacion
  TablaSimbologiaEstatica | Catalogo estatico de todos los simbolos del lenguaje

---

## 3. Uso del IDE Pedagogico (GUI)

Al lanzar MainFX se abre una ventana dividida en tres zonas.

### 3.1 Barra de Herramientas

  Boton                    | Color        | Descripcion
  -------------------------|--------------|------------------------------------------
  Nuevo                    | Naranja      | Crea una pestana en blanco con plantilla
  Abrir                    | Azul         | Carga un archivo .dac desde el explorador
  Guardar                  | Morado       | Guarda el archivo activo en disco
  Compilar e Inspeccionar  | Verde        | Ejecuta las tres fases del compilador
  Simbologia del Lenguaje  | Verde oscuro | Muestra la tabla estatica de simbolos
  Manual de Usuario        | Azul oscuro  | Abre este manual dentro del IDE

### 3.2 Panel Central

Se divide en dos columnas:

**Izquierda -- Editor con Pestanas**
  - Cada archivo ocupa su propia pestana.
  - Los archivos nuevos sin guardar se nombran sin_titulo_N.dac
  - El editor es monoespaciado y totalmente editable.

**Derecha -- Panel de Analisis Interno**
  - 1. Tokens: todos los tokens generados por el Lexer (tipo, lexema, linea).
  - 2. Tabla de Simbolos: contexto activo e identificadores registrados con su rol.

### 3.3 Consola de Diagnostico

Panel inferior que muestra:
  - Confirmacion de carga de archivo.
  - Resultado final de compilacion (exito o fallo).
  - Todos los errores con linea, contexto, descripcion y sugerencia.

### 3.4 Ventana de Simbologia del Lenguaje (Modal)

Al presionar el boton verde oscuro se abre una ventana modal con:
  - Tabla de 4 columnas: Lexema / Tipo Token / Categoria / Descripcion
  - ComboBox de filtro: Todos | Flujo | BD | Redes | Global | Puntuacion
  - Conteo dinamico de simbolos segun el filtro activo
  - Filas coloreadas por modulo:
      Morado claro  = Global (Cabecera y Meta-instrucciones)
      Azul claro    = Modulo Flujo
      Amarillo claro = Modulo BD
      Verde claro   = Modulo Redes
      Gris claro    = Puntuacion, Literales y Comentarios
  - Leyenda de colores en la parte inferior

---

## 4. Uso desde la Terminal (CLI)

El compilador puede ejecutarse sin interfaz grafica desde la clase Main.

### Comando con JavaFX SDK (para MainFX)

  java --module-path "javafx-sdk-21.0.9\lib" ^
       --add-modules javafx.controls,javafx.fxml ^
       -cp out\production\DiagramsAsCode ^
       com.diagramas.MainFX

### Comando solo consola (sin JavaFX)

  java -cp out\production\DiagramsAsCode com.diagramas.Main <ruta>.dac

### Ejemplo

  java -cp out\production\DiagramsAsCode com.diagramas.Main test_flujo_completo.dac

### Reglas de uso CLI

  - El archivo debe tener extension .dac (obligatorio).
  - Si no se pasa argumento, el programa muestra instrucciones y termina.
  - Los errores se imprimen en System.err.
  - La tabla de simbologia estatica se imprime al inicio de cada ejecucion.

---

## 5. Estructura del Lenguaje DAC

### 5.1 Cabecera Obligatoria

Todo archivo .dac DEBE comenzar con la declaracion de tipo de diagrama.

  Sintaxis:
    diagrama <TipoDiagrama>;

  Valores validos para TipoDiagrama:
    Flujo  -->  Activa el modulo de diagramas de flujo
    BD     -->  Activa el modulo de base de datos
    Redes  -->  Activa el modulo de topologia de red

  Ejemplos validos:
    diagrama Flujo;
    diagrama BD;
    diagrama Redes;

  Ejemplos invalidos:
    inicio MiNodo;          -- Sin cabecera
    diagrama Flujo          -- Sin punto y coma
    diagrama Otro;          -- Modulo no reconocido

### 5.2 Meta-Instrucciones (Opcionales, antes de la cabecera)

Las meta-instrucciones pueden colocarse ANTES de la linea diagrama <Tipo>;

  autor "Nombre del autor";
  version "1.0";
  tema "oscuro";
  exportar "ruta/salida.png";
  importar "archivo_externo.dac";

### 5.3 Identificadores

Secuencia de letras, digitos y guiones bajos que comienza con letra o guion bajo.

  Validos:   Usuarios   nodo1   Mi_Proceso   LeerDatos
  Invalidos: 1nodo      mi-proceso   @tabla

### 5.4 Literales de Texto

Cadenas entre comillas dobles. Se usan para descripciones de nodos.

  "Leer credenciales del sistema"
  "Verificar base de datos"

  Regla: La cadena debe cerrarse en la misma linea.
         Una cadena sin cerrar genera un error lexico.

### 5.5 Signos de Puntuacion

  Caracter | Token         | Uso
  ---------|---------------|----------------------------------------
  ;        | PUNTO_Y_COMA  | Cierra toda instruccion
  :        | DOS_PUNTOS    | Separa nombre de tipo en atributos BD/Redes
  {        | LLAVE_IZQ     | Abre bloque de atributos
  }        | LLAVE_DER     | Cierra bloque de atributos

### 5.6 Comentarios

El Lexer absorbe completamente las lineas de comentario sin generar tokens.
Se soportan dos estilos:

  // Comentario estilo Java (ignorado por el compilador)
  #  Comentario estilo Script (ignorado por el compilador)

  Ejemplo:
    diagrama Flujo;
    // Este es un comentario de una linea
    # Este tambien es un comentario
    inicio Arrancar;  // Comentario al final de instruccion

---

## 6. Meta-Instrucciones Globales

Las meta-instrucciones son instrucciones del nucleo que se procesan ANTES
de delegar al modulo. Se colocan al inicio del archivo, antes de diagrama <Tipo>;

  Instruccion | Valor        | Descripcion
  ------------|--------------|----------------------------------------
  autor       | TEXTO_LITERAL | Nombre del autor del diagrama
  version     | TEXTO_LITERAL | Version del archivo
  tema        | TEXTO_LITERAL | Tema visual (oscuro, claro, etc.)
  exportar    | TEXTO_LITERAL | Ruta del archivo de exportacion
  importar    | TEXTO_LITERAL | Ruta de un archivo externo a importar

  Sintaxis:
    <instruccion> "valor";

  Ejemplo completo:
    autor "Juan Perez";
    version "2.0";
    tema "oscuro";
    exportar "diagrama_login.png";
    diagrama Flujo;
    inicio Arrancar;
    ...

---

## 7. Modulo: Diagramas de Flujo

Activado con: diagrama Flujo;

Permite modelar procesos secuenciales con nodos tipados y conexiones dirigidas.

### 7.1 Instrucciones de Nodo

  Instruccion | Sintaxis                             | Descripcion
  ------------|--------------------------------------|-----------------------------
  inicio      | inicio <ID>;                         | Punto de entrada del flujo
  fin         | fin <ID>;                            | Punto de terminacion
  nodo        | nodo <ID> "descripcion";             | Proceso o accion generica
  condicion   | condicion <ID> "pregunta?";          | Decision o bifurcacion
  bucle       | bucle <ID> "condicion de ciclo";     | Estructura de repeticion
  subproceso  | subproceso <ID> "nombre del sub";    | Proceso referenciado externo
  entrada     | entrada <ID> "solicitar datos";      | Lectura de datos del usuario
  salida      | salida <ID> "mostrar resultado";     | Escritura o presentacion
  parada      | parada <ID> "motivo de detencion";   | Interrupcion del proceso

  Todas las instrucciones anteriores registran el identificador en la tabla de simbolos.
  El identificador debe ser unico en todo el archivo.

### 7.2 Conexiones entre Nodos

  Sintaxis:
    <IDOrigen> conecta <IDDestino>;

  Ejemplos:
    Arrancar conecta LeerDatos;
    Validar conecta Procesar;
    Validar conecta Bloquear;

  Reglas semanticas:
    - Origen y destino DEBEN estar declarados previamente.
    - Una referencia a un ID inexistente genera error semantico.
    - El verbo exacto es "conecta" (minusculas). Otro verbo genera error.
    - Las conexiones NO registran nuevos identificadores en la tabla.

### 7.3 Ejemplo Completo (Flujo)

  autor "Ingeniero Maestro";
  version "2.0";
  diagrama Flujo;

  inicio Arrancar;
  entrada LeerDatos "Solicitar usuario y password";
  condicion Validar "Usuario en base de datos?";
  nodo Procesar "Generar token de sesion";
  salida MostrarExito "Bienvenido al sistema";
  parada Bloquear "Demasiados intentos fallidos";
  fin Terminar;

  Arrancar conecta LeerDatos;
  LeerDatos conecta Validar;
  Validar conecta Procesar;
  Validar conecta Bloquear;
  Procesar conecta MostrarExito;
  MostrarExito conecta Terminar;

### 7.4 Gramatica del Modulo Flujo

  programa_flujo    ::= cabecera instruccion_flujo*
  cabecera          ::= "diagrama" "Flujo" ";"
  instruccion_flujo ::= decl_nodo_simple | decl_nodo_texto | conexion
  decl_nodo_simple  ::= ("inicio" | "fin") IDENTIFICADOR ";"
  decl_nodo_texto   ::= ("nodo" | "condicion" | "bucle" | "subproceso" |
                          "entrada" | "salida" | "parada") IDENTIFICADOR TEXTO_LITERAL ";"
  conexion          ::= IDENTIFICADOR "conecta" IDENTIFICADOR ";"

---

## 8. Modulo: Diagramas de Base de Datos

Activado con: diagrama BD;

Permite modelar esquemas de base de datos con tablas, vistas, esquemas y relaciones.

### 8.1 Componentes con Bloque de Atributos

Las siguientes instrucciones abren un bloque { } con atributos internos:

  Instruccion | Sintaxis
  ------------|----------------------------------------------
  tabla       | tabla <Nombre> { atributos }
  vista       | vista <Nombre> { atributos }
  esquema     | esquema <Nombre> { atributos }
  paquete     | paquete <Nombre> { atributos }

  Ejemplo:
    tabla Clientes {
        id_cliente: INT PK;
        nombre: VARCHAR;
        credito: DECIMAL;
    }

    vista ClientesVIP {
        id_cliente: INT;
        nivel: VARCHAR;
    }

### 8.2 Definicion de Atributos

  Sintaxis:
    <nombre>: <TipoDato>;
    <nombre>: <TipoDato> <Modificador>;

  Tipos de dato comunes (cualquier identificador es valido):
    INT      FLOAT     VARCHAR    TEXT
    DECIMAL  BOOLEAN   DATE       TIMESTAMP

  Modificadores opcionales:
    PK  -- Llave primaria (Primary Key)
    FK  -- Llave foranea (Foreign Key)

  Ejemplos:
    id: INT PK;
    usuario_id: INT FK;
    nombre: VARCHAR;
    saldo: DECIMAL;

### 8.3 Componentes Lineales (Sin Bloque)

Las siguientes instrucciones no llevan bloque de atributos:

  Instruccion    | Sintaxis                    | Descripcion
  ---------------|-----------------------------|--------------------------
  procedimiento  | procedimiento <Nombre>;     | Procedimiento almacenado
  indice         | indice <Nombre>;            | Indice de optimizacion
  disparador     | disparador <Nombre>;        | Trigger de base de datos
  secuencia      | secuencia <Nombre>;         | Generador de secuencias
  funcion        | funcion <Nombre>;           | Funcion de usuario

  Ejemplo:
    procedimiento ActualizarCredito;
    secuencia SeqClientes;
    indice IdxNombre;

### 8.4 Relaciones entre Entidades

  Sintaxis:
    <EntidadOrigen> relaciona <EntidadDestino>;

  Ejemplo:
    Ventas relaciona Clientes;
    Clientes relaciona ClientesVIP;

  Reglas semanticas:
    - Ambas entidades deben estar declaradas previamente.
    - El verbo exacto es "relaciona". Otro verbo genera error.
    - Las relaciones NO registran nuevos identificadores.

### 8.5 Ejemplo Completo (BD)

  autor "DBA Principal";
  exportar "esquema_db.png";
  diagrama BD;

  esquema Ventas {
      region: VARCHAR;
  }

  tabla Clientes {
      id_cliente: INT PK;
      nombre: VARCHAR;
      credito: DECIMAL;
  }

  vista ClientesVIP {
      id_cliente: INT;
      nivel: VARCHAR;
  }

  procedimiento ActualizarCredito;
  secuencia SeqClientes;

  Ventas relaciona Clientes;
  Clientes relaciona ClientesVIP;

### 8.6 Gramatica del Modulo BD

  programa_bd         ::= cabecera instruccion_bd*
  cabecera            ::= "diagrama" "BD" ";"
  instruccion_bd      ::= bloque_complejo | componente_lineal | relacion
  bloque_complejo     ::= ("tabla"|"vista"|"esquema"|"paquete") IDENTIFICADOR "{" atributo* "}"
  atributo            ::= IDENTIFICADOR ":" IDENTIFICADOR [IDENTIFICADOR] ";"
  componente_lineal   ::= ("procedimiento"|"indice"|"disparador"|"secuencia"|"funcion") IDENTIFICADOR ";"
  relacion            ::= IDENTIFICADOR "relaciona" IDENTIFICADOR ";"

---

## 9. Modulo: Diagramas de Redes

Activado con: diagrama Redes;

Permite modelar topologias de red con dispositivos tipados y enlaces entre ellos.

### 9.1 Declaracion de Componentes de Red

  Instruccion | Sintaxis                              | Descripcion
  ------------|---------------------------------------|-----------------------------
  dispositivo | dispositivo <Nombre> <Tipo> { ... };  | Dispositivo de red generico
  nube        | nube <Nombre> <Tipo> { ... };         | Infraestructura en la nube
  vlan        | vlan <Nombre> <Tipo> { ... };         | Red de area local virtual
  subred      | subred <Nombre> <Tipo> { ... };       | Segmento de red (subnet)
  cluster     | cluster <Nombre> <Tipo> { ... };      | Agrupacion de servidores
  tunel       | tunel <Nombre> <Tipo> { ... };        | Tunel cifrado (VPN/GRE)
  zona        | zona <Nombre> <Tipo> { ... };         | Zona de seguridad
  puerto      | puerto <Nombre> <Tipo> { ... };       | Puerto de red
  politica    | politica <Nombre> <Tipo> { ... };     | Regla de acceso o firewall

  El bloque de propiedades { ... } es OPCIONAL.
  El punto y coma ; al final es OBLIGATORIO.

### 9.2 Propiedades del Bloque (Opcionales)

El bloque puede contener pares clave: "valor"; para configurar el componente.

  Sintaxis:
    dispositivo <Nombre> <Tipo> { <Clave>: "valor"; };

  Ejemplos:
    dispositivo AWS Nube { Region: "us-east-1"; };
    dispositivo DMZ Zona { Seguridad: "Alta"; };
    dispositivo Balanceador LoadBalancer { IP: "10.0.0.5"; };
    dispositivo WebServers Cluster { Nodos: "3"; };

### 9.3 Declaracion de Tipo de Dispositivo

Cada componente de red lleva un IDENTIFICADOR de tipo que clasifica el dispositivo:

  Ejemplos de tipos:  Router   Switch   Firewall   LoadBalancer
                      Nube     Cluster  Zona       Servidor

  El tipo es un identificador libre -- el Lexer lo trata como IDENTIFICADOR ordinario.

### 9.4 Enlace entre Dispositivos

  Sintaxis:
    <IDOrigen> enlaza <IDDestino>;

  Ejemplo:
    AWS enlaza DMZ;
    DMZ enlaza Balanceador;
    Balanceador enlaza WebServers;

  Reglas semanticas:
    - Ambos dispositivos deben estar declarados previamente.
    - El verbo exacto es "enlaza". Otro verbo genera error.
    - Los enlaces NO registran nuevos identificadores.

### 9.5 Ejemplo Completo (Redes)

  diagrama Redes;

  dispositivo AWS Nube { Region: "us-east-1"; };
  dispositivo DMZ Zona { Seguridad: "Alta"; };
  dispositivo WebServers Cluster { Nodos: "3"; };
  dispositivo Balanceador LoadBalancer { IP: "10.0.0.5"; };

  AWS enlaza DMZ;
  DMZ enlaza Balanceador;
  Balanceador enlaza WebServers;

### 9.6 Gramatica del Modulo Redes

  programa_redes      ::= cabecera instruccion_red*
  cabecera            ::= "diagrama" "Redes" ";"
  instruccion_red     ::= componente_red | enlace
  componente_red      ::= TIPO_COMPONENTE IDENTIFICADOR IDENTIFICADOR ["{" propiedades* "}"] ";"
  TIPO_COMPONENTE     ::= "dispositivo" | "nube" | "vlan" | "subred" | "cluster" |
                          "tunel" | "zona" | "puerto" | "politica"
  propiedades         ::= IDENTIFICADOR ":" TEXTO_LITERAL ";"
  enlace              ::= IDENTIFICADOR "enlaza" IDENTIFICADOR ";"

---

## 10. Sistema de Errores y Diagnosticos

El ManejadorErrores acumula todos los errores. Cada error sigue el formato:

  ==================================================
  ERROR DE COMPILACION [Linea N] [Contexto: <fase>]
  Detalle: <descripcion del problema>
  Sugerencia: <indicacion de correccion>
  ==================================================

### 10.1 Contextos de Error

  Contexto            | Fase       | Que detecta
  --------------------|------------|------------------------------------------
  Analisis Lexico     | Lexica     | Caracteres no permitidos en el alfabeto
  Lexico              | Lexica     | Cadenas de texto sin cerrar
  Sintactico Nucleo   | Sintactica | Cabecera ausente o malformada
  Sintactico Flujo    | Sintactica | Estructura incorrecta en modulo Flujo
  Semantico Flujo     | Semantica  | IDs duplicados o referencias no declaradas (Flujo)
  Sintactico BD       | Sintactica | Estructura incorrecta en modulo BD
  Semantico BD        | Semantica  | IDs duplicados o referencias no declaradas (BD)
  Sintactico Redes    | Sintactica | Estructura incorrecta en modulo Redes
  Semantico Redes     | Semantica  | IDs duplicados o referencias no declaradas (Redes)
  Semantico           | Semantica  | Modulo no reconocido en la cabecera

### 10.2 Comportamiento de Detencion

  - Errores lexicos  --> el pipeline se detiene ANTES del analisis sintactico.
  - Errores sintacticos/semanticos --> se acumulan y reportan al final.
  - El parser usa recuperacion de panico para seguir reportando tras un error.

### 10.3 Errores Comunes

  Causa                 | Ejemplo              | Contexto
  ----------------------|----------------------|----------------------
  Caracter invalido     | @tabla  !nodo        | Analisis Lexico
  Cadena sin cerrar     | "texto sin cerrar    | Lexico
  Sin cabecera          | (archivo vacio)      | Sintactico Nucleo
  Tipo invalido         | diagrama Otro;       | Semantico
  Verbo incorrecto      | A relaciona B (en Flujo) | Sintactico Flujo
  ID no declarado       | A conecta B (B sin declarar) | Semantico Flujo
  ID duplicado          | dos nodos con mismo nombre | Semantico <Modulo>
  Falta punto y coma    | nodo Proceso "desc"  | Sintactico <Modulo>

---

## 11. Tabla de Simbolos

La TablaSimbolos es el componente de memoria del compilador. Se reinicia en cada compilacion.

### Comportamiento

  - Registra cada identificador con su tipo de rol (inicio, nodo, tabla, dispositivo_Router, etc.)
  - Valida unicidad: si el ID ya fue registrado, emite error semantico.
  - Mantiene el contexto activo (el modulo en proceso).
  - Las conexiones, relaciones y enlaces NO registran nuevos simbolos.

### Roles registrados por modulo

  Modulo  | Instruccion   | Rol en tabla
  --------|---------------|-----------------------------
  Flujo   | inicio        | inicio
  Flujo   | fin           | fin
  Flujo   | nodo          | nodo
  Flujo   | condicion     | condicion
  Flujo   | bucle         | bucle
  Flujo   | subproceso    | subproceso
  Flujo   | entrada       | entrada
  Flujo   | salida        | salida
  Flujo   | parada        | parada
  BD      | tabla         | tabla
  BD      | vista         | vista
  BD      | esquema       | esquema
  BD      | paquete       | paquete
  BD      | procedimiento | procedimiento
  BD      | indice        | indice
  BD      | disparador    | disparador
  BD      | secuencia     | secuencia
  BD      | funcion       | funcion
  Redes   | dispositivo   | dispositivo_<Tipo>
  Redes   | nube          | dispositivo_<Tipo>
  Redes   | vlan          | dispositivo_<Tipo>
  Redes   | subred        | dispositivo_<Tipo>
  Redes   | cluster       | dispositivo_<Tipo>
  Redes   | tunel         | dispositivo_<Tipo>
  Redes   | zona          | dispositivo_<Tipo>
  Redes   | puerto        | dispositivo_<Tipo>
  Redes   | politica      | dispositivo_<Tipo>

### Visualizacion en el IDE

En el panel derecho del IDE se muestra:

  Contexto Bloqueado: Flujo

  IDENTIFICADOR ENCONTRADO  | TIPO/ROL ASIGNADO
  -------------------------------------------------
  Arrancar                  | inicio
  LeerDatos                 | entrada
  Validar                   | condicion
  Procesar                  | nodo

### Tabla de Simbologia Estatica

Accesible desde el boton "Simbologia del Lenguaje" (verde oscuro, extremo derecho).
Muestra los 42 simbolos del lenguaje organizados por categoria con filtrado por modulo.

---

## 12. Referencia Rapida de Sintaxis

### Modulo Flujo

  autor "nombre";           -- Opcional
  version "1.0";            -- Opcional
  diagrama Flujo;           -- Obligatorio

  inicio <ID>;
  fin <ID>;
  nodo <ID> "descripcion";
  condicion <ID> "pregunta";
  bucle <ID> "condicion";
  subproceso <ID> "nombre";
  entrada <ID> "solicitud";
  salida <ID> "resultado";
  parada <ID> "motivo";

  <IDOrigen> conecta <IDDestino>;

### Modulo BD

  diagrama BD;

  tabla <Nombre> {
      <campo>: <Tipo>;
      <campo>: <Tipo> PK;
      <campo>: <Tipo> FK;
  }
  vista <Nombre> { <campo>: <Tipo>; }
  esquema <Nombre> { <campo>: <Tipo>; }
  paquete <Nombre> { <campo>: <Tipo>; }

  procedimiento <Nombre>;
  indice <Nombre>;
  disparador <Nombre>;
  secuencia <Nombre>;
  funcion <Nombre>;

  <EntidadA> relaciona <EntidadB>;

### Modulo Redes

  diagrama Redes;

  dispositivo <Nombre> <Tipo> { <Clave>: "valor"; };
  nube <Nombre> <Tipo>;
  vlan <Nombre> <Tipo>;
  subred <Nombre> <Tipo>;
  cluster <Nombre> <Tipo>;
  tunel <Nombre> <Tipo>;
  zona <Nombre> <Tipo>;
  puerto <Nombre> <Tipo>;
  politica <Nombre> <Tipo>;

  <DispositivoA> enlaza <DispositivoB>;

### Palabras reservadas completas del lenguaje

  Palabra        | Tipo Token    | Modulo
  ---------------|---------------|--------------------
  diagrama       | PR_DIAGRAMA   | Global (cabecera)
  autor          | IDENTIFICADOR | Meta-instruccion
  version        | IDENTIFICADOR | Meta-instruccion
  tema           | IDENTIFICADOR | Meta-instruccion
  exportar       | IDENTIFICADOR | Meta-instruccion
  importar       | IDENTIFICADOR | Meta-instruccion
  inicio         | IDENTIFICADOR | Flujo
  fin            | IDENTIFICADOR | Flujo
  nodo           | IDENTIFICADOR | Flujo
  condicion      | IDENTIFICADOR | Flujo
  bucle          | IDENTIFICADOR | Flujo
  subproceso     | IDENTIFICADOR | Flujo
  entrada        | IDENTIFICADOR | Flujo
  salida         | IDENTIFICADOR | Flujo
  parada         | IDENTIFICADOR | Flujo
  conecta        | IDENTIFICADOR | Flujo (verbo)
  tabla          | IDENTIFICADOR | BD
  vista          | IDENTIFICADOR | BD
  esquema        | IDENTIFICADOR | BD
  paquete        | IDENTIFICADOR | BD
  procedimiento  | IDENTIFICADOR | BD
  indice         | IDENTIFICADOR | BD
  disparador     | IDENTIFICADOR | BD
  secuencia      | IDENTIFICADOR | BD
  funcion        | IDENTIFICADOR | BD
  relaciona      | IDENTIFICADOR | BD (verbo)
  dispositivo    | IDENTIFICADOR | Redes
  nube           | IDENTIFICADOR | Redes
  vlan           | IDENTIFICADOR | Redes
  subred         | IDENTIFICADOR | Redes
  cluster        | IDENTIFICADOR | Redes
  tunel          | IDENTIFICADOR | Redes
  zona           | IDENTIFICADOR | Redes
  puerto         | IDENTIFICADOR | Redes
  politica       | IDENTIFICADOR | Redes
  enlaza         | IDENTIFICADOR | Redes (verbo)

---

## 13. Ejemplos Completos

### Ejemplo 1: Diagrama de Flujo (Login)

  autor "Ingeniero Maestro";
  version "2.0";
  tema "oscuro";
  diagrama Flujo;

  inicio Arrancar;
  entrada LeerDatos "Solicitar usuario y password";
  condicion Validar "Usuario en base de datos?";
  nodo Procesar "Generar token de sesion";
  salida MostrarExito "Bienvenido al sistema";
  parada Bloquear "Demasiados intentos fallidos";
  fin Terminar;

  // Logica de ruteo
  Arrancar conecta LeerDatos;
  LeerDatos conecta Validar;
  Validar conecta Procesar;
  Validar conecta Bloquear;
  Procesar conecta MostrarExito;
  MostrarExito conecta Terminar;

---

### Ejemplo 2: Diagrama de Base de Datos (Ventas)

  autor "DBA Principal";
  exportar "esquema_db.png";
  diagrama BD;

  esquema Ventas {
      region: VARCHAR;
  }

  tabla Clientes {
      id_cliente: INT PK;
      nombre: VARCHAR;
      credito: DECIMAL;
  }

  vista ClientesVIP {
      id_cliente: INT;
      nivel: VARCHAR;
  }

  procedimiento ActualizarCredito;
  secuencia SeqClientes;

  Ventas relaciona Clientes;
  Clientes relaciona ClientesVIP;

---

### Ejemplo 3: Diagrama de Redes (Infraestructura en Nube)

  diagrama Redes;

  dispositivo AWS Nube { Region: "us-east-1"; };
  dispositivo DMZ Zona { Seguridad: "Alta"; };
  dispositivo WebServers Cluster { Nodos: "3"; };
  dispositivo Balanceador LoadBalancer { IP: "10.0.0.5"; };

  AWS enlaza DMZ;
  DMZ enlaza Balanceador;
  Balanceador enlaza WebServers;

---

### Ejemplo 4: Error por verbo incorrecto (Flujo)

  diagrama Flujo;
  inicio A;
  nodo B "proceso";
  A relaciona B;   // ERROR: verbo 'relaciona' no existe en Flujo

  Error generado:
    ERROR DE COMPILACION [Linea 4] [Contexto: Sintactico Flujo]
    Detalle: Instruccion o verbo invalido en 'A'.
    Sugerencia: Si deseas conectar elementos utiliza el verbo exclusivo 'conecta'.

---

### Ejemplo 5: Error por identificador no declarado (BD)

  diagrama BD;
  tabla Pedidos {
      id: INT PK;
  }
  Pedidos relaciona Proveedores;   // ERROR: Proveedores no fue declarado

  Error generado:
    ERROR DE COMPILACION [Linea 5] [Contexto: Semantico BD]
    Detalle: La entidad destino 'Proveedores' no existe.
    Sugerencia: Declarala primero.

---

*Manual de Usuario -- Diagrams As Code v2.0*
*Compilador pedagogico de analisis lexico, sintactico y semantico.*
