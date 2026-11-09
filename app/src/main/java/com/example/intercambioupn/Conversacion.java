package com.example.intercambioupn;

import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/** Conversación entre un interesado y el dueño de un material (HU08/HU09). */
public class Conversacion implements Serializable {

    public final String id;
    public final String materialId;
    public final String materialNombre;
    public final String materialTipo;
    public final String uidInteresado;
    public final String nombreInteresado;
    public final String uidPropietario;
    public final String nombrePropietario;
    public final String ultimoMensaje;
    public final String ultimoUid;
    public final int ultimoId;
    public final String fecha;
    public String operacionEstado;

    public Conversacion(String id, String materialId, String materialNombre, String materialTipo,
                        String uidInteresado, String nombreInteresado,
                        String uidPropietario, String nombrePropietario,
                        String ultimoMensaje, String ultimoUid, int ultimoId,
                        String fecha, String operacionEstado) {
        this.id = id;
        this.materialId = materialId;
        this.materialNombre = materialNombre;
        this.materialTipo = materialTipo;
        this.uidInteresado = uidInteresado;
        this.nombreInteresado = nombreInteresado;
        this.uidPropietario = uidPropietario;
        this.nombrePropietario = nombrePropietario;
        this.ultimoMensaje = ultimoMensaje;
        this.ultimoUid = ultimoUid;
        this.ultimoId = ultimoId;
        this.fecha = fecha;
        this.operacionEstado = operacionEstado;
    }

    public boolean soyPropietario(String miUid) {
        return uidPropietario.equals(miUid);
    }

    /** Nombre de la otra persona de la conversación. */
    public String otroNombre(String miUid) {
        String n = soyPropietario(miUid) ? nombreInteresado : nombrePropietario;
        return n.isEmpty() ? "UPN" : n;
    }

    /** true si el último mensaje es de la otra persona y aún no se ha visto. */
    public boolean tieneNoLeidos(String miUid, int ultimoVisto) {
        return ultimoId > ultimoVisto && !ultimoUid.isEmpty() && !ultimoUid.equals(miUid);
    }

    static String texto(JSONObject o, String clave) {
        return o.isNull(clave) ? "" : o.optString(clave, "");
    }

    public static ArrayList<Conversacion> desdeRespuesta(JSONObject response) throws JSONException {
        ArrayList<Conversacion> lista = new ArrayList<>();
        JSONArray arreglo = response.getJSONArray("datos");
        for (int i = 0; i < arreglo.length(); i++) {
            JSONObject o = arreglo.getJSONObject(i);
            lista.add(new Conversacion(
                    texto(o, "id"), texto(o, "material_id"), texto(o, "material_nombre"), texto(o, "material_tipo"),
                    texto(o, "uid_interesado"), texto(o, "nombre_interesado"),
                    texto(o, "uid_propietario"), texto(o, "nombre_propietario"),
                    texto(o, "ultimo_mensaje"), texto(o, "ultimo_uid"), o.optInt("ultimo_id", 0),
                    texto(o, "fecha_actualizacion"), texto(o, "operacion_estado")));
        }
        return lista;
    }

    /** Convierte "2026-10-25 20:15:00" (MySQL) al formato pedido, p. ej. "HH:mm" o "dd/MM HH:mm". */
    public static String formatear(String fechaMysql, String patron) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(fechaMysql);
            return d == null ? "" : new SimpleDateFormat(patron, new Locale("es", "PE")).format(d);
        } catch (ParseException e) {
            return "";
        }
    }

    /** Arma una URL GET con un parámetro codificado. */
    public static String url(String base, String clave, String valor) {
        return base + "?" + clave + "=" + Uri.encode(valor);
    }
}
