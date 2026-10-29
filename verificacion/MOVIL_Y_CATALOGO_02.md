# Responsividad y catálogo 02

Verificado el 29 de octubre de 2026 en una base PostgreSQL temporal, sin ejecutar la migración en la base del usuario.

- WhatsApp flotante oculto hasta 1000 px; Contacto tiene una tarjeta estática.
- Menú móvil con imagen de colección y enlaces de 56 px de alto.
- Secciones con mayor separación, acceso a Tobilleras en Inicio y controles de precio amplios.
- Menú y filtros comprobados a 320, 375, 600, 812, 1000 y 1440 px, incluyendo 812 × 375 horizontal.
- Catálogo: 60 productos nuevos, 10 por cada una de seis categorías, exactamente 30 personalizables.
- Precios: piezas individuales desde S/ 40; collares S/ 70 a S/ 85; máximo S/ 239.
- 60 respuestas HTTP 200 de imágenes, 60 hashes distintos y resolución mínima de 900 px por lado.
- Revisión visual de las 60 fotos en seis hojas de categoría.
- La migración se ejecutó varias veces: primera inserción de 60, siguientes de 0. Conserva las ediciones manuales y los ocho productos originales.
- La API y la interfaz muestran 68 productos y todas sus categorías. Filtros, Personaliza, detalle y persistencia de grabado/medida comprobados.
- Prueba de navegador: 73 comprobaciones correctas, sin errores JavaScript. Incluye movimiento reducido.

## Archivos

- `sql/migracion_02_catalogo_60_productos.sql`: archivo completo para copiar en pgAdmin 4 tras migración 01.
- `catalogo_02_fuentes.csv`: página de origen y URL de cada fotografía.
- `catalogo_02_imagenes.json`: estado HTTP, resolución y hash de cada archivo.
- `movil-catalogo-02.json`: resultados de la prueba de navegador.
- `menu-movil-final.png`, `filtros-movil-final.png`, `contacto-movil-final.png`: capturas finales.
- `catalogo-02-1.jpg` a `catalogo-02-6.jpg`: revisión de imágenes por categoría.

Los precios y existencias son demostrativos. Las fotografías externas ilustran los diseños; no acreditan material 316L ni existencias reales. Las fuentes se incluyen también en los comentarios del SQL.
