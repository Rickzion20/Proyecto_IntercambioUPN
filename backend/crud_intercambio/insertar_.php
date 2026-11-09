<?php
require "conexion.php";

$nombre      = $_POST['nombre_material'] ?? ($_POST['nombre'] ?? '');
$descripcion = $_POST['descripcion'] ?? '';
$categoria   = $_POST['categoria'] ?? '';
$tipo        = $_POST['tipo'] ?? '';
$estado      = $_POST['estado'] ?? '';
$sede        = $_POST['sede'] ?? '';
$foto        = $_POST['foto'] ?? '';
$uid         = $_POST['uid_usuario'] ?? '';
$u_nombre    = $_POST['usuario_nombre'] ?? '';
$u_correo    = $_POST['usuario_correo'] ?? '';

$stmt = $conexion->prepare(
    "INSERT INTO materiales (nombre_material, descripcion, categoria, tipo, estado, sede, foto, uid_usuario, usuario_nombre, usuario_correo)
     VALUES (?,?,?,?,?,?,?,?,?,?)"
);
$stmt->bind_param("ssssssssss", $nombre, $descripcion, $categoria, $tipo, $estado, $sede, $foto, $uid, $u_nombre, $u_correo);

if ($stmt->execute()) {
    echo "Material publicado exitosamente";
} else {
    http_response_code(500);
    echo "Error al publicar";
}
