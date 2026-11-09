<?php
// XAMPP por defecto: usuario root, sin contraseña, puerto 3306 (cámbialo si tu MySQL usa otro)
$host = "127.0.0.1";
$user = "root";
$pass = "";
$db   = "intercambio_upn";

mysqli_report(MYSQLI_REPORT_OFF);
$conexion = new mysqli($host, $user, $pass, $db, 3306);
if ($conexion->connect_errno) {
    http_response_code(500);
    die("Error de conexion: " . $conexion->connect_error);
}
$conexion->set_charset("utf8mb4");
