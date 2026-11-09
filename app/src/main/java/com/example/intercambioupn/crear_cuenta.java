package com.example.intercambioupn;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
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

import java.util.HashMap;
import java.util.Map;

public class crear_cuenta extends BaseActivity {
    private EditText txtNombres;
    private EditText txtApellidos;
    private EditText txtCorreo;
    private EditText txtCarrera;
    private EditText txtContrasena;
    private EditText txtConfirmarContrasena;
    private Button btnRegistrarCuenta;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String modo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_crear_cuenta);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("Usuarios");

        txtNombres = findViewById(R.id.txtNombres);
        txtApellidos = findViewById(R.id.txtApellidos);
        txtCorreo = findViewById(R.id.txtCorreo);
        txtCarrera = findViewById(R.id.txtCarrera);
        txtContrasena = findViewById(R.id.txtContrasena);
        txtConfirmarContrasena = findViewById(R.id.txtConfirmarContrasena);
        btnRegistrarCuenta = findViewById(R.id.btnRegistrarCuenta);

        // Modalidad elegida al inicio: define qué correos se aceptan
        modo = getIntent().getStringExtra(TipoCuenta.MODO);
        if (!TipoCuenta.GMAIL.equals(modo)) modo = TipoCuenta.UPN;
        boolean upn = TipoCuenta.UPN.equals(modo);
        ((TextView) findViewById(R.id.lblCorreoRegistro)).setText(upn ? R.string.m_correo_institucional : R.string.m_correo_gmail);
        txtCorreo.setHint(upn ? R.string.m_ej_upn_largo : R.string.m_ej_gmail_largo);
    }

    public void regresar(View view) {
        // El login ya está debajo; abrir otro lo duplicaba en la pila
        finish();
    }

    public void crearcuenta(View view) {
        String nombres = txtNombres.getText().toString().trim();
        String apellidos = txtApellidos.getText().toString().trim();
        String correo = TipoCuenta.normalizar(txtCorreo.getText().toString());
        String carrera = txtCarrera.getText().toString().trim();
        String contrasena = txtContrasena.getText().toString().trim();
        String confirmarContrasena = txtConfirmarContrasena.getText().toString().trim();

        if (TextUtils.isEmpty(nombres) || TextUtils.isEmpty(apellidos) ||
                TextUtils.isEmpty(correo) || TextUtils.isEmpty(carrera) ||
                TextUtils.isEmpty(contrasena) || TextUtils.isEmpty(confirmarContrasena)) {
            Toast.makeText(this, R.string.m_completa_campos, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!TipoCuenta.correspondeAlModo(correo, modo)) {
            txtCorreo.setError(TipoCuenta.UPN.equals(modo)
                    ? getString(R.string.m_usa_upn) : getString(R.string.m_usa_gmail));
            txtCorreo.requestFocus();
            return;
        }

        if (!contrasena.equals(confirmarContrasena)) {
            Toast.makeText(this, R.string.m_no_coinciden, Toast.LENGTH_SHORT).show();
            return;
        }

        if (contrasena.length() < 6) {
            Toast.makeText(this, R.string.m_min6, Toast.LENGTH_SHORT).show();
            return;
        }

        // Evita crear la cuenta dos veces por un doble toque
        if (btnRegistrarCuenta != null) btnRegistrarCuenta.setEnabled(false);

        // Crear la cuenta en Firebase Authentication
        mAuth.createUserWithEmailAndPassword(correo, contrasena)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Actualizar el nombre completo en el perfil de Firebase Authentication
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(nombres + " " + apellidos)
                                    .build();
                            user.updateProfile(profileUpdates);

                            String uid = user.getUid();

                            // Guardar datos adicionales en Firebase Realtime Database
                            Map<String, Object> usuarioMap = new HashMap<>();
                            usuarioMap.put("uid", uid);
                            usuarioMap.put("nombres", nombres);
                            usuarioMap.put("apellidos", apellidos);
                            usuarioMap.put("correo", correo);
                            usuarioMap.put("carrera", carrera);
                            usuarioMap.put("tipoUsuario", TipoCuenta.tipoDe(correo));
                            String codigo = TipoCuenta.codigoEstudiante(correo);
                            if (!codigo.isEmpty()) usuarioMap.put("codigoEstudiante", codigo);

                            mDatabase.child(uid).setValue(usuarioMap)
                                    .addOnCompleteListener(dbTask -> {
                                        if (dbTask.isSuccessful()) {
                                            if (TipoCuenta.UPN.equals(modo)) {
                                                // Estudiantes UPN: confirmar que el correo institucional es suyo.
                                                // Se cierra la sesión recién cuando el correo salió.
                                                user.sendEmailVerification().addOnCompleteListener(v -> irACuentaCreada(correo));
                                            } else {
                                                irACuentaCreada(correo);
                                            }
                                        } else {
                                            if (btnRegistrarCuenta != null) btnRegistrarCuenta.setEnabled(true);
                                            Toast.makeText(crear_cuenta.this, R.string.m_error_bd, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    } else {
                        if (btnRegistrarCuenta != null) btnRegistrarCuenta.setEnabled(true);
                        Toast.makeText(crear_cuenta.this, mensajeRegistro(task.getException()), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void irACuentaCreada(String correo) {
        // Cerrar sesión activa para que el usuario pueda iniciar sesión manualmente en Login
        mAuth.signOut();
        Intent intent = new Intent(crear_cuenta.this, cuenta_creada.class);
        intent.putExtra(TipoCuenta.MODO, modo);
        intent.putExtra("correo", correo);
        startActivity(intent);
        finish();
    }

    /** Mensajes comprensibles para los errores más comunes al registrarse. */
    private String mensajeRegistro(Exception e) {
        if (e instanceof com.google.firebase.auth.FirebaseAuthUserCollisionException) {
            return getString(R.string.m_cuenta_existe);
        }
        if (e instanceof com.google.firebase.auth.FirebaseAuthWeakPasswordException) {
            return getString(R.string.m_debil);
        }
        if (e instanceof com.google.firebase.FirebaseNetworkException) {
            return getString(R.string.m_sin_conexion_largo);
        }
        return getString(R.string.m_no_creo_cuenta);
    }
}
