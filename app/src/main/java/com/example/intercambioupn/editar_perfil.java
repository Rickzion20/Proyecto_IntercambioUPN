package com.example.intercambioupn;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** HU17 - Actualizar datos personales (nombres, carrera, ciclo y sede; el código sale del correo UPN). */
public class editar_perfil extends BaseActivity {

    private EditText txtNombres, txtApellidos, txtCarrera, txtCiclo, txtCodigo;
    private Spinner spSede;
    private Button btnGuardar;
    private DatabaseReference ref;
    private FirebaseUser user;
    private List<String> sedes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_editar_perfil);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, R.string.m_inicia_editar, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        ref = FirebaseDatabase.getInstance().getReference("Usuarios").child(user.getUid());

        txtNombres = findViewById(R.id.txtEditNombres);
        txtApellidos = findViewById(R.id.txtEditApellidos);
        txtCarrera = findViewById(R.id.txtEditCarrera);
        txtCiclo = findViewById(R.id.txtEditCiclo);
        txtCodigo = findViewById(R.id.txtEditCodigo);
        spSede = findViewById(R.id.spEditSede);
        btnGuardar = findViewById(R.id.btnGuardarPerfil);

        // La primera opción del arreglo es "Todas las sedes" (solo sirve para filtrar), se omite
        String[] todas = getResources().getStringArray(R.array.sedes_universitarias);
        sedes = new ArrayList<>(Arrays.asList(todas).subList(1, todas.length));
        spSede.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sedes));

        // El código de estudiante sale del correo UPN y no se puede editar; en Gmail no aplica
        if (TipoCuenta.esUpn(user.getEmail())) {
            String codigo = TipoCuenta.codigoEstudiante(user.getEmail());
            txtCodigo.setText(codigo.isEmpty() ? getString(R.string.m_sin_codigo) : codigo);
            txtCodigo.setEnabled(false);
        } else {
            txtCodigo.setVisibility(View.GONE);
        }

        ref.get().addOnSuccessListener(snap -> {
            if (!snap.exists()) return;
            poner(txtNombres, snap.child("nombres").getValue(String.class));
            poner(txtApellidos, snap.child("apellidos").getValue(String.class));
            poner(txtCarrera, snap.child("carrera").getValue(String.class));
            poner(txtCiclo, snap.child("ciclo").getValue(String.class));
            String sede = snap.child("sede").getValue(String.class);
            if (sede != null && sedes.contains(sede)) spSede.setSelection(sedes.indexOf(sede));
        });
    }

    private static void poner(EditText campo, String valor) {
        if (valor != null && !valor.isEmpty()) campo.setText(valor);
    }

    /** Si el correo es de la forma n12345678@upn.pe devuelve el código en mayúsculas; si no, "". */
    static String codigoDesdeCorreo(String correo) {
        return TipoCuenta.codigoEstudiante(correo);
    }

    public void guardar(View view) {
        String nombres = txtNombres.getText().toString().trim();
        String apellidos = txtApellidos.getText().toString().trim();
        String carrera = txtCarrera.getText().toString().trim();
        String ciclo = txtCiclo.getText().toString().trim();
        String sede = spSede.getSelectedItem() != null ? spSede.getSelectedItem().toString() : "";

        if (TextUtils.isEmpty(nombres) || TextUtils.isEmpty(apellidos) || TextUtils.isEmpty(carrera)) {
            Toast.makeText(this, R.string.m_obligatorios, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!ciclo.isEmpty()) {
            int n;
            try {
                n = Integer.parseInt(ciclo);
            } catch (NumberFormatException e) {
                n = 0;
            }
            if (n < 1 || n > 10) {
                Toast.makeText(this, R.string.m_ciclo_rango, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        btnGuardar.setEnabled(false);

        Map<String, Object> datos = new HashMap<>();
        datos.put("nombres", nombres);
        datos.put("apellidos", apellidos);
        datos.put("carrera", carrera);
        datos.put("ciclo", ciclo);
        datos.put("telefono", null); // el teléfono ya no se usa: se borra el que hubiera quedado guardado
        datos.put("sede", sede);

        ref.updateChildren(datos).addOnCompleteListener(task -> {
            btnGuardar.setEnabled(true);
            if (task.isSuccessful()) {
                user.updateProfile(new UserProfileChangeRequest.Builder()
                        .setDisplayName(nombres + " " + apellidos).build());
                Almacen.agregarNotificacion(getApplicationContext(), getString(R.string.m_n_sistema),
                        getString(R.string.m_n_perfil_actualizado), getString(R.string.m_n_perfil_msj));
                Toast.makeText(this, R.string.m_datos_actualizados, Toast.LENGTH_SHORT).show();
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
