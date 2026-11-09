package com.example.intercambioupn;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;

public class eliminar_publicacion extends BaseActivity {

    private TextView txtNombreMaterial;
    private String id = "";
    private String nombre = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_eliminar_publicacion);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        txtNombreMaterial = findViewById(R.id.txtNombreMaterial);

        Intent intent = getIntent();
        if (intent != null) {
            id = intent.getStringExtra("id") != null ? intent.getStringExtra("id") : "";
            nombre = intent.getStringExtra("nombre") != null ? intent.getStringExtra("nombre") : "";
            txtNombreMaterial.setText(nombre);
        }
    }

    public void regresar(View view) {
        finish();
    }

    public void confirmarEliminar(View view) {
        Eliminar(id);
    }

    public void Eliminar(final String id) {
        StringRequest request = new StringRequest(
                Request.Method.POST,
                Api.ELIMINAR,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Almacen.agregarNotificacion(getApplicationContext(), getString(R.string.m_n_publicacion),
                                getString(R.string.m_n_pub_eliminada), getString(R.string.m_n_retiraste, nombre));
                        Toast.makeText(eliminar_publicacion.this, R.string.m_elimino, Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(eliminar_publicacion.this, getString(R.string.m_error_eliminar, String.valueOf(error.getMessage())), Toast.LENGTH_SHORT).show();
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("id", id);
                // El servidor solo elimina si la publicación es de este usuario
                com.google.firebase.auth.FirebaseUser u = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                params.put("uid_usuario", u != null ? u.getUid() : "");
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(eliminar_publicacion.this);
        requestQueue.add(Api.reintentos(request));
    }
}