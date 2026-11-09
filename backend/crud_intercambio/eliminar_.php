<?php
require "conexion.php";

$id  = $_POST['id'] ?? 0;
$uid = $_POST['uid_usuario'] ?? '';

// Solo el dueño puede eliminar su publicación
$stmt = $conexion->prepare("DELETE FROM materiales WHERE id=? AND uid_usuario=? AND uid_usuario<>''");
$stmt->bind_param("is", $id, $uid);

if (!$stmt->execute()) {
    http_response_code(500);
    echo "Error al eliminar";
} elseif ($stmt->affected_rows === 0) {
    http_response_code(403);
    echo "No puedes eliminar esta publicación";
} else {
    echo "Eliminado correctamente";
}
