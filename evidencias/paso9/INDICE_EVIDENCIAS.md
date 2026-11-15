# Evidencias Reales — PASO 9 (Sección 9.1: Monitoreo y Funcionamiento)

Evidencias auténticas capturadas sobre la aplicación **Cielo Joyas** (`ica.joyas:maricielojoyas`) en ejecución local contra PostgreSQL `17.11` (`http://localhost:8090`).

## 1. Aplicación web y exportación Excel real

| Archivo | Descripción |
| :--- | :--- |
| [01_tienda_index.png](01_tienda_index.png) | Portada pública de la tienda (`http://localhost:8090/index.html`, HTTP 200). |
| [02_admin_reportes.png](02_admin_reportes.png) | Panel **Reportes** (`admin-reportes.html`) autenticado con `admincielo@gmail.com`: `S/ 1,227.00` ingresos confirmados, `7` pedidos, promedio `S/ 306.75`, `4` clientes y botón **Exportar Excel (.xlsx)**. |
| [02b_admin_reportes_completo.png](02b_admin_reportes_completo.png) | Vista de página completa de `admin-reportes.html`. |
| [03_admin_pedidos.png](03_admin_pedidos.png) | Panel **Pedidos** (`admin-pedidos.html`) mostrando los 7 registros reales y el botón **Exportar Excel (.xlsx)** junto a **Descargar CSV**. |
| [03b_admin_pedidos_completo.png](03b_admin_pedidos_completo.png) | Vista de página completa de `admin-pedidos.html`. |
| [04_api_health.png](04_api_health.png) | Respuesta en vivo de `GET /api/health` (`{"database":"postgres","ok":true}`). |
| [cielo-reporte-ventas-30d.xlsx](cielo-reporte-ventas-30d.xlsx) | Libro Excel real descargado desde `admin-reportes.html` (`6,949` bytes, 4 hojas: `Resumen`, `Ventas por Día`, `Productos Vendidos`, `Pedidos`). |
| [cielo-pedidos-30d.xlsx](cielo-pedidos-30d.xlsx) | Libro Excel real descargado desde `admin-pedidos.html` (`4,787` bytes, 1 hoja `Pedidos` con 7 filas de pedidos y fila de total). |
| [11_excel_reporte_ventas.png](11_excel_reporte_ventas.png) | Apertura real de `cielo-reporte-ventas-30d.xlsx` en Microsoft Excel sin advertencias ni corrupción. |
| [12_excel_pedidos.png](12_excel_pedidos.png) | Apertura real de `cielo-pedidos-30d.xlsx` en Microsoft Excel mostrando columnas, fechas y moneda `S/`. |

## 2. Monitoreo JVM en vivo con JConsole (`PID 12416`)

| Archivo | Pestaña / Contenido |
| :--- | :--- |
| [05_jconsole_overview.png](05_jconsole_overview.png) | **Overview:** Heap usado `59.7 Mb` (`59,693,208` bytes), committed `155.2 Mb`, max `8.5 Gb`; `31` hilos vivos (`35` iniciados); `18,176` clases cargadas; uso de CPU estabilizado en `0.0%` tras pico inicial de `0.6%`. |
| [06_jconsole_memory.png](06_jconsole_memory.png) | **Memory:** Detalle Heap/Non-Heap y colector G1 (`G1 Young Generation`: 12 colecciones / `0.055 s`; `G1 Concurrent GC`: 8 colecciones / `0.007 s`; `G1 Old Generation`: 0 colecciones). |
| [07_jconsole_threads.png](07_jconsole_threads.png) | **Threads:** `31` hilos activos (`27` daemon, pico `31`), incluyendo pool Tomcat `http-nio-8090-Acceptor`, `http-nio-8090-Poller` y `http-nio-8090-exec-1..10`. |
| [08_jconsole_classes.png](08_jconsole_classes.png) | **Classes:** `18,180` clases cargadas, `0` descargadas. |
| [09_jconsole_vm_summary.png](09_jconsole_vm_summary.png) | **VM Summary:** Java HotSpot(TM) 64-Bit Server VM `25.0.4+7-LTS-189`, `12416@Dinosaurio`, uptime `54.235 s`, CPU time `8.593 s`, Windows 11 `amd64` (`20` procesadores). |
| [10_jconsole_mbeans.png](10_jconsole_mbeans.png) | **MBeans:** Árbol JMX con `JMImplementation`, `com.sun.management` y `java.lang` (`GarbageCollector`, `Memory`, `MemoryManager`, `MemoryPool`). |
| [jconsole-vm-summary.txt](jconsole-vm-summary.txt) | Volcado textual de métricas de la pestaña **VM Summary** para consulta exacta en el informe. |

## 3. Evidencias de despliegue público en Render (`https://cielojoyas.onrender.com/`)

Despliegue automático tras los commits `b372cff` y `128dce8` en `https://github.com/Mch2026-design/CieloJoyas` (`GET /api/health` -> HTTP `200`, `{"database":"postgres","ok":true}`).

| Archivo | Descripción |
| :--- | :--- |
| [13_render_admin_pedidos.png](13_render_admin_pedidos.png) | Panel **Pedidos** en Render (`/admin-pedidos.html`) mostrando botones **Descargar CSV** y **Exportar Excel (.xlsx)** junto al aviso `"Archivo Excel (.xlsx) descargado."`. |
| [14_render_admin_reportes.png](14_render_admin_reportes.png) | Panel **Reportes** en Render (`/admin-reportes.html`) autenticado (`S/ 1,227.00`, `7` pedidos, `S/ 306.75` promedio, `4` clientes) con descarga Excel exitosa. |
| [15_render_tienda_publica.png](15_render_tienda_publica.png) | Vista pública de **Cielo Joyas** en `https://cielojoyas.onrender.com/` tras el despliegue actualizado. |
| [render-cielo-reporte-ventas-30d.xlsx](render-cielo-reporte-ventas-30d.xlsx) | Libro Excel real descargado desde Render (`/admin-reportes.html`, `6,931` bytes). |
| [render-cielo-pedidos-30d.xlsx](render-cielo-pedidos-30d.xlsx) | Libro Excel real descargado desde Render (`/admin-pedidos.html`, `4,775` bytes). |
