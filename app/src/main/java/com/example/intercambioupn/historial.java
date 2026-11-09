package com.example.intercambioupn;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HU16 - Consultar historial de intercambios y préstamos.
 * Pestañas Todos y Pendientes; cada operación muestra material, persona, tipo, estado y fecha.
 * Tocar una operación permite cambiar su estado.
 */
public class historial extends BaseActivity {

    private static final String[] ESTADOS = {"Pendiente", "Prestado", "Completado", "Cancelado"};

    /** Una operación registrada desde el chat. */
    private static class Operacion {
        final String id, materialNombre, tipo, uidPropietario, nombrePropietario, nombreInteresado, fecha;
        String estado;

        Operacion(JSONObject o) {
            id = Conversacion.texto(o, "id");
            materialNombre = Conversacion.texto(o, "material_nombre");
            tipo = Conversacion.texto(o, "tipo");
            uidPropietario = Conversacion.texto(o, "uid_propietario");
            nombrePropietario = Conversacion.texto(o, "nombre_propietario");
            nombreInteresado = Conversacion.texto(o, "nombre_interesado");
            fecha = Conversacion.texto(o, "fecha_actualizacion");
            estado = Conversacion.texto(o, "estado");
        }

        /** Pendientes = aún no terminan (Pendiente o Prestado). */
        boolean esPendiente() {
            return "Pendiente".equals(estado) || "Prestado".equals(estado);
        }
    }

    private final List<Operacion> todas = new ArrayList<>();
    private final List<Operacion> visibles = new ArrayList<>();
    private Adaptador adaptador;
    private RequestQueue rq;
    private String miUid;
    private MaterialButtonToggleGroup grupo;
    private TextView txtMensaje;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_historial);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int margen = (int) (24 * v.getResources().getDisplayMetrics().density); // 24dp, igual que el XML
            v.setPadding(systemBars.left + margen, systemBars.top + margen, systemBars.right + margen, systemBars.bottom + margen);
            return insets;
        });

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, R.string.m_inicia_historial, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        miUid = user.getUid();
        rq = Volley.newRequestQueue(this);

        grupo = findViewById(R.id.grupoFiltroHistorial);
        txtMensaje = findViewById(R.id.txtSinOperaciones);

        RecyclerView recycler = findViewById(R.id.recyclerOperaciones);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adaptador = new Adaptador();
        recycler.setAdapter(adaptador);

        grupo.addOnButtonCheckedListener((g, id, marcado) -> { if (marcado) filtrar(); });
        findViewById(R.id.btnRegresarHistorial).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (miUid != null) cargar();
    }

    private void cargar() {
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET,
                Conversacion.url(ApiChat.OPERACIONES_LISTAR, "uid", miUid), null,
                response -> {
                    try {
                        JSONArray datos = response.getJSONArray("datos");
                        todas.clear();
                        for (int i = 0; i < datos.length(); i++) todas.add(new Operacion(datos.getJSONObject(i)));
                        filtrar();
                    } catch (JSONException e) {
                        Toast.makeText(this, R.string.m_error_leer_hist, Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, getString(R.string.m_error_cargar_hist, String.valueOf(error)), Toast.LENGTH_LONG).show());
        rq.add(Api.reintentos(req));
    }

    private void filtrar() {
        boolean soloPendientes = grupo.getCheckedButtonId() == R.id.btnFiltroPendientes;
        visibles.clear();
        for (Operacion o : todas) {
            if (soloPendientes && !o.esPendiente()) continue;
            visibles.add(o);
        }
        adaptador.notifyDataSetChanged();

        if (visibles.isEmpty()) {
            txtMensaje.setText(todas.isEmpty()
                    ? getString(R.string.m_sin_acuerdos)
                    : getString(R.string.m_sin_pendientes));
            txtMensaje.setVisibility(View.VISIBLE);
        } else {
            txtMensaje.setVisibility(View.GONE);
        }
    }

    private void elegirEstado(Operacion op) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.m_cambiar_estado_de, op.materialNombre))
                // En pantalla, traducidos; se guarda el valor en español
                .setItems(getResources().getStringArray(R.array.estados_acuerdo), (d, i) -> cambiarEstado(op, ESTADOS[i]))
                .setNegativeButton(R.string.m_cancelar, null)
                .show();
    }

    private void cambiarEstado(Operacion op, String nuevo) {
        if (nuevo.equals(op.estado)) return;

        StringRequest req = new StringRequest(Request.Method.POST, ApiChat.OPERACION_ESTADO,
                response -> {
                    Almacen.agregarNotificacion(getApplicationContext(), getString(R.string.m_n_intercambio),
                            getString(R.string.m_n_estado_actualizado), getString(R.string.m_n_estado_msj, op.materialNombre, Catalogo.mostrar(this, nuevo)));
                    Toast.makeText(this, R.string.m_n_estado_actualizado, Toast.LENGTH_SHORT).show();
                    cargar();
                },
                error -> Toast.makeText(this, R.string.m_no_actualizo_estado, Toast.LENGTH_SHORT).show()) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("id", op.id);
                p.put("uid", miUid);
                p.put("estado", nuevo);
                return p;
            }
        };
        rq.add(Api.reintentos(req));
    }

    /** Color de fondo de cada estado (con versión oscura en values-night). El estado siempre va escrito: no depende solo del color. */
    private int colorEstado(String estado) {
        switch (estado) {
            case "Prestado": return getColor(R.color.estado_prestado);
            case "Completado": return getColor(R.color.estado_completado);
            case "Cancelado": return getColor(R.color.estado_cancelado);
            default: return getColor(R.color.estado_pendiente);
        }
    }

    private class Adaptador extends RecyclerView.Adapter<Adaptador.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_item_operacion, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            Operacion o = visibles.get(position);
            boolean soyDueno = o.uidPropietario.equals(miUid);

            h.tvMaterial.setText(o.materialNombre);
            h.tvPersona.setText(soyDueno
                    ? getString(R.string.m_con_interesado, o.nombreInteresado)
                    : getString(R.string.m_con_dueno, o.nombrePropietario));
            h.tvTipo.setText(getString(R.string.m_modalidad_x, Catalogo.mostrar(historial.this, o.tipo)));
            h.tvFecha.setText(getString(R.string.m_actualizado_x, Conversacion.formatear(o.fecha, "dd/MM/yyyy HH:mm")));

            h.tvEstado.setText(Catalogo.mostrar(historial.this, o.estado));
            GradientDrawable fondo = new GradientDrawable();
            fondo.setCornerRadius(100f);
            fondo.setColor(colorEstado(o.estado));
            h.tvEstado.setBackground(fondo);

            h.itemView.setOnClickListener(v -> elegirEstado(o));
        }

        @Override
        public int getItemCount() {
            return visibles.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final TextView tvMaterial, tvPersona, tvTipo, tvFecha, tvEstado;

            Holder(@NonNull View itemView) {
                super(itemView);
                tvMaterial = itemView.findViewById(R.id.txtMaterialOperacion);
                tvPersona = itemView.findViewById(R.id.txtPersonaOperacion);
                tvTipo = itemView.findViewById(R.id.txtTipoOperacion);
                tvFecha = itemView.findViewById(R.id.txtFechaOperacion);
                tvEstado = itemView.findViewById(R.id.txtEstadoOperacionItem);
            }
        }
    }
}
