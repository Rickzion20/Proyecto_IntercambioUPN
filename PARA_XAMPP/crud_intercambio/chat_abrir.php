<?php
// HU08 - Abre (o crea si no existe) la conversación entre un interesado y el dueño de un material.
header("Content-Type: application/json; charset=utf-8");
require "conexion.php";

$material_id   = intval($_POST['material_id'] ?? 0);
$material_nom  = $_POST['material_nombre'] ?? '';
$material_tipo = $_POST['material_tipo'] ?? '';
$uid_int       = $_POST['uid_interesado'] ?? '';
$nom_int       = $_POST['nombre_interesado'] ?? '';
$uid_prop      = $_POST['uid_propietario'] ?? '';
$nom_prop      = $_POST['nombre_propietario'] ?? '';

if ($material_id <= 0 || $uid_int === '' || $uid_prop === '' || $uid_int === $uid_prop) {
    http_response_code(400);
    echo json_encode(["ok" => false, "error" => "Datos incompletos"]);
    exit;
}

// El dueño y el nombre del material se toman de la base, no de lo que manda el celular
$m = $conexion->prepare("SELECT nombre_material, tipo, uid_usuario, usuario_nombre FROM materiales WHERE id = ?");
$m->bind_param("i", $material_id);
$m->execute();
$mat = $m->get_result()->fetch_assoc();
if (!$mat || $mat['uid_usuario'] === null || $mat['uid_usuario'] === '' || $mat['uid_usuario'] === $uid_int) {
    http_response_code(400);
    echo json_encode(["ok" => false, "error" => "Esta publicación no tiene un dueño con quien chatear"]);
    exit;
}
$material_nom  = $mat['nombre_material'];
$material_tipo = $mat['tipo'];
$uid_prop      = $mat['uid_usuario'];
if ($mat['usuario_nombre'] !== null && $mat['usuario_nombre'] !== '') $nom_prop = $mat['usuario_nombre'];

// Si ya existe (mismo material e interesado) se reutiliza y se devuelve su id
$stmt = $conexion->prepare(
    "INSERT INTO conversaciones (material_id, material_nombre, material_tipo, uid_interesado, nombre_interesado, uid_propietario, nombre_propietario)
     VALUES (?,?,?,?,?,?,?)
     ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id)"
);
$stmt->bind_param("issssss", $material_id, $material_nom, $material_tipo, $uid_int, $nom_int, $uid_prop, $nom_prop);

if ($stmt->execute()) {
    echo json_encode(["ok" => true, "id" => $conexion->insert_id]);
} else {
    http_response_code(500);
    echo json_encode(["ok" => false, "error" => "No se pudo abrir la conversación"]);
}
