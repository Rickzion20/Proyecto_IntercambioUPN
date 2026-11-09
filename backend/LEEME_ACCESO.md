# Acceso UPN / Gmail — pasos de configuración

## 1. Reglas de Realtime Database (obligatorio)
Firebase Console > Realtime Database > Reglas > pegar el contenido de
`firebase_reglas_realtime_database.json` > Publicar.

Qué hacen:
- Solo leen/escriben perfiles las cuentas @upn.pe o @gmail.com.
- Cada usuario solo escribe su propio perfil (Usuarios/{uid}).
- `tipoUsuario` y `codigoEstudiante` deben coincidir con el correo autenticado:
  nadie puede ponerse "estudiante_upn" con un Gmail ni cambiar su código.
- No se pueden agregar campos extra (por ejemplo "admin").

Si ya había otras reglas para otros nodos, combínalas en vez de reemplazarlas.

## 2. Continuar con Google (opcional, lo hace el dueño del Firebase)
1. En cada PC que compila: `gradlew signingReport` > copiar SHA1 y SHA-256 (Variant: debug).
2. Configuración del proyecto > Tus apps > com.example.intercambioupn > Agregar huella digital.
3. Authentication > Método de inicio de sesión > Google > Habilitar > Guardar.
   (Dejar habilitado también Correo electrónico/contraseña.)
4. Descargar el google-services.json nuevo y reemplazarlo en app/.
   Debe tener datos dentro de "oauth_client" (ya no vacío).
Sin estos pasos, el botón muestra "Google aún no está configurado" y se puede
seguir entrando con correo @gmail.com y contraseña.

## 3. Cuentas existentes
- Cuentas @upn.pe creadas antes: deben verificar su correo. Al iniciar sesión verán
  "Verifica tu correo UPN" con el botón "Reenviar correo".
- Cuentas de otros dominios (hotmail, outlook…): ya no pueden ingresar.
