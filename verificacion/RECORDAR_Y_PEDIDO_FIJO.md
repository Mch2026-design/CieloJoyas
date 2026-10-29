# Recuérdame y confirmación del pedido

Comprobado el 29 de octubre de 2026 en `/Users/flavio/Documents/VScode/maricielojoyas`.

«Recuérdame» aparece debajo del teléfono en todos los documentos que incluyen checkout. Se guarda la preferencia por cuenta solamente dentro de la transacción de una compra válida, con consentimiento explícito. Se autocompletan los datos en una compra posterior y después de volver a iniciar sesión. «Olvidar datos guardados» retira la preferencia. El formulario limpia los campos antes de consultar la sesión actual para evitar mostrar datos de otra cuenta. No se usa localStorage para direcciones o documentos.

El servidor obtiene la cuenta de la sesión, valida el pedido y guarda una referencia a la compra recordada en `usuarios.pedido_recordado_id`. La consulta comprueba también la propiedad del pedido. GET y DELETE `/api/account/delivery` requieren sesión; la eliminación requiere CSRF. No se guarda como preferencia el medio de pago ni el carrito.

En escritorio la ventana del pedido permanece fija sin desplazamiento. Se usa selección de vista para las piezas y los datos de entrega/documentos y paginación para pedidos largos. En móvil/tablet la ventana admite desplazamiento, mantiene QR o datos bancarios primero y conserva las acciones de cierre y estado accesibles.

## Pruebas

Todas las mutaciones se ejecutaron en PostgreSQL temporal, puerto 55439, base `cielo_recordar_pruebas`.

| Comprobación | Resultado |
|---|---:|
| Compra, inventario y moderación existentes | 32 correctas |
| Cuentas y roles | 53 correctas |
| Pagos y certificados | 50 correctas |
| Recuérdame, consentimiento y aislamiento | 21 correctas |
| Navegador, formulario y responsividad | 27 correctas |
| Login del administrador SQL y acceso al catálogo | 2 correctas |
| Contexto Java | 1 correcta |

El navegador verificó compra real desde el formulario, guardado opcional, autocompletado de ubicación, eliminación y ausencia de datos previos al salir de la cuenta. Sin errores JavaScript. Se midió ausencia de scroll o contenido recortado en escritorio a 1440 × 900, 1366 × 768 y 1280 × 640. En 375 × 812, 320 × 640 y 812 × 375 se comprobó desplazamiento permitido, QR primero y ausencia de desbordamiento horizontal.

Capturas revisadas: `pedido-fijo-escritorio.png` y `pedido-qr-movil.png`. Resultados: `api-recordar*.txt`, `navegador-recordar.json`, `login-admin-sql.txt`, `java-recordar.txt` y `sql-admin-conteo.txt`.

## Migraciones y puesta en marcha

`migracion_02_cuentas_y_roles.sql` crea o restablece exclusivamente el correo solicitado `admincielo@gmail.com` como ADMIN activo con contraseña `admin123`, almacenada como hash PBKDF2-HMAC-SHA256 de 210000 iteraciones y sal. Dos ejecuciones conservaron exactamente una cuenta para ese correo. Se probó su login real contra Java y su acceso al catálogo administrativo.

`migracion_04_recordar_entrega.sql` incorpora la preferencia por cuenta y la relación con el pedido, incluso si Hibernate ya creó la columna. Es repetible. Se mantienen exactamente 12 tablas.

No se aplicaron estas dos migraciones a la base local de la tienda durante esta actualización. El usuario debe detener Java, ejecutar 02 y luego 04 completas en Query Tool sobre maricielojoyas, volver a iniciar con F5 y recargar con Cmd + Shift + R. La migración 03 de reseñas ya se entregó y aplicó anteriormente.
