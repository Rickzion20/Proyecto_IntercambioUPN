package com.example.intercambioupn;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * HU22 - Consultar ayuda, sostenibilidad y acerca de.
 * Muestra el centro de ayuda y contacto, la sección de sostenibilidad y la versión de la app.
 */
public class ayuda_info extends BaseActivity {

    /** Correo al que escribe el botón "Contactar a soporte". Cámbialo por el del equipo. */
    private static final String CORREO_SOPORTE = "melgarejogonzalesjoseph@gmail.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ayuda_info);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ((TextView) findViewById(R.id.txtVersion)).setText(getString(R.string.m_version_x, obtenerVersion()));
    }

    private String obtenerVersion() {
        try {
            return getPackageManager()
                    .getPackageInfo(getPackageName(), PackageManager.PackageInfoFlags.of(0))
                    .versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "1.0";
        }
    }

    public void contactarSoporte(View view) {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + CORREO_SOPORTE));
        intent.putExtra(Intent.EXTRA_SUBJECT, "Ayuda con Intercambio UPN");
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.m_sin_app_correo, Toast.LENGTH_SHORT).show();
        }
    }

    public void regresar(View view) {
        finish();
    }
}
