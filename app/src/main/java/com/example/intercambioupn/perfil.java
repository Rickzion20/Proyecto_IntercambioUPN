package com.example.intercambioupn;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Outline;
import android.os.Bundle;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * HU17 - Consultar y gestionar perfil.
 * Muestra nombre, carrera, ciclo, correo, tipo de usuario, código de estudiante y sede,
 * permite cambiar la foto (por URL), actualizar datos y entrar a contraseña, ajustes y privacidad.
 */
public class perfil extends BaseActivity {

    private TextView txtNombreCompleto;
    private TextView txtCorreoPerfil;
    private TextView txtNombrePerfil;
    private TextView txtCarreraPerfil;
    private TextView txtCicloPerfil;
    private TextView txtCodigoPerfil;
    private TextView txtTipoUsuarioPerfil;
    private TextView txtSedePerfil;
    private ImageView imgFotoPerfil;

    private String fotoActual = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_perfil);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        txtNombreCompleto = findViewById(R.id.txtNombreCompleto);
        txtCorreoPerfil = findViewById(R.id.txtCorreoPerfil);
        txtNombrePerfil = findViewById(R.id.txtNombrePerfil);
        txtCarreraPerfil = findViewById(R.id.txtCarreraPerfil);
        txtCicloPerfil = findViewById(R.id.txtCicloPerfil);
        txtCodigoPerfil = findViewById(R.id.txtCodigoPerfil);
        txtTipoUsuarioPerfil = findViewById(R.id.txtTipoUsuarioPerfil);
        txtSedePerfil = findViewById(R.id.txtSedePerfil);
        imgFotoPerfil = findViewById(R.id.imgFotoPerfil);

        // Foto redonda sin necesidad de un drawable extra
        imgFotoPerfil.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });
        imgFotoPerfil.setClipToOutline(true);
    }

    /** Se recarga al volver de "Actualizar datos" para ver los cambios. */
    @Override
    protected void onResume() {
        super.onResume();
        cargarDatosUsuario();
    }

    private void cargarDatosUsuario() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        if (currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()) {
            txtNombreCompleto.setText(currentUser.getDisplayName());
            txtNombrePerfil.setText(currentUser.getDisplayName());
        }
        if (currentUser.getEmail() != null) {
            txtCorreoPerfil.setText(currentUser.getEmail());
        }
        // Tipo de cuenta y código: salen del correo autenticado, no de lo que escriba el usuario
        String correo = currentUser.getEmail();
        txtTipoUsuarioPerfil.setText(TipoCuenta.etiqueta(this, correo));
        boolean esUpn = TipoCuenta.esUpn(correo);
        findViewById(R.id.lblCodigoPerfil).setVisibility(esUpn ? View.VISIBLE : View.GONE);
        txtCodigoPerfil.setVisibility(esUpn ? View.VISIBLE : View.GONE);
        String codigo = TipoCuenta.codigoEstudiante(correo);
        txtCodigoPerfil.setText(codigo.isEmpty() ? getString(R.string.m_no_registrado) : codigo);

        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference("Usuarios").child(currentUser.getUid());
        mDatabase.get().addOnSuccessListener(dataSnapshot -> {
            if (!dataSnapshot.exists()) return;

            String nombres = dataSnapshot.child("nombres").getValue(String.class);
            String apellidos = dataSnapshot.child("apellidos").getValue(String.class);
            if (nombres != null && apellidos != null) {
                String nombreCompleto = nombres + " " + apellidos;
                txtNombreCompleto.setText(nombreCompleto);
                txtNombrePerfil.setText(nombreCompleto);
            }
            poner(txtCarreraPerfil, dataSnapshot.child("carrera").getValue(String.class));
            poner(txtCicloPerfil, dataSnapshot.child("ciclo").getValue(String.class));
            poner(txtSedePerfil, dataSnapshot.child("sede").getValue(String.class));

            String foto = dataSnapshot.child("fotoUrl").getValue(String.class);
            if (foto != null && !foto.isEmpty()) {
                fotoActual = foto;
                cargarFoto(foto);
            }
        });
    }

    private static void poner(TextView campo, String valor) {
        if (valor != null && !valor.isEmpty()) campo.setText(valor);
    }

    private void cargarFoto(String url) {
        Volley.newRequestQueue(this).add(new ImageRequest(
                url,
                (Bitmap b) -> imgFotoPerfil.setImageBitmap(b),
                0, 0,
                ImageView.ScaleType.CENTER_CROP,
                Bitmap.Config.RGB_565,
                error -> { /* se deja el icono por defecto */ }
        ));
    }

    /** Cambiar foto: se pega el enlace (URL) de una imagen, igual que en las publicaciones. */
    public void cambiarFoto(View view) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        EditText campo = new EditText(this);
        campo.setHint("https://...");
        campo.setSingleLine(true);
        campo.setText(fotoActual);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        campo.setPadding(pad, pad, pad, pad);

        new AlertDialog.Builder(this)
                .setTitle(R.string.m_cambiar_foto_titulo)
                .setMessage(R.string.m_cambiar_foto_msj)
                .setView(campo)
                .setNegativeButton(R.string.m_cancelar, null)
                .setPositiveButton(R.string.m_guardar, (d, w) -> {
                    String url = campo.getText().toString().trim();
                    if (!url.isEmpty() && !url.startsWith("http://") && !url.startsWith("https://")) {
                        Toast.makeText(this, R.string.m_url_http, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    FirebaseDatabase.getInstance().getReference("Usuarios").child(user.getUid())
                            .child("fotoUrl").setValue(url)
                            .addOnCompleteListener(task -> {
                                if (!task.isSuccessful()) {
                                    Toast.makeText(this, R.string.m_no_guardo_foto, Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                fotoActual = url;
                                if (url.isEmpty()) {
                                    imgFotoPerfil.setImageResource(R.drawable.usuario);
                                } else {
                                    cargarFoto(url);
                                }
                                Toast.makeText(this, R.string.m_foto_actualizada, Toast.LENGTH_SHORT).show();
                            });
                })
                .show();
    }

    public void actualizarDatos(View view) {
        startActivity(new Intent(this, editar_perfil.class));
    }

    public void abrirAjustes(View view) {
        startActivity(new Intent(this, ajustes.class));
    }

    public void abrirHistorial(View view) {
        startActivity(new Intent(this, historial.class));
    }

    public void abrirPrivacidad(View view) {
        startActivity(new Intent(this, privacidad.class));
    }

    public void regresar(View view) {
        finish();
    }

    public void cambiarcontrasena(View view) {
        startActivity(new Intent(this, cambiar_contrasena.class));
    }

    public void cerrarsesion(View view) {
        Intent intent = new Intent(this, cerrar_sesion.class);
        startActivity(intent);
    }
}
