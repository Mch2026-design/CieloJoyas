-- Imagen opcional de cada proveedor. Conserva los registros existentes.
BEGIN;
ALTER TABLE public.proveedores ADD COLUMN IF NOT EXISTS imagen_url VARCHAR(2000);
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conname = 'ck_proveedores_imagen_url'
                   AND conrelid = 'public.proveedores'::regclass) THEN
        ALTER TABLE public.proveedores ADD CONSTRAINT ck_proveedores_imagen_url
            CHECK (imagen_url IS NULL OR
                   (char_length(imagen_url) <= 2000 AND imagen_url ~* '^https://[^[:space:]]+$'));
    END IF;
END $$;
COMMENT ON COLUMN public.proveedores.imagen_url IS 'URL HTTPS opcional de imagen o logo del proveedor; NULL muestra sus iniciales.';
COMMIT;
