<?php
// HU09 - Conversaciones de un usuario (como interesado o como dueño), la más reciente primero.
header("Content-Type: application/json; charset=utf-8");
require "conexion.php";

$uid = $_GET['uid'] ?? '';

$stmt = $conexion->prepare(
    "SELECT c.*, IFNULL(o.estado, '') AS operacion_estado
     FROM conversaciones c
     LEFT JOIN operaciones o ON o.conversacion_id = c.id
     WHERE c.uid_interesado = ? OR c.uid_propietario = ?
     ORDER BY c.fecha_actualizacion DESC"
);
$stmt->bind_param("ss", $uid, $uid);
$stmt->execute();
$res = $stmt->get_result();

$datos = [];
while ($fila = $res->fetch_assoc()) {
    $datos[] = $fila;
}
echo json_encode(["datos" => $datos], JSON_UNESCAPED_UNICODE);
