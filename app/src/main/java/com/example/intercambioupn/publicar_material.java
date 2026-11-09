package com.example.intercambioupn;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HU10 - Publicar material.
 * Formulario con vista previa de la foto, validación en cada campo y botón fijo abajo.
 */
public class publicar_material extends BaseActivity {

    private EditText txtNombreMaterial, txtDescripcionMaterial, txtUrlImagen;
    private MaterialAutoCompleteTextView spCategoriaMaterial, spSedeMaterial;
    private TextInputLayout tilUrl, tilNombre, tilDescripcion, tilCategoria, tilSede;
    private ChipGroup chipsTipo, chipsEstado;
    private TextView txtErrorTipo, txtErrorEstado, txtEstadoFoto;
    private ImageView imgVistaPrevia;
    private View layoutSinFoto;
    private MaterialButton btnPublicar;
    private NestedScrollView scroll;

    private RequestQueue rq;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable cargaPendiente;

    // Datos del autor (se completan con el perfil de Firebase si existe)
    private String autorNombre = "";
    private boolean publicando = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_publicar_material);

        // Barras del sistema + teclado: el botón "Publicar" sube junto con el teclado
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets teclado = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(barras.left, barras.top, barras.right, Math.max(barras.bottom, teclado.bottom));
            return insets;
        });

        rq = Volley.newRequestQueue(this);

        txtNombreMaterial = findViewById(R.id.txtNombreMaterial);
        txtDescripcionMaterial = findViewById(R.id.txtDescripcionMaterial);
        txtUrlImagen = findViewById(R.id.txtUrlImagen);
        spCategoriaMaterial = findViewById(R.id.spCategoriaMaterial);
        spSedeMaterial = findViewById(R.id.spSedeMaterial);
        tilUrl = findViewById(R.id.tilUrl);
        tilNombre = findViewById(R.id.tilNombre);
        tilDescripcion = findViewById(R.id.tilDescripcion);
        tilCategoria = findViewById(R.id.tilCategoria);
        tilSede = findViewById(R.id.tilSede);
        chipsTipo = findViewById(R.id.chipsTipo);
        chipsEstado = findViewById(R.id.chipsEstado);
        txtErrorTipo = findViewById(R.id.txtErrorTipo);
        txtErrorEstado = findViewById(R.id.txtErrorEstado);
        txtEstadoFoto = findViewById(R.id.txtEstadoFoto);
        imgVistaPrevia = findViewById(R.id.imgVistaPrevia);
        layoutSinFoto = findViewById(R.id.layoutSinFoto);
        btnPublicar = findViewById(R.id.btnGuardarPublicacion);
        scroll = findViewById(R.id.scrollPublicar);

        // La primera opción de cada arreglo ("Todas las...", "Todos") solo sirve para filtrar: se omite
        spCategoriaMaterial.setSimpleItems(sinPrimera(R.array.categorias_material));
        spSedeMaterial.setSimpleItems(sinPrimera(R.array.sedes_universitarias));
        crearChips(chipsTipo, sinPrimera(R.array.tipos_intercambio));
        crearChips(chipsEstado, sinPrimera(R.array.estados_material));

        // Al corregir un campo, su error desaparece
        limpiarErrorAlEscribir(txtNombreMaterial, tilNombre);
        limpiarErrorAlEscribir(txtDescripcionMaterial, tilDescripcion);
        spCategoriaMaterial.setOnItemClickListener((p, v, pos, id) -> tilCategoria.setError(null));
        spSedeMaterial.setOnItemClickListener((p, v, pos, id) -> tilSede.setError(null));
        chipsTipo.setOnCheckedStateChangeListener((g, ids) -> txtErrorTipo.setVisibility(View.GONE));
        chipsEstado.setOnCheckedStateChangeListener((g, ids) -> txtErrorEstado.setVisibility(View.GONE));

        // Vista previa: se carga la imagen poco después de dejar de escribir el enlace
        txtUrlImagen.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                tilUrl.setError(null);
                if (cargaPendiente != null) handler.removeCallbacks(cargaPendiente);
                cargaPendiente = () -> cargarVistaPrevia(s.toString().trim());
                handler.postDelayed(cargaPendiente, 700);
            }
        });
        // Tocar la tarjeta de la foto lleva al campo del enlace
        findViewById(R.id.cardVistaPrevia).setOnClickListener(v -> {
            txtUrlImagen.requestFocus();
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(txtUrlImagen, 0);
        });

        precargarDatosDelPerfil();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    // ---------------------------------------------------------------- Interfaz

    private String[] sinPrimera(int arrayId) {
        String[] todas = getResources().getStringArray(arrayId);
        return Arrays.copyOfRange(todas, 1, todas.length);
    }

    private void crearChips(ChipGroup grupo, String[] opciones) {
        LayoutInflater inflater = LayoutInflater.from(this);
        for (String opcion : opciones) {
            Chip chip = (Chip) inflater.inflate(R.layout.item_chip_opcion, grupo, false);
            chip.setId(View.generateViewId());
            chip.setText(opcion);
            grupo.addView(chip);
        }
    }

    private static void limpiarErrorAlEscribir(EditText campo, TextInputLayout til) {
        campo.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { til.setError(null); }
        });
    }

    private void cargarVistaPrevia(String url) {
        if (url.isEmpty()) {
            mostrarSinFoto(getString(R.string.m_pega_enlace));
            return;
        }
        if (!esUrlValida(url)) {
            mostrarSinFoto(getString(R.string.m_enlace_https));
            return;
        }
        mostrarSinFoto(getString(R.string.m_cargando_vista));
        rq.cancelAll("vista_previa");
        ImageRequest peticion = new ImageRequest(
                url,
                (Bitmap bmp) -> {
                    // Solo si el enlace no cambió mientras cargaba
                    if (!url.equals(txtUrlImagen.getText().toString().trim())) return;
                    imgVistaPrevia.setImageBitmap(bmp);
                    imgVistaPrevia.setVisibility(View.VISIBLE);
                    layoutSinFoto.setVisibility(View.GONE);
                },
                1080, 1080,
                ImageView.ScaleType.CENTER_CROP,
                Bitmap.Config.RGB_565,
                error -> {
                    if (url.equals(txtUrlImagen.getText().toString().trim())) {
                        mostrarSinFoto(getString(R.string.m_no_cargo_imagen));
                    }
                });
        peticion.setTag("vista_previa");
        rq.add(peticion);
    }

    private void mostrarSinFoto(String mensaje) {
        imgVistaPrevia.setVisibility(View.GONE);
        layoutSinFoto.setVisibility(View.VISIBLE);
        txtEstadoFoto.setText(mensaje);
    }

    /** Sede por defecto = la del perfil; y el nombre completo del autor si no hay displayName. */
    private void precargarDatosDelPerfil() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        if (user.getDisplayName() != null) autorNombre = user.getDisplayName();

        FirebaseDatabase.getInstance().getReference("Usuarios").child(user.getUid()).get()
                .addOnSuccessListener(snap -> {
                    if (!snap.exists()) return;
                    String sede = snap.child("sede").getValue(String.class);
                    List<String> sedes = Arrays.asList(sinPrimera(R.array.sedes_universitarias));
                    if (sede != null && sedes.contains(sede)
                            && spSedeMaterial.getText().toString().isEmpty()) {
                        spSedeMaterial.setText(sede, false);
                    }
                    if (autorNombre.isEmpty()) {
                        String n = snap.child("nombres").getValue(String.class);
                        String a = snap.child("apellidos").getValue(String.class);
                        autorNombre = ((n != null ? n : "") + " " + (a != null ? a : "")).trim();
                    }
                });
    }

    // ---------------------------------------------------------------- Acciones

    public void regresar(View view) {
        finish();
    }

    public void publicar_material(View view) {
        insert();
    }

    public void insert() {
        if (publicando) return;

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            Toast.makeText(this, R.string.m_inicia_publicar, Toast.LENGTH_SHORT).show();
            return;
        }

        final String p_nombre = txtNombreMaterial.getText().toString().trim();
        final String p_descripcion = txtDescripcionMaterial.getText().toString().trim();
        final String p_foto = txtUrlImagen.getText().toString().trim();
        final String p_categoria = Catalogo.guardar(this, R.array.categorias_material, spCategoriaMaterial.getText().toString().trim());
        final String p_sede = spSedeMaterial.getText().toString().trim();
        final String p_tipo = Catalogo.guardar(this, R.array.tipos_intercambio, textoChipElegido(chipsTipo));
        final String p_estado = Catalogo.guardar(this, R.array.estados_material, textoChipElegido(chipsEstado));

        // Validación: marca todos los campos con error y lleva al primero
        View primerError = null;
        if (p_foto.isEmpty()) {
            tilUrl.setError(getString(R.string.m_agrega_foto));
            primerError = tilUrl;
        } else if (!esUrlValida(p_foto)) {
            tilUrl.setError(getString(R.string.m_enlace_http));
            primerError = tilUrl;
        }
        if (p_nombre.isEmpty()) {
            tilNombre.setError(getString(R.string.m_escribe_nombre));
            if (primerError == null) primerError = tilNombre;
        }
        if (p_categoria.isEmpty()) {
            tilCategoria.setError(getString(R.string.m_elige_categoria));
            if (primerError == null) primerError = tilCategoria;
        }
        if (p_descripcion.isEmpty()) {
            tilDescripcion.setError(getString(R.string.m_agrega_descripcion));
            if (primerError == null) primerError = tilDescripcion;
        }
        if (p_tipo.isEmpty()) {
            txtErrorTipo.setVisibility(View.VISIBLE);
            if (primerError == null) primerError = chipsTipo;
        }
        if (p_estado.isEmpty()) {
            txtErrorEstado.setVisibility(View.VISIBLE);
            if (primerError == null) primerError = chipsEstado;
        }
        if (p_sede.isEmpty()) {
            tilSede.setError(getString(R.string.m_elige_sede));
            if (primerError == null) primerError = tilSede;
        }
        if (primerError != null) {
            final View destino = primerError;
            scroll.post(() -> scroll.smoothScrollTo(0, Math.max(0, destino.getTop() - 120)));
            Toast.makeText(this, R.string.m_revisa_campos, Toast.LENGTH_SHORT).show();
            return;
        }

        final String uid = usuario.getUid();
        final String usuarioNombre = autorNombre;
        final String usuarioCorreo = usuario.getEmail() != null ? usuario.getEmail() : "";

        ponerPublicando(true);

        StringRequest request = new StringRequest(
                Request.Method.POST,
                Api.INSERTAR,
                response -> {
                    Almacen.agregarNotificacion(getApplicationContext(), getString(R.string.m_n_publicacion),
                            getString(R.string.m_n_pub_registrada),
                            getString(R.string.m_n_pub_disponible, p_nombre));
                    // HU11: pantalla de confirmación
                    startActivity(new Intent(publicar_material.this, publicacion_confirmada.class));
                    finish();
                },
                error -> {
                    ponerPublicando(false);
                    Toast.makeText(publicar_material.this,
                            getString(R.string.m_no_publico),
                            Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("nombre_material", p_nombre);
                params.put("nombre", p_nombre);
                params.put("descripcion", p_descripcion);
                params.put("categoria", p_categoria);
                params.put("tipo", p_tipo);
                params.put("estado", p_estado);
                params.put("sede", p_sede);
                params.put("foto", p_foto);
                params.put("uid_usuario", uid);
                params.put("usuario_nombre", usuarioNombre);
                params.put("usuario_correo", usuarioCorreo);
                return params;
            }
        };
        rq.add(ApiChat.sinReintento(request));
    }

    private void ponerPublicando(boolean activo) {
        publicando = activo;
        btnPublicar.setEnabled(!activo);
        btnPublicar.setText(activo ? R.string.m_publicando : R.string.m_publicar_material);
    }

    private static String textoChipElegido(ChipGroup grupo) {
        int id = grupo.getCheckedChipId();
        if (id == View.NO_ID) return "";
        Chip chip = grupo.findViewById(id);
        return chip != null ? chip.getText().toString() : "";
    }

    private static boolean esUrlValida(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }
}
