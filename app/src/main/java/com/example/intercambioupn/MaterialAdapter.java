package com.example.intercambioupn;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageRequest;

import java.util.List;

/**
 * Adaptador compartido por Inicio, Buscar, Mis publicaciones y Favoritos.
 * Usa el layout activity_item_publicacion.
 */
public class MaterialAdapter extends RecyclerView.Adapter<MaterialAdapter.Holder> {

    public interface OnMaterialClick {
        void onClick(Material material);
    }

    /** Se avisa cuando el usuario marca o desmarca el corazón de una tarjeta (HU15). */
    public interface OnFavoritoCambiado {
        void onCambio(Material material, boolean esFavorito);
    }

    private OnFavoritoCambiado favoritoListener;

    public void setOnFavoritoCambiado(OnFavoritoCambiado listener) {
        this.favoritoListener = listener;
    }

    /** Corazón relleno (rojo) si es favorito, vacío si no. Lo usa también la pantalla de detalle. */
    public static void pintarCorazon(ImageView corazon, boolean esFavorito) {
        corazon.setImageResource(esFavorito ? R.drawable.ic_corazon_lleno : R.drawable.ic_corazon_vacio);
        corazon.setContentDescription(corazon.getContext().getString(esFavorito ? R.string.m_quitar_fav : R.string.m_agregar_fav));
    }

    private final List<Material> items;
    private final RequestQueue rq;
    private final OnMaterialClick listener;

    public MaterialAdapter(List<Material> items, RequestQueue rq, OnMaterialClick listener) {
        this.items = items;
        this.rq = rq;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_item_publicacion, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Material m = items.get(position);

        h.tvNombre.setText(m.getNombre());
        h.tvDescripcion.setText(m.getDescripcion());
        Context c0 = h.itemView.getContext();
        h.tvCategoria.setText(c0.getString(R.string.m_categoria_x, Catalogo.mostrar(c0, m.getCategoria())));
        h.tvTipo.setText(c0.getString(R.string.m_modalidad_x, Catalogo.mostrar(c0, m.getTipo())));
        h.tvEstado.setText(m.getEstado());
        h.tvSede.setText(m.getSede());

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(m);
        });

        cargarImagen(m.getUrlimagen(), h.ivFoto);

        // HU15 - Favoritos
        Context ctx = h.itemView.getContext();
        pintarCorazon(h.tvFavorito, Almacen.esFavorito(ctx, m.getId()));
        h.tvFavorito.setOnClickListener(v -> {
            boolean esFavorito = Almacen.alternarFavorito(ctx, m.getId());
            pintarCorazon(h.tvFavorito, esFavorito);
            Toast.makeText(ctx, esFavorito ? R.string.m_agregado_fav : R.string.m_quitado_fav,
                    Toast.LENGTH_SHORT).show();
            if (favoritoListener != null) favoritoListener.onCambio(m, esFavorito);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void cargarImagen(String url, ImageView iv) {
        iv.setTag(url);
        iv.setImageResource(R.drawable.ic_launcher_foreground);

        if (url == null || url.trim().isEmpty()) return;

        ImageRequest peticion = new ImageRequest(
                url,
                (Bitmap response) -> {
                    // Evita poner la imagen en una fila que ya fue reciclada
                    if (url.equals(iv.getTag())) iv.setImageBitmap(response);
                },
                0,
                0,
                ImageView.ScaleType.CENTER_CROP,
                Bitmap.Config.RGB_565,
                error -> {
                    if (url.equals(iv.getTag())) iv.setImageResource(R.drawable.ic_launcher_foreground);
                }
        );
        rq.add(peticion);
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView tvNombre, tvDescripcion, tvCategoria, tvTipo, tvEstado, tvSede;
        final ImageView ivFoto, tvFavorito;

        Holder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.txtNombreMaterial);
            tvDescripcion = itemView.findViewById(R.id.txtDescripcion);
            tvCategoria = itemView.findViewById(R.id.txtCategoria);
            tvTipo = itemView.findViewById(R.id.txtTipo);
            tvEstado = itemView.findViewById(R.id.txtEstado);
            tvSede = itemView.findViewById(R.id.txtSede);
            tvFavorito = itemView.findViewById(R.id.btnFavorito);
            ivFoto = itemView.findViewById(R.id.imgMaterial);
        }
    }
}
