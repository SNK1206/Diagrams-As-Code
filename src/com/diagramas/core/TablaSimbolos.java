package com.diagramas.core;

import java.util.LinkedHashMap;
import java.util.Map;

public class TablaSimbolos {

    public static class Entrada {
        public final String rol;
        public final int linea;
        public Entrada(String rol, int linea) {
            this.rol   = rol;
            this.linea = linea;
        }
    }

    private final Map<String, Entrada> simbolos = new LinkedHashMap<>();
    private String contextoActivo = "Ninguno";

    public void bloquearContexto(String contexto) {
        this.contextoActivo = contexto;
        System.out.println("[TABLA] Contexto bloqueado para: " + contexto);
    }

    public boolean registrar(String nombre, String rol, int linea) {
        if (simbolos.containsKey(nombre)) {
            return false;
        }
        simbolos.put(nombre, new Entrada(rol, linea));
        System.out.println("[TABLA] Registrado: " + nombre + " -> " + rol + " [" + contextoActivo + "] L:" + linea);
        return true;
    }

    // Sobrecarga de compatibilidad para llamadas sin número de línea
    public boolean registrar(String nombre, String rol) {
        return registrar(nombre, rol, 0);
    }

    public boolean existe(String nombre) {
        return simbolos.containsKey(nombre);
    }

    public void limpiar() {
        simbolos.clear();
        contextoActivo = "Ninguno";
    }

    public Map<String, Entrada> getSimbolos() {
        return simbolos;
    }

    public String getContextoActivo() {
        return contextoActivo;
    }

    public static String descripcionPara(String nombre, String rol) {
        switch (nombre.toLowerCase()) {
            case "autor":    return "Metadato: autor del documento";
            case "version":  return "Metadato: versión del diagrama";
            case "tema":     return "Metadato: tema o estilo";
            case "exportar": return "Metadato: ruta de exportación";
            case "importar": return "Metadato: archivo importado";
            case "diagrama": return "Encabezado del diagrama (" + rol + ")";
        }
        switch (rol.toLowerCase()) {
            case "tipo_diagrama":   return "Módulo activo del compilador";
            case "inicio":        return "Nodo de inicio del flujo";
            case "fin":           return "Nodo de terminación del flujo";
            case "nodo":          return "Elemento de proceso";
            case "condicion":     return "Decisión condicional (bifurcación)";
            case "bucle":         return "Estructura iterativa";
            case "subproceso":    return "Subproceso encapsulado";
            case "entrada":       return "Lectura de datos del usuario";
            case "salida":        return "Presentación de datos al usuario";
            case "parada":        return "Interrupción del proceso";
            case "tabla":         return "Entidad de base de datos";
            case "vista":         return "Vista de base de datos";
            case "esquema":       return "Esquema de BD";
            case "paquete":       return "Paquete de BD";
            case "relacion":      return "Relación entre entidades BD";
            case "clase":         return "Clase UML";
            case "interfaz":      return "Interfaz UML";
            case "enum":          return "Enumeración UML";
            case "desconocido":   return "Tipo no reconocido (error sintáctico)";
            default:
                if (rol.startsWith("dispositivo_"))
                    return "Dispositivo de red — tipo: " + rol.substring(12);
                return "Identificador del diagrama";
        }
    }
}
