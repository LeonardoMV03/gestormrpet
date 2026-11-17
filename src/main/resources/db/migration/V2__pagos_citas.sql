ALTER TABLE citas ADD COLUMN estado_pago TEXT NOT NULL DEFAULT 'a_deber'
  CHECK (estado_pago IN ('pagado','a_deber'));

ALTER TABLE citas ADD COLUMN metodo_pago TEXT
  CHECK (metodo_pago IS NULL OR metodo_pago IN ('efectivo','transferencia'));

ALTER TABLE citas ADD COLUMN pagado_en TEXT;
