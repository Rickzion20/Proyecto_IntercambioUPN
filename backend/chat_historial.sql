-- Paquete 3: chat (HU08/HU09) e historial de intercambios (HU16)
-- Pegar en phpMyAdmin > base intercambio_upn > pestaña SQL > Continuar
USE intercambio_upn;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS mensajes (
  id INT AUTO_INCREMENT PRIMARY KEY,
  conversacion_id INT NOT NULL,
  uid_emisor VARCHAR(128) NOT NULL,
  texto TEXT NOT NULL,
  fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_conversacion (conversacion_id),
  FOREIGN KEY (conversacion_id) REFERENCES conversaciones(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
