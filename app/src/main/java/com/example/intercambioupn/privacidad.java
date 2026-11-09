package com.example.intercambioupn;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

/**
 * HU18 - Configurar privacidad del perfil.
 * Se guarda en Firebase: Usuarios/{uid}/privacidad. Por defecto el perfil es "Solo universitarios UPN",
 * se muestra el correo y la carrera/ciclo.
 */
public class privacidad extends BaseActivity {

    private RadioGroup rgVisibilidad;
    private MaterialSwitch swCorreo, swCarreraCiclo;
    private Button btnGuardar;
    private DatabaseReference ref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_privacidad);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rgVisibilidad = findViewById(R.id.rgVisibilidad);
        swCorreo = findViewById(R.id.swCorreo);
        swCarreraCiclo = findViewById(R.id.swCarreraCiclo);
        btnGuardar = findViewById(R.id.btnGuardarPrivacidad);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, R.string.m_inicia_privacidad, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        ref = FirebaseDatabase.getInstance().getReference("Usuarios").child(user.getUid()).child("privacidad");

        // Valores por defecto mientras llegan los guardados
        marcarVisibilidad("upn");
        swCorreo.setChecked(true);
        swCarreraCiclo.setChecked(true);

        ref.get().addOnSuccessListener(snap -> {
            if (!snap.exists()) return;
            String vis = snap.child("visibilidad").getValue(String.class);
            if (vis != null) marcarVisibilidad(vis);
            Boolean correo = snap.child("mostrarCorreo").getValue(Boolean.class);
            Boolean carrera = snap.child("mostrarCarreraCiclo").getValue(Boolean.class);
            if (correo != null) swCorreo.setChecked(correo);
            if (carrera != null) swCarreraCiclo.setChecked(carrera);
        });
    }

    private void marcarVisibilidad(String valor) {
        int id = "publico".equals(valor) ? R.id.rbPublico
                : "contactos".equals(valor) ? R.id.rbContactos
                : R.id.rbUpn;
        ((RadioButton) findViewById(id)).setChecked(true);
    }

    private String visibilidadElegida() {
        int id = rgVisibilidad.getCheckedRadioButtonId();
        if (id == R.id.rbPublico) return "publico";
        if (id == R.id.rbContactos) return "contactos";
        return "upn";
    }

    public void guardar(View view) {
        btnGuardar.setEnabled(false);

        Map<String, Object> datos = new HashMap<>();
        datos.put("visibilidad", visibilidadElegida());
        datos.put("mostrarCorreo", swCorreo.isChecked());
        datos.put("mostrarCarreraCiclo", swCarreraCiclo.isChecked());

        ref.setValue(datos).addOnCompleteListener(task -> {
            btnGuardar.setEnabled(true);
            if (task.isSuccessful()) {
                Almacen.agregarNotificacion(getApplicationContext(), getString(R.string.m_n_seguridad),
                        getString(R.string.m_n_privacidad), getString(R.string.m_n_privacidad_msj));
                Toast.makeText(this, R.string.m_n_privacidad, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, R.string.m_no_guardo_conexion, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void regresar(View view) {
        finish();
    }
}
