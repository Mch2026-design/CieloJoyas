# Reseñas y fotografías

Implementación comprobada el 29 de octubre de 2026 en `/Users/flavio/Documents/VScode/maricielojoyas`.

«Nosotras» ahora es «Reseñas», con un HTML propio, navegación activa, tarjetas con foto ampliable y producto, filtro por pieza y diseño para escritorio y móvil. La dirección anterior redirige a la página nueva. Se conserva el estilo de Cielo.

El formulario muestra solo productos comprados por la cuenta con pago confirmado, recibe texto y un archivo JPG/PNG con vista previa y envía a moderación. Java obtiene el autor de la sesión y comprueba propiedad, pago, producto y duplicados; conocer el correo o el número del pedido no permite publicar. Una fotografía es obligatoria. Se comprueba el formato real y los límites de tamaño y dimensiones, se normaliza sin metadatos a JPEG y se guarda fuera de los recursos estáticos. CSRF también se aplica a multipart.

En Posventa se muestra la foto y se puede publicar u ocultar cada reseña. Las fotos pendientes están reservadas a la propietaria y administración. Ocultar la reseña o cancelar el pago retira tanto la reseña como su foto pública. Las ventanas tienen fondo desenfocado, cierre con Escape y recorrido de foco por teclado.

## Base local

La migración `sql/migracion_03_resenas_con_fotos.sql` ya fue aplicada a `maricielojoyas` en el puerto 5432. Antes: 3 pedidos, 8 productos y 0 reseñas. Después: los mismos 3 pedidos y 8 productos, y 5 reseñas con fotografía. Se mantienen 12 tablas.

Los cinco registros son ejemplos explícitos con nombres ficticios y fotografías ilustrativas del catálogo. La columna `demostracion`, la etiqueta visible y una restricción de PostgreSQL los separan de las compras verificadas. No se crearon pedidos falsos ni ingresos para respaldarlos. La instalación inicial también incluye estos ejemplos. La migración repetida conservó exactamente cinco ejemplos.

## Verificación

Todas las compras, cuentas y cargas de prueba se realizaron sobre la instancia temporal PostgreSQL 55439, base `cielo_resenas_pruebas`; sus datos no se incorporaron a la tienda local.

| Comprobación | Resultado |
|---|---:|
| API de tienda | 32 correctas |
| Cuentas y roles | 53 correctas |
| Pagos y certificados | 50 correctas |
| Reseñas, fotos y propiedad | 38 correctas |
| Navegador | 32 correctas |
| Contexto Java | 1 correctas |

La prueba de navegador usa Chrome con Playwright. Comprueba la carga de las cinco fotos, enlaces de producto, filtros, acceso, formulario, carga de foto, moderación y sello de compra real, sin errores JavaScript. Verifica ausencia de desbordamiento horizontal a 320, 375, 768 y 1440 píxeles y formulario encuadrado a 375 × 812, 812 × 375 y 1440 × 1000. Se corrigió el recorrido de Tab dentro del diálogo y se comprobó de nuevo. También se ocultó visualmente el anuncio del carrito destinado a lectores de pantalla.

Evidencias: `api-resenas-base.txt`, `api-resenas-cuentas.txt`, `api-resenas-pagos.txt`, `api-resenas.txt`, `navegador-resenas.json`, `java-resenas.txt` y `resenas-base-local.txt`. Vistas: `resenas-1440.png`, `resenas-375.png`, `resenas-movil.png` y `resenas-formulario-375.png`. Se revisaron las capturas de escritorio y móvil. El paquete JAR final contiene los mismos HTML, CSS y JavaScript verificados.

## Puesta en marcha

Detén la ejecución anterior de Java y vuelve a iniciarla con F5. Recarga el navegador con Cmd + Shift + R y abre Reseñas desde la barra superior. La actualización de la base ya está hecha en este equipo.

Las fotos reales se conservan en `uploads/reviews` o en `REVIEW_UPLOAD_DIR`. Respalda esa carpeta junto con PostgreSQL. Se conservaron las dependencias Java existentes.
