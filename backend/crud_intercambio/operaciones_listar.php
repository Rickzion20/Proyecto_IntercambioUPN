<?php
// HU16 - Historial de intercambios y préstamos del usuario.
header("Content-Type: application/json; charset=utf-8");
require "conexion.php";

$uid = $_GET['uid'] ?? '';

$stmt = $conexion->prepare(
    "SELECT * FROM operaciones
     WHERE uid_propietario = ? OR uid_interesado = ?
     ORDER BY fecha_actualizacion DESC"
);
$stmt->bind_param("ss", $uid, $uid);
$stmt->execute();
$res = $stmt->get_result();

$datos = [];
while ($fila = $res->fetch_assoc()) {
    $datos[] = $fila;
}
echo json_encode(["datos" => $datos], JSON_UNESCAPED_UNICODE);
