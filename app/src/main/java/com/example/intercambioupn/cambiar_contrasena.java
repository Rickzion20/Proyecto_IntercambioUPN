package com.example.intercambioupn;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.regex.Pattern;

/**
 * HU19 - Cambiar contraseña (Firebase Authentication).
 * Firebase exige un login reciente para cambiar la clave, por eso se pide la contraseña actual
 * y se reautentica antes de actualizar.
 */
public class cambiar_contrasena extends BaseActivity {

    // Mínimo 8 caracteres, 1 mayúscula, 1 minúscula, 1 número y 1 carácter especial
    private static final Pattern REQUISITOS = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");

    private EditText txtActual, txtNueva, txtConfirmar;
    private Button btnActualizar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cambiar_contrasena);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        txtActual = findViewById(R.id.txtContrasenaActual);
        txtNueva = findViewById(R.id.txtNuevaContrasena);
        txtConfirmar = findViewById(R.id.txtConfirmarNuevaContrasena);
        btnActualizar = findViewById(R.id.btnActualizarContrasena);
    }

    public void regresar(View view) {
        finish();
    }

    public void actualizarContrasena(View view) {
        String actual = txtActual.getText().toString();
        String nueva = txtNueva.getText().toString();
        String confirmar = txtConfirmar.getText().toString();

        if (TextUtils.isEmpty(actual) || TextUtils.isEmpty(nueva) || TextUtils.isEmpty(confirmar)) {
            toast(getString(R.string.m_cc_completa));
            return;
        }
        if (!REQUISITOS.matcher(nueva).matches()) {
            toast(getString(R.string.m_cc_requisitos));
            return;
        }
        if (!nueva.equals(confirmar)) {
            toast(getString(R.string.m_cc_confirmacion));
            return;
        }
        if (nueva.equals(actual)) {
            toast(getString(R.string.m_cc_distinta));
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) {
            toast(getString(R.string.m_cc_sin_sesion));
            return;
        }

        btnActualizar.setEnabled(false);

        AuthCredential credencial = EmailAuthProvider.getCredential(user.getEmail(), actual);
        user.reauthenticate(credencial).addOnCompleteListener(reauth -> {
            if (!reauth.isSuccessful()) {
                btnActualizar.setEnabled(true);
                toast(getString(R.string.m_cc_actual_mal));
                return;
            }

            user.updatePassword(nueva).addOnCompleteListener(upd -> {
                if (upd.isSuccessful()) {
                    Almacen.agregarNotificacion(getApplicationContext(), getString(R.string.m_n_seguridad),
                            getString(R.string.m_n_contrasena_titulo), getString(R.string.m_n_contrasena_msj));
                    // Se cierra la sesión para que entre con la nueva clave
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(this, contrasena_actualizada.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    btnActualizar.setEnabled(true);
                    toast(getString(R.string.m_cc_error));
                }
            });
        });
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
