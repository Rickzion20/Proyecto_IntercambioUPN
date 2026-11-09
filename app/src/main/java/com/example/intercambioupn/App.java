package com.example.intercambioupn;

import android.app.Application;

/** Se ejecuta una vez al abrir la app, antes de cualquier pantalla: aplica el tema guardado. */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Preferencias.aplicarTema(this);
    }
}
