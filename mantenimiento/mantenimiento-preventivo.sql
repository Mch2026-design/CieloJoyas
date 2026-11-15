-- ============================================================================
-- Cielo Joyas | Mantenimiento preventivo e integridad (PostgreSQL 17 / Supabase)
-- Script estrictamente NO destructivo (sin DROP, TRUNCATE, DELETE ni UPDATE).
-- ============================================================================

-- 1. Tamaño físico de tablas e índices reales del esquema public
SELECT relname AS tabla,
       pg_size_pretty(pg_total_relation_size(relid)) AS tamano_total,
       pg_size_pretty(pg_relation_size(relid)) AS tamano_datos,
       pg_size_pretty(pg_total_relation_size(relid) - pg_relation_size(relid)) AS tamano_indices
FROM pg_catalog.pg_statio_user_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(relid) DESC;

-- 2. Salud de almacenamiento: tuplas vivas vs. tuplas muertas y último VACUUM/ANALYZE
SELECT relname AS tabla,
       n_live_tup AS filas_activas,
       n_dead_tup AS filas_muertas,
       last_vacuum,
       last_autovacuum,
       last_analyze,
       last_autoanalyze
FROM pg_stat_user_tables
WHERE schemaname = 'public'
ORDER BY n_dead_tup DESC, relname ASC;

-- 3. Uso de índices existentes (detectar índices sin escaneos o subutilizados)
SELECT relname AS tabla,
       indexrelname AS indice,
       idx_scan AS escaneos_indice,
       pg_size_pretty(pg_relation_size(indexrelid)) AS tamano_indice
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
ORDER BY idx_scan ASC, relname ASC;

-- 4. Auditoría preventiva de integridad referencial y reglas de negocio (solo lectura)
-- 4.1 Pedidos sin registro de pago asociado
SELECT COUNT(*) AS pedidos_sin_pago
FROM pedidos p
LEFT JOIN pagos pg ON pg.pedido_id = p.id
WHERE pg.id IS NULL;

-- 4.2 Diferencias entre total del pedido y monto del pago
SELECT p.id AS pedido_id, p.total AS total_pedido, pg.monto AS monto_pago
FROM pedidos p
JOIN pagos pg ON pg.pedido_id = p.id
WHERE p.total <> pg.monto;

-- 4.3 Cupones vencidos que aún figuran activos (informativo; revisar antes de desactivar)
SELECT id, codigo, descuento, vence_en
FROM cupones
WHERE activo = TRUE
  AND vence_en IS NOT NULL
  AND vence_en < CURRENT_TIMESTAMP
ORDER BY vence_en ASC;

-- 4.4 Productos activos sin existencias disponibles (alerta de reposición)
SELECT id, referencia, nombre, stock
FROM productos
WHERE activo = TRUE
  AND stock <= 3
ORDER BY stock ASC, id ASC;

-- 5. Sesiones activas o bloqueos prolongados en PostgreSQL
SELECT pid,
       state,
       wait_event_type,
       wait_event,
       now() - query_start AS duracion
FROM pg_stat_activity
WHERE datname = current_database()
  AND pid <> pg_backend_pid()
ORDER BY query_start ASC NULLS LAST;

-- 6. Mantenimiento físico seguro (ejecutar comando a comando fuera de transacciones BEGIN/COMMIT)
-- Descomentar únicamente durante ventana de mantenimiento si n_dead_tup es elevado:
-- VACUUM (VERBOSE, ANALYZE) pedidos;
-- VACUUM (VERBOSE, ANALYZE) detalle_pedidos;
-- VACUUM (VERBOSE, ANALYZE) pagos;
-- VACUUM (VERBOSE, ANALYZE) productos;
-- VACUUM (VERBOSE, ANALYZE) clientes;
-- VACUUM (VERBOSE, ANALYZE) resenas;
