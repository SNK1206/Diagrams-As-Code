================================================================
  GRAMÁTICA DEL LENGUAJE — Diagrams As Code (DAC)
================================================================

TIPO DE GRAMÁTICA
-----------------
  Tipo 2 — Gramática Libre de Contexto (GLC / CFG)
  Implementada como LL(1) — Analizador Descendente Recursivo

  LL(1) significa:
    L  →  La entrada se lee de izquierda a derecha
    L  →  Se produce la derivación más a la izquierda
    1  →  Se usa exactamente 1 token de anticipación (lookahead)
          para decidir qué regla de producción aplicar

  El analizador léxico (LexerBase) implementa un
  Autómata Finito Determinista (AFD) — Gramática Regular (Tipo 3).

----------------------------------------------------------------


NOTACIÓN UTILIZADA
------------------
  ::=        Define una regla de producción
  |          Alternativa (OR)
  [ ... ]    Elemento opcional  (0 ó 1 vez)
  { ... }    Repetición         (0 ó más veces)
  'palabra'  Terminal literal (palabra reservada del lenguaje)
  MAYUSCULA  Clase de token producida por el Lexer
  minuscula  Símbolo no terminal (regla de la gramática)

----------------------------------------------------------------


TOKENS RECONOCIDOS POR EL LEXER (AFD)
--------------------------------------

  IDENTIFICADOR  ::=  (letra | '_') { letra | digito | '_' }
  TEXTO_LITERAL  ::=  '"' { caracter } '"'
  PR_DIAGRAMA    ::=  'diagrama'
  PUNTO_Y_COMA   ::=  ';'
  DOS_PUNTOS     ::=  ':'
  LLAVE_IZQ      ::=  '{'
  LLAVE_DER      ::=  '}'
  EOF            ::=  <fin de archivo>

  Nota: Los comentarios (// y #) son absorbidos por el Lexer
        y no generan ningún token.

----------------------------------------------------------------


GRAMÁTICA GLOBAL — CABECERA Y ESTRUCTURA
-----------------------------------------

  programa
      ::= { meta_instruccion } cabecera cuerpo_modulo

  meta_instruccion
      ::= ( 'autor' | 'version' | 'tema'
            | 'exportar' | 'importar' ) TEXTO_LITERAL ';'

  cabecera
      ::= 'diagrama' tipo_diagrama ';'

  tipo_diagrama
      ::= 'Flujo' | 'BD' | 'Redes' | 'Conceptual' | 'UML'

  cuerpo_modulo
      ::= cuerpo_flujo
        | cuerpo_bd
        | cuerpo_redes
        | cuerpo_conceptual
        | cuerpo_uml

----------------------------------------------------------------


MÓDULO FLUJO  (activado con: diagrama Flujo;)
---------------------------------------------

  cuerpo_flujo
      ::= { instruccion_flujo }

  instruccion_flujo
      ::= decl_nodo_simple
        | decl_nodo_texto
        | conexion_flujo

  decl_nodo_simple
      ::= ( 'inicio' | 'fin' ) IDENTIFICADOR ';'

  decl_nodo_texto
      ::= ( 'nodo'      | 'condicion' | 'bucle'
            | 'subproceso' | 'entrada'  | 'salida'
            | 'parada' )
          IDENTIFICADOR TEXTO_LITERAL ';'

  conexion_flujo
      ::= IDENTIFICADOR 'conecta' IDENTIFICADOR ';'

----------------------------------------------------------------


MÓDULO BD  (activado con: diagrama BD;)
----------------------------------------

  cuerpo_bd
      ::= { instruccion_bd }

  instruccion_bd
      ::= bloque_bd
        | componente_lineal_bd
        | relacion_bd

  bloque_bd
      ::= ( 'tabla' | 'vista' | 'esquema' | 'paquete' )
          IDENTIFICADOR '{' { atributo } '}'

  atributo
      ::= IDENTIFICADOR ':' IDENTIFICADOR [ IDENTIFICADOR ] ';'

        Nota: el IDENTIFICADOR opcional es el modificador (PK, FK).

  componente_lineal_bd
      ::= ( 'procedimiento' | 'indice' | 'disparador'
            | 'secuencia'   | 'funcion' )
          IDENTIFICADOR ';'

  relacion_bd
      ::= IDENTIFICADOR 'relaciona' IDENTIFICADOR ';'

----------------------------------------------------------------


MÓDULO REDES  (activado con: diagrama Redes;)
----------------------------------------------

  cuerpo_redes
      ::= { instruccion_redes }

  instruccion_redes
      ::= componente_red
        | enlace_red

  componente_red
      ::= tipo_componente IDENTIFICADOR IDENTIFICADOR
          [ '{' { propiedad_red } '}' ] ';'

  tipo_componente
      ::= 'dispositivo' | 'nube'   | 'vlan'   | 'subred'
        | 'cluster'     | 'tunel'  | 'zona'   | 'puerto'
        | 'politica'

  propiedad_red
      ::= IDENTIFICADOR ':' TEXTO_LITERAL ';'

  enlace_red
      ::= IDENTIFICADOR 'enlaza' IDENTIFICADOR ';'

----------------------------------------------------------------


MÓDULO CONCEPTUAL  (activado con: diagrama Conceptual;)
--------------------------------------------------------

  cuerpo_conceptual
      ::= { instruccion_conceptual }

  instruccion_conceptual
      ::= decl_concepto
        | relacion_conceptual

  decl_concepto
      ::= ( 'concepto' | 'categoria' | 'propiedad' )
          IDENTIFICADOR TEXTO_LITERAL ';'

  relacion_conceptual
      ::= IDENTIFICADOR
          ( 'agrupa' | 'asocia' | 'depende' )
          IDENTIFICADOR ';'

----------------------------------------------------------------


MÓDULO UML  (activado con: diagrama UML;)
------------------------------------------

  cuerpo_uml
      ::= { instruccion_uml }

  instruccion_uml
      ::= decl_clase
        | decl_lineal_uml
        | relacion_uml

  decl_clase
      ::= 'clase' IDENTIFICADOR '{' { miembro_clase } '}'

  miembro_clase
      ::= ( 'atributo' | 'metodo' )
          IDENTIFICADOR ':' IDENTIFICADOR ';'

  decl_lineal_uml
      ::= ( 'interfaz' | 'enum' ) IDENTIFICADOR ';'

  relacion_uml
      ::= IDENTIFICADOR
          ( 'extiende' | 'implementa' | 'usa' )
          IDENTIFICADOR ';'

----------------------------------------------------------------


RESUMEN DE VERBOS DE RELACIÓN POR MÓDULO
-----------------------------------------

  Módulo       Verbo de relación    Descripción
  ----------   -----------------    ----------------------------
  Flujo        conecta              Une dos nodos del flujo
  BD           relaciona            Vincula dos entidades de BD
  Redes        enlaza               Conecta dos dispositivos
  Conceptual   agrupa               Incluye un concepto en una categoría
               asocia               Relaciona dos conceptos
               depende              Indica dependencia entre conceptos
  UML          extiende             Herencia entre clases
               implementa           Clase implementa una interfaz
               usa                  Dependencia entre clases

================================================================
  Diagrams As Code — Gramática Formal del Lenguaje DAC
================================================================
