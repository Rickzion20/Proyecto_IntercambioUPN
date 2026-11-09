package com.example.intercambioupn;


public class Api {

    // Emulador: 10.0.2.2  |  Celular físico: la IPv4 de la PC (ipconfig), ej. "http://192.168.1.50/crud_intercambio/"
    public static final String BASE_URL = "http://192.168.56.1/crud_intercambio/";

    public static final String MOSTRAR = BASE_URL + "mostrar_.php";
    public static final String INSERTAR = BASE_URL + "insertar_.php";
    public static final String ACTUALIZAR = BASE_URL + "actualizar_.php";
    public static final String ELIMINAR = BASE_URL + "eliminar_.php";

    private Api() {
    }
    public static <T> com.android.volley.Request<T> reintentos(com.android.volley.Request<T> r) {
        r.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(15000, 1, 1.0f));
        return r;
    }
}
