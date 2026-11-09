package com.example.intercambioupn;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public class recuperar_contrasena extends BaseActivity {

    private EditText txtCorreo;
    private Button btnEnviarSolicitud;
    private View btnRegresar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recuperar_contrasena);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        txtCorreo = findViewById(R.id.txtCorreo);
        btnEnviarSolicitud = findViewById(R.id.btnEnviarSolicitud);
        btnRegresar = findViewById(R.id.btnRegresar);

        if (btnRegresar != null) {
            btnRegresar.setOnClickListener(v -> finish());
        }

        if (btnEnviarSolicitud != null) {
            btnEnviarSolicitud.setOnClickListener(v -> recuperar());
        }
    }

    private void recuperar() {
        if (TextUtils.isEmpty(txtCorreo.getText().toString().trim())) {
            Toast.makeText(this, R.string.m_ingresa_correo, Toast.LENGTH_SHORT).show();
        } else {
            String emailAddress = txtCorreo.getText().toString().trim();
            mAuth.sendPasswordResetEmail(emailAddress)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(getApplicationContext(), R.string.m_recuperacion_enviada, Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(recuperar_contrasena.this, contrasena_actualizada.class);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(getApplicationContext(), R.string.m_recuperacion_error, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }
}
