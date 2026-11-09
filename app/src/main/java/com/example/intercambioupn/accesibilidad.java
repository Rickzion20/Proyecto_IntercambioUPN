package com.example.intercambioupn;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.RadioGroup;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.materialswitch.MaterialSwitch;

/**
 * Accesibilidad: tamaño de texto, texto fácil de leer y sonido de bienvenida.
 * Se abre desde Ajustes y desde la pantalla de bienvenida (sin iniciar sesión).
 * Al cambiar el texto la pantalla se recarga para mostrar el resultado; al salir,
 * la app se reabre para que todas las pantallas usen la nueva configuración.
 */
public class accesibilidad extends BaseActivity {

    private static final String HUBO_CAMBIOS = "hubo_cambios";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_accesibilidad);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        // Tamaño de texto
        RadioGroup rg = findViewById(R.id.rgTamanoTexto);
        float escala = Preferencias.escalaTexto(this);
        rg.check(escala >= 1.29f ? R.id.rbTextoMuyGrande : escala >= 1.14f ? R.id.rbTextoGrande : R.id.rbTextoNormal);
        rg.setOnCheckedChangeListener((g, id) -> {
            float nueva = id == R.id.rbTextoMuyGrande ? 1.3f : id == R.id.rbTextoGrande ? 1.15f : 1.0f;
            if (nueva != Preferencias.escalaTexto(this)) {
                Preferencias.guardarEscalaTexto(this, nueva);
                recargar();
            }
        });

        // Texto fácil de leer
        MaterialSwitch swLegible = findViewById(R.id.swTextoLegible);
        swLegible.setChecked(Preferencias.textoLegible(this));
        swLegible.setOnCheckedChangeListener((b, activo) -> {
            if (activo != Preferencias.textoLegible(this)) {
                Preferencias.guardarTextoLegible(this, activo);
                recargar();
            }
        });

        // Sonido de bienvenida (no necesita recargar)
        MaterialSwitch swSonido = findViewById(R.id.swSonidoBienvenida);
        swSonido.setChecked(Preferencias.sonidoBienvenida(this));
        swSonido.setOnCheckedChangeListener((b, activo) -> Preferencias.guardarSonidoBienvenida(this, activo));

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                salir();
            }
        });
    }

    /** Recarga esta pantalla para ver el cambio en la vista previa. */
    private void recargar() {
        getIntent().putExtra(HUBO_CAMBIOS, true);
        recreate();
    }

    private void salir() {
        if (!getIntent().getBooleanExtra(HUBO_CAMBIOS, false)) {
            finish();
            return;
        }
        // Se reabre la app desde su pantalla principal para aplicar el cambio en todas las pantallas
        boolean conSesion = TipoCuenta.sesionValida(com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser());
        Intent i = new Intent(this, conSesion ? MainActivity.class : seleccionar_acceso.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    public void regresar(View view) {
        salir();
    }

    public void abrirAccesibilidadSistema(View view) {
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }
}
