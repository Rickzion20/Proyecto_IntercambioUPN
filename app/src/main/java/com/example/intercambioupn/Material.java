package com.example.intercambioupn;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

public class Material implements Serializable {

    private final String id;
    private final String nombre;
    private final String descripcion;
    private final String categoria;
    private final String tipo;
    private final String estado;
    private final String sede;
    private final String urlimagen;

    // Datos de quien publicó (vienen de Firebase al momento de publicar)
    private final String uidUsuario;
    private final String usuarioNombre;
    private final String usuarioCorreo;

    public Material(
            String id,
            String nombre,
            String descripcion,
            String categoria,
            String tipo,
            String estado,
            String sede,
            String urlimagen,
            String uidUsuario,
            String usuarioNombre,
            String usuarioCorreo) {

        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.tipo = tipo;
        this.estado = estado;
        this.sede = sede;
        this.urlimagen = urlimagen;
        this.uidUsuario = uidUsuario;
        this.usuarioNombre = usuarioNombre;
        this.usuarioCorreo = usuarioCorreo;
    }

    /**
     * Lee un campo como texto. Si en MySQL es NULL devuelve "" (optString devolvería la palabra "null",
     * y eso hacía que las publicaciones de ejemplo mostraran "null" o tuvieran un dueño falso).
     */
    private static String texto(JSONObject o, String clave) {
        return o.isNull(clave) ? "" : o.optString(clave, "");
    }

    /** Convierte la respuesta de mostrar_.php ({"datos":[...]}) en una lista de materiales. */
    public static ArrayList<Material> desdeRespuesta(JSONObject response) throws JSONException {
        ArrayList<Material> lista = new ArrayList<>();
        JSONArray jsonArray = response.getJSONArray("datos");

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject o = jsonArray.getJSONObject(i);

            String nombre = o.has("nombre_material") ? texto(o, "nombre_material") : texto(o, "nombre");

            lista.add(new Material(
                    texto(o, "id"),
                    nombre,
                    texto(o, "descripcion"),
                    texto(o, "categoria"),
                    texto(o, "tipo"),
                    texto(o, "estado"),
                    texto(o, "sede"),
                    texto(o, "foto"),
                    texto(o, "uid_usuario"),
                    texto(o, "usuario_nombre"),
                    texto(o, "usuario_correo")
            ));
        }
        return lista;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getCategoria() { return categoria; }
    public String getTipo() { return tipo; }
    public String getEstado() { return estado; }
    public String getSede() { return sede; }
    public String getUrlimagen() { return urlimagen; }
    public String getUidUsuario() { return uidUsuario; }
    public String getUsuarioNombre() { return usuarioNombre; }
    public String getUsuarioCorreo() { return usuarioCorreo; }
}
