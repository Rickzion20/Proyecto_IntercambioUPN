<?php
// HU08 - Mensajes de una conversación. Con "desde" solo trae los más nuevos (para refrescar).
header("Content-Type: application/json; charset=utf-8");
require "conexion.php";

$conversacion_id = intval($_GET['conversacion_id'] ?? 0);
$desde           = intval($_GET['desde'] ?? 0);

$stmt = $conexion->prepare(
    "SELECT id, conversacion_id, uid_emisor, texto, fecha
     FROM mensajes
     WHERE conversacion_id = ? AND id > ?
     ORDER BY id ASC"
);
$stmt->bind_param("ii", $conversacion_id, $desde);
$stmt->execute();
$res = $stmt->get_result();

$datos = [];
while ($fila = $res->fetch_assoc()) {
    $datos[] = $fila;
}
echo json_encode(["datos" => $datos], JSON_UNESCAPED_UNICODE);
