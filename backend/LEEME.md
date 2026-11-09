# Servidor de Intercambio UPN (XAMPP)

1. Abre XAMPP y enciende **Apache** y **MySQL**.
2. phpMyAdmin (http://localhost/phpmyadmin) > pestaña **SQL** > pega `intercambio_upn_completo.sql` > Continuar.
3. Copia la carpeta `crud_intercambio` a `C:\xampp\htdocs\`.
4. En `conexion.php` revisa el puerto de MySQL (por defecto 3306; el panel de XAMPP lo muestra).
5. Prueba: http://localhost/crud_intercambio/mostrar_.php debe mostrar `{"datos":[...]}`.
6. En la app, `Api.java` > `BASE_URL`: `10.0.2.2` para el emulador o la IPv4 de la PC para un celular.
