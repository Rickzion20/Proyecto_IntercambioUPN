package com.example.intercambioupn;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
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
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONException;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * HU09 - Consultar conversaciones.
 * Lista los chats del usuario con buscador y las pestañas Todos, Con acuerdo y Favoritos.
 */
public class conversaciones extends BaseActivity {

    private final List<Conversacion> todas = new ArrayList<>();
    private final List<Conversacion> visibles = new ArrayList<>();
    private Adaptador adaptador;
    private RequestQueue rq;
    private String miUid;

    private EditText txtBuscar;
    private MaterialButtonToggleGroup grupoFiltro;
    private TextView txtMensaje;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_conversaciones);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int margen = (int) (24 * v.getResources().getDisplayMetrics().density); // 24dp, igual que el XML
            v.setPadding(systemBars.left + margen, systemBars.top + margen, systemBars.right + margen, systemBars.bottom + margen);
            return insets;
        });

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, R.string.m_inicia_msjs, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        miUid = user.getUid();
        rq = Volley.newRequestQueue(this);

        txtBuscar = findViewById(R.id.txtBuscarConversacion);
        grupoFiltro = findViewById(R.id.grupoFiltroChat);
        txtMensaje = findViewById(R.id.txtSinConversaciones);

        RecyclerView recycler = findViewById(R.id.recyclerConversaciones);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adaptador = new Adaptador();
        recycler.setAdapter(adaptador);

        txtBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) { filtrar(); }
        });
        grupoFiltro.addOnButtonCheckedListener((g, id, marcado) -> { if (marcado) filtrar(); });

        findViewById(R.id.btnRegresarConversaciones).setOnClickListener(v -> finish());
        findViewById(R.id.btnVerHistorial).setOnClickListener(v -> startActivity(new Intent(this, historial.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (miUid != null) cargar();
    }

    private void cargar() {
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET,
                Conversacion.url(ApiChat.LISTAR, "uid", miUid), null,
                response -> {
                    try {
                        todas.clear();
                        todas.addAll(Conversacion.desdeRespuesta(response));
                        filtrar();
                    } catch (JSONException e) {
                        Toast.makeText(this, R.string.m_error_leer_msjs, Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, getString(R.string.m_error_cargar_msjs, String.valueOf(error)), Toast.LENGTH_LONG).show());
        rq.add(Api.reintentos(req));
    }

    private void filtrar() {
        String termino = normalizar(txtBuscar.getText().toString().trim());
        int pestana = grupoFiltro.getCheckedButtonId();

        visibles.clear();
        for (Conversacion c : todas) {
            if (pestana == R.id.btnFiltroIntercambios && c.operacionEstado.isEmpty()) continue;
            if (pestana == R.id.btnFiltroFavoritos && !Almacen.esFavorito(this, c.materialId)) continue;
            if (!termino.isEmpty()
                    && !normalizar(c.otroNombre(miUid)).contains(termino)
                    && !normalizar(c.materialNombre).contains(termino)) continue;
            visibles.add(c);
        }
        adaptador.notifyDataSetChanged();

        if (visibles.isEmpty()) {
            txtMensaje.setText(todas.isEmpty()
                    ? getString(R.string.m_sin_conversaciones)
                    : getString(R.string.m_sin_conv_seccion));
            txtMensaje.setVisibility(View.VISIBLE);
        } else {
            txtMensaje.setVisibility(View.GONE);
        }
    }

    private static String normalizar(String s) {
        if (TextUtils.isEmpty(s)) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private class Adaptador extends RecyclerView.Adapter<Adaptador.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_item_conversacion, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            Conversacion c = visibles.get(position);
            String nombre = c.otroNombre(miUid);
            boolean noLeido = c.tieneNoLeidos(miUid, Almacen.ultimoVisto(conversaciones.this, c.id));

            h.tvInicial.setText(nombre.substring(0, 1).toUpperCase(Locale.ROOT));
            GradientDrawable circulo = new GradientDrawable();
            circulo.setShape(GradientDrawable.OVAL);
            circulo.setColor(getColor(R.color.amarillo));
            h.tvInicial.setBackground(circulo);

            h.tvNombre.setText(nombre);
            h.tvFecha.setText(Conversacion.formatear(c.fecha, "dd/MM HH:mm"));
            h.tvMaterial.setText(getString(R.string.m_sobre, c.materialNombre));

            String ultimo = c.ultimoMensaje.isEmpty() ? getString(R.string.m_sin_mensajes) : c.ultimoMensaje;
            if (!c.ultimoUid.isEmpty() && c.ultimoUid.equals(miUid)) ultimo = getString(R.string.m_tu_mensaje, ultimo);
            h.tvUltimo.setText(ultimo);
            h.tvUltimo.setTypeface(null, noLeido ? Typeface.BOLD : Typeface.NORMAL);
            h.punto.setVisibility(noLeido ? View.VISIBLE : View.GONE);

            if (c.operacionEstado.isEmpty()) {
                h.tvEstado.setVisibility(View.GONE);
            } else {
                h.tvEstado.setText(c.operacionEstado);
                h.tvEstado.setVisibility(View.VISIBLE);
            }

            h.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(conversaciones.this, chat.class);
                intent.putExtra("conversacion", c);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return visibles.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final TextView tvInicial, tvNombre, tvFecha, tvMaterial, tvUltimo, tvEstado, punto;

            Holder(@NonNull View itemView) {
                super(itemView);
                tvInicial = itemView.findViewById(R.id.txtInicialConversacion);
                tvNombre = itemView.findViewById(R.id.txtNombreConversacion);
                tvFecha = itemView.findViewById(R.id.txtFechaConversacion);
                tvMaterial = itemView.findViewById(R.id.txtMaterialConversacion);
                tvUltimo = itemView.findViewById(R.id.txtUltimoMensaje);
                tvEstado = itemView.findViewById(R.id.txtEstadoConversacion);
                punto = itemView.findViewById(R.id.puntoConversacion);
            }
        }
    }
}
