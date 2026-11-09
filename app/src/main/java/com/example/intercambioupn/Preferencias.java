package com.example.intercambioupn;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import java.util.Locale;

/**
 * Preferencias del dispositivo (no dependen de la cuenta, valen también antes de iniciar sesión):
 * tema claro/oscuro y opciones de accesibilidad.
 */
public final class Preferencias {

    private static final String ARCHIVO = "intercambio_upn_ajustes";

    public static final String TEMA_SISTEMA = "sistema";
    public static final String TEMA_CLARO = "claro";
    public static final String TEMA_OSCURO = "oscuro";

    private Preferencias() { }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE);
    }

    // ------------------------------------------------------------ Tema

    public static String tema(Context c) {
        return prefs(c).getString("tema", TEMA_SISTEMA);
    }

    /** Guarda el tema y lo aplica en toda la app (las pantallas abiertas se recrean solas). */
    public static void guardarTema(Context c, String tema) {
        prefs(c).edit().putString("tema", tema).apply();
        aplicarTema(c);
    }

    public static void aplicarTema(Context c) {
        String t = tema(c);
        int modo = TEMA_CLARO.equals(t) ? AppCompatDelegate.MODE_NIGHT_NO
                : TEMA_OSCURO.equals(t) ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        if (AppCompatDelegate.getDefaultNightMode() != modo) AppCompatDelegate.setDefaultNightMode(modo);
    }

    // ------------------------------------------------------------ Accesibilidad

    /** 1.0 = Normal, 1.15 = Grande, 1.3 = Muy grande (se multiplica por el tamaño del sistema). */
    public static float escalaTexto(Context c) {
        return prefs(c).getFloat("escala_texto", 1.0f);
    }

    public static void guardarEscalaTexto(Context c, float escala) {
        prefs(c).edit().putFloat("escala_texto", escala).apply();
    }

    public static boolean textoLegible(Context c) {
        return prefs(c).getBoolean("texto_legible", false);
    }

    public static void guardarTextoLegible(Context c, boolean activo) {
        prefs(c).edit().putBoolean("texto_legible", activo).apply();
    }

    public static boolean sonidoBienvenida(Context c) {
        return prefs(c).getBoolean("sonido_bienvenida", true);
    }

    public static void guardarSonidoBienvenida(Context c, boolean activo) {
        prefs(c).edit().putBoolean("sonido_bienvenida", activo).apply();
    }

    // ------------------------------------------------------------ Idioma

    /** "es" o "en". Si nunca se eligió, sigue el idioma del celular. */
    public static String idioma() {
        LocaleListCompat lista = AppCompatDelegate.getApplicationLocales();
        Locale l = lista.isEmpty() ? Locale.getDefault() : lista.get(0);
        return l != null && "en".equals(l.getLanguage()) ? "en" : "es";
    }

    /** Cambia el idioma de la app (Android lo recuerda y recarga las pantallas abiertas). */
    public static void cambiarIdioma(String etiqueta) {
        if (!etiqueta.equals(idioma()) || AppCompatDelegate.getApplicationLocales().isEmpty()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(etiqueta));
        }
    }
}
