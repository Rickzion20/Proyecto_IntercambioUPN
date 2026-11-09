-- =====================================================================
--  Intercambio UPN - Base de datos completa (MySQL / MariaDB de XAMPP)
--  Crea la base y las 4 tablas que usa la app:
--    materiales      -> publicaciones (HU04-HU07, HU10-HU14)
--    conversaciones  -> chats entre interesado y dueño (HU08, HU09)
--    mensajes        -> mensajes de cada chat (HU08)
--    operaciones     -> historial de intercambios y préstamos (HU16)
--  Uso: phpMyAdmin > pestaña SQL (sin elegir base) > pegar todo > Continuar
--  Las cuentas de usuario NO van aquí: viven en Firebase.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS intercambio_upn
  CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE intercambio_upn;

-- Publicaciones de materiales
CREATE TABLE IF NOT EXISTS materiales (
  id INT AUTO_INCREMENT PRIMARY KEY,
  nombre_material VARCHAR(150) NOT NULL,
  descripcion TEXT NOT NULL,
  categoria VARCHAR(50) NOT NULL,
  tipo VARCHAR(30) NOT NULL,
  estado VARCHAR(30) NOT NULL,
  sede VARCHAR(60) NOT NULL,
  foto VARCHAR(500) NOT NULL,
  uid_usuario VARCHAR(128) NULL,
  usuario_nombre VARCHAR(150) NULL,
  usuario_correo VARCHAR(150) NULL,
  fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Conversaciones (una por material e interesado)
CREATE TABLE IF NOT EXISTS conversaciones (
  id INT AUTO_INCREMENT PRIMARY KEY,
  material_id INT NOT NULL,
  material_nombre VARCHAR(150) NOT NULL,
  material_tipo VARCHAR(30) NOT NULL DEFAULT '',
  uid_interesado VARCHAR(128) NOT NULL,
  nombre_interesado VARCHAR(150) NOT NULL,
  uid_propietario VARCHAR(128) NOT NULL,
  nombre_propietario VARCHAR(150) NOT NULL,
  ultimo_mensaje TEXT NULL,
  ultimo_uid VARCHAR(128) NULL,
  ultimo_id INT NOT NULL DEFAULT 0,
  fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_conversacion (material_id, uid_interesado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Mensajes de cada conversación
CREATE TABLE IF NOT EXISTS mensajes (
  id INT AUTO_INCREMENT PRIMARY KEY,
  conversacion_id INT NOT NULL,
  uid_emisor VARCHAR(128) NOT NULL,
  texto TEXT NOT NULL,
  fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_conversacion (conversacion_id),
  FOREIGN KEY (conversacion_id) REFERENCES conversaciones(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Historial de intercambios y préstamos (una operación por conversación)
CREATE TABLE IF NOT EXISTS operaciones (
  id INT AUTO_INCREMENT PRIMARY KEY,
  conversacion_id INT NOT NULL,
  material_id INT NOT NULL,
  material_nombre VARCHAR(150) NOT NULL,
  tipo VARCHAR(30) NOT NULL,
  uid_propietario VARCHAR(128) NOT NULL,
  nombre_propietario VARCHAR(150) NOT NULL,
  uid_interesado VARCHAR(128) NOT NULL,
  nombre_interesado VARCHAR(150) NOT NULL,
  estado VARCHAR(20) NOT NULL DEFAULT 'Pendiente',
  fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_operacion (conversacion_id),
  FOREIGN KEY (conversacion_id) REFERENCES conversaciones(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
