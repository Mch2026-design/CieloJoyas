# Productos y certificado de compra

29 de octubre de 2026. Proyecto activo: `/Users/flavio/Documents/VScode/maricielojoyas`.

## Resultado

- Se retira Resumen del panel. El acceso administrativo abre `productos.html`. Los enlaces anteriores `admin.html` y `admin-productos.html` redirigen desde Java al nuevo apartado.
- Productos muestra una lista con foto, referencia, categoría, precio, existencias, visibilidad y acciones. Incluye búsqueda y filtros conservados en la URL.
- Agregar y editar usan una ventana nativa accesible. Tiene cierre con Escape, foco contenido y opción de conservar o descartar una edición sin guardar.
- Se elimina el campo de precio anterior. El certificado deja de solicitarse como URL: se usa el archivo original adjuntado para todas las piezas, incluidas las ya existentes.
- Eliminar retira la pieza del catálogo público y conserva sus pedidos anteriores. Desde Editar puede volver a publicarse.
- La confirmación y el historial permiten descargar la información de compra con el certificado en un mismo PDF. El certificado también puede descargarse por separado.
- El documento usa los nombres, dirección, productos, importes y estado de pago guardados por Java. El navegador genera el PDF con una biblioteca local, sin enviar datos a terceros.
- La imagen original se conserva sin edición. El certificado en PDF aparece como última página del documento de compra.

## Validación

Todas las compras y cuentas ficticias se crearon exclusivamente en PostgreSQL temporal, en `cielo_productos_pruebas` y `cielo_productos_final`, puerto 55439. No se modificaron los datos de la tienda local.

- API de compras e inventario: 32 comprobaciones correctas (`api-productos-pdf.txt`).
- Cuentas, roles y páginas protegidas: 53 comprobaciones correctas (`cuentas-productos-pdf.txt`).
- Maven: compilación y prueba de contexto sin fallos (`maven-productos-pdf.txt`).
- Navegador: 61 comprobaciones correctas, sin errores JavaScript (`productos-pdf-flujos.json`).
- Lista y ventana revisadas a 1440 × 1000, 1024 × 768, 768 × 1024, 375 × 812, 320 × 568 y 812 × 375. Sin desbordamiento horizontal; la ventana queda dentro de la pantalla y conserva acceso a todos los campos.
- Alta, edición, eliminación, nueva publicación, confirmación de cambios sin guardar y búsqueda tras recarga comprobadas con la API real.
- Checkout real y descarga desde el historial comprobados en Chrome. Historial revisado también a 320 px.
- PDF de compra y de historial: dos páginas válidas, con acentos y certificado intacto. Prueba de 50 líneas con nombres largos: nueve páginas y todas las líneas presentes. El hash de la imagen del certificado coincide con el original en los tres documentos (`pdf-verificacion.json`).
- PDFs renderizados y revisados visualmente: datos, totales, filas largas y certificado sin recortes ni superposiciones.

## Archivos principales

- `static/productos.html`, `productos-admin.js`, `productos-admin.css`.
- `static/compra-pdf.js` y copia local de pdf-lib 1.17.1 con licencia MIT en `static/vendor/`.
- `static/assets/certificado-material.png` y `certificado-material.pdf`.
- Certificado independiente para compartir: `output/pdf/certificado-material.pdf`.

Reinicia Spring Boot desde VS Code y recarga con Cmd + Shift + R. No hay tablas nuevas ni migraciones adicionales.
