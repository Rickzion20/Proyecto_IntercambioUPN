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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONException;

import java.util.ArrayList;

/** HU12 - Mis publicaciones: solo las del usuario logueado. Desde aquí se edita o elimina (HU13, HU14). */
public class mis_publicaciones extends BaseActivity {

    public static ArrayList<Material> listaMisMateriales = new ArrayList<>();

    private RequestQueue rq;
    private MaterialAdapter adaptador;
    private TextView txtSinPublicaciones;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mis_publicaciones);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int margen = (int) (24 * v.getResources().getDisplayMetrics().density); // 24dp, igual que el XML
            v.setPadding(systemBars.left + margen, systemBars.top + margen, systemBars.right + margen, systemBars.bottom + margen);
            return insets;
        });

        rq = Volley.newRequestQueue(this);
        txtSinPublicaciones = findViewById(R.id.txtSinPublicaciones);

        RecyclerView recycler = findViewById(R.id.recyclerMisPublicaciones);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adaptador = new MaterialAdapter(listaMisMateriales, rq, this::abrirEdicion);
        recycler.setAdapter(adaptador);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarMisPublicaciones();
    }

    public void regresar(View view) {
        finish();
    }

    public void nuevaPublicacion(View view) {
        startActivity(new Intent(this, publicar_material.class));
    }

    private void abrirEdicion(Material m) {
        Intent intent = new Intent(this, editar_publicacion.class);
        intent.putExtra("id", m.getId());
        intent.putExtra("nombre", m.getNombre());
        intent.putExtra("descripcion", m.getDescripcion());
        intent.putExtra("foto", m.getUrlimagen());
        intent.putExtra("categoria", m.getCategoria());
        intent.putExtra("tipo", m.getTipo());
        intent.putExtra("estado", m.getEstado());
        intent.putExtra("sede", m.getSede());
        startActivity(intent);
    }

    private void cargarMisPublicaciones() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, R.string.m_inicia_pubs, Toast.LENGTH_SHORT).show();
            return;
        }
        final String uid = user.getUid();

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                Api.MOSTRAR,
                null,
                response -> {
                    try {
                        listaMisMateriales.clear();
                        for (Material m : Material.desdeRespuesta(response)) {
                            if (uid.equals(m.getUidUsuario())) listaMisMateriales.add(m);
                        }
                        adaptador.notifyDataSetChanged();
                        txtSinPublicaciones.setVisibility(listaMisMateriales.isEmpty() ? View.VISIBLE : View.GONE);
                    } catch (JSONException e) {
                        Toast.makeText(this, R.string.m_error_leer_pubs2, Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, getString(R.string.m_error_cargar, String.valueOf(error)), Toast.LENGTH_LONG).show()
        );
        rq.add(Api.reintentos(req));
    }
}