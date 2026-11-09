package com.example.intercambioupn;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * HU21 - Configurar preferencias de la aplicación (idioma, tema y accesibilidad).
 */
public class ajustes extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ajustes);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int margen = (int) (24 * v.getResources().getDisplayMetrics().density); // 24dp, igual que el XML
            v.setPadding(systemBars.left + margen, systemBars.top + margen, systemBars.right + margen, systemBars.bottom + margen);
            return insets;
        });

        // Idioma: Android recarga la pantalla en el idioma elegido
        RadioGroup rgIdioma = findViewById(R.id.rgIdioma);
        rgIdioma.check("en".equals(Preferencias.idioma()) ? R.id.rbIngles : R.id.rbEspanol);
        rgIdioma.setOnCheckedChangeListener((g, id) ->
                Preferencias.cambiarIdioma(id == R.id.rbIngles ? "en" : "es"));

        // Tema: se muestra el guardado y al elegir otro se aplica en toda la app
        String tema = Preferencias.tema(this);
        int marcado = Preferencias.TEMA_CLARO.equals(tema) ? R.id.rbClaro
                : Preferencias.TEMA_OSCURO.equals(tema) ? R.id.rbOscuro
                : R.id.rbSistema;
        RadioGroup rgTema = findViewById(R.id.rgTema);
        rgTema.check(marcado);
        rgTema.setOnCheckedChangeListener((g, id) -> Preferencias.guardarTema(this,
                id == R.id.rbClaro ? Preferencias.TEMA_CLARO
                        : id == R.id.rbOscuro ? Preferencias.TEMA_OSCURO
                        : Preferencias.TEMA_SISTEMA));
    }

    public void regresar(View view) {
        finish();
    }

    public void abrirPerfil(View view) {
        startActivity(new Intent(this, perfil.class));
    }

    public void abrirAccesibilidad(View view) {
        startActivity(new Intent(this, accesibilidad.class));
    }

    public void abrirAyuda(View view) {
        startActivity(new Intent(this, ayuda_info.class));
    }
}
