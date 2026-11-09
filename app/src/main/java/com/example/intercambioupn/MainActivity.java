package com.example.intercambioupn;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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

/** HU04 - Consultar publicaciones y materiales (pantalla de inicio). */
public class MainActivity extends BaseActivity {

    public static ArrayList<Material> listaMateriales = new ArrayList<>();

    /** Cantidad de publicaciones que se muestran en "Publicaciones recientes". */
    private static final int MAX_RECIENTES = 5;

    private RequestQueue rq;
    private MaterialAdapter adaptador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rq = Volley.newRequestQueue(this);

        RecyclerView recycler = findViewById(R.id.recyclerPublicaciones);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adaptador = new MaterialAdapter(listaMateriales, rq, this::abrirDetalle);
        recycler.setAdapter(adaptador);

        // Categorías: abren la búsqueda ya filtrada por esa categoría
        findViewById(R.id.btnCategoriaLibros).setOnClickListener(v -> abrirCategoria("Libros"));
        findViewById(R.id.btnCategoriaApuntes).setOnClickListener(v -> abrirCategoria("Apuntes"));
        findViewById(R.id.btnCategoriaTecnologia).setOnClickListener(v -> abrirCategoria("Tecnología"));
        findViewById(R.id.btnCategoriaOtros).setOnClickListener(v -> abrirCategoria("Otros objetos"));

        // HU20 - Campana de notificaciones con contador de no leídas
        Almacen.sembrarNotificacionesIniciales(this);
        findViewById(R.id.btnNotificaciones).setOnClickListener(v -> startActivity(new Intent(this, notificaciones.class)));
        GradientDrawable fondoBadge = new GradientDrawable();
        fondoBadge.setShape(GradientDrawable.RECTANGLE);
        fondoBadge.setCornerRadius(100f);
        fondoBadge.setColor(getColor(R.color.rojo_favorito));
        // Borde blanco para que el contador se despegue del ícono
        fondoBadge.setStroke((int) (2 * getResources().getDisplayMetrics().density), getColor(R.color.fondo));
        findViewById(R.id.badgeNotificaciones).setBackground(fondoBadge);

        mostrarAvatar();

        // Barra inferior: Publicar, Favoritos (HU15) y Ajustes (HU21/HU22)
        findViewById(R.id.btnPublicarMaterial).setOnClickListener(v -> publicar(v));
        findViewById(R.id.btnFavoritos).setOnClickListener(v -> startActivity(new Intent(this, favoritos.class)));
        findViewById(R.id.btnConfiguracion).setOnClickListener(v -> startActivity(new Intent(this, ajustes.class)));
        findViewById(R.id.btnChat).setOnClickListener(v -> startActivity(new Intent(this, conversaciones.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarPublicaciones();
        actualizarBadge();
        revisarMensajesNuevos();
    }

    /** HU20 + HU09 - Crea una notificación "Mensaje" por cada chat con mensajes nuevos de la otra persona. */
    private void revisarMensajesNuevos() {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String miUid = user.getUid();

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET,
                Conversacion.url(ApiChat.LISTAR, "uid", miUid), null,
                response -> {
                    try {
                        for (Conversacion c : Conversacion.desdeRespuesta(response)) {
                            int yaConocido = Math.max(Almacen.ultimoVisto(this, c.id), Almacen.ultimoAvisado(this, c.id));
                            if (c.tieneNoLeidos(miUid, yaConocido)) {
                                Almacen.agregarNotificacion(getApplicationContext(), getString(R.string.m_n_mensaje),
                                        getString(R.string.m_n_nuevo_mensaje, c.otroNombre(miUid)),
                                        getString(R.string.m_n_sobre, c.materialNombre, c.ultimoMensaje));
                                Almacen.marcarAvisado(this, c.id, c.ultimoId);
                            }
                        }
                        actualizarBadge();
                    } catch (JSONException ignorada) {
                        // sin cambios
                    }
                },
                error -> { /* si el chat no responde, Inicio sigue funcionando igual */ });
        rq.add(Api.reintentos(req));
    }

    /** Avatar del encabezado: inicial del nombre (o del correo); si no hay, ícono de persona. */
    private void mostrarAvatar() {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        String base = "";
        if (user != null) {
            if (user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) base = user.getDisplayName().trim();
            else if (user.getEmail() != null) base = user.getEmail().trim();
        }
        TextView inicial = findViewById(R.id.txtInicialPerfil);
        View icono = findViewById(R.id.imgIconoPerfil);
        if (!base.isEmpty() && Character.isLetter(base.charAt(0))) {
            inicial.setText(base.substring(0, 1).toUpperCase(java.util.Locale.ROOT));
            inicial.setVisibility(View.VISIBLE);
            icono.setVisibility(View.GONE);
        } else {
            inicial.setVisibility(View.GONE);
            icono.setVisibility(View.VISIBLE);
        }
    }

    private void actualizarBadge() {
        TextView badge = findViewById(R.id.badgeNotificaciones);
        int n = Almacen.noLeidas(this);
        badge.setVisibility(n > 0 ? View.VISIBLE : View.GONE);
        badge.setText(n > 9 ? "9+" : String.valueOf(n));
    }

    private void abrirDetalle(Material material) {
        Intent intent = new Intent(this, detalle_publicacion.class);
        intent.putExtra("material", material);
        startActivity(intent);
    }

    private void abrirCategoria(String categoria) {
        Intent intent = new Intent(this, buscar_material.class);
        intent.putExtra("categoria", categoria);
        startActivity(intent);
    }

    public void perfil(View view) {
        startActivity(new Intent(this, perfil.class));
    }

    public void publicar(View view) {
        startActivity(new Intent(this, publicar_material.class));
    }

    public void buscar(View view) {
        startActivity(new Intent(this, buscar_material.class));
    }

    public void misPublicaciones(View view) {
        startActivity(new Intent(this, mis_publicaciones.class));
    }

    private void cargarPublicaciones() {
        JsonObjectRequest requerimiento = new JsonObjectRequest(
                Request.Method.GET,
                Api.MOSTRAR,
                null,
                response -> {
                    try {
                        listaMateriales.clear();
                        ArrayList<Material> todos = Material.desdeRespuesta(response); // ya vienen de la más nueva a la más antigua
                        listaMateriales.addAll(todos.subList(0, Math.min(MAX_RECIENTES, todos.size())));
                        adaptador.notifyDataSetChanged();
                    } catch (JSONException e) {
                        Toast.makeText(this, R.string.m_error_leer_pubs, Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, getString(R.string.m_error_cargar_pubs, String.valueOf(error)), Toast.LENGTH_LONG).show()
        );
        rq.add(Api.reintentos(requerimiento));
    }
}