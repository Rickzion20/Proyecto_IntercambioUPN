<?php
// HU08 - Envía un mensaje. Solo pueden escribir los dos participantes de la conversación.
header("Content-Type: application/json; charset=utf-8");
require "conexion.php";

$conversacion_id = intval($_POST['conversacion_id'] ?? 0);
$uid             = $_POST['uid_emisor'] ?? '';
$texto           = trim($_POST['texto'] ?? '');

if ($conversacion_id <= 0 || $uid === '' || $texto === '') {
    http_response_code(400);
    echo json_encode(["ok" => false, "error" => "Mensaje vacío"]);
    exit;
}

$check = $conexion->prepare("SELECT id FROM conversaciones WHERE id = ? AND (uid_interesado = ? OR uid_propietario = ?)");
$check->bind_param("iss", $conversacion_id, $uid, $uid);
$check->execute();
if ($check->get_result()->num_rows === 0) {
    http_response_code(403);
    echo json_encode(["ok" => false, "error" => "No perteneces a esta conversación"]);
    exit;
}

$stmt = $conexion->prepare("INSERT INTO mensajes (conversacion_id, uid_emisor, texto) VALUES (?,?,?)");
$stmt->bind_param("iss", $conversacion_id, $uid, $texto);
if (!$stmt->execute()) {
    http_response_code(500);
    echo json_encode(["ok" => false, "error" => "No se pudo enviar"]);
    exit;
}
$id = $conexion->insert_id;

$upd = $conexion->prepare("UPDATE conversaciones SET ultimo_mensaje = ?, ultimo_uid = ?, ultimo_id = ?, fecha_actualizacion = NOW() WHERE id = ?");
$upd->bind_param("ssii", $texto, $uid, $id, $conversacion_id);
$upd->execute();

echo json_encode(["ok" => true, "id" => $id]);
