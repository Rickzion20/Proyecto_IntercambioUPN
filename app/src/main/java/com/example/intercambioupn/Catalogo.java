package com.example.intercambioupn;

import android.content.Context;
import android.content.res.Configuration;

import java.util.Locale;

/**
 * Valores de catálogo (categoría, modalidad, estado del material, estado del acuerdo).
 *
 * En la base de datos se guardan SIEMPRE en español ("Libros", "Préstamo", "Pendiente"…),
 * para que los filtros y búsquedas funcionen igual sin importar el idioma de quien publicó.
 * En pantalla se muestran en el idioma elegido.
 *
 * Funciona por posición: el elemento N del arreglo traducido corresponde al elemento N
 * del mismo arreglo en español (res/values y res/values-en tienen el mismo orden).
 */
public final class Catalogo {

    private static final int[] ARREGLOS = {
            R.array.categorias_material,
            R.array.tipos_intercambio,
            R.array.estados_material,
            R.array.estados_acuerdo,
            R.array.sedes_universitarias,
    };

    private Catalogo() { }

    /** Recursos en español, sin importar el idioma activo. */
    private static Context espanol(Context c) {
        Configuration conf = new Configuration(c.getResources().getConfiguration());
        conf.setLocale(new Locale("es"));
        return c.createConfigurationContext(conf);
    }

    /** Valor guardado (español) → texto para mostrar en el idioma actual. */
    public static String mostrar(Context c, String valor) {
        if (valor == null || valor.isEmpty()) return "";
        Context es = espanol(c);
        for (int id : ARREGLOS) {
            String[] base = es.getResources().getStringArray(id);
            String[] vista = c.getResources().getStringArray(id);
            for (int i = 0; i < base.length && i < vista.length; i++) {
                if (base[i].equalsIgnoreCase(valor)) return vista[i];
            }
        }
        return valor;
    }

    /** Texto elegido en pantalla (idioma actual) → valor que se guarda (español). */
    public static String guardar(Context c, int arreglo, String mostrado) {
        if (mostrado == null || mostrado.isEmpty()) return "";
        String[] vista = c.getResources().getStringArray(arreglo);
        String[] base = espanol(c).getResources().getStringArray(arreglo);
        for (int i = 0; i < vista.length && i < base.length; i++) {
            if (vista[i].equalsIgnoreCase(mostrado)) return base[i];
        }
        return mostrado;
    }

    /** Valor guardado (español) → posición dentro del arreglo, o -1. */
    public static int posicion(Context c, int arreglo, String valor) {
        if (valor == null) return -1;
        String[] base = espanol(c).getResources().getStringArray(arreglo);
        for (int i = 0; i < base.length; i++) {
            if (base[i].equalsIgnoreCase(valor)) return i;
        }
        return -1;
    }
}
