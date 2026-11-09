package com.example.intercambioupn;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * HU08 - Contactar al usuario de una publicación.
 * Muestra los mensajes de la conversación, permite escribir y enviar, y se refresca cada 3 segundos.
 * Desde aquí también se registra el intercambio/préstamo para el historial (HU16).
 */
public class chat extends BaseActivity {

    private static final long REFRESCO_MS = 3000;

    /** Un mensaje del chat. */
    private static class Mensaje {
        final int id;
        final String uid;
        final String texto;
        final String fecha;

        Mensaje(int id, String uid, String texto, String fecha) {
            this.id = id;
            this.uid = uid;
            this.texto = texto;
            this.fecha = fecha;
        }
    }

    private Conversacion conv;
    private String miUid;
    private RequestQueue rq;

    private final List<Mensaje> mensajes = new ArrayList<>();
    private Adaptador adaptador;
    private RecyclerView recycler;
    private EditText txtMensaje;
    private Button btnEnviar;
    private Button btnRegistrar;
    private TextView txtEstadoOperacion;
    private TextView txtSinMensajes;

    private int ultimoId = 0;
    private boolean cargando = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refresco = new Runnable() {
        @Override
        public void run() {
            cargarNuevos();
            handler.postDelayed(this, REFRESCO_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets teclado = insets.getInsets(WindowInsetsCompat.Type.ime());
            int margen = (int) (16 * v.getResources().getDisplayMetrics().density);
            // Con el teclado abierto, la caja de texto sube junto con él
            v.setPadding(barras.left + margen, barras.top + margen, barras.right + margen,
                    Math.max(barras.bottom, teclado.bottom) + margen);
            return insets;
        });

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        conv = (Conversacion) getIntent().getSerializableExtra("conversacion");
        if (user == null || conv == null) {
            Toast.makeText(this, R.string.m_no_abrio_chat, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        miUid = user.getUid();
        rq = Volley.newRequestQueue(this);

        ((TextView) findViewById(R.id.txtChatNombre)).setText(conv.otroNombre(miUid));
        String rol = getString(conv.soyPropietario(miUid) ? R.string.m_interesado_en_tu : R.string.m_dueno_material);
        ((TextView) findViewById(R.id.txtChatMaterial)).setText(
                rol + ": " + conv.materialNombre + (conv.materialTipo.isEmpty() ? "" : " · " + Catalogo.mostrar(this, conv.materialTipo)));

        txtMensaje = findViewById(R.id.txtMensajeChat);
        btnEnviar = findViewById(R.id.btnEnviarMensaje);
        btnRegistrar = findViewById(R.id.btnRegistrarOperacion);
        txtEstadoOperacion = findViewById(R.id.txtEstadoOperacion);
        txtSinMensajes = findViewById(R.id.txtSinMensajes);

        recycler = findViewById(R.id.recyclerMensajes);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        recycler.setLayoutManager(lm);
        adaptador = new Adaptador();
        recycler.setAdapter(adaptador);

        btnEnviar.setOnClickListener(v -> enviar());
        btnRegistrar.setOnClickListener(v -> confirmarRegistro());
        txtEstadoOperacion.setOnClickListener(v -> startActivity(new Intent(this, historial.class)));
        findViewById(R.id.btnRegresarChat).setOnClickListener(v -> finish());

        mostrarOperacion();
        refrescarOperacion();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(refresco);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(refresco);
    }

    // ===== Mensajes =====

    private void cargarNuevos() {
        if (cargando) return;
        cargando = true;

        String url = ApiChat.MENSAJES + "?conversacion_id=" + conv.id + "&desde=" + ultimoId;
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    cargando = false;
                    try {
                        JSONArray datos = response.getJSONArray("datos");
                        int antes = mensajes.size();
                        for (int i = 0; i < datos.length(); i++) {
                            JSONObject o = datos.getJSONObject(i);
                            int id = o.optInt("id", 0);
                            if (id <= ultimoId) continue;
                            mensajes.add(new Mensaje(id, Conversacion.texto(o, "uid_emisor"),
                                    Conversacion.texto(o, "texto"), Conversacion.texto(o, "fecha")));
                            ultimoId = id;
                        }
                        if (mensajes.size() > antes) {
                            adaptador.notifyItemRangeInserted(antes, mensajes.size() - antes);
                            recycler.scrollToPosition(mensajes.size() - 1);
                            Almacen.marcarVisto(this, conv.id, ultimoId);
                        }
                        txtSinMensajes.setVisibility(mensajes.isEmpty() ? View.VISIBLE : View.GONE);
                    } catch (JSONException e) {
                        // respuesta inesperada: se intenta de nuevo en el próximo refresco
                    }
                },
                error -> cargando = false);
        rq.add(Api.reintentos(req));
    }

    private void enviar() {
        String texto = txtMensaje.getText().toString().trim();
        if (texto.isEmpty()) return;
        btnEnviar.setEnabled(false);

        StringRequest req = new StringRequest(Request.Method.POST, ApiChat.ENVIAR,
                response -> {
                    btnEnviar.setEnabled(true);
                    txtMensaje.setText("");
                    cargarNuevos();
                },
                error -> {
                    btnEnviar.setEnabled(true);
                    Toast.makeText(this, R.string.m_no_envio_msj, Toast.LENGTH_SHORT).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("conversacion_id", conv.id);
                p.put("uid_emisor", miUid);
                p.put("texto", texto);
                return p;
            }
        };
        rq.add(ApiChat.sinReintento(req));
    }

    // ===== HU16 - Operación de intercambio/préstamo =====

    private void mostrarOperacion() {
        if (conv.operacionEstado == null || conv.operacionEstado.isEmpty()) {
            btnRegistrar.setText(conv.materialTipo.isEmpty() ? getString(R.string.m_registrar_acuerdo)
                    : getString(R.string.m_registrar_x, Catalogo.mostrar(this, conv.materialTipo).toLowerCase(Locale.getDefault())));
            btnRegistrar.setVisibility(View.VISIBLE);
            txtEstadoOperacion.setVisibility(View.GONE);
        } else {
            btnRegistrar.setVisibility(View.GONE);
            txtEstadoOperacion.setText(getString(R.string.m_estado_ver_historial, Catalogo.mostrar(this, conv.operacionEstado)));
            txtEstadoOperacion.setVisibility(View.VISIBLE);
        }
    }

    /** Consulta si esta conversación ya tiene una operación registrada. */
    private void refrescarOperacion() {
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET,
                Conversacion.url(ApiChat.LISTAR, "uid", miUid), null,
                response -> {
                    try {
                        for (Conversacion c : Conversacion.desdeRespuesta(response)) {
                            if (c.id.equals(conv.id)) {
                                conv.operacionEstado = c.operacionEstado;
                                mostrarOperacion();
                                break;
                            }
                        }
                    } catch (JSONException ignorada) {
                        // se deja como estaba
                    }
                },
                error -> { });
        rq.add(Api.reintentos(req));
    }

    private void confirmarRegistro() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.m_registrar_acuerdo)
                .setMessage(getString(R.string.m_registrar_msj, conv.materialNombre))
                .setNegativeButton(R.string.m_cancelar, null)
                .setPositiveButton(R.string.m_registrar, (d, w) -> registrarOperacion())
                .show();
    }

    private void registrarOperacion() {
        btnRegistrar.setEnabled(false);
        StringRequest req = new StringRequest(Request.Method.POST, ApiChat.OPERACION_REGISTRAR,
                response -> {
                    btnRegistrar.setEnabled(true);
                    try {
                        JSONObject o = new JSONObject(response);
                        if (o.optBoolean("ok")) {
                            conv.operacionEstado = o.optString("estado", "Pendiente");
                            mostrarOperacion();
                            Almacen.agregarNotificacion(getApplicationContext(), getString(R.string.m_n_intercambio),
                                    getString(R.string.m_n_acuerdo_registrado), getString(R.string.m_n_acuerdo_msj,
                                            conv.materialNombre, conv.otroNombre(miUid), Catalogo.mostrar(this, conv.operacionEstado)));
                            Toast.makeText(this, R.string.m_n_acuerdo_registrado, Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } catch (JSONException ignorada) {
                        // cae al mensaje de error
                    }
                    Toast.makeText(this, R.string.m_no_registro, Toast.LENGTH_SHORT).show();
                },
                error -> {
                    btnRegistrar.setEnabled(true);
                    Toast.makeText(this, R.string.m_no_registro, Toast.LENGTH_SHORT).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("conversacion_id", conv.id);
                p.put("uid", miUid);
                return p;
            }
        };
        rq.add(Api.reintentos(req));
    }

    // ===== Burbujas =====

    private class Adaptador extends RecyclerView.Adapter<Adaptador.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_item_mensaje, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            Mensaje m = mensajes.get(position);
            boolean mio = m.uid.equals(miUid);

            h.tvTexto.setText(m.texto);
            h.tvHora.setText(Conversacion.formatear(m.fecha, "HH:mm"));
            h.fila.setGravity(mio ? Gravity.END : Gravity.START);

            GradientDrawable fondo = new GradientDrawable();
            fondo.setCornerRadius(16 * getResources().getDisplayMetrics().density);
            fondo.setColor(getColor(mio ? R.color.burbuja_mia : R.color.burbuja_otro));
            h.burbuja.setBackground(fondo);
        }

        @Override
        public int getItemCount() {
            return mensajes.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final LinearLayout fila, burbuja;
            final TextView tvTexto, tvHora;

            Holder(@NonNull View itemView) {
                super(itemView);
                fila = itemView.findViewById(R.id.filaMensaje);
                burbuja = itemView.findViewById(R.id.burbujaMensaje);
                tvTexto = itemView.findViewById(R.id.txtTextoMensaje);
                tvHora = itemView.findViewById(R.id.txtHoraMensaje);
            }
        }
    }
}
