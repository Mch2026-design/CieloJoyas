# Manual de Mantenimiento Preventivo, Correctivo y Diagnóstico — Cielo Joyas

Este directorio centraliza las guías y herramientas no destructivas para operar, diagnosticar y respaldar el sistema **Cielo Joyas** (`ica.joyas:maricielojoyas`, Spring Boot 4.1.2 / Java 21 / PostgreSQL 17).

---

## 1. Registro estructurado de eventos (`logback-spring.xml`)

- **Ubicación:** `src/main/resources/logback-spring.xml`
- **Comportamiento:**
  - Registra eventos simultáneamente en consola (`CONSOLA`) y en archivo rotativo (`logs/cielojoyas.log`).
  - Rotación diaria y por tamaño (`10 MB` por archivo, hasta `15` días de historial y tope global de `150 MB`).
  - Zona horaria configurada en `America/Lima`.
- **Protección de datos sensibles:**
  - Los loggers `org.hibernate.SQL` y `org.hibernate.orm.jdbc.bind` permanecen en nivel `WARN` para no registrar parámetros JDBC con correos, hashes ni tokens.
  - El patrón de salida incluye enmascaramiento defensivo (`%replace`) sobre claves como `password`, `password_hash`, `clave`, `token`, `csrf`, `secret`, `api_key` y `authorization`.
  - Nunca almacenes el archivo `.env` ni la carpeta `logs/` en control de versiones (ambos están excluidos en `.gitignore`).

---

## 2. Monitoreo JVM y de hilos con JConsole (`jconsole-monitoreo.ps1`)

1. Inicia la aplicación Spring Boot con variables de entorno cargadas (por ejemplo `.\mvnw.cmd spring-boot:run`).
2. Verifica o abre **JConsole** ejecutando:
   ```powershell
   # Solo verificación previa (no abre ventana)
   .\mantenimiento\jconsole-monitoreo.ps1 -DryRun

   # Conectar automáticamente al PID local de MaricielojoyasApplication
   .\mantenimiento\jconsole-monitoreo.ps1
   ```
3. **Qué revisar periódicamente en JConsole:**
   - **Memory / Heap:** confirmar que tras recolecciones GC el consumo base se estabilice y no haya crecimiento sostenido tras generar reportes `.xlsx`.
   - **Threads:** verificar que el pool de hilos HTTP (`http-nio-*`) no presente hilos bloqueados (`BLOCKED`) de forma prolongada.
   - **VM Summary / CPU:** comprobar carga de CPU y tiempo acumulado en Garbage Collector.
   - **MBeans (`com.zaxxer.hikari`):** revisar conexiones activas/inactivas del pool hacia Supabase PostgreSQL para detectar fugas de conexión.
4. En paralelo, el endpoint de salud **Spring Boot Actuator** está disponible en `/actuator/health`.

---

## 3. Respaldo preventivo de base de datos (`backup-bd.ps1` y `backup-bd.sh`)

Los scripts generan volcados binarios comprimidos (`--format=custom`) en modo **solo lectura** sin alterar las tablas existentes ni imprimir credenciales en consola (`PGPASSWORD` solo vive temporalmente en la memoria del proceso).

- **Windows PowerShell:**
  ```powershell
  # Validar parámetros y ruta de salida sin ejecutar pg_dump
  .\mantenimiento\backup-bd.ps1 -DryRun

  # Ejecutar respaldo real (requiere pg_dump en PATH)
  .\mantenimiento\backup-bd.ps1
  ```
- **Linux / macOS / Git Bash:**
  ```bash
  ./mantenimiento/backup-bd.sh --dry-run
  ./mantenimiento/backup-bd.sh
  ```
- **Política recomendada:**
  - Guardar los `.dump` en `mantenimiento/backups/` (excluido de Git) o en almacenamiento externo cifrado.
  - Conservar copia de la carpeta de evidencias (`uploads/reviews`) definida en `REVIEW_UPLOAD_DIR`.
  - Probar la restauración al menos una vez sobre una base local vacía con `pg_restore --clean --if-exists --no-owner --dbname=maricielojoyas_prueba <archivo.dump>`.

---

## 4. SQL de diagnóstico y mantenimiento preventivo (`mantenimiento-preventivo.sql`)

- **Principio de seguridad:** el archivo solo contiene consultas `SELECT` sobre vistas de catálogo/negocio (`pg_statio_user_tables`, `pg_stat_user_tables`, `pg_stat_user_indexes`, `pg_stat_activity`, `pedidos`, `pagos`, `cupones`, `productos`) y sentencias `VACUUM (VERBOSE, ANALYZE)` **comentadas**. No incluye `DROP`, `TRUNCATE`, `DELETE` ni `UPDATE`.
- **Uso recomendado:**
  1. Ejecutar las secciones `1` a `5` en el SQL Editor de Supabase o `pgAdmin 4` / `psql` mensualmente o tras picos de pedidos.
  2. Si `filas_muertas` (`n_dead_tup`) supera el 15–20 % de las filas activas y `last_autovacuum` es antiguo, ejecutar manualmente `VACUUM (VERBOSE, ANALYZE) <tabla>;` **fuera** de cualquier bloque `BEGIN ... COMMIT`.
