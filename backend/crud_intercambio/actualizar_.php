<?php
require "conexion.php";

$id          = $_POST['id'] ?? 0;
$nombre      = $_POST['nombre_material'] ?? ($_POST['nombre'] ?? '');
$descripcion = $_POST['descripcion'] ?? '';
$categoria   = $_POST['categoria'] ?? '';
$tipo        = $_POST['tipo'] ?? '';
$estado      = $_POST['estado'] ?? '';
$sede        = $_POST['sede'] ?? '';
$foto        = $_POST['foto'] ?? '';
$uid         = $_POST['uid_usuario'] ?? '';

// Solo el dueño puede editar su publicación
$check = $conexion->prepare("SELECT id FROM materiales WHERE id=? AND uid_usuario=? AND uid_usuario<>''");
$check->bind_param("is", $id, $uid);
$check->execute();
if ($check->get_result()->num_rows === 0) {
    http_response_code(403);
    echo "No puedes editar esta publicación";
    exit;
}

$stmt = $conexion->prepare(
    "UPDATE materiales SET nombre_material=?, descripcion=?, categoria=?, tipo=?, estado=?, sede=?, foto=? WHERE id=?"
);
$stmt->bind_param("sssssssi", $nombre, $descripcion, $categoria, $tipo, $estado, $sede, $foto, $id);

if ($stmt->execute()) {
    echo "Actualizado correctamente";
} else {
    http_response_code(500);
    echo "Error al actualizar";
}
