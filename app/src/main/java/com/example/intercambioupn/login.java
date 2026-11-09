package com.example.intercambioupn;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.GoogleAuthProvider;

/**
 * Login con dos modalidades (extra TipoCuenta.MODO):
 *  - "upn":   solo correos @upn.pe, con correo verificado.
 *  - "gmail": solo correos @gmail.com, con contraseña o con "Continuar con Google".
 */
public class login extends BaseActivity {

    private EditText txtCorreo;
    private EditText txtContrasena;
    private TextInputLayout tilCorreo, tilContrasena;
    private MaterialButton btnIniciar, btnGoogle;
    private FirebaseAuth mAuth;
    private String modo;
    private boolean ocupado = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets teclado = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(barras.left, barras.top, barras.right, Math.max(barras.bottom, teclado.bottom));
            return insets;
        });

        modo = getIntent().getStringExtra(TipoCuenta.MODO);
        if (!TipoCuenta.GMAIL.equals(modo)) modo = TipoCuenta.UPN;

        mAuth = FirebaseAuth.getInstance();
        txtCorreo = findViewById(R.id.txtCorreo);
        txtContrasena = findViewById(R.id.txtContrasena);
        tilCorreo = findViewById(R.id.tilCorreo);
        tilContrasena = findViewById(R.id.tilContrasena);
        btnIniciar = findViewById(R.id.btnIniciarSesion);
        btnGoogle = findViewById(R.id.btnGoogle);

        aplicarModo();

        txtContrasena.setOnEditorActionListener((v, actionId, e) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) { iniciarSesion(v); return true; }
            return false;
        });
    }

    /** Textos y opciones de cada modalidad. */
    private void aplicarModo() {
        TextView etiqueta = findViewById(R.id.txtModoLogin);
        GradientDrawable fondo = new GradientDrawable();
        fondo.setCornerRadius(12 * getResources().getDisplayMetrics().density);

        if (TipoCuenta.UPN.equals(modo)) {
            etiqueta.setText(R.string.m_etiqueta_upn);
            fondo.setColor(getColor(R.color.amarillo));
            etiqueta.setTextColor(getColor(R.color.negro_fijo));
            ((TextView) findViewById(R.id.txtSubtituloLogin)).setText(R.string.m_sub_login_upn);
            tilCorreo.setHint(getString(R.string.m_correo_institucional));
            tilCorreo.setPlaceholderText("N00123456@upn.pe");
            findViewById(R.id.layoutGoogle).setVisibility(View.GONE);
        } else {
            etiqueta.setText(R.string.m_etiqueta_gmail);
            fondo.setColor(getColor(R.color.boton_gris));
            etiqueta.setTextColor(getColor(R.color.texto));
            ((TextView) findViewById(R.id.txtSubtituloLogin)).setText(R.string.m_sub_login_gmail);
            tilCorreo.setHint(getString(R.string.m_correo_gmail));
            tilCorreo.setPlaceholderText(getString(R.string.m_ej_gmail));
            findViewById(R.id.layoutGoogle).setVisibility(View.VISIBLE);
        }
        etiqueta.setBackground(fondo);
    }

    // ------------------------------------------------------------ Correo y contraseña

    public void iniciarSesion(View view) {
        if (ocupado) return;
        tilCorreo.setError(null);
        tilContrasena.setError(null);

        String correo = TipoCuenta.normalizar(txtCorreo.getText().toString());
        String contrasena = txtContrasena.getText().toString();

        boolean ok = true;
        if (correo.isEmpty()) {
            tilCorreo.setError(getString(R.string.m_escribe_correo));
            ok = false;
        } else if (!TipoCuenta.correspondeAlModo(correo, modo)) {
            tilCorreo.setError(TipoCuenta.UPN.equals(modo)
                    ? getString(R.string.m_debe_upn) : getString(R.string.m_debe_gmail));
            ok = false;
        }
        if (contrasena.isEmpty()) {
            tilContrasena.setError(getString(R.string.m_escribe_contrasena));
            ok = false;
        }
        if (!ok) return;

        ponerOcupado(true, btnIniciar, getString(R.string.m_ingresando));
        mAuth.signInWithEmailAndPassword(correo, contrasena).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                // Segunda validación con el correo que devuelve Firebase
                TipoCuenta.completarIngreso(this, modo, listo -> {
                    if (!listo) ponerOcupado(false, btnIniciar, getString(R.string.m_iniciar_sesion));
                });
            } else {
                ponerOcupado(false, btnIniciar, getString(R.string.m_iniciar_sesion));
                mostrarErrorFirebase(task.getException());
            }
        });
    }

    private void mostrarErrorFirebase(Exception e) {
        if (e instanceof FirebaseAuthInvalidCredentialsException || e instanceof FirebaseAuthInvalidUserException) {
            tilContrasena.setError(getString(R.string.m_credenciales));
        } else if (e instanceof FirebaseTooManyRequestsException) {
            TipoCuenta.mostrar(this, getString(R.string.m_demasiados_titulo), getString(R.string.m_demasiados));
        } else if (e instanceof FirebaseNetworkException) {
            TipoCuenta.mostrar(this, getString(R.string.m_sin_conexion), getString(R.string.m_revisa_internet));
        } else {
            TipoCuenta.mostrar(this, getString(R.string.m_no_inicio), getString(R.string.m_intenta_minutos));
        }
    }

    // ------------------------------------------------------------ Continuar con Google

    public void continuarConGoogle(View view) {
        if (ocupado) return;

        // Este recurso lo crea el plugin de Google Services cuando el google-services.json
        // trae el cliente web (proveedor Google activado + huella SHA-1 registrada).
        int idCliente = getResources().getIdentifier("default_web_client_id", "string", getPackageName());
        if (idCliente == 0) {
            TipoCuenta.mostrar(this, getString(R.string.m_google_no_config), getString(R.string.m_google_mientras));
            return;
        }

        GetSignInWithGoogleOption opcion = new GetSignInWithGoogleOption.Builder(getString(idCliente)).build();
        GetCredentialRequest solicitud = new GetCredentialRequest.Builder().addCredentialOption(opcion).build();

        ponerOcupado(true, btnGoogle, getString(R.string.m_conectando_google));
        CredentialManager.create(this).getCredentialAsync(this, solicitud, new CancellationSignal(),
                ContextCompat.getMainExecutor(this),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse respuesta) {
                        procesarGoogle(respuesta.getCredential());
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        ponerOcupado(false, btnGoogle, getString(R.string.m_continuar_google));
                        if (!(e instanceof GetCredentialCancellationException)) {
                            TipoCuenta.mostrar(login.this, getString(R.string.m_google_error), getString(R.string.m_google_revisa));
                        }
                    }
                });
    }

    private void procesarGoogle(Credential credencial) {
        if (!(credencial instanceof CustomCredential)
                || !GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credencial.getType())) {
            ponerOcupado(false, btnGoogle, getString(R.string.m_continuar_google));
            TipoCuenta.mostrar(this, getString(R.string.m_google_error), getString(R.string.m_intenta_otra));
            return;
        }
        GoogleIdTokenCredential google = GoogleIdTokenCredential.createFrom(credencial.getData());

        // Primera revisión: el correo que entrega Google (no el que se escribe en un formulario)
        if (!TipoCuenta.esGmail(google.getId())) {
            ponerOcupado(false, btnGoogle, getString(R.string.m_continuar_google));
            TipoCuenta.mostrar(this, getString(R.string.m_cuenta_no_permitida),
                    getString(R.string.m_elegiste, google.getId(), TipoCuenta.mensajeDominio(this, TipoCuenta.GMAIL)));
            return;
        }

        AuthCredential firebaseCred = GoogleAuthProvider.getCredential(google.getIdToken(), null);
        mAuth.signInWithCredential(firebaseCred).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                // Segunda revisión con el correo que guarda Firebase
                TipoCuenta.completarIngreso(this, TipoCuenta.GMAIL, listo -> {
                    if (!listo) ponerOcupado(false, btnGoogle, getString(R.string.m_continuar_google));
                });
            } else {
                ponerOcupado(false, btnGoogle, getString(R.string.m_continuar_google));
                mostrarErrorFirebase(task.getException());
            }
        });
    }

    // ------------------------------------------------------------ Otros

    private void ponerOcupado(boolean si, MaterialButton boton, String texto) {
        ocupado = si;
        btnIniciar.setEnabled(!si);
        if (btnGoogle != null) btnGoogle.setEnabled(!si);
        if (boton != null) boton.setText(texto);
    }

    public void cambiarAcceso(View view) {
        finish();
    }

    public void recuperarContrasena(View view) {
        startActivity(new Intent(login.this, recuperar_contrasena.class));
    }

    public void irCrearCuenta(View view) {
        Intent intent = new Intent(login.this, crear_cuenta.class);
        intent.putExtra(TipoCuenta.MODO, modo);
        startActivity(intent);
    }
}
