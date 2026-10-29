# Controles de Cielo y checkout

Resultado: 91 comprobaciones correctas en Chrome y WebKit, más 4 pruebas unitarias correctas de AvisosPedido. Sin errores JavaScript ni diálogos nativos durante las pruebas.

## Alcance verificado

- WhatsApp visible para visitantes en escritorio y ausente en las seis páginas administrativas.
- Ordenamiento propio con ratón, teclado y Escape; selección de departamento, provincia y distrito conservando el ubigeo.
- Selectores administrativos, cambio de estado confirmado o cancelado, colores por estado y guardado real en la base temporal.
- Edición de reseñas y vista previa de foto, confirmación propia al descartar cambios y guardado.
- Creación de proveedor y actualización del selector; calendario propio conservando el valor ISO.
- Menús dentro de la pantalla a 375 × 812 y 812 × 375; respeto de movimiento reducido.
- Errores propios de campos vacíos, teléfono y dirección; medio de pago repetido en el resumen.
- Procesando visible, cierre bloqueado y una sola petición ante varios envíos rápidos.
- Número de pedido, pago pendiente y mensaje de correo coherente con el resultado SMTP.
- Repetición de requestId sin otro pedido ni segundo correo; fallo de red seguido de reintento con datos conservados.

Las cuatro pruebas unitarias cubren correo desactivado, fallo SMTP, envío aceptado y pedido ya notificado. La aceptación SMTP no demuestra entrega final al destinatario.

## Entorno

PostgreSQL temporal en 55439, aplicación de prueba en 18083 y SMTP local de captura en 18084. No se modificó la base de uso habitual ni se enviaron mensajes a destinatarios externos. El correo real necesita configurar SMTP y activar MAIL_ENABLED.

No se añadieron carpetas de código. Se añadieron archivos dentro de los directorios existentes. El selector de archivos del disco sigue siendo responsabilidad del sistema operativo; el botón, nombre del archivo y vista previa sí pertenecen a Cielo.

## Evidencias

- `controles-cielo.json`: resultados detallados.
- `selector-chrome.png` y `selector-webkit.png`: menú propio.
- `procesando-chrome.png` y `procesando-webkit.png`: estado de envío.
- `tests/verificar_controles_cielo.cjs`: pruebas reproducibles; requiere CIELO_TEST_ALLOW_MUTATIONS=1 y una base de prueba.
- `src/test/java/ica/joyas/maricielojoyas/AvisosPedidoTests.java`: pruebas unitarias.

Fuentes técnicas: [Popover API de MDN](https://developer.mozilla.org/en-US/docs/Web/API/Popover_API) y [correo de Spring Boot](https://docs.spring.io/spring-boot/reference/io/email.html).
