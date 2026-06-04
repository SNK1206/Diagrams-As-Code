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
        t.add(new EntradaSimbolo("autor",         "PALABRA_RESERVADA", "Meta-Instruccion",     "Nombre del autor del diagrama"));
        t.add(new EntradaSimbolo("version",       "PALABRA_RESERVADA", "Meta-Instruccion",     "Version del archivo .dac"));
        t.add(new EntradaSimbolo("tema",          "PALABRA_RESERVADA", "Meta-Instruccion",     "Tema visual aplicado al diagrama"));

        // --- MODULO FLUJO: Nodos ---
        t.add(new EntradaSimbolo("inicio",        "PALABRA_RESERVADA", "Flujo - Nodo",         "Punto de arranque del diagrama de flujo"));
        t.add(new EntradaSimbolo("fin",           "PALABRA_RESERVADA", "Flujo - Nodo",         "Punto de terminacion del diagrama de flujo"));
        t.add(new EntradaSimbolo("nodo",          "PALABRA_RESERVADA", "Flujo - Nodo",         "Proceso o accion generica"));
        t.add(new EntradaSimbolo("condicion",     "PALABRA_RESERVADA", "Flujo - Nodo",         "Decision o bifurcacion logica"));
        t.add(new EntradaSimbolo("bucle",         "PALABRA_RESERVADA", "Flujo - Nodo",         "Estructura de repeticion o iteracion"));
        t.add(new EntradaSimbolo("subproceso",    "PALABRA_RESERVADA", "Flujo - Nodo",         "Proceso referenciado o subrutina"));
        t.add(new EntradaSimbolo("entrada",       "PALABRA_RESERVADA", "Flujo - Nodo",         "Lectura o solicitud de datos al usuario"));
        t.add(new EntradaSimbolo("salida",        "PALABRA_RESERVADA", "Flujo - Nodo",         "Escritura o presentacion de datos"));
        t.add(new EntradaSimbolo("parada",        "PALABRA_RESERVADA", "Flujo - Nodo",         "Interrupcion o detencion del proceso"));

        // --- MODULO FLUJO: Verbo ---
        t.add(new EntradaSimbolo("conecta",       "PALABRA_RESERVADA", "Flujo - Verbo",        "Une dos nodos con una flecha direccional"));

        // --- MODULO BD: Bloques con atributos ---
        t.add(new EntradaSimbolo("tabla",         "PALABRA_RESERVADA", "BD - Bloque",          "Define una tabla relacional con columnas"));
        t.add(new EntradaSimbolo("vista",         "PALABRA_RESERVADA", "BD - Bloque",          "Define una vista de base de datos"));
        t.add(new EntradaSimbolo("esquema",       "PALABRA_RESERVADA", "BD - Bloque",          "Agrupa objetos bajo un espacio de nombres"));
        t.add(new EntradaSimbolo("paquete",       "PALABRA_RESERVADA", "BD - Bloque",          "Contenedor logico de objetos de BD"));

        // --- MODULO BD: Componentes lineales ---
        t.add(new EntradaSimbolo("procedimiento", "PALABRA_RESERVADA", "BD - Lineal",          "Procedimiento almacenado"));
        t.add(new EntradaSimbolo("indice",        "PALABRA_RESERVADA", "BD - Lineal",          "Indice de optimizacion de consultas"));
        t.add(new EntradaSimbolo("disparador",    "PALABRA_RESERVADA", "BD - Lineal",          "Trigger o disparador de base de datos"));
        t.add(new EntradaSimbolo("secuencia",     "PALABRA_RESERVADA", "BD - Lineal",          "Generador de valores secuenciales"));
        t.add(new EntradaSimbolo("funcion",       "PALABRA_RESERVADA", "BD - Lineal",          "Funcion definida por el usuario en BD"));

        // --- MODULO BD: Verbo ---
        t.add(new EntradaSimbolo("relaciona",     "PALABRA_RESERVADA", "BD - Verbo",           "Establece una relacion entre dos entidades BD"));

        // --- MODULO REDES: Componentes ---
        t.add(new EntradaSimbolo("dispositivo",   "PALABRA_RESERVADA", "Redes - Componente",   "Dispositivo de red con tipo y configuracion"));
        t.add(new EntradaSimbolo("nube",          "PALABRA_RESERVADA", "Redes - Componente",   "Servicio o infraestructura en la nube"));
        t.add(new EntradaSimbolo("vlan",          "PALABRA_RESERVADA", "Redes - Componente",   "Red de area local virtual (VLAN)"));
        t.add(new EntradaSimbolo("subred",        "PALABRA_RESERVADA", "Redes - Componente",   "Segmento de red (subnet/CIDR)"));
        t.add(new EntradaSimbolo("cluster",       "PALABRA_RESERVADA", "Redes - Componente",   "Agrupacion de servidores o nodos"));
        t.add(new EntradaSimbolo("tunel",         "PALABRA_RESERVADA", "Redes - Componente",   "Tunel de red cifrado (VPN/GRE/IPSec)"));
        t.add(new EntradaSimbolo("zona",          "PALABRA_RESERVADA", "Redes - Componente",   "Zona de seguridad o segmento logico"));
        t.add(new EntradaSimbolo("puerto",        "PALABRA_RESERVADA", "Redes - Componente",   "Puerto fisico o logico de red"));
        t.add(new EntradaSimbolo("politica",      "PALABRA_RESERVADA", "Redes - Componente",   "Politica de acceso o regla de firewall"));

        // --- MODULO REDES: Verbo ---
        t.add(new EntradaSimbolo("enlaza",        "PALABRA_RESERVADA", "Redes - Verbo",        "Conecta dos dispositivos de red"));

        // --- MODULO CONCEPTUAL: Nodos ---
        t.add(new EntradaSimbolo("concepto",      "PALABRA_RESERVADA", "Conceptual - Nodo",    "Define un concepto principal del mapa"));
        t.add(new EntradaSimbolo("categoria",     "PALABRA_RESERVADA", "Conceptual - Nodo",    "Define una categoria o grupo de conceptos"));
        t.add(new EntradaSimbolo("propiedad",     "PALABRA_RESERVADA", "Conceptual - Nodo",    "Define una propiedad o atributo de un concepto"));

        // --- MODULO CONCEPTUAL: Verbos ---
        t.add(new EntradaSimbolo("agrupa",        "PALABRA_RESERVADA", "Conceptual - Verbo",   "Agrupa un concepto dentro de una categoria"));
        t.add(new EntradaSimbolo("asocia",        "PALABRA_RESERVADA", "Conceptual - Verbo",   "Establece asociacion entre dos conceptos"));
        t.add(new EntradaSimbolo("depende",       "PALABRA_RESERVADA", "Conceptual - Verbo",   "Indica dependencia entre dos conceptos"));
        t.add(new EntradaSimbolo("abarca",        "PALABRA_RESERVADA", "Conceptual - Verbo",   "Indica que un concepto abarca a otro"));
        t.add(new EntradaSimbolo("incluye",       "PALABRA_RESERVADA", "Conceptual - Verbo",   "Indica que un concepto incluye a otro"));

        // --- MODULO UML: Bloques ---
        t.add(new EntradaSimbolo("clase",         "PALABRA_RESERVADA", "UML - Bloque",         "Define una clase UML con atributos y metodos"));
        t.add(new EntradaSimbolo("interfaz",      "PALABRA_RESERVADA", "UML - Lineal",         "Define una interfaz UML"));
        t.add(new EntradaSimbolo("enum",          "PALABRA_RESERVADA", "UML - Lineal",         "Define una enumeracion UML"));

        // --- MODULO UML: Miembros de clase ---
        t.add(new EntradaSimbolo("atributo",      "PALABRA_RESERVADA", "UML - Miembro",        "Define un atributo dentro de una clase UML"));
        t.add(new EntradaSimbolo("metodo",        "PALABRA_RESERVADA", "UML - Miembro",        "Define un metodo dentro de una clase UML"));

        // --- MODULO UML: Verbos de relacion ---
        t.add(new EntradaSimbolo("extiende",      "PALABRA_RESERVADA", "UML - Verbo",          "Herencia: una clase extiende a otra"));
        t.add(new EntradaSimbolo("implementa",    "PALABRA_RESERVADA", "UML - Verbo",          "Implementacion: una clase implementa una interfaz"));
        t.add(new EntradaSimbolo("usa",           "PALABRA_RESERVADA", "UML - Verbo",          "Dependencia: una clase usa a otra"));

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
     * Los filtros de modulo incluyen siempre los simbolos globales y de puntuacion
     * porque son comunes a todos los diagramas.
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
