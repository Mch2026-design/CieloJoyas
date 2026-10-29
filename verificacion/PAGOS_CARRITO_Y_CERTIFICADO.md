# Pago, carrito y certificado

29 de octubre de 2026. Proyecto activo: `/Users/flavio/Documents/VScode/maricielojoyas`.

## Comportamiento

- El filtro de precio usa S/ 0 a S/ 250. Campos, deslizadores, URL y restablecimiento permanecen sincronizados.
- «Añadir al carrito» agrega una unidad con un clic. No abre el carrito ni exige abrir el detalle en piezas personalizables. La personalización opcional sigue en el detalle de la pieza.
- La primera adición a un carrito vacío muestra un aviso. Los siguientes clics acumulan unidades sin repetirlo. El contador ofrece una actualización accesible y se conservan los límites de stock y cantidad.
- Confirmación e historial usan la misma ventana nativa con fondo desenfocado. Presenta el importe, productos, entrega, estado y medio de pago.
- Yape y Plin muestran el QR original adjuntado. El lector del sistema detectó un código con contenido legible. Esta comprobación no confirma el destinatario ni la aceptación de una aplicación bancaria; no se realizaron pagos reales.
- Transferencia muestra MARICIELO GIRAO CHACALIAZA y un CCI ficticio de 20 ceros, marcado como demostración. Incluye copia del CCI. Para recibir transferencias reales, definir `PAYMENT_CCI` con el CCI correcto.
- Mientras esté pendiente, la ventana informa: «La confirmación de tu pago puede tardar hasta 5 días hábiles después de realizarlo. Cuando validemos el abono, actualizaremos el estado y habilitaremos tu certificado».
- El plazo de verificación es la política solicitada en este cambio. No se confirma un pago automáticamente por transcurrir el plazo.
- El pedido pendiente puede descargarse como PDF, sin certificado. Al confirmar el pago, se habilita la descarga individual del certificado y su incorporación al PDF de compra.
- La ventana pendiente consulta el estado cada 15 segundos mientras está visible. También ofrece actualización manual y comprueba al recuperar el foco. El historial conserva el estado actualizado.

## Protección del certificado

Java comprueba la propiedad del pedido y que el pago esté confirmado. La descarga se permite también a la sesión que creó una compra como visitante. Otra cuenta o sesión no puede consultar ni descargar un pedido ajeno. Las plantillas estáticas PDF y PNG quedan reservadas a administración; no permiten saltar la comprobación del pedido. La cancelación retira la disponibilidad del certificado.

## Pruebas

Datos ficticios creados exclusivamente en PostgreSQL temporal `cielo_pagos_pruebas`, puerto 55439. No se alteró la base de la tienda local.

- API de tienda e inventario: 32 comprobaciones correctas (`api-pagos.txt`).
- Cuentas y roles: 53 comprobaciones correctas (`cuentas-pagos.txt`).
- Propiedad del pedido, pagos y certificado: 50 comprobaciones correctas (`pagos-certificados-api.txt`).
- Navegador: 48 comprobaciones correctas, sin errores JavaScript (`pagos-flujos.json`).
- Actualización automática sin recarga ni pulsar el botón: correcta (`pago-actualizacion-automatica.txt`).
- Ventana revisada a 1440 × 1000, 1024 × 768, 375 × 812, 320 × 568 y 812 × 375. Sin desbordamiento horizontal ni ventana fuera de pantalla. QR priorizado en móvil.
- Compra real en el checkout de pruebas, historial, Yape, Plin, transferencia, aviso de cinco días y certificado habilitado tras confirmar: correctos.
- PDF pendiente: una página sin certificado. PDF pagado: dos páginas con certificado. Certificado individual: una página válida. Nombres con acentos conservados (`pagos-pdf-validacion.json`). Renderizado revisado visualmente.
- Maven: prueba de contexto sin fallos (`maven-pagos.txt`). JavaScript y HTML revisados.

## Archivos

`static/pedido.js` y `pedido.css` comparten la ventana. `app.js` gestiona carrito y filtros; `cuenta.js`, el historial. `compra-pdf.js` incorpora el certificado según el estado actual. `AppController.java` comprueba las descargas por pedido; `AccesoConfig.java` protege las plantillas.

Reiniciar Spring Boot desde VS Code y recargar con Cmd + Shift + R. No se agregan tablas ni migraciones.
