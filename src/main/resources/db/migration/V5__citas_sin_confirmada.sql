PRAGMA defer_foreign_keys = ON;

CREATE TABLE citas_nueva (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  mascota_id INTEGER NOT NULL REFERENCES mascotas(id),
  fecha_hora TEXT NOT NULL,
  estado TEXT NOT NULL DEFAULT 'pendiente' CHECK (estado IN ('pendiente','atendida','cancelada')),
  notas TEXT,
  creado_en TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now')),
  estado_pago TEXT NOT NULL DEFAULT 'a_deber' CHECK (estado_pago IN ('pagado','a_deber')),
  metodo_pago TEXT CHECK (metodo_pago IS NULL OR metodo_pago IN ('efectivo','transferencia')),
  pagado_en TEXT
);

INSERT INTO citas_nueva (id, mascota_id, fecha_hora, estado, notas, creado_en,
                         estado_pago, metodo_pago, pagado_en)
SELECT id, mascota_id, fecha_hora,
       CASE WHEN estado = 'confirmada' THEN 'pendiente' ELSE estado END,
       notas, creado_en, estado_pago, metodo_pago, pagado_en
  FROM citas;

CREATE TABLE cita_servicios_nueva (
  cita_id INTEGER NOT NULL REFERENCES citas_nueva(id) ON DELETE CASCADE,
  servicio_id INTEGER NOT NULL REFERENCES servicios(id),
  precio_centavos INTEGER NOT NULL CHECK (precio_centavos >= 0),
  PRIMARY KEY (cita_id, servicio_id)
);

INSERT INTO cita_servicios_nueva (cita_id, servicio_id, precio_centavos)
SELECT cita_id, servicio_id, precio_centavos
  FROM cita_servicios;

DROP TABLE cita_servicios;
DROP TABLE citas;

ALTER TABLE citas_nueva RENAME TO citas;
ALTER TABLE cita_servicios_nueva RENAME TO cita_servicios;

CREATE INDEX idx_citas_mascota ON citas(mascota_id);
CREATE INDEX idx_citas_fecha_hora ON citas(fecha_hora);
CREATE INDEX idx_citas_pago ON citas(estado_pago, estado);
