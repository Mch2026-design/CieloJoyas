-- RF-16: proveedores por apartado de inventario.
-- Acceso mediante el backend Java, con sesión de administrador y CSRF.
BEGIN;
ALTER TABLE public.proveedores
    ADD COLUMN IF NOT EXISTS categoria_insumo VARCHAR(30) NOT NULL DEFAULT 'otros';
UPDATE public.proveedores SET categoria_insumo = CASE
    WHEN lower(material) LIKE '%acero%' THEN 'acero'
    WHEN lower(material) LIKE '%gema%' THEN 'gemas'
    WHEN lower(material) LIKE '%perla%' THEN 'perlas'
    ELSE 'otros' END
WHERE categoria_insumo = 'otros';
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_proveedores_categoria_insumo'
                   AND conrelid = 'public.proveedores'::regclass) THEN
        ALTER TABLE public.proveedores ADD CONSTRAINT ck_proveedores_categoria_insumo
            CHECK (categoria_insumo IN ('acero', 'gemas', 'perlas', 'otros'));
    END IF;
END $$;
-- Sin políticas de acceso público: el backend usa su conexión privada a Postgres.
ALTER TABLE public.proveedores ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON TABLE public.proveedores FROM anon, authenticated;

-- Ejemplos solicitados. No representan empresas ni contactos reales.
-- INSERT ... SELECT inserta filas en la tabla existente; SELECT INTO crearía otra tabla.
INSERT INTO public.proveedores (nombre, contacto, material, notas, activo, categoria_insumo)
SELECT ejemplo.nombre, ejemplo.contacto, ejemplo.material,
       'DEMOSTRACIÓN: proveedor ficticio. Reemplazar por datos reales antes de registrar compras.',
       FALSE, ejemplo.categoria
FROM (VALUES
    ('Ejemplo · Acero quirúrgico', 'acero@example.test', 'Acero quirúrgico 316L', 'acero'),
    ('Ejemplo · Accesorios con gemas', 'gemas@example.test', 'Accesorios con gemas', 'gemas'),
    ('Ejemplo · Perlas', 'perlas@example.test', 'Perlas para accesorios', 'perlas'),
    ('Ejemplo · Otros insumos', 'insumos@example.test', 'Broches y cierres', 'otros')
) AS ejemplo(nombre, contacto, material, categoria)
WHERE NOT EXISTS (SELECT 1 FROM public.proveedores p WHERE p.nombre = ejemplo.nombre);
COMMIT;
