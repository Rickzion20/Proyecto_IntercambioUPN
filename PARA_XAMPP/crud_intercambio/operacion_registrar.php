<?php
// HU16 - Registra el intercambio/préstamo de una conversación (queda "Pendiente").
header("Content-Type: application/json; charset=utf-8");
require "conexion.php";

$conversacion_id = intval($_POST['conversacion_id'] ?? 0);
$uid             = $_POST['uid'] ?? '';

$stmt = $conexion->prepare("SELECT * FROM conversaciones WHERE id = ? AND (uid_interesado = ? OR uid_propietario = ?)");
$stmt->bind_param("iss", $conversacion_id, $uid, $uid);
$stmt->execute();
$c = $stmt->get_result()->fetch_assoc();
if (!$c) {
    http_response_code(403);
    echo json_encode(["ok" => false, "error" => "Conversación no encontrada"]);
    exit;
}

$tipo = $c['material_tipo'] !== '' ? $c['material_tipo'] : 'Intercambio';

// INSERT IGNORE: si ya existe una operación para esta conversación, no se duplica
$ins = $conexion->prepare(
    "INSERT IGNORE INTO operaciones (conversacion_id, material_id, material_nombre, tipo, uid_propietario, nombre_propietario, uid_interesado, nombre_interesado)
     VALUES (?,?,?,?,?,?,?,?)"
);
$ins->bind_param("iissssss", $conversacion_id, $c['material_id'], $c['material_nombre'], $tipo,
    $c['uid_propietario'], $c['nombre_propietario'], $c['uid_interesado'], $c['nombre_interesado']);
$ins->execute();

$sel = $conexion->prepare("SELECT id, estado FROM operaciones WHERE conversacion_id = ?");
$sel->bind_param("i", $conversacion_id);
$sel->execute();
$op = $sel->get_result()->fetch_assoc();

echo json_encode(["ok" => true, "id" => intval($op['id']), "estado" => $op['estado']], JSON_UNESCAPED_UNICODE);
