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

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Set;

/** HU15 - Gestionar favoritos: lista los materiales que el usuario marcó con el corazón. */
public class favoritos extends BaseActivity {

    private final ArrayList<Material> lista = new ArrayList<>();
    private MaterialAdapter adaptador;
    private RequestQueue rq;
    private TextView txtMensaje;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_favoritos);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int margen = (int) (24 * v.getResources().getDisplayMetrics().density); // 24dp, igual que el XML
            v.setPadding(systemBars.left + margen, systemBars.top + margen, systemBars.right + margen, systemBars.bottom + margen);
            return insets;
        });

        rq = Volley.newRequestQueue(this);
        txtMensaje = findViewById(R.id.txtSinFavoritos);

        RecyclerView recycler = findViewById(R.id.recyclerFavoritos);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adaptador = new MaterialAdapter(lista, rq, m -> {
            Intent intent = new Intent(this, detalle_publicacion.class);
            intent.putExtra("material", m);
            startActivity(intent);
        });
        // Al quitar el corazón, la publicación sale de esta lista
        adaptador.setOnFavoritoCambiado((m, esFavorito) -> {
            if (!esFavorito) {
                lista.remove(m);
                adaptador.notifyDataSetChanged();
                actualizarMensaje();
            }
        });
        recycler.setAdapter(adaptador);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarFavoritos();
    }

    private void cargarFavoritos() {
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                Api.MOSTRAR,
                null,
                response -> {
                    try {
                        Set<String> ids = Almacen.favoritos(this);
                        lista.clear();
                        for (Material m : Material.desdeRespuesta(response)) {
                            if (ids.contains(m.getId())) lista.add(m);
                        }
                        adaptador.notifyDataSetChanged();
                        actualizarMensaje();
                    } catch (JSONException e) {
                        Toast.makeText(this, R.string.m_error_leer_fav, Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, getString(R.string.m_error_cargar_fav, String.valueOf(error)), Toast.LENGTH_LONG).show()
        );
        rq.add(Api.reintentos(req));
    }

    private void actualizarMensaje() {
        txtMensaje.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
    }

    public void regresar(View view) {
        finish();
    }
}
