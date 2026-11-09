package com.example.intercambioupn;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.view.inputmethod.InputMethodManager;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.json.JSONException;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class buscar_material extends BaseActivity {

    private final ArrayList<Material> todos = new ArrayList<>();
    private final ArrayList<Material> resultados = new ArrayList<>();

    private EditText txtBuscar;
    private TextView txtMensaje;
    private TextView txtTitulo;
    private Button btnFiltros;
    private View layoutRecientes;
    private ChipGroup chipsRecientes;
    private MaterialAdapter adaptador;
    private RequestQueue rq;

    // Filtros activos ("" = sin filtro)
    private String fCategoria = "";
    private String fTipo = "";
    private String fEstado = "";
    private String fSede = "";

    private boolean datosCargados = false;

    // Categoría fijada al entrar desde Inicio ("" = búsqueda general)
    private String categoriaFija = "";
    private static final String OTROS = "Otros objetos";
    private static final String[] CATEGORIAS_PRINCIPALES = {"Libros", "Apuntes", "Tecnología"};

    private final ActivityResultLauncher<Intent> lanzadorFiltros = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent d = result.getData();
                    // En modo categoría, la categoría no se puede cambiar desde Filtros
                    fCategoria = categoriaFija.isEmpty() ? nvl(d.getStringExtra("categoria")) : categoriaFija;
                    fTipo = nvl(d.getStringExtra("tipo"));
                    fEstado = nvl(d.getStringExtra("estado"));
                    fSede = nvl(d.getStringExtra("sede"));
                    actualizarTextoBotonFiltros();
                    aplicarBusqueda();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_buscar_material);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int margen = (int) (24 * v.getResources().getDisplayMetrics().density); // 24dp, igual que el XML
            v.setPadding(systemBars.left + margen, systemBars.top + margen, systemBars.right + margen, systemBars.bottom + margen);
            return insets;
        });

        rq = Volley.newRequestQueue(this);

        txtBuscar = findViewById(R.id.txtBuscarMaterial);
        txtMensaje = findViewById(R.id.txtMensajeResultados);
        txtTitulo = findViewById(R.id.txtTituloResultados);
        btnFiltros = findViewById(R.id.btnAbrirFiltros);
        layoutRecientes = findViewById(R.id.layoutRecientes);
        chipsRecientes = findViewById(R.id.chipsRecientes);

        RecyclerView lista = findViewById(R.id.listaResultados);
        lista.setLayoutManager(new LinearLayoutManager(this));
        adaptador = new MaterialAdapter(resultados, rq, m -> {
            Intent intent = new Intent(this, detalle_publicacion.class);
            intent.putExtra("material", m);
            startActivity(intent);
        });
        lista.setAdapter(adaptador);

        findViewById(R.id.btnRegresar).setOnClickListener(v -> finish());
        findViewById(R.id.btnBuscar).setOnClickListener(v -> buscarDesdeUsuario());
        txtBuscar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                buscarDesdeUsuario();
                return true;
            }
            return false;
        });

        // HU25 - Al vaciar el campo se vuelve a mostrar el historial
        txtBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                if (s.length() == 0) aplicarBusqueda();
            }
        });

        // HU25 - Borrar todo el historial
        findViewById(R.id.btnBorrarRecientes).setOnClickListener(v -> {
            Almacen.limpiarBusquedas(this);
            mostrarRecientes();
        });

        btnFiltros.setOnClickListener(v -> {
            Intent intent = new Intent(this, filtros.class);
            intent.putExtra("categoria", fCategoria);
            intent.putExtra("tipo", fTipo);
            intent.putExtra("estado", fEstado);
            intent.putExtra("sede", fSede);
            intent.putExtra("categoriaFija", !categoriaFija.isEmpty());
            lanzadorFiltros.launch(intent);
        });

        // Si se llegó desde una categoría del inicio, la pantalla pasa a ser "la sección" de esa categoría
        String categoriaInicial = getIntent().getStringExtra("categoria");
        if (!TextUtils.isEmpty(categoriaInicial)) {
            categoriaFija = categoriaInicial;
            fCategoria = categoriaInicial;
            ((TextView) findViewById(R.id.txtTituloBuscar)).setText(Catalogo.mostrar(this, categoriaInicial));
            ((TextView) findViewById(R.id.txtSubtituloBuscar)).setText(OTROS.equals(categoriaInicial)
                    ? getString(R.string.m_accesorios_y_mas)
                    : getString(R.string.m_materiales_de, Catalogo.mostrar(this, categoriaInicial)));
            txtBuscar.setHint(getString(R.string.m_buscar_en, Catalogo.mostrar(this, categoriaInicial)));
            actualizarTextoBotonFiltros();
        }

        cargarMateriales();
        aplicarBusqueda(); // muestra el historial desde el inicio
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Al volver del detalle, refresca los corazones por si cambió algún favorito
        if (adaptador != null) adaptador.notifyDataSetChanged();
    }

    private void cargarMateriales() {
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                Api.MOSTRAR,
                null,
                response -> {
                    try {
                        todos.clear();
                        todos.addAll(Material.desdeRespuesta(response));
                        datosCargados = true;
                        aplicarBusqueda();
                    } catch (JSONException e) {
                        Toast.makeText(this, R.string.m_error_leer_mat, Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    mostrarMensaje(getString(R.string.m_no_cargaron_mat));
                    Toast.makeText(this, getString(R.string.m_error_cargar_mat, String.valueOf(error)), Toast.LENGTH_LONG).show();
                }
        );
        rq.add(Api.reintentos(req));
    }

    /** Búsqueda hecha por el usuario (botón o teclado): se guarda en el historial. */
    private void buscarDesdeUsuario() {
        Almacen.agregarBusqueda(this, txtBuscar.getText().toString());
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(txtBuscar.getWindowToken(), 0);
        aplicarBusqueda();
    }

    /** HU25 - Muestra las búsquedas recientes como chips (tocar = repetir, X = eliminar). */
    private void mostrarRecientes() {
        List<String> recientes = Almacen.busquedasRecientes(this);
        chipsRecientes.removeAllViews();

        for (String termino : recientes) {
            Chip chip = new Chip(this);
            chip.setText(termino);
            chip.setCloseIconVisible(true);
            chip.setCloseIconContentDescription(getString(R.string.m_eliminar_busqueda));
            chip.setOnClickListener(v -> {
                txtBuscar.setText(termino);
                txtBuscar.setSelection(termino.length());
                buscarDesdeUsuario();
            });
            chip.setOnCloseIconClickListener(v -> {
                Almacen.eliminarBusqueda(this, termino);
                mostrarRecientes();
            });
            chipsRecientes.addView(chip);
        }
        layoutRecientes.setVisibility(recientes.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void aplicarBusqueda() {
        String termino = normalizar(txtBuscar.getText().toString().trim());
        boolean hayFiltros = hayFiltrosActivos(); // no cuenta la categoría fijada

        resultados.clear();

        // El historial solo se ve cuando no hay una búsqueda ni filtros activos
        if (termino.isEmpty() && !hayFiltros) {
            mostrarRecientes();
        } else {
            layoutRecientes.setVisibility(View.GONE);
        }

        // Sin búsqueda ni filtros se muestran todos los materiales (los más nuevos primero)
        txtTitulo.setText(termino.isEmpty() && !hayFiltros
                ? (categoriaFija.isEmpty() ? getString(R.string.m_todos_materiales)
                                           : getString(R.string.m_todo_en, Catalogo.mostrar(this, categoriaFija)))
                : getString(R.string.m_resultados));

        if (!datosCargados) {
            adaptador.notifyDataSetChanged();
            mostrarMensaje(getString(R.string.m_cargando_mat));
            return;
        }

        for (Material m : todos) {
            if (!termino.isEmpty() && !coincideTermino(m, termino)) continue;
            if (!fCategoria.isEmpty() && !coincideCategoria(m, fCategoria)) continue;
            if (!fTipo.isEmpty() && !fTipo.equalsIgnoreCase(m.getTipo())) continue;
            if (!fEstado.isEmpty() && !fEstado.equalsIgnoreCase(m.getEstado())) continue;
            if (!fSede.isEmpty() && !fSede.equalsIgnoreCase(m.getSede())) continue;
            resultados.add(m);
        }

        adaptador.notifyDataSetChanged();

        if (resultados.isEmpty()) {
            mostrarMensaje(termino.isEmpty() && !hayFiltros
                    ? (categoriaFija.isEmpty() ? getString(R.string.m_sin_materiales)
                                               : getString(R.string.m_sin_materiales_en, Catalogo.mostrar(this, categoriaFija)))
                    : getString(R.string.m_sin_resultados));
        } else {
            txtMensaje.setVisibility(View.GONE);
        }
    }

    private boolean coincideTermino(Material m, String termino) {
        return normalizar(m.getNombre()).contains(termino)
                || normalizar(m.getDescripcion()).contains(termino)
                || normalizar(m.getCategoria()).contains(termino)
                || normalizar(m.getTipo()).contains(termino)
                || normalizar(Catalogo.mostrar(this, m.getCategoria())).contains(termino)
                || normalizar(Catalogo.mostrar(this, m.getTipo())).contains(termino);
    }

    private void mostrarMensaje(String texto) {
        txtMensaje.setText(texto);
        txtMensaje.setVisibility(View.VISIBLE);
    }

    /** Filtros elegidos por el usuario (la categoría fijada desde Inicio no cuenta). */
    private boolean hayFiltrosActivos() {
        boolean categoriaElegida = categoriaFija.isEmpty() && !fCategoria.isEmpty();
        return categoriaElegida || !fTipo.isEmpty() || !fEstado.isEmpty() || !fSede.isEmpty();
    }

    /**
     * En la sección "Otros objetos" de Inicio entra todo lo que no es Libros, Apuntes ni Tecnología
     * (por ejemplo, Accesorios). En Filtros, cada categoría se compara tal cual.
     */
    private boolean coincideCategoria(Material m, String categoria) {
        if (OTROS.equalsIgnoreCase(categoria) && !categoriaFija.isEmpty()) {
            for (String principal : CATEGORIAS_PRINCIPALES) {
                if (principal.equalsIgnoreCase(m.getCategoria())) return false;
            }
            return true;
        }
        return categoria.equalsIgnoreCase(m.getCategoria());
    }

    private void actualizarTextoBotonFiltros() {
        int n = 0;
        if (categoriaFija.isEmpty() && !fCategoria.isEmpty()) n++;
        if (!fTipo.isEmpty()) n++;
        if (!fEstado.isEmpty()) n++;
        if (!fSede.isEmpty()) n++;
        btnFiltros.setText(n == 0 ? getString(R.string.m_filtros) : getString(R.string.m_filtros_n, n));
    }

    /** Minúsculas y sin tildes, para que "tecnologia" encuentre "Tecnología". */
    private static String normalizar(String s) {
        if (TextUtils.isEmpty(s)) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.toLowerCase(Locale.ROOT);
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }
}