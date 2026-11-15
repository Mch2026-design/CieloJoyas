-- Ejecutar en Supabase SQL Editor después de hacer git push con las imágenes en assets/resenas/
UPDATE resenas r
SET foto_url = v.foto
FROM (VALUES
    ('ANI-001', 'assets/resenas/resena-ani-001.webp'),
    ('COL-001', 'assets/resenas/resena-col-001.webp'),
    ('PUL-001', 'assets/resenas/resena-pul-001.webp'),
    ('ARE-001', 'assets/resenas/resena-are-001.webp'),
    ('COL-002', 'assets/resenas/resena-col-002.webp')
) AS v(referencia, foto)
JOIN productos p ON p.referencia = v.referencia
WHERE r.producto_id = p.id
  AND r.demostracion = TRUE;
