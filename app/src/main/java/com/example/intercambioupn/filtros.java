package com.example.intercambioupn;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * HU06 - Filtrar materiales.
 * Devuelve a buscar_material los valores elegidos ("" cuando está en la primera opción = sin filtro).
 */
public class filtros extends BaseActivity {

    private Spinner spCategoria, spTipo, spEstado, spSede;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_filtros);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        spCategoria = findViewById(R.id.spCategoria);
        spTipo = findViewById(R.id.spTipoIntercambio);
        spEstado = findViewById(R.id.spEstadoMaterial);
        spSede = findViewById(R.id.spSede);

        // Si se entró desde una categoría del inicio, la categoría ya está fijada: se oculta su selector
        if (getIntent().getBooleanExtra("categoriaFija", false)) {
            findViewById(R.id.txtLabelCategoria).setVisibility(android.view.View.GONE);
            spCategoria.setVisibility(android.view.View.GONE);
        }

        // Mantiene seleccionados los filtros que ya estaban aplicados
        seleccionar(spCategoria, getIntent().getStringExtra("categoria"));
        seleccionar(spTipo, getIntent().getStringExtra("tipo"));
        seleccionar(spEstado, getIntent().getStringExtra("estado"));
        seleccionar(spSede, getIntent().getStringExtra("sede"));

        findViewById(R.id.btnRegresar).setOnClickListener(v -> finish());

        findViewById(R.id.btnAplicarFiltros).setOnClickListener(v -> devolver(
                valor(spCategoria), valor(spTipo), valor(spEstado), valor(spSede)));

        findViewById(R.id.btnLimpiarFiltros).setOnClickListener(v -> {
            spCategoria.setSelection(0);
            spTipo.setSelection(0);
            spEstado.setSelection(0);
            spSede.setSelection(0);
            devolver("", "", "", "");
        });
    }

    private void devolver(String categoria, String tipo, String estado, String sede) {
        Intent data = new Intent();
        data.putExtra("categoria", categoria);
        data.putExtra("tipo", tipo);
        data.putExtra("estado", estado);
        data.putExtra("sede", sede);
        setResult(RESULT_OK, data);
        finish();
    }

    /** La posición 0 ("Todas las...", "Todos") significa sin filtro. */
    private String valor(Spinner sp) {
        if (sp.getSelectedItemPosition() <= 0 || sp.getSelectedItem() == null) return "";
        // Se devuelve el valor en español, que es como está guardado en la base de datos
        return Catalogo.guardar(this, arregloDe(sp), sp.getSelectedItem().toString());
    }

    private int arregloDe(Spinner sp) {
        if (sp == spCategoria) return R.array.categorias_material;
        if (sp == spTipo) return R.array.tipos_intercambio;
        if (sp == spEstado) return R.array.estados_material;
        return R.array.sedes_universitarias;
    }

    private void seleccionar(Spinner sp, String valor) {
        if (valor == null || valor.isEmpty()) return;
        int i = Catalogo.posicion(this, arregloDe(sp), valor);
        if (i >= 0 && sp.getAdapter() != null && i < sp.getAdapter().getCount()) sp.setSelection(i);
    }
}
