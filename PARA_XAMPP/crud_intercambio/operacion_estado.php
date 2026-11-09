<?php
// HU16 - Cambia el estado de una operación (solo sus participantes).
header("Content-Type: application/json; charset=utf-8");
require "conexion.php";

$id     = intval($_POST['id'] ?? 0);
$uid    = $_POST['uid'] ?? '';
$estado = $_POST['estado'] ?? '';

$permitidos = ["Pendiente", "Prestado", "Completado", "Cancelado"];
if (!in_array($estado, $permitidos, true)) {
    http_response_code(400);
    echo json_encode(["ok" => false, "error" => "Estado no válido"]);
    exit;
}

$stmt = $conexion->prepare("UPDATE operaciones SET estado = ? WHERE id = ? AND (uid_propietario = ? OR uid_interesado = ?)");
$stmt->bind_param("siss", $estado, $id, $uid, $uid);
$stmt->execute();

if ($stmt->affected_rows >= 0) {
    echo json_encode(["ok" => true, "estado" => $estado], JSON_UNESCAPED_UNICODE);
} else {
    http_response_code(500);
    echo json_encode(["ok" => false, "error" => "No se pudo actualizar"]);
}
