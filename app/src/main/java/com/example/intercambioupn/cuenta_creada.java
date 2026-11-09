package com.example.intercambioupn;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class cuenta_creada extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cuenta_creada);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Estudiantes UPN: recordar que deben verificar el correo antes del primer ingreso
        if (TipoCuenta.UPN.equals(getIntent().getStringExtra(TipoCuenta.MODO))) {
            String correo = getIntent().getStringExtra("correo");
            ((android.widget.TextView) findViewById(R.id.txtMensajeCuentaCreada)).setText(
                    getString(R.string.m_verificacion_enviada, correo != null ? correo : getString(R.string.m_tu_correo_upn)));
        }
    }

    public void irIniciarSesion(View view) {
        FirebaseAuth.getInstance().signOut();
        // Pila limpia: selección de acceso debajo y el login de la misma modalidad encima
        Intent seleccion = TipoCuenta.irASeleccion(this);
        Intent login = new Intent(cuenta_creada.this, login.class);
        login.putExtra(TipoCuenta.MODO, getIntent().getStringExtra(TipoCuenta.MODO));
        startActivities(new Intent[]{seleccion, login});
        finish();
    }
}
