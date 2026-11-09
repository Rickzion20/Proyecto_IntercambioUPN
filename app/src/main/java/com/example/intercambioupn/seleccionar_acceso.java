package com.example.intercambioupn;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/** Primera pantalla sin sesión: elegir entre acceso UPN o acceso con Gmail. */
public class seleccionar_acceso extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_seleccionar_acceso);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        findViewById(R.id.cardAccesoUpn).setOnClickListener(v -> abrirLogin(TipoCuenta.UPN));
        findViewById(R.id.cardAccesoGmail).setOnClickListener(v -> abrirLogin(TipoCuenta.GMAIL));
        findViewById(R.id.btnIdiomaBienvenida).setOnClickListener(v -> elegirIdioma());
        findViewById(R.id.btnAccesibilidadBienvenida).setOnClickListener(v ->
                startActivity(new Intent(this, accesibilidad.class)));
    }

    private void abrirLogin(String modo) {
        Intent i = new Intent(this, login.class);
        i.putExtra(TipoCuenta.MODO, modo);
        startActivity(i);
    }

    /** Idioma antes de iniciar sesión (los nombres van en su propio idioma para reconocerlos). */
    private void elegirIdioma() {
        String[] opciones = {"Español", "English"};
        int actual = "en".equals(Preferencias.idioma()) ? 1 : 0;
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.m_idioma_titulo)
                .setSingleChoiceItems(opciones, actual, (d, i) -> {
                    d.dismiss();
                    Preferencias.cambiarIdioma(i == 1 ? "en" : "es");
                })
                .show();
    }
}
