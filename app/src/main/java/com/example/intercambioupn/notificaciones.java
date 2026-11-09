package com.example.intercambioupn;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** HU20 - Consultar notificaciones: tipo, fecha y hora, y "marcar todas como leídas". */
public class notificaciones extends BaseActivity {

    private final List<Almacen.Notificacion> lista = new ArrayList<>();
    private Adaptador adaptador;
    private TextView txtSin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notificaciones);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int margen = (int) (24 * v.getResources().getDisplayMetrics().density); // 24dp, igual que el XML
            v.setPadding(systemBars.left + margen, systemBars.top + margen, systemBars.right + margen, systemBars.bottom + margen);
            return insets;
        });

        txtSin = findViewById(R.id.txtSinNotificaciones);
        RecyclerView recycler = findViewById(R.id.recyclerNotificaciones);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adaptador = new Adaptador();
        recycler.setAdapter(adaptador);

        recargar();
    }

    private void recargar() {
        lista.clear();
        lista.addAll(Almacen.notificaciones(this));
        adaptador.notifyDataSetChanged();
        txtSin.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
    }

    public void marcarLeidas(View view) {
        Almacen.marcarTodasLeidas(this);
        recargar();
    }

    public void regresar(View view) {
        finish();
    }

    private class Adaptador extends RecyclerView.Adapter<Adaptador.Holder> {

        private final SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("es", "PE"));

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.activity_item_notificacion, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            Almacen.Notificacion n = lista.get(position);

            h.tvTipo.setText(n.tipo);
            h.tvFecha.setText(formato.format(new Date(n.fecha)));
            h.tvTitulo.setText(n.titulo);
            h.tvMensaje.setText(n.mensaje);
            h.punto.setVisibility(n.leida ? View.INVISIBLE : View.VISIBLE);
            h.tarjeta.setCardBackgroundColor(getColor(n.leida ? R.color.superficie : R.color.amarillo_tenue));

            // Tocar una notificación la marca como leída
            h.itemView.setOnClickListener(v -> {
                if (!n.leida) {
                    Almacen.marcarLeida(notificaciones.this, n.id);
                    n.leida = true;
                    int pos = h.getBindingAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) notifyItemChanged(pos);
                }
            });
        }

        @Override
        public int getItemCount() {
            return lista.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final CardView tarjeta;
            final TextView tvTipo, tvFecha, tvTitulo, tvMensaje, punto;

            Holder(@NonNull View itemView) {
                super(itemView);
                tarjeta = itemView.findViewById(R.id.tarjetaNotificacion);
                tvTipo = itemView.findViewById(R.id.txtTipoNotificacion);
                tvFecha = itemView.findViewById(R.id.txtFechaNotificacion);
                tvTitulo = itemView.findViewById(R.id.txtTituloNotificacion);
                tvMensaje = itemView.findViewById(R.id.txtMensajeNotificacion);
                punto = itemView.findViewById(R.id.puntoNoLeida);
            }
        }
    }
}
