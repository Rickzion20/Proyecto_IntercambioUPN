package com.example.intercambioupn;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Datos que se guardan en el propio celular (SharedPreferences), separados por usuario:
 * favoritos (HU15), búsquedas recientes (HU25) y preferencias de la app (HU21).
 */
public final class Almacen {

    private static final String ARCHIVO = "intercambio_upn_prefs";
    private static final int MAX_BUSQUEDAS = 5;

    private Almacen() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE);
    }

    /** UID de Firebase del usuario con sesión; así cada cuenta tiene sus propios datos. */
    private static String uid() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        return u != null ? u.getUid() : "anonimo";
    }

    // ===== HU15 - Favoritos =====

    public static Set<String> favoritos(Context c) {
        return new HashSet<>(prefs(c).getStringSet("fav_" + uid(), new HashSet<>()));
    }

    public static boolean esFavorito(Context c, String idMaterial) {
        return favoritos(c).contains(idMaterial);
    }

    /** Agrega o quita el material de favoritos. Devuelve true si quedó como favorito. */
    public static boolean alternarFavorito(Context c, String idMaterial) {
        Set<String> ids = favoritos(c);
        boolean ahoraEsFavorito = !ids.remove(idMaterial);
        if (ahoraEsFavorito) ids.add(idMaterial);
        prefs(c).edit().putStringSet("fav_" + uid(), ids).apply();
        return ahoraEsFavorito;
    }

    // ===== HU25 - Búsquedas recientes =====

    public static List<String> busquedasRecientes(Context c) {
        List<String> lista = new ArrayList<>();
        try {
            JSONArray arreglo = new JSONArray(prefs(c).getString("bus_" + uid(), "[]"));
            for (int i = 0; i < arreglo.length(); i++) lista.add(arreglo.getString(i));
        } catch (JSONException ignorada) {
            // si el dato está dañado, se parte con una lista vacía
        }
        return lista;
    }

    /** Guarda la búsqueda al inicio de la lista, sin repetidos y con un máximo de 5. */
    public static void agregarBusqueda(Context c, String termino) {
        String t = termino == null ? "" : termino.trim();
        if (t.isEmpty()) return;

        List<String> lista = busquedasRecientes(c);
        quitar(lista, t);
        lista.add(0, t);
        while (lista.size() > MAX_BUSQUEDAS) lista.remove(lista.size() - 1);
        guardarBusquedas(c, lista);
    }

    public static void eliminarBusqueda(Context c, String termino) {
        List<String> lista = busquedasRecientes(c);
        quitar(lista, termino);
        guardarBusquedas(c, lista);
    }

    public static void limpiarBusquedas(Context c) {
        guardarBusquedas(c, new ArrayList<>());
    }

    private static void quitar(List<String> lista, String termino) {
        for (Iterator<String> it = lista.iterator(); it.hasNext(); ) {
            if (it.next().equalsIgnoreCase(termino)) it.remove();
        }
    }

    private static void guardarBusquedas(Context c, List<String> lista) {
        prefs(c).edit().putString("bus_" + uid(), new JSONArray(lista).toString()).apply();
    }

    // ===== HU21 - Preferencias de la aplicación =====

    public static String idioma(Context c) {
        return prefs(c).getString("idioma", "Español");
    }

    public static void guardarIdioma(Context c, String idioma) {
        prefs(c).edit().putString("idioma", idioma).apply();
    }

    public static String tema(Context c) {
        return prefs(c).getString("tema", "Claro");
    }

    public static void guardarTema(Context c, String tema) {
        prefs(c).edit().putString("tema", tema).apply();
    }

    // ===== HU20 - Notificaciones =====

    /** Una notificación guardada en el celular. */
    public static class Notificacion {
        public final String id;
        public final String tipo;
        public final String titulo;
        public final String mensaje;
        public final long fecha;
        public boolean leida;

        Notificacion(String id, String tipo, String titulo, String mensaje, long fecha, boolean leida) {
            this.id = id;
            this.tipo = tipo;
            this.titulo = titulo;
            this.mensaje = mensaje;
            this.fecha = fecha;
            this.leida = leida;
        }
    }

    private static final int MAX_NOTIFICACIONES = 50;

    /** Notificaciones del usuario, de la más reciente a la más antigua. */
    public static List<Notificacion> notificaciones(Context c) {
        List<Notificacion> lista = new ArrayList<>();
        try {
            JSONArray arreglo = new JSONArray(prefs(c).getString("not_" + uid(), "[]"));
            for (int i = 0; i < arreglo.length(); i++) {
                JSONObject o = arreglo.getJSONObject(i);
                lista.add(new Notificacion(
                        o.optString("id"), o.optString("tipo"), o.optString("titulo"),
                        o.optString("mensaje"), o.optLong("fecha"), o.optBoolean("leida")));
            }
        } catch (JSONException ignorada) {
            // dato dañado: se parte con la lista vacía
        }
        return lista;
    }

    private static void guardarNotificaciones(Context c, List<Notificacion> lista) {
        JSONArray arreglo = new JSONArray();
        try {
            for (Notificacion n : lista) {
                JSONObject o = new JSONObject();
                o.put("id", n.id);
                o.put("tipo", n.tipo);
                o.put("titulo", n.titulo);
                o.put("mensaje", n.mensaje);
                o.put("fecha", n.fecha);
                o.put("leida", n.leida);
                arreglo.put(o);
            }
        } catch (JSONException ignorada) {
            return;
        }
        prefs(c).edit().putString("not_" + uid(), arreglo.toString()).apply();
    }

    public static void agregarNotificacion(Context c, String tipo, String titulo, String mensaje) {
        agregarNotificacion(c, tipo, titulo, mensaje, System.currentTimeMillis());
    }

    private static void agregarNotificacion(Context c, String tipo, String titulo, String mensaje, long fecha) {
        List<Notificacion> lista = notificaciones(c);
        lista.add(0, new Notificacion(UUID.randomUUID().toString(), tipo, titulo, mensaje, fecha, false));
        while (lista.size() > MAX_NOTIFICACIONES) lista.remove(lista.size() - 1);
        guardarNotificaciones(c, lista);
    }

    public static int noLeidas(Context c) {
        int n = 0;
        for (Notificacion x : notificaciones(c)) if (!x.leida) n++;
        return n;
    }

    public static void marcarLeida(Context c, String id) {
        List<Notificacion> lista = notificaciones(c);
        for (Notificacion n : lista) if (n.id.equals(id)) n.leida = true;
        guardarNotificaciones(c, lista);
    }

    public static void marcarTodasLeidas(Context c) {
        List<Notificacion> lista = notificaciones(c);
        for (Notificacion n : lista) n.leida = true;
        guardarNotificaciones(c, lista);
    }

    /** La primera vez que entra una cuenta, se le dejan 3 avisos de bienvenida. */
    public static void sembrarNotificacionesIniciales(Context c) {
        String clave = "not_ini_" + uid();
        if (prefs(c).getBoolean(clave, false)) return;
        long ahora = System.currentTimeMillis();
        agregarNotificacion(c, c.getString(R.string.m_n_actualizacion), c.getString(R.string.m_n_nuevas),
                c.getString(R.string.m_n_nuevas_msj), ahora);
        agregarNotificacion(c, c.getString(R.string.m_n_recordatorio), c.getString(R.string.m_n_coordina),
                c.getString(R.string.m_n_coordina_msj), ahora + 1);
        agregarNotificacion(c, c.getString(R.string.m_n_sistema), c.getString(R.string.m_n_bienvenido),
                c.getString(R.string.m_n_bienvenido_msj), ahora + 2);
        prefs(c).edit().putBoolean(clave, true).apply();
    }

    // ===== HU08/HU09 - Mensajes vistos y avisados =====

    /** Id del último mensaje que el usuario ya vio en esa conversación. */
    public static int ultimoVisto(Context c, String conversacionId) {
        return prefs(c).getInt("visto_" + uid() + "_" + conversacionId, 0);
    }

    public static void marcarVisto(Context c, String conversacionId, int mensajeId) {
        if (mensajeId > ultimoVisto(c, conversacionId)) {
            prefs(c).edit().putInt("visto_" + uid() + "_" + conversacionId, mensajeId).apply();
        }
    }

    /** Evita repetir la notificación del mismo mensaje. */
    public static int ultimoAvisado(Context c, String conversacionId) {
        return prefs(c).getInt("avisado_" + uid() + "_" + conversacionId, 0);
    }

    public static void marcarAvisado(Context c, String conversacionId, int mensajeId) {
        prefs(c).edit().putInt("avisado_" + uid() + "_" + conversacionId, mensajeId).apply();
    }
}
