-- Ejecutar sobre maricielojoyas en pgAdmin. Se puede repetir sin alterar pedidos.
BEGIN;
ALTER TABLE pedidos ADD COLUMN IF NOT EXISTS correo_enviado_en TIMESTAMPTZ;
COMMENT ON COLUMN pedidos.correo_enviado_en IS
    'Fecha en que SMTP aceptó el aviso del pedido; evita reenvíos ordinarios. No acredita entrega final.';
COMMIT;
