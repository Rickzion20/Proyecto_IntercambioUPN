package com.example.intercambioupn;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * HU07 - Consultar detalle de una publicación.
 * Si la publicación es del usuario logueado, se muestran además Editar / Eliminar (HU13, HU14).
 */
public class detalle_publicacion extends BaseActivity {

    private Material material;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detalle_publicacion);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        material = (Material) getIntent().getSerializableExtra("material");
        if (material == null) {
            Toast.makeText(this, R.string.m_no_cargo_pub, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ((TextView) findViewById(R.id.txtDetalleNombre)).setText(material.getNombre());
        ((TextView) findViewById(R.id.txtDetalleCategoria)).setText(Catalogo.mostrar(this, material.getCategoria()));
        ((TextView) findViewById(R.id.txtDetalleEstado)).setText(Catalogo.mostrar(this, material.getEstado()));
        ((TextView) findViewById(R.id.txtDetalleTipo)).setText(Catalogo.mostrar(this, material.getTipo()));
        ((TextView) findViewById(R.id.txtDetalleSede)).setText(material.getSede());
        ((TextView) findViewById(R.id.txtDetalleDescripcion)).setText(
                material.getDescripcion().isEmpty() ? getString(R.string.m_sin_descripcion) : material.getDescripcion());

        String nombre = material.getUsuarioNombre().isEmpty() ? getString(R.string.m_estudiante_upn) : material.getUsuarioNombre();
        String correo = material.getUsuarioCorreo().isEmpty() ? getString(R.string.m_correo_no_disp) : material.getUsuarioCorreo();
        ((TextView) findViewById(R.id.txtDetallePublicador)).setText(nombre);
        ((TextView) findViewById(R.id.txtDetalleCorreo)).setText(correo);

        cargarImagen(material.getUrlimagen(), findViewById(R.id.imgDetalle));

        // HU15 - Favoritos
        ImageView corazon = findViewById(R.id.btnFavoritoDetalle);
        MaterialAdapter.pintarCorazon(corazon, Almacen.esFavorito(this, material.getId()));
        corazon.setOnClickListener(v -> {
            boolean esFavorito = Almacen.alternarFavorito(this, material.getId());
            MaterialAdapter.pintarCorazon(corazon, esFavorito);
            Toast.makeText(this, esFavorito ? R.string.m_agregado_fav : R.string.m_quitado_fav,
                    Toast.LENGTH_SHORT).show();
        });

        boolean esMio = esPublicacionPropia();
        findViewById(R.id.layoutAccionesPropietario).setVisibility(esMio ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnContactar).setVisibility(esMio ? View.GONE : View.VISIBLE);

        // HU18 - Si el publicador ocultó su correo en Privacidad, no se muestra
        if (!esMio && !material.getUidUsuario().isEmpty()) {
            FirebaseDatabase.getInstance().getReference("Usuarios")
                    .child(material.getUidUsuario()).child("privacidad").child("mostrarCorreo")
                    .get().addOnSuccessListener(snap -> {
                        if (Boolean.FALSE.equals(snap.getValue(Boolean.class))) {
                            // El chat sigue disponible porque no muestra el correo
                            ((TextView) findViewById(R.id.txtDetalleCorreo)).setText(R.string.m_correo_oculto);
                        }
                    });
        }
    }

    private boolean esPublicacionPropia() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null
                && !material.getUidUsuario().isEmpty()
                && material.getUidUsuario().equals(user.getUid());
    }

    private void cargarImagen(String url, ImageView iv) {
        if (url == null || url.trim().isEmpty()) return;
        Volley.newRequestQueue(this).add(new ImageRequest(
                url,
                (Bitmap b) -> iv.setImageBitmap(b),
                0, 0,
                ImageView.ScaleType.CENTER_CROP,
                Bitmap.Config.RGB_565,
                error -> { /* se deja la imagen por defecto */ }
        ));
    }

    public void regresar(View view) {
        finish();
    }

    /**
     * HU08 - Contactar: abre (o crea) el chat con el dueño de la publicación.
     * Las publicaciones de ejemplo sin dueño en la app usan el correo como respaldo.
     */
    public void contactar(View view) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || material.getUidUsuario().isEmpty()) {
            contactarPorCorreo();
            return;
        }

        String miNombre = user.getDisplayName() != null && !user.getDisplayName().isEmpty()
                ? user.getDisplayName()
                : (user.getEmail() != null ? user.getEmail() : "Estudiante UPN");
        String nombreDueno = material.getUsuarioNombre().isEmpty() ? "Estudiante UPN" : material.getUsuarioNombre();
        View boton = findViewById(R.id.btnContactar);
        boton.setEnabled(false);

        StringRequest req = new StringRequest(Request.Method.POST, ApiChat.ABRIR,
                response -> {
                    boton.setEnabled(true);
                    try {
                        JSONObject o = new JSONObject(response);
                        if (o.optBoolean("ok")) {
                            Conversacion conv = new Conversacion(o.optString("id"), material.getId(),
                                    material.getNombre(), material.getTipo(), user.getUid(), miNombre,
                                    material.getUidUsuario(), nombreDueno, "", "", 0, "", "");
                            Intent intent = new Intent(this, chat.class);
                            intent.putExtra("conversacion", conv);
                            startActivity(intent);
                            return;
                        }
                    } catch (JSONException ignorada) {
                        // cae al mensaje de error
                    }
                    Toast.makeText(this, R.string.m_no_abrio_chat, Toast.LENGTH_SHORT).show();
                },
                error -> {
                    boton.setEnabled(true);
                    Toast.makeText(this, getString(R.string.m_no_abrio_chat_e, String.valueOf(error)), Toast.LENGTH_LONG).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("material_id", material.getId());
                p.put("material_nombre", material.getNombre());
                p.put("material_tipo", material.getTipo());
                p.put("uid_interesado", user.getUid());
                p.put("nombre_interesado", miNombre);
                p.put("uid_propietario", material.getUidUsuario());
                p.put("nombre_propietario", nombreDueno);
                return p;
            }
        };
        Volley.newRequestQueue(this).add(Api.reintentos(req));
    }

    /** Respaldo: abre el correo del propietario con un mensaje listo. */
    private void contactarPorCorreo() {
        String correo = material.getUsuarioCorreo();
        if (correo.isEmpty()) {
            Toast.makeText(this, R.string.m_sin_contacto, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + correo));
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.m_asunto_interes, material.getNombre()));
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.m_cuerpo_interes, material.getNombre()));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.m_sin_app_correo, Toast.LENGTH_SHORT).show();
        }
    }

    public void editar(View view) {
        Intent intent = new Intent(this, editar_publicacion.class);
        intent.putExtra("id", material.getId());
        intent.putExtra("nombre", material.getNombre());
        intent.putExtra("descripcion", material.getDescripcion());
        intent.putExtra("foto", material.getUrlimagen());
        intent.putExtra("categoria", material.getCategoria());
        intent.putExtra("tipo", material.getTipo());
        intent.putExtra("estado", material.getEstado());
        intent.putExtra("sede", material.getSede());
        startActivity(intent);
    }

    public void eliminar(View view) {
        Intent intent = new Intent(this, eliminar_publicacion.class);
        intent.putExtra("id", material.getId());
        intent.putExtra("nombre", material.getNombre());
        startActivity(intent);
    }
}
