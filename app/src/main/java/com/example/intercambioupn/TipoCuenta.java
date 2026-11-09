package com.example.intercambioupn;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Reglas de las dos modalidades de acceso:
 *  - UPN:  solo correos @upn.pe, con correo verificado.
 *  - GMAIL: solo correos @gmail.com (con contraseña o con "Continuar con Google").
 *
 * El dominio se valida dos veces: antes de llamar a Firebase (con lo que escribió el usuario)
 * y después (con el correo que devuelve Firebase, que es el que vale).
 * Las reglas de Realtime Database repiten la validación del lado del servidor.
 */
public final class TipoCuenta {

    public static final String MODO = "modo";
    public static final String UPN = "upn";
    public static final String GMAIL = "gmail";

    // Valores que se guardan en Usuarios/{uid}/tipoUsuario
    public static final String TIPO_UPN = "estudiante_upn";
    public static final String TIPO_GMAIL = "gmail";

    /**
     * Exigir que el correo UPN esté verificado antes de entrar.
     * Si el correo de la universidad bloquea los mensajes de Firebase, se puede poner en false
     * temporalmente (por ejemplo, para una demostración). El dominio @upn.pe se sigue exigiendo.
     */
    public static final boolean EXIGIR_VERIFICACION_UPN = true;

    private static final String DOMINIO_UPN = "@upn.pe";
    private static final String DOMINIO_GMAIL = "@gmail.com";

    private TipoCuenta() { }

    /** Quita espacios y pasa a minúsculas. */
    public static String normalizar(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean esUpn(String correo) {
        String c = normalizar(correo);
        return c.endsWith(DOMINIO_UPN) && c.length() > DOMINIO_UPN.length();
    }

    public static boolean esGmail(String correo) {
        String c = normalizar(correo);
        return c.endsWith(DOMINIO_GMAIL) && c.length() > DOMINIO_GMAIL.length();
    }

    /** ¿El correo pertenece a la modalidad elegida? */
    public static boolean correspondeAlModo(String correo, String modo) {
        return UPN.equals(modo) ? esUpn(correo) : esGmail(correo);
    }

    /** "estudiante_upn", "gmail" o "" si el dominio no está permitido. */
    public static String tipoDe(String correo) {
        if (esUpn(correo)) return TIPO_UPN;
        if (esGmail(correo)) return TIPO_GMAIL;
        return "";
    }

    /** Nombre para mostrar en el perfil. */
    public static String etiqueta(Context c, String correo) {
        if (esUpn(correo)) return c.getString(R.string.m_estudiante_upn);
        if (esGmail(correo)) return c.getString(R.string.m_usuario_gmail);
        return c.getString(R.string.m_no_permitido);
    }

    /**
     * Código de estudiante tomado del correo (n00232323@upn.pe → N00232323).
     * Es un dato provisional: no prueba que la persona sea estudiante activo.
     */
    public static String codigoEstudiante(String correo) {
        String c = normalizar(correo);
        if (!esUpn(c)) return "";
        String local = c.substring(0, c.length() - DOMINIO_UPN.length());
        return local.matches("^n\\d{8}$") ? local.toUpperCase(Locale.ROOT) : "";
    }

    /** Mensaje cuando el correo no corresponde a la modalidad. */
    public static String mensajeDominio(Context c, String modo) {
        return UPN.equals(modo)
                ? c.getString(R.string.m_dominio_upn)
                : c.getString(R.string.m_dominio_gmail);
    }

    /** ¿La sesión guardada sigue cumpliendo las reglas? (la usa el splash al abrir la app) */
    public static boolean sesionValida(FirebaseUser user) {
        if (user == null) return false;
        String correo = user.getEmail();
        if (esUpn(correo)) return user.isEmailVerified() || !EXIGIR_VERIFICACION_UPN;
        return esGmail(correo);
    }

    public static Intent irASeleccion(Activity a) {
        Intent i = new Intent(a, seleccionar_acceso.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return i;
    }

    public interface AlTerminar { void listo(boolean ok); }

    /**
     * Paso común después de autenticar (contraseña o Google):
     * 1) revisa el correo que devolvió Firebase contra la modalidad,
     * 2) en UPN exige correo verificado,
     * 3) guarda tipoUsuario y codigoEstudiante sin sobrescribir el resto del perfil,
     * 4) entra al Inicio.
     */
    public static void completarIngreso(Activity a, String modo, AlTerminar fin) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) { fin.listo(false); return; }

        String correo = normalizar(user.getEmail());

        if (!correspondeAlModo(correo, modo)) {
            auth.signOut();
            mostrar(a, a.getString(R.string.m_correo_no_permitido), mensajeDominio(a, modo));
            fin.listo(false);
            return;
        }

        if (UPN.equals(modo) && EXIGIR_VERIFICACION_UPN && !user.isEmailVerified()) {
            new AlertDialog.Builder(a)
                    .setTitle(R.string.m_verifica_titulo)
                    .setMessage(a.getString(R.string.m_verifica_mensaje, correo))
                    .setPositiveButton(R.string.m_reenviar_correo, (d, w) -> user.sendEmailVerification()
                            .addOnCompleteListener(t -> {
                                Toast.makeText(a, t.isSuccessful()
                                        ? a.getString(R.string.m_reenvio_ok)
                                        : a.getString(R.string.m_reenvio_error),
                                        Toast.LENGTH_LONG).show();
                                auth.signOut();
                            }))
                    .setNegativeButton(R.string.m_entendido, (d, w) -> auth.signOut())
                    .setOnCancelListener(d -> auth.signOut())
                    .show();
            fin.listo(false);
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Usuarios").child(user.getUid());
        ref.get().addOnCompleteListener(lectura -> {
            Map<String, Object> datos = new HashMap<>();
            datos.put("uid", user.getUid());
            datos.put("correo", correo);
            datos.put("tipoUsuario", tipoDe(correo));
            String codigo = codigoEstudiante(correo);
            datos.put("codigoEstudiante", codigo.isEmpty() ? null : codigo); // null = se borra (Gmail)

            // Cuenta nueva con Google: se crea el perfil con el nombre de la cuenta de Google
            boolean sinNombres = !lectura.isSuccessful() || lectura.getResult() == null
                    || !lectura.getResult().child("nombres").exists();
            if (sinNombres && user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
                String[] partes = user.getDisplayName().trim().split("\\s+", 2);
                datos.put("nombres", partes[0]);
                datos.put("apellidos", partes.length > 1 ? partes[1] : "");
            }

            ref.updateChildren(datos).addOnCompleteListener(escritura -> {
                if (!escritura.isSuccessful()) {
                    auth.signOut();
                    mostrar(a, a.getString(R.string.m_no_se_pudo_ingresar), a.getString(R.string.m_no_guardo_perfil));
                    fin.listo(false);
                    return;
                }
                fin.listo(true);
                Intent i = new Intent(a, MainActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                a.startActivity(i);
                a.finish();
            });
        });
    }

    static void mostrar(Activity a, String titulo, String mensaje) {
        if (a.isFinishing()) return;
        new AlertDialog.Builder(a).setTitle(titulo).setMessage(mensaje)
                .setPositiveButton(R.string.m_entendido, null).show();
    }
}
