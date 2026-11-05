# Confirmaciones de pedido en Render

Render Free bloquea conexiones salientes a los puertos SMTP 25, 465 y 587. Una configuración de Gmail que funciona en la laptop no puede enviar por esos puertos desde este plan.

Fuentes: [limitaciones de Render Free](https://render.com/docs/free), [API transaccional de Brevo](https://developers.brevo.com/reference/send-transac-email).

## Configuración por HTTPS

Verificar el remitente y habilitar el servicio transaccional en Brevo. En Render, en Environment, configurar:

```dotenv
MAIL_ENABLED=true
MAIL_TRANSPORT=brevo
MAIL_FROM=correo_verificado_en_brevo
BREVO_API_KEY=clave_API_privada_de_Brevo
```

Usar una clave API, no la clave SMTP. Guardarla exclusivamente en variables privadas del servidor. El frontend no recibe la clave. Las variables SMTP existentes no se utilizan cuando `MAIL_TRANSPORT=brevo`.

Publicar el código y desplegarlo con estas variables. El backend envía a `https://api.brevo.com/v3/smtp/email`, por HTTPS. Conserva el destinatario del pedido, el resumen histórico, el texto plano y el HTML existentes. No añade contactos a campañas ni envía copias del pedido.

## Verificación

- `mvn test`: pruebas del contenido, reintentos, fallos y adaptador HTTPS con un proveedor simulado.
- Crear un pedido de prueba dirigido exclusivamente a un correo autorizado por su propietario.
- Comprobar la respuesta `emailStatus=sent`, el evento de entrega en Brevo y la recepción en la bandeja de entrada o spam.
- `sent` y `correo_enviado_en` indican aceptación por el proveedor, no garantizan llegada a la bandeja de entrada.
- Un rechazo del proveedor conserva el pedido y no registra fecha de envío. No se reintenta automáticamente un envío HTTP para evitar duplicados ante respuestas perdidas.

La base de Supabase guarda el pedido y la fecha de aceptación del correo. No envía esta confirmación y no necesita cambios de esquema para usar HTTPS.

## Uso local

`MAIL_TRANSPORT=smtp` mantiene el envío SMTP existente. Es el valor predeterminado. La configuración privada `application-local.properties` sigue funcionando localmente y no se incorpora a la imagen Docker.
