-- Migración 04: Stock por talla en anillos, grabado configurable y soporte Mercado Pago.
-- Script idempotente para bases existentes.

ALTER TABLE productos ADD COLUMN IF NOT EXISTS admite_grabado boolean NOT NULL DEFAULT false;
ALTER TABLE productos ADD COLUMN IF NOT EXISTS requiere_talla boolean NOT NULL DEFAULT false;

CREATE TABLE IF NOT EXISTS producto_tallas (
    id BIGSERIAL PRIMARY KEY,
    producto_id BIGINT NOT NULL REFERENCES productos(id) ON DELETE CASCADE,
    talla VARCHAR(10) NOT NULL,
    stock INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0),
    UNIQUE (producto_id, talla)
);

UPDATE productos
SET admite_grabado = personalizable
WHERE admite_grabado = false AND personalizable = true;

UPDATE productos p
SET requiere_talla = true
FROM categorias c
WHERE p.categoria_id = c.id AND LOWER(c.nombre) = 'anillos';

UPDATE productos
SET personalizable = (admite_grabado OR requiere_talla);
