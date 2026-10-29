# Correo real de pedidos de Cielo Joyas

Implementado y comprobado el 29 de octubre de 2026 con Java 21, Spring Boot Mail y PostgreSQL. La aplicación conserva su frontend HTML/CSS/JavaScript y sus 12 tablas.

## Resultado

- El pedido guardado genera un correo HTML y una alternativa de texto, con remitente visible Cielo Joyas.
- Incluye saludo, número de pedido, productos, cantidades, grabado, talla, color, importes, descuento, total, dirección, referencia y medio de pago.
- Los datos proceden del pedido persistido. Java utiliza el correo de la cuenta registrada y descarta un correo alternativo enviado por el navegador. También mantiene la compra como visitante.
- La confirmación informa de pago pendiente y envío por cotizar cuando corresponde. No atribuye al sistema un cobro automático.
- La fecha `correo_enviado_en` se guarda después de la aceptación SMTP. Los reintentos ordinarios y concurrentes no vuelven a enviar un mensaje ya aceptado. Si SMTP rechaza el mensaje, el pedido continúa guardado y el checkout informa del fallo.

## Configuración real en este equipo

`application-local.properties` contiene la configuración SMTP facilitada en la referencia de Naia, adaptada a Spring. Tiene permisos 600, está excluido en `.gitignore` y no se incluye en el JAR. El nombre visible del remitente es Cielo Joyas. No se enviaron copias a los destinatarios de la referencia.

Spring lo carga al iniciar desde la raíz del proyecto. Las credenciales no aparecen en esta guía ni en los informes. Para otro equipo, consulta las variables de `.env.example` y la sección de correo de `README.md`.

Java verificó autenticación y TLS con el servidor real. Después se creó una cuenta con el correo de la misma cuenta SMTP y se registró una compra en una base temporal. El SMTP real aceptó un único mensaje de Cielo Joyas; repetir la petición no produjo otro envío. El producto estaba identificado como prueba de correo y sin envío de productos. No se hizo un cobro ni se añadieron compras a la base habitual. La base temporal se eliminó.

La aceptación SMTP acredita el envío al proveedor. No se inspeccionó la bandeja de entrada del destinatario ni se verificó entrega final. Revisa también spam si el mensaje no aparece.

## Base de datos y arranque

Se aplicó `sql/migracion_03_correo_pedidos.sql` sobre la base local `maricielojoyas`. La columna ya existía y se conservó; se añadió su comentario explicativo. La migración puede repetirse y no altera clientes, precios, inventario ni pedidos. La migración 01 incluye la columna para instalaciones nuevas. No necesitas volver a ejecutar la 01 para activar el correo.

La versión final empaquetada se inicia en `http://localhost:8080/`. Para ejecutarla otra vez desde la raíz del proyecto:

```sh
java -jar target/maricielojoyas-0.0.1-SNAPSHOT.jar
```

Si esa dirección ya está ocupada por otra instancia de Cielo, detén esa instancia antes de iniciar otra. Para trabajar desde VS Code también puedes usar F5 con la configuración existente.

## Verificación

- Maven: 11 pruebas correctas, incluyendo 10 pruebas unitarias de correo y el arranque del contexto Java.
- Flujo completo: 23 comprobaciones correctas en PostgreSQL temporal y SMTP local. Incluyen cuenta registrada, destinatario de sesión, resumen MIME, descuento, privacidad, historial, rechazo SMTP, recuperación al repetir y reintentos concurrentes.
- SMTP real: autenticación TLS y aceptación de un mensaje real de compra a la misma cuenta remitente.
- Presentación: vista de escritorio a 800 px y móvil a 375 px, sin desplazamiento horizontal. Texto de 16 px, tabla con encabezados, cantidades e importes claros y contraste mínimo de 4.5:1 para el texto normal del mensaje.

Para reproducir las pruebas locales necesitas Java, Maven, PostgreSQL y Python 3.9 a 3.11 con `smtpd`. El script crea y elimina exclusivamente una base cuyo nombre comienza con `cielo_correo_test_`; requiere permiso para crear bases. No utiliza SMTP externo:

```sh
CIELO_TEST_ALLOW_MUTATIONS=1 python3 tests/verificar_correo_pedidos.py
```

## Revisión con las habilidades solicitadas

`ui-ux-pro-max`: revisión de confirmación explícita, legibilidad, contraste y adaptación móvil. El aviso conserva número de pedido y resultado del envío con mensajes comprensibles.

`Relationship-Centric Interfaces`: continuidad entre la cuenta, el pedido, el correo y el historial; transparencia sobre recepción, pago y envío; recuperación del aviso fallido sin repetir la compra. No se añadieron datos de seguimiento ni nuevas preferencias de memoria.

`graphify`: consulta del grafo documental existente con los términos de su vocabulario `cliente correo pedido pago piezas total`. El grafo describe Cliente, Pedido y Pago en las páginas 36 a 38 del informe original. Es un modelo conceptual anterior y no contiene la implementación de correo actual. Se usó para revisar las relaciones del dominio; la funcionalidad actual se comprobó con código y pruebas. No hubo nueva extracción: 0 tokens de extracción LLM añadidos al grafo en esta revisión.

## Evidencias

- `correo-real-resultado.json` y `correo-real-prueba.txt`: aceptación SMTP real y destinatario descrito sin publicar su dirección.
- `smtp-real-conexion.txt`: autenticación TLS desde Java.
- `correo-pedidos-resultados.json` y `correo-pedidos-prueba.txt`: las 23 comprobaciones del flujo local.
- `maven-correo-completo.txt`: compilación y las 11 pruebas de Java.
- `sql-correo-local.txt`: aplicación de la migración local.
- `correo-pedido-vista-previa.html` y `correo-pedido-vista-previa.txt`: ejemplo generado por el envío Java con datos ficticios.
- `correo-pedido-escritorio.png` y `correo-pedido-movil.png`: presentación de ese ejemplo.

Documentación técnica consultada: [correo en Spring Boot](https://docs.spring.io/spring-boot/reference/io/email.html), [MIME en Spring Framework](https://docs.spring.io/spring-framework/reference/integration/email.html) y [configuración externa de Spring Boot](https://docs.spring.io/spring-boot/reference/features/external-config.html).
