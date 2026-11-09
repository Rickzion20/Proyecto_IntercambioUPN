<?php
header("Content-Type: application/json; charset=utf-8");
require "conexion.php";

$datos = [];
$res = $conexion->query("SELECT * FROM materiales ORDER BY id DESC");
if ($res) {
    while ($fila = $res->fetch_assoc()) {
        $datos[] = $fila;
    }
}
echo json_encode(["datos" => $datos], JSON_UNESCAPED_UNICODE);
