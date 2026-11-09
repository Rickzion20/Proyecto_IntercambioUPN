package com.example.intercambioupn;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;
import android.media.MediaPlayer;

import android.os.Handler;
import android.os.Looper;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

public class splash extends BaseActivity {
    private ImageView imagenSplash;
    private TextView textoBienvenida;

    private MediaPlayer reproductorMusica;
    private final Handler manejador = new Handler(Looper.getMainLooper());

    private boolean pantallaFinalizada = false;
    private final Runnable abrirLogin = new Runnable() {
        @Override
        public void run() {
            if (isFinishing() || pantallaFinalizada) {
                return;
            }

            pantallaFinalizada = true;

            detenerMusica();

            // Sesión abierta y válida (dominio permitido y, en UPN, correo verificado) -> Inicio.
            // Si no, se cierra y se elige el tipo de acceso.
            com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
            boolean haySesion = TipoCuenta.sesionValida(auth.getCurrentUser());
            if (!haySesion && auth.getCurrentUser() != null) auth.signOut();
            Intent intencion = new Intent(
                    splash.this,
                    haySesion ? MainActivity.class : seleccionar_acceso.class
            );

            startActivity(intencion);
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        imagenSplash = findViewById(R.id.imagenSplash);
        textoBienvenida = findViewById(R.id.textoBienvenida);

        iniciarAnimacion();
        reproducirMusica();

        // Abrir el login después de 10 segundos.
        manejador.postDelayed(abrirLogin, 10000);
    }

    private void iniciarAnimacion() {

        // Aparición gradual de la imagen.
        imagenSplash.setAlpha(0f);
        imagenSplash.setScaleX(1.08f);
        imagenSplash.setScaleY(1.08f);

        imagenSplash.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(1800)
                .setInterpolator(
                        new AccelerateDecelerateInterpolator()
                )
                .start();

        // El mensaje sube desde abajo.
        textoBienvenida.setAlpha(0f);
        textoBienvenida.setTranslationY(70f);

        textoBienvenida.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(600)
                .setDuration(1200)
                .setInterpolator(
                        new AccelerateDecelerateInterpolator()
                )
                .start();
    }

    private void reproducirMusica() {
        // Accesibilidad: el sonido de bienvenida se puede desactivar
        if (!Preferencias.sonidoBienvenida(this)) return;
        reproductorMusica = MediaPlayer.create(
                this,
                R.raw.musica
        );

        if (reproductorMusica != null) {
            reproductorMusica.setLooping(false);
            reproductorMusica.start();
        }
    }

    private void detenerMusica() {
        if (reproductorMusica != null) {
            if (reproductorMusica.isPlaying()) {
                reproductorMusica.stop();
            }

            reproductorMusica.release();
            reproductorMusica = null;
        }
    }

    @Override
    protected void onDestroy() {
        manejador.removeCallbacks(abrirLogin);
        detenerMusica();
        super.onDestroy();
    }
}

