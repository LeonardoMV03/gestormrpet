CREATE TABLE clientes (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  nombre TEXT NOT NULL,
  telefono TEXT,
  email TEXT,
  direccion TEXT,
  notas TEXT,
  activo INTEGER NOT NULL DEFAULT 1,
  creado_en TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now')),
  actualizado_en TEXT
);

CREATE TABLE mascotas (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  cliente_id INTEGER NOT NULL REFERENCES clientes(id) ON DELETE CASCADE,
  nombre TEXT NOT NULL,
  especie TEXT NOT NULL DEFAULT 'perro',
  raza TEXT,
  sexo TEXT,
  fecha_nacimiento TEXT,
  notas TEXT,
  activo INTEGER NOT NULL DEFAULT 1,
  creado_en TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now')),
  actualizado_en TEXT
);

CREATE TABLE servicios (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  nombre TEXT NOT NULL UNIQUE,
  descripcion TEXT,
  precio_centavos INTEGER NOT NULL CHECK (precio_centavos >= 0),
  duracion_min INTEGER CHECK (duracion_min IS NULL OR duracion_min >= 0),
  activo INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE citas (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  mascota_id INTEGER NOT NULL REFERENCES mascotas(id),
  fecha_hora TEXT NOT NULL,
  estado TEXT NOT NULL DEFAULT 'pendiente' CHECK (estado IN ('pendiente','confirmada','atendida','cancelada')),
  notas TEXT,
  creado_en TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now'))
);

CREATE TABLE cita_servicios (
  cita_id INTEGER NOT NULL REFERENCES citas(id) ON DELETE CASCADE,
  servicio_id INTEGER NOT NULL REFERENCES servicios(id),
  precio_centavos INTEGER NOT NULL CHECK (precio_centavos >= 0),
  PRIMARY KEY (cita_id, servicio_id)
);

CREATE TABLE notas_historial (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  mascota_id INTEGER NOT NULL REFERENCES mascotas(id) ON DELETE CASCADE,
  cita_id INTEGER REFERENCES citas(id),
  fecha TEXT NOT NULL,
  texto TEXT NOT NULL,
  autor TEXT,
  creado_en TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now'))
);

CREATE INDEX idx_mascotas_cliente ON mascotas(cliente_id);
CREATE INDEX idx_citas_mascota ON citas(mascota_id);
CREATE INDEX idx_notas_mascota ON notas_historial(mascota_id);
