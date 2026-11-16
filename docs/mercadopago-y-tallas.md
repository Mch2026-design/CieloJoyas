# Mercado Pago, tallas por stock y autenticación — Cielo Joyas

Documentación técnica de los módulos incorporados para la entrega final del sistema.

## 1. Pasarela de pagos con tarjeta (Mercado Pago Checkout API)

- **Endpoints (`MercadoPagoController.java` / `MercadoPagoService.java`):**
  - `GET /api/payments/mercadopago/config`: devuelve `{ provider, enabled, sandbox, publicKey }` al frontend sin exponer tokens privados.
  - `POST /api/payments/mercadopago/preference`: genera la referencia de pago para el pedido.
  - `POST /api/payments/mercadopago/pay`: procesa el token de tarjeta generado en cliente con `MercadoPago.js` v2, registra el pago, marca el pedido como `pagado`, descuenta inventario (incluyendo stock por talla) y habilita comprobante PDF y certificado de autenticidad.
  - `POST /api/payments/mercadopago/webhook`: recibe notificaciones HTTP verificando firma HMAC (`x-signature`) con `MERCADOPAGO_WEBHOOK_SECRET`.
- **Variables de entorno requeridas (`.env` / Render):**
  - `MERCADOPAGO_ENABLED=true|false`
  - `MERCADOPAGO_SANDBOX=true|false`
  - `MERCADOPAGO_PUBLIC_KEY` (`TEST-...` en sandbox o `APP_USR-...` en producción)
  - `MERCADOPAGO_ACCESS_TOKEN` (secreto privado de servidor)
  - `MERCADOPAGO_WEBHOOK_SECRET` (secreto HMAC para `/api/payments/mercadopago/webhook`)
- **Reglas en administración (`/admin/admin-pedidos.html`):**
  - Los pedidos pagados con tarjeta restringen transiciones manuales impropias en el selector de estado y muestran el botón **Descargar PDF** junto a **Ver detalle** usando `CieloPDF.downloadOrder`.

## 2. Inventario por talla US en anillos (`producto_tallas`) y grabado

- **Migración SQL:** `sql/migracion_04_mercadopago_y_tallas.sql` (idempotente).
  - Crea la tabla `producto_tallas` (`id`, `producto_id`, `talla`, `stock`) mapeada por la entidad JPA `ProductoTalla` y `ProductoTallaRepository`.
  - Añade `admite_grabado` y `requiere_talla` en `productos`.
- **Flujo en tienda (`/tienda/catalogo.html`, `app.js`):**
  - Los anillos exigen seleccionar una talla US con stock (`4 (US)`–`8 (US)` por defecto, con lista desplazable si supera 15 tallas) antes de agregar al carrito.
  - Incluye el modal **Conversión de tallas** (equivalencia diámetro/circunferencia mm a talla US `4.5`–`10`) adaptado a tema claro y oscuro.
  - El grabado opcional (habilitado por producto desde `/admin/productos.html`) acepta solo letras mayúsculas hasta 16 caracteres.
- **Integridad transaccional (`PedidoService.java`):**
  - Al confirmar el pago (`pagado`), se descuenta stock tanto del producto como de la fila correspondiente en `producto_tallas`; si un pedido pagado pasa a `cancelado`, se repone exactamente una vez.

## 3. Política de contraseñas, Google OAuth y panel administrativo

- **Contraseñas seguras (`AccesoService.java`, `registro.html`, `configurar-admin.html`, `acceso.js`):** mínimo **12 caracteres** (máximo 128), al menos 1 letra mayúscula, 1 dígito y 1 carácter especial.
- **Google OAuth (`SUPABASE_URL`, `SUPABASE_ANON_KEY`):** redirección dinámica por el origen actual del navegador (`window.location.origin`) en `localhost` o `cielojoyas.onrender.com`.
- **Panel administrativo:** la sección **Proveedores** reemplaza la etiqueta anterior **Inventario** en todas las vistas `admin/*.html`, con soporte completo para tema claro y oscuro.
