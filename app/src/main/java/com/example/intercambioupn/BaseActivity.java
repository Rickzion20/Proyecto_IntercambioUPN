package com.example.intercambioupn;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Base de todas las pantallas. Aplica las opciones de Accesibilidad:
 *  - Tamaño de texto: multiplica el tamaño de letra del sistema (Normal, Grande, Muy grande).
 *  - Texto fácil de leer: usa la fuente Atkinson Hyperlegible con más espacio entre letras y líneas.
 * El tema claro/oscuro lo aplica App al iniciar.
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context base) {
        float escala = Preferencias.escalaTexto(base);
        if (escala != 1f) {
            Configuration config = new Configuration(base.getResources().getConfiguration());
            config.fontScale = config.fontScale * escala;
            base = base.createConfigurationContext(config);
        }
        super.attachBaseContext(base);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Preferencias.textoLegible(this)) setTheme(R.style.Theme_IntercambioUPN_Legible);
        super.onCreate(savedInstanceState);
    }
}
