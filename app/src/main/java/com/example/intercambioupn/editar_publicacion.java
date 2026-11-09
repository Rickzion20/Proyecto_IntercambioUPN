package com.example.intercambioupn;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
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

public class editar_publicacion extends BaseActivity {

    private EditText txtNombreMaterial;
    private EditText txtDescripcionMaterial;
    private EditText txtUrlImagen;

    private Spinner spCategoriaMaterial;
    private Spinner spTipoIntercambio;
    private Spinner spEstadoMaterial;
    private Spinner spSedeMaterial;

    private String id = "";
    private String nombre = "";
    private String descripcion = "";
    private String foto = "";
    private String categoria = "";
    private String tipo = "";
    private String estado = "";
    private String sede = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_editar_publicacion);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        txtNombreMaterial = findViewById(R.id.txtNombreMaterial);
        txtDescripcionMaterial = findViewById(R.id.txtDescripcionMaterial);
        txtUrlImagen = findViewById(R.id.txtUrlImagen);

        spCategoriaMaterial = findViewById(R.id.spCategoriaMaterial);
        spTipoIntercambio = findViewById(R.id.spTipoIntercambio);
        spEstadoMaterial = findViewById(R.id.spEstadoMaterial);
        spSedeMaterial = findViewById(R.id.spSedeMaterial);

        Intent intent = getIntent();
        if (intent != null) {
            id = intent.getStringExtra("id") != null ? intent.getStringExtra("id") : "";
            nombre = intent.getStringExtra("nombre") != null ? intent.getStringExtra("nombre") : "";
            descripcion = intent.getStringExtra("descripcion") != null ? intent.getStringExtra("descripcion") : "";
            foto = intent.getStringExtra("foto") != null ? intent.getStringExtra("foto") : "";
            categoria = intent.getStringExtra("categoria") != null ? intent.getStringExtra("categoria") : "";
            tipo = intent.getStringExtra("tipo") != null ? intent.getStringExtra("tipo") : "";
            estado = intent.getStringExtra("estado") != null ? intent.getStringExtra("estado") : "";
            sede = intent.getStringExtra("sede") != null ? intent.getStringExtra("sede") : "";

            txtNombreMaterial.setText(nombre);
            txtDescripcionMaterial.setText(descripcion);
            txtUrlImagen.setText(foto);

            setSpinnerSelection(spCategoriaMaterial, categoria);
            setSpinnerSelection(spTipoIntercambio, tipo);
            setSpinnerSelection(spEstadoMaterial, estado);
            setSpinnerSelection(spSedeMaterial, sede);
        }
    }

    /** El valor viene en español (base de datos); se busca su posición en el arreglo del spinner. */
    private void setSpinnerSelection(Spinner spinner, String value) {
        if (spinner == null || value == null || value.isEmpty()) return;
        int i = Catalogo.posicion(this, arregloDe(spinner), value);
        if (i >= 0 && spinner.getAdapter() != null && i < spinner.getAdapter().getCount()) spinner.setSelection(i);
    }

    private int arregloDe(Spinner sp) {
        if (sp == spCategoriaMaterial) return R.array.categorias_material;
        if (sp == spTipoIntercambio) return R.array.tipos_intercambio;
        if (sp == spEstadoMaterial) return R.array.estados_material;
        return R.array.sedes_universitarias;
    }

    /** Texto elegido en pantalla → valor en español para guardar. */
    private String valorGuardado(Spinner sp) {
        return sp != null && sp.getSelectedItem() != null
                ? Catalogo.guardar(this, arregloDe(sp), sp.getSelectedItem().toString()) : "";
    }

    public void regresar(View view) {
        finish();
    }

    public void irEliminarPublicacion(View view) {
        Intent intent = new Intent(editar_publicacion.this, eliminar_publicacion.class);
        intent.putExtra("id", id);
        intent.putExtra("nombre", txtNombreMaterial.getText().toString());
        startActivity(intent);
        finish();
    }

    public void actualizarPublicacion(View view) {
        Update();
    }

    public void Update() {
        String p_nombre = txtNombreMaterial.getText().toString().trim();
        String p_descripcion = txtDescripcionMaterial.getText().toString().trim();
        String p_imagen_url = txtUrlImagen.getText().toString().trim();

        String p_categoria = valorGuardado(spCategoriaMaterial);
        String p_tipo = valorGuardado(spTipoIntercambio);
        String p_estado = valorGuardado(spEstadoMaterial);
        String p_sede = spSedeMaterial != null && spSedeMaterial.getSelectedItem() != null ? spSedeMaterial.getSelectedItem().toString() : "";

        if (p_nombre.isEmpty()) {
            Toast.makeText(this, R.string.m_ingrese_nombre, Toast.LENGTH_SHORT).show();
        } else if (p_descripcion.isEmpty()) {
            Toast.makeText(this, R.string.m_ingrese_descripcion, Toast.LENGTH_SHORT).show();
        } else if (p_imagen_url.isEmpty()) {
            Toast.makeText(this, R.string.m_ingrese_url, Toast.LENGTH_SHORT).show();
        } else if (!p_imagen_url.startsWith("http://") && !p_imagen_url.startsWith("https://")) {
            Toast.makeText(this, R.string.m_url_http, Toast.LENGTH_SHORT).show();
        } else if (spCategoriaMaterial.getSelectedItemPosition() <= 0) {
            Toast.makeText(this, R.string.m_seleccione_categoria, Toast.LENGTH_SHORT).show();
        } else if (spTipoIntercambio.getSelectedItemPosition() <= 0) {
            Toast.makeText(this, R.string.m_seleccione_modalidad, Toast.LENGTH_SHORT).show();
        } else if (spEstadoMaterial.getSelectedItemPosition() <= 0) {
            Toast.makeText(this, R.string.m_seleccione_estado, Toast.LENGTH_SHORT).show();
        } else if (spSedeMaterial != null && spSedeMaterial.getSelectedItemPosition() <= 0) {
            Toast.makeText(this, R.string.m_seleccione_sede, Toast.LENGTH_SHORT).show();
        } else {
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setMessage(getString(R.string.m_actualizando));
            progressDialog.show();

            StringRequest request = new StringRequest(
                    Request.Method.POST,
                    Api.ACTUALIZAR,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            if (progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            Almacen.agregarNotificacion(getApplicationContext(), getString(R.string.m_n_publicacion),
                                    getString(R.string.m_n_pub_actualizada), getString(R.string.m_n_cambios_guardados, p_nombre));
                            Toast.makeText(editar_publicacion.this, R.string.m_actualizo, Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivity(intent);
                            finish();
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            if (progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            Toast.makeText(editar_publicacion.this, getString(R.string.m_error_e, String.valueOf(error.getMessage())), Toast.LENGTH_SHORT).show();
                        }
                    }
            ) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    Map<String, String> params = new HashMap<>();
                    params.put("id", id);
                    params.put("nombre_material", p_nombre);
                    params.put("nombre", p_nombre);
                    params.put("descripcion", p_descripcion);
                    params.put("foto", p_imagen_url);
                    params.put("categoria", p_categoria);
                    params.put("tipo", p_tipo);
                    params.put("estado", p_estado);
                    params.put("sede", p_sede);
                    // El servidor solo actualiza si la publicación es de este usuario
                    com.google.firebase.auth.FirebaseUser u = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                    params.put("uid_usuario", u != null ? u.getUid() : "");
                    return params;
                }
            };

            RequestQueue requestQueue = Volley.newRequestQueue(editar_publicacion.this);
            requestQueue.add(Api.reintentos(request));
        }
    }
}