package com.example.intercambioupn;

/**
 * Direcciones del chat (HU08/HU09) y del historial (HU16).
 * Usan la misma BASE_URL de Api.java, así que la IP se sigue cambiando en un solo lugar.
 */
public final class ApiChat {

    public static final String ABRIR = Api.BASE_URL + "chat_abrir.php";
    public static final String LISTAR = Api.BASE_URL + "chat_listar.php";
    public static final String MENSAJES = Api.BASE_URL + "chat_mensajes.php";
    public static final String ENVIAR = Api.BASE_URL + "chat_enviar.php";

    public static final String OPERACION_REGISTRAR = Api.BASE_URL + "operacion_registrar.php";
    public static final String OPERACIONES_LISTAR = Api.BASE_URL + "operaciones_listar.php";
    public static final String OPERACION_ESTADO = Api.BASE_URL + "operacion_estado.php";

    /**
     * Para peticiones que CREAN algo (publicar, enviar mensaje): 15 s de espera pero sin reintento,
     * porque si el servidor ya lo guardó y solo tardó en responder, reintentar lo duplicaría.
     */
    public static <T> com.android.volley.Request<T> sinReintento(com.android.volley.Request<T> r) {
        r.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(15000, 0, 1.0f));
        return r;
    }

    private ApiChat() {
    }
}
