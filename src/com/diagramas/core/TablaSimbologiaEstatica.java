package com.diagramas.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TablaSimbologiaEstatica {

    public static class EntradaSimbolo {
        public final String lexema;
        public final String tipoToken;
        public final String categoria;
        public final String descripcion;

        public EntradaSimbolo(String lexema, String tipoToken, String categoria, String descripcion) {
            this.lexema = lexema;
            this.tipoToken = tipoToken;
            this.categoria = categoria;
            this.descripcion = descripcion;
        }
    }

    public static final List<EntradaSimbolo> TABLA;

    static {
        List<EntradaSimbolo> t = new ArrayList<>();

        // --- CABECERA GLOBAL ---
        t.add(new EntradaSimbolo("diagrama",      "PR_DIAGRAMA",   "Cabecera",             "Declara el tipo de diagrama activo"));

        // --- META-INSTRUCCIONES GLOBALES ---
        t.add(new EntradaSimbolo("autor",         "IDENTIFICADOR", "Meta-Instruccion",     "Nombre del autor del diagrama"));
        t.add(new EntradaSimbolo("version",       "IDENTIFICADOR", "Meta-Instruccion",     "Version del archivo .dac"));
        t.add(new EntradaSimbolo("tema",          "IDENTIFICADOR", "Meta-Instruccion",     "Tema visual aplicado al diagrama"));
        t.add(new EntradaSimbolo("exportar",      "IDENTIFICADOR", "Meta-Instruccion",     "Ruta de exportacion de la imagen generada"));
        t.add(new EntradaSimbolo("importar",      "IDENTIFICADOR", "Meta-Instruccion",     "Importa una definicion externa"));

        // --- MODULO FLUJO: Nodos ---
        t.add(new EntradaSimbolo("inicio",        "IDENTIFICADOR", "Flujo - Nodo",         "Punto de arranque del diagrama de flujo"));
        t.add(new EntradaSimbolo("fin",           "IDENTIFICADOR", "Flujo - Nodo",         "Punto de terminacion del diagrama de flujo"));
        t.add(new EntradaSimbolo("nodo",          "IDENTIFICADOR", "Flujo - Nodo",         "Proceso o accion generica"));
        t.add(new EntradaSimbolo("condicion",     "IDENTIFICADOR", "Flujo - Nodo",         "Decision o bifurcacion logica"));
        t.add(new EntradaSimbolo("bucle",         "IDENTIFICADOR", "Flujo - Nodo",         "Estructura de repeticion o iteracion"));
        t.add(new EntradaSimbolo("subproceso",    "IDENTIFICADOR", "Flujo - Nodo",         "Proceso referenciado o subrutina"));
        t.add(new EntradaSimbolo("entrada",       "IDENTIFICADOR", "Flujo - Nodo",         "Lectura o solicitud de datos al usuario"));
        t.add(new EntradaSimbolo("salida",        "IDENTIFICADOR", "Flujo - Nodo",         "Escritura o presentacion de datos"));
        t.add(new EntradaSimbolo("parada",        "IDENTIFICADOR", "Flujo - Nodo",         "Interrupcion o detencion del proceso"));

        // --- MODULO FLUJO: Verbo ---
        t.add(new EntradaSimbolo("conecta",       "IDENTIFICADOR", "Flujo - Verbo",        "Une dos nodos con una flecha direccional"));

        // --- MODULO BD: Bloques con atributos ---
        t.add(new EntradaSimbolo("tabla",         "IDENTIFICADOR", "BD - Bloque",          "Define una tabla relacional con columnas"));
        t.add(new EntradaSimbolo("vista",         "IDENTIFICADOR", "BD - Bloque",          "Define una vista de base de datos"));
        t.add(new EntradaSimbolo("esquema",       "IDENTIFICADOR", "BD - Bloque",          "Agrupa objetos bajo un espacio de nombres"));
        t.add(new EntradaSimbolo("paquete",       "IDENTIFICADOR", "BD - Bloque",          "Contenedor logico de objetos de BD"));

        // --- MODULO BD: Componentes lineales ---
        t.add(new EntradaSimbolo("procedimiento", "IDENTIFICADOR", "BD - Lineal",          "Procedimiento almacenado"));
        t.add(new EntradaSimbolo("indice",        "IDENTIFICADOR", "BD - Lineal",          "Indice de optimizacion de consultas"));
        t.add(new EntradaSimbolo("disparador",    "IDENTIFICADOR", "BD - Lineal",          "Trigger o disparador de base de datos"));
        t.add(new EntradaSimbolo("secuencia",     "IDENTIFICADOR", "BD - Lineal",          "Generador de valores secuenciales"));
        t.add(new EntradaSimbolo("funcion",       "IDENTIFICADOR", "BD - Lineal",          "Funcion definida por el usuario en BD"));

        // --- MODULO BD: Verbo ---
        t.add(new EntradaSimbolo("relaciona",     "IDENTIFICADOR", "BD - Verbo",           "Establece una relacion entre dos entidades BD"));

        // --- MODULO REDES: Componentes ---
        t.add(new EntradaSimbolo("dispositivo",   "IDENTIFICADOR", "Redes - Componente",   "Dispositivo de red con tipo y configuracion"));
        t.add(new EntradaSimbolo("nube",          "IDENTIFICADOR", "Redes - Componente",   "Servicio o infraestructura en la nube"));
        t.add(new EntradaSimbolo("vlan",          "IDENTIFICADOR", "Redes - Componente",   "Red de area local virtual (VLAN)"));
        t.add(new EntradaSimbolo("subred",        "IDENTIFICADOR", "Redes - Componente",   "Segmento de red (subnet/CIDR)"));
        t.add(new EntradaSimbolo("cluster",       "IDENTIFICADOR", "Redes - Componente",   "Agrupacion de servidores o nodos"));
        t.add(new EntradaSimbolo("tunel",         "IDENTIFICADOR", "Redes - Componente",   "Tunel de red cifrado (VPN/GRE/IPSec)"));
        t.add(new EntradaSimbolo("zona",          "IDENTIFICADOR", "Redes - Componente",   "Zona de seguridad o segmento logico"));
        t.add(new EntradaSimbolo("puerto",        "IDENTIFICADOR", "Redes - Componente",   "Puerto fisico o logico de red"));
        t.add(new EntradaSimbolo("politica",      "IDENTIFICADOR", "Redes - Componente",   "Politica de acceso o regla de firewall"));

        // --- MODULO REDES: Verbo ---
        t.add(new EntradaSimbolo("enlaza",        "IDENTIFICADOR", "Redes - Verbo",        "Conecta dos dispositivos de red"));

        // --- MODULO CONCEPTUAL: Nodos ---
        t.add(new EntradaSimbolo("concepto",      "IDENTIFICADOR", "Conceptual - Nodo",    "Define un concepto principal del mapa"));
        t.add(new EntradaSimbolo("categoria",     "IDENTIFICADOR", "Conceptual - Nodo",    "Define una categoria o grupo de conceptos"));
        t.add(new EntradaSimbolo("propiedad",     "IDENTIFICADOR", "Conceptual - Nodo",    "Define una propiedad o atributo de un concepto"));

        // --- MODULO CONCEPTUAL: Verbos ---
        t.add(new EntradaSimbolo("agrupa",        "IDENTIFICADOR", "Conceptual - Verbo",   "Agrupa un concepto dentro de una categoria"));
        t.add(new EntradaSimbolo("asocia",        "IDENTIFICADOR", "Conceptual - Verbo",   "Establece asociacion entre dos conceptos"));
        t.add(new EntradaSimbolo("depende",       "IDENTIFICADOR", "Conceptual - Verbo",   "Indica dependencia entre dos conceptos"));

        // --- MODULO UML: Bloques ---
        t.add(new EntradaSimbolo("clase",         "IDENTIFICADOR", "UML - Bloque",         "Define una clase UML con atributos y metodos"));
        t.add(new EntradaSimbolo("interfaz",      "IDENTIFICADOR", "UML - Lineal",         "Define una interfaz UML"));
        t.add(new EntradaSimbolo("enum",          "IDENTIFICADOR", "UML - Lineal",         "Define una enumeracion UML"));

        // --- MODULO UML: Miembros de clase ---
        t.add(new EntradaSimbolo("atributo",      "IDENTIFICADOR", "UML - Miembro",        "Define un atributo dentro de una clase UML"));
        t.add(new EntradaSimbolo("metodo",        "IDENTIFICADOR", "UML - Miembro",        "Define un metodo dentro de una clase UML"));

        // --- MODULO UML: Verbos de relacion ---
        t.add(new EntradaSimbolo("extiende",      "IDENTIFICADOR", "UML - Verbo",          "Herencia: una clase extiende a otra"));
        t.add(new EntradaSimbolo("implementa",    "IDENTIFICADOR", "UML - Verbo",          "Implementacion: una clase implementa una interfaz"));
        t.add(new EntradaSimbolo("usa",           "IDENTIFICADOR", "UML - Verbo",          "Dependencia: una clase usa a otra"));

        // --- PUNTUACION ---
        t.add(new EntradaSimbolo(";",             "PUNTO_Y_COMA",  "Puntuacion",           "Cierra y termina una instruccion"));
        t.add(new EntradaSimbolo(":",             "DOS_PUNTOS",    "Puntuacion",           "Separa el nombre del atributo de su tipo"));
        t.add(new EntradaSimbolo("{",             "LLAVE_IZQ",     "Puntuacion",           "Abre un bloque de atributos o propiedades"));
        t.add(new EntradaSimbolo("}",             "LLAVE_DER",     "Puntuacion",           "Cierra un bloque de atributos o propiedades"));

        // --- LITERALES ---
        t.add(new EntradaSimbolo("\"...\"",       "TEXTO_LITERAL", "Literal",              "Cadena de texto delimitada por comillas dobles"));

        // --- COMENTARIOS (absorbidos por el Lexer, no generan token) ---
        t.add(new EntradaSimbolo("//",            "-",             "Comentario",           "Comentario de linea estilo Java (ignorado)"));
        t.add(new EntradaSimbolo("#",             "-",             "Comentario",           "Comentario de linea estilo Script (ignorado)"));

        TABLA = Collections.unmodifiableList(t);
    }

    /**
     * Filtra la tabla por modulo de diagrama.
     * Opciones: "Todos", "Flujo", "BD", "Redes", "Conceptual", "UML", "Global", "Puntuacion"
     */
    public static List<EntradaSimbolo> filtrar(String modulo) {
        if (modulo == null || modulo.equals("Todos")) return new ArrayList<>(TABLA);

        return TABLA.stream().filter(e -> {
            boolean esGlobal = e.categoria.equals("Cabecera") || e.categoria.equals("Meta-Instruccion");
            boolean esPunct  = e.categoria.equals("Puntuacion") || e.categoria.equals("Literal") || e.categoria.equals("Comentario");

            switch (modulo) {
                case "Flujo":       return esGlobal || e.categoria.startsWith("Flujo")      || esPunct;
                case "BD":          return esGlobal || e.categoria.startsWith("BD")         || esPunct;
                case "Redes":       return esGlobal || e.categoria.startsWith("Redes")      || esPunct;
                case "Conceptual":  return esGlobal || e.categoria.startsWith("Conceptual") || esPunct;
                case "UML":         return esGlobal || e.categoria.startsWith("UML")        || esPunct;
                case "Global":      return esGlobal;
                case "Puntuacion":  return esPunct;
                default:            return true;
            }
        }).collect(Collectors.toList());
    }

    public static void imprimir() {
        String sep    = "+-----------------------+---------------------+------------------------+--------------------------------------------------+";
        String formato = "| %-21s | %-19s | %-22s | %-48s |";

        System.out.println("\n" + sep);
        System.out.println(String.format(formato, "LEXEMA", "TIPO TOKEN", "CATEGORIA", "DESCRIPCION"));
        System.out.println(sep);

        String catActual = "";
        for (EntradaSimbolo s : TABLA) {
            if (!s.categoria.equals(catActual)) {
                catActual = s.categoria;
                System.out.println(sep);
            }
            System.out.println(String.format(formato, s.lexema, s.tipoToken, s.categoria, s.descripcion));
        }

        System.out.println(sep);
        System.out.println("  Total de simbolos registrados: " + TABLA.size());
    }
}
