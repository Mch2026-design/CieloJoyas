# Cielo Joyas

Proyecto para Visual Studio Code con Java 21, Maven, Spring Boot, PostgreSQL y frontend en HTML, CSS y JavaScript. La apariencia procede de las referencias originales. Los archivos de backend de esas referencias no se utilizaron.

## Iniciar en Visual Studio Code

1. Abre esta carpeta completa: `maricielojoyas`.
2. En pgAdmin 4 crea una base **nueva** llamada `maricielojoyas`. Puedes hacerlo desde la interfaz o ejecutar `sql/00_crear_base.sql` sobre `postgres`, con Auto commit.
3. Abre un Query Tool **sobre maricielojoyas**. Copia y ejecuta todo `sql/migracion_01_incio.sql`.
4. Para ampliar el catálogo, ejecuta `sql/migracion_02_catalogo_60_productos.sql` en el mismo Query Tool. En instalaciones anteriores, ejecuta también `sql/migracion_03_correo_pedidos.sql` para registrar los avisos enviados.
5. Copia `.env.example` como `.env` y escribe la contraseña de tu servidor PostgreSQL en `DB_PASSWORD`.
6. En **Ejecutar y depurar**, elige **Cielo Joyas: iniciar tienda y API** y presiona F5.

Abre [la tienda](http://localhost:8080/) o [inicia sesión](http://localhost:8080/login.html). Si aún no existe un administrador, la pantalla de acceso muestra «Configurar administrador». Ábrela desde este mismo equipo y elige tu correo y contraseña. Esta configuración se permite una sola vez. El script inicial no crea credenciales.

La migración 01 reúne el esquema inicial, las cuentas, las reseñas y Recuérdame. Si ya la ejecutaste, añade únicamente la nueva migración 02 de catálogo. Conserva productos, clientes y pedidos; al repetirla no se duplican referencias ni se restablece stock.

Usa Spring Boot para abrir las páginas. No abras los HTML con doble clic ni uses Live Server, porque necesitan la API Java en el mismo servidor.

## Conexión con PostgreSQL

En `src/main/resources/application.properties`:

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/maricielojoyas}
spring.datasource.username=${DB_USER:postgres}
spring.datasource.password=${DB_PASSWORD:}
spring.jpa.hibernate.ddl-auto=update
```

Puedes escribir la contraseña directamente en esa propiedad para trabajar localmente. La configuración F5 carga `.env`. Si ejecutas `./mvnw spring-boot:run` o `mvnw.cmd spring-boot:run` desde una terminal, define las variables allí primero: Maven no lee `.env` por sí solo.

La contraseña es la del usuario del servidor PostgreSQL, no la contraseña maestra de pgAdmin. Si usas otro puerto, cambia `DB_URL`. El nombre `decision_digital` del ejemplo se sustituyó por `maricielojoyas`.

Ejecuta la migración antes de iniciar para incorporar índices y restricciones. La migración se puede repetir sobre este mismo modelo sin duplicar las categorías, productos y cupón iniciales. Es una instalación para una base nueva; no transforma ni elimina el esquema anterior de 18 tablas.

## Páginas independientes

| Archivo | Contenido |
|---|---|
| `index.html` | Portada, selección editorial y acceso a las secciones |
| `catalogo.html` | Catálogo completo con búsqueda, categorías, precio y ordenamiento |
| `novedades.html` | Piezas recientes y esenciales |
| `personaliza.html` | Presentación y catálogo de piezas personalizables |
| `resenas.html` | Fotos, reseñas por pieza y formulario para compras verificadas |
| `nosotras.html` | Redirección a Reseñas para conservar enlaces anteriores |
| `contacto.html` | Contacto, garantías y suscripción |
| `ofertas.html` | Productos cuyo precio anterior supera al actual |
| `productos.html` | Entrada del panel, catálogo de administración, búsqueda y filtros |
| `admin.html` | Redirección a Productos para conservar enlaces anteriores |
| `admin-pedidos.html` | Pedidos, detalle, estados y exportación CSV |
| `admin-productos.html` | Redirección a Productos para conservar enlaces anteriores |
| `admin-reportes.html` | Estadísticas y reportes de ventas |
| `admin-compras.html` | Compras y proveedores |
| `admin-posventa.html` | Garantías, moderación y edición de texto/fotos de reseñas |
| `admin-promociones.html` | Cupones y clientas suscritas |
| `login.html` | Acceso de clientes y administradores |
| `registro.html` | Creación de cuentas de clientes |
| `configurar-admin.html` | Configuración local del primer administrador |
| `cuenta.html` | Historial y detalle de los pedidos de la cuenta |

El menú superior cambia de archivo HTML y señala la página actual. Las categorías de la portada abren el catálogo con su filtro aplicado. Los filtros se conservan en la URL, por ejemplo `catalogo.html?categoria=Anillos`. El carrito y sus grabados se conservan en este navegador al cambiar de página o recargar; puedes borrar la selección guardada.

## Java y estructura

```text
src/main/java/ica/joyas/maricielojoyas/
  MaricielojoyasApplication.java
  modelo/       12 clases con getters y setters
  repositorio/  12 interfaces NombreRepository.java
  controller/   AppController.java, ResenasController.java, ErroresApi.java y AccesoConfig.java
  validacion/   PedidoService.java, AccesoService.java, ClaveAcceso.java,
                Reglas.java, Ubigeos.java, FotosResenas.java y AvisosPedido.java
src/main/resources/
  application.properties
  ubigeos.tsv
  static/       páginas, CSS, JavaScript y datos de ubicaciones
sql/
  00_crear_base.sql
  migracion_01_incio.sql
  migracion_02_catalogo_60_productos.sql
  migracion_03_correo_pedidos.sql
```

Se conservan **Spring Web**, **Spring Data JPA** y **PostgreSQL Driver**, y se incorpora **Spring Boot Mail** para los avisos opcionales por correo. El proyecto original usa `spring-boot-starter-webmvc` para Spring Web. Las dependencias de prueba originales también se conservan. No hay Lombok, Spring Security ni servidor Node. El nombre técnico de la carpeta, la base y el paquete Java sigue siendo `maricielojoyas`; la marca de la tienda es **Cielo Joyas**.

## Cuentas y administración

El registro público siempre crea el rol `CLIENTE`. La sesión del administrador abre `productos.html`; la del cliente abre `index.html`, tanto al iniciar sesión como al registrarse. El registro solicita nombres, apellidos, correo y una contraseña de al menos 8 caracteres. La cabecera muestra solo el primer nombre y un icono de cierre de sesión junto al carrito. El nombre abre el panel o el historial de pedidos, según el rol. Java comprueba el rol en las operaciones administrativas de productos, pedidos y reportes. Las contraseñas se guardan como hashes PBKDF2 y el acceso utiliza una sesión del servidor con cookie HttpOnly. Cerrar sesión retira el acceso.

Los pedidos realizados con una sesión de cliente se vinculan a esa cuenta y aparecen en su historial. Los pedidos anteriores como visitante se conservan; no se asignan automáticamente a una cuenta nueva. También puedes seguir comprando como visitante. El carrito se conserva al entrar o registrarte.

Cada apartado administrativo abre un HTML independiente desde la barra superior. En móviles, esa barra permite desplazamiento horizontal y mantiene visible el apartado activo. Java exige una sesión administrativa para cada uno de esos documentos.

Productos muestra primero el catálogo, con búsqueda por nombre o referencia y filtros por categoría y estado. «Agregar producto» y «Editar» abren una ventana accesible. El formulario no solicita precio anterior ni una URL de certificado; todas las piezas incluyen el certificado proporcionado por la tienda. El cierre de una edición con cambios ofrece conservarlos o descartarlos.

El panel permite agregar, consultar, editar y eliminar productos del catálogo. Eliminar los oculta para conservar los pedidos históricos; puedes reactivarlos con «Editar». Los reportes muestran ingresos confirmados, promedio, ventas diarias, productos vendidos, estados y medios de pago, con filtro de periodo y exportación CSV de los pedidos mostrados.

## Las 12 tablas

`usuarios`, `clientes`, `categorias`, `productos`, `cupones`, `pedidos`, `detalle_pedidos`, `pagos`, `proveedores`, `compras`, `garantias` y `resenas`.

El rol se guarda en `usuarios`; la fotografía y el enlace de certificado se guardan en `productos`. Cada fila de `compras` registra un producto, costo y cantidad. Los sets tienen su propio producto y stock. El inventario controla cantidades, sin identificación ni certificado por unidad física y sin descomponer automáticamente los sets. Los ubigeos se ofrecen desde JSON y se validan en Java sin añadir una tabla.

## Funciones conectadas

- Precios, stock, cupones y pedidos se consultan y guardan en PostgreSQL.
- Java recalcula el total y conserva los precios de cada pedido.
- Cada grabado o talla se guarda como una línea independiente.
- Comprar crea un pago pendiente. Confirmarlo desde el panel descuenta stock una vez, con bloqueo de filas para evitar sobreventas.
- Cancelar un pedido pagado repone existencias una vez. El registro no ejecuta una devolución bancaria.
- Los reportes incluyen pagos confirmados; hay exportación CSV y edición de datos de entrega.
- Las compras a proveedores incrementan stock.
- Garantías, reseñas verificadas y moderadas, y suscripciones se guardan en la base.

Los 8 productos, sus fotografías externas y las 20 unidades iniciales son de demostración. Ajusta el inventario y las imágenes antes de usarlos con compras reales. Yape, Plin y transferencia se coordinan con la tienda. La tarjeta está deshabilitada; no hay cobro automático ni tarifa de envío automática. El transporte se cotiza por separado. Los correos de pedido se envían automáticamente cuando SMTP está configurado.

`WHATSAPP` permite indicar el número de contacto, incluyendo el código de país y solo dígitos.

## Comprobación

La verificación más reciente de reseñas está en `verificacion/RESENAS_Y_FOTOS.md`. Los informes anteriores se conservan en `verificacion/`. Las pruebas se ejecutan en una instancia temporal de PostgreSQL, separada de tus bases. `tests/verificar_api.py` y `tests/verificar_cuentas.py` crean datos de prueba y solo deben ejecutarse sobre una base temporal recién cargada con `CIELO_TEST_ALLOW_MUTATIONS=1`.

## Información de compra y certificado en PDF

La confirmación de compra y «Ver detalle del pedido» abren la misma ventana con fondo desenfocado. Muestran el QR proporcionado para Yape o Plin, o los datos de transferencia según el medio elegido. Mientras el pago esté pendiente, se informa del plazo máximo de cinco días hábiles para su verificación después de realizarlo.

La información del pedido puede descargarse en PDF mientras está pendiente. El certificado, tanto individual como adjunto al PDF de compra, solo se habilita cuando Java confirma el pago. El historial y la ventana consultan el estado actualizado; la ventana pendiente se actualiza cada 15 segundos mientras está visible. El servidor verifica que el pedido pertenezca a la cuenta o a la sesión de compra. Los archivos de plantilla se reservan a administración. Su imagen original se conserva en `static/assets/certificado-material.png` y su PDF en `static/assets/certificado-material.pdf`.

La generación del documento de compra ocurre en el navegador y no envía los datos a servicios externos. `compra-pdf.js` carga la copia local de pdf-lib 1.17.1 únicamente al descargar. Su licencia MIT está en `static/vendor/pdf-lib-LICENSE.md`. Este documento de compra conserva las 12 tablas y no necesita una migración específica.

## Pago y carrito

El QR original está en `static/assets/pago-yape-plin.png`. El lector del sistema detectó contenido legible; esa prueba no comprueba el destinatario, el saldo ni la aceptación por una aplicación de pagos. Verifica el destinatario en tu app antes de confirmar una operación.

La transferencia muestra **MARICIELO GIRAO CHACALIAZA** como titular y el CCI **00238012593976701545**, proporcionado por la propietaria. El botón «Copiar CCI» copia los veinte dígitos sin espacios. `PAYMENT_CCI` permite sustituirlo en otro entorno. Administración conserva el control de la confirmación del pago.

El filtro de precios va de S/ 0 a S/ 250. Sus campos y deslizadores se sincronizan, los valores se conservan en la URL y «Limpiar filtros» restaura los límites. «Añadir al carrito» agrega una unidad con un solo clic, incluso en piezas personalizables; la personalización sigue disponible desde el detalle. El carrito no se abre automáticamente. El aviso visual aparece al iniciar una selección vacía y las unidades posteriores se acumulan, respetando stock y límites.

## Reseñas con fotografías

«Nosotras» ahora abre `resenas.html`. Cada tarjeta muestra la fotografía ampliable, el nombre, la pieza reseñada y su enlace al catálogo. El filtro permite explorar por producto. La dirección anterior continúa funcionando.

La migración 01 consolidada incluye las reseñas y cinco ejemplos ilustrativos. Se mantienen las 12 tablas; no se inventan pedidos ni ventas para crear esos ejemplos. Las reseñas de demostración usan nombres ficticios y fotos del catálogo, con una etiqueta visible y sin sello de compra verificada.

Una cuenta de cliente puede elegir únicamente piezas de sus pedidos con pago confirmado. Cada combinación de pedido y producto admite una reseña. El nombre y la verificación se obtienen de la sesión; no se solicita un número de pedido ni correo para acreditar la compra. La fotografía es obligatoria: JPG o PNG, hasta 5 MB, al menos 100 × 100 píxeles, hasta 6000 por lado y 24 megapíxeles. Java comprueba el contenido real, normaliza a JPEG y guarda una copia de hasta 1600 píxeles sin metadatos. El formulario conserva los cambios mientras permanezcas en la página y permite ver la foto antes de enviar.

La reseña y foto esperan aprobación en **Posventa**. Solo una compra pagada y aprobada recibe «Compra verificada». Una reseña oculta o cuyo pedido fue cancelado deja de aparecer públicamente; su foto tampoco es pública. La propietaria y administración pueden consultar la foto para su revisión. Las cargas se protegen con sesión, CSRF, validación de archivo y comprobación de propiedad en Java.

Las rutas de las fotos se guardan en PostgreSQL y los archivos en `uploads/reviews`, fuera de los recursos públicos. Define `REVIEW_UPLOAD_DIR` si necesitas otra carpeta persistente. Conserva esta carpeta junto con los respaldos de PostgreSQL y no la publiques en un repositorio. La carga de fotografías no requiere una dependencia Java adicional.

Las pruebas `verificar_api.py`, `verificar_cuentas.py`, `verificar_pagos.py`, `verificar_resenas.py` y `verificar_resenas_ui.cjs` se ejecutaron exclusivamente contra PostgreSQL temporal en el puerto 55439. Requieren `CIELO_TEST_ALLOW_MUTATIONS=1` y `API_URL` apuntando al servidor de pruebas. Las pruebas de navegador usan Playwright y Chrome instalados en este equipo.

## Recuérdame y ventana del pedido

Debajo del teléfono aparece «Recuérdame». Está sin marcar por defecto y requiere iniciar sesión. Si lo marcas y el pedido se registra correctamente, la cuenta recordará nombres, apellidos, tipo y número de documento, dirección, referencia, departamento, provincia, distrito, código postal y teléfono. El correo procede de la sesión. La confirmación muestra que los datos se guardaron. No se guarda una nueva preferencia si el pedido falla o si no marcas la casilla.

Al volver a comprar se rellenan los datos recordados de esa cuenta, también después de cerrar sesión y volver a entrar. Marcar de nuevo permite actualizarlos con la compra; dejar la casilla sin marcar conserva los anteriores. «Olvidar datos guardados» retira la preferencia sin borrar pedidos históricos. Los datos se consultan en el servidor según la sesión, nunca en un almacenamiento de navegador compartido entre usuarios. La columna `usuarios.pedido_recordado_id` referencia la compra que se eligió recordar; se mantienen las 12 tablas. El medio de pago y el contenido del carrito no se guardan como datos de entrega.

En escritorio, la confirmación y el detalle del pedido permanecen fijos sin scroll. Las vistas «Tu selección» y «Entrega y documentos» mantienen toda la información accesible; los pedidos largos permiten recorrer sus piezas con paginación. En móvil y tablet se conserva el desplazamiento y se muestra primero el QR o los datos bancarios, según el medio elegido.

### Migraciones actuales

La carpeta `sql` contiene `migracion_01_incio.sql`, `migracion_02_catalogo_60_productos.sql` y `migracion_03_correo_pedidos.sql`. La 01 reúne el esquema inicial. La 02 añade exclusivamente productos. La 03 incorpora la fecha de aceptación del correo por SMTP, sin crear tablas ni modificar compras existentes. En una instalación anterior ejecuta la 03; no necesitas repetir la 01 para activar el correo.

Resultados y capturas: `verificacion/RECORDAR_Y_PEDIDO_FIJO.md`.

## Estados, edición de reseñas y WhatsApp

En Pedidos, el selector conserva el texto del estado y cambia su color según el estado guardado: pendiente beige, pagado verde y cancelado rojo. Preparando, enviado y entregado conservan verde como estados posteriores al pago. El color se actualiza después de guardar el cambio; si se cancela la confirmación o falla la operación, se conserva el estado anterior.

Posventa incorpora «Editar» en cada reseña. La ventana permite modificar el texto y reemplazar la imagen cargando un archivo JPG/PNG, con vista previa y opción de conservar la foto actual. No se cambia la autora, el producto, el carácter de demostración, la verificación de compra ni la publicación. Una reseña publicada refleja los cambios al guardar. La edición requiere ADMIN y CSRF. Se validan los mismos límites de foto que en las reseñas de clientes. Una imagen local reemplazada se elimina después de confirmar la transacción; los archivos externos ilustrativos no se modifican.

En escritorio, los visitantes ven una tarjeta de WhatsApp fija abajo a la derecha con el número **900 934 669**. Hasta 1000 px se oculta en todas las páginas. `contacto.html` ofrece una tarjeta estática dentro del contenido, accesible sin iniciar sesión y sin depender de la API. El perfil y todas las páginas del panel administrativo quedan excluidos del aviso flotante. Los enlaces solo abren WhatsApp cuando se pulsan; no envían mensajes automáticamente.

El icono vectorial está en `static/assets/whatsapp.svg`, procedente de [Simple Icons](https://raw.githubusercontent.com/simple-icons/simple-icons/develop/icons/whatsapp.svg). El color se aplica por CSS sin cambiar su forma.

Estos ajustes no requieren otra migración. Detén Java, inicia con F5 y recarga con Cmd + Shift + R. Verificación: `verificacion/ESTADOS_RESENAS_WHATSAPP.md`.


La visibilidad pública se comprobó también directamente en la tienda local del puerto 8080: Inicio, Novedades, Catálogo, Personaliza, Reseñas, Contacto, Ofertas, Login y Registro. La actualización incorpora una versión nueva de los recursos para evitar reutilizar el código anterior de sesión. Recarga con Cmd + Shift + R. Evidencias: `verificacion/WHATSAPP_PUBLICO.md`.

## Catálogo ampliado y menú móvil

`sql/migracion_02_catalogo_60_productos.sql` añade 10 anillos, 10 collares, 10 pulseras, 10 pares de aretes, 10 tobilleras y 10 sets. Exactamente 30 productos nuevos permiten personalización. La mayoría de piezas individuales cuestan S/ 40 a S/ 50; los collares S/ 70 a S/ 85 y los sets S/ 110 a S/ 239. Con los ocho iniciales, el catálogo tiene 68 productos.

El script usa una transacción y comprueba el total, las categorías, las 30 personalizaciones y las 60 URLs diferentes antes de insertar. `ON CONFLICT (referencia) DO NOTHING` conserva ediciones y stock al repetirlo. No modifica los ocho productos iniciales. Los precios y existencias son de demostración. Las fotografías externas proceden de fichas de Caitlyn Minimalist y son ilustrativas; no certifican acero 316L ni existencias reales. Las fuentes individuales se conservan en los comentarios del SQL y en `verificacion/catalogo_02_fuentes.csv`.

El menú móvil combina enlaces de 56 px de alto con una imagen de colección en el espacio lateral. Las secciones tienen mayor separación y los filtros dejan los campos de precio y el botón en filas amplias. El contenido del panel se desplaza para mantener accesibles todos los controles en pantallas bajas. Inicio incluye acceso a Tobilleras.


## Controles de Cielo y confirmación del pedido

Los selectores de catálogo, administración y ubicaciones del carrito usan desplegables propios, con búsqueda, teclado, opciones deshabilitadas y diseño adaptado al espacio disponible. `controles.js` y `controles.css` también presentan el calendario, los campos de archivo, las confirmaciones y los errores de formulario con los colores de Cielo. El explorador que permite escoger un archivo del equipo sigue siendo una ventana del sistema operativo.

El carrito marca los campos inválidos y explica cómo corregirlos. El medio de pago elegido se resalta y se repite en el resumen. Durante el envío aparece «Procesando…» y se bloquean el formulario y su cierre. Los reintentos conservan la misma clave de petición para evitar pedidos duplicados. La pantalla final muestra el número del pedido y distingue el registro del pedido de la confirmación posterior del pago.

### Aviso por correo

Al registrar un pedido, Java envía al correo de la cuenta una confirmación HTML con la marca **Cielo Joyas**, saludo, número de pedido, piezas, cantidades, personalización, importes, descuento, total, dirección y medio de pago. Incluye una versión de texto para lectores que no muestran HTML. Los importes y nombres proceden del pedido guardado, aunque después cambie el catálogo. Para una compra con sesión, Java utiliza el correo del usuario registrado y descarta cualquier dirección alternativa enviada por el navegador. Los visitantes reciben el aviso en el correo del checkout.

En este equipo se ha preparado `application-local.properties` con la configuración SMTP existente en la referencia facilitada. Spring carga ese archivo privado tanto con F5 como al ejecutar Maven desde la raíz del proyecto. El nombre visible del remitente es Cielo Joyas. El archivo tiene permisos de lectura y escritura solo para su propietario y está excluido en `.gitignore`; no lo publiques ni lo incluyas en entregas públicas. No se envían copias a destinatarios de la referencia.

En otro equipo, configura las variables de `.env.example`: `MAIL_ENABLED=true`, `MAIL_FROM`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`, `SMTP_AUTH`, `SMTP_STARTTLS`, `SMTP_STARTTLS_REQUIRED` y `SMTP_SECURE`. F5 carga `.env`; en una terminal exporta las variables antes de iniciar Maven. También puedes crear el archivo privado `application-local.properties` con las propiedades equivalentes de Spring. Gmail por el puerto 587 utiliza STARTTLS y una contraseña de aplicación válida; puerto 465 utiliza `SMTP_SECURE=true`, `SMTP_STARTTLS=false` y `SMTP_STARTTLS_REQUIRED=false`. Se validan los certificados TLS. Reinicia Java después de cambiar la configuración.

El aviso distingue un pedido registrado de un pago confirmado. La tienda verifica el abono; el envío se cotiza por separado cuando todavía no tiene importe. SMTP debe aceptar el mensaje antes de marcar `pedidos.correo_enviado_en`. Repetir la misma solicitud no genera otro aviso si ya fue aceptado. Si SMTP falla, la compra permanece guardada y el checkout informa del fallo; no hay un envío automático en segundo plano. La aceptación de SMTP no garantiza la llegada a la bandeja de entrada: revisa también spam.

La migración `sql/migracion_03_correo_pedidos.sql` se puede repetir y mantiene las 12 tablas. Solo añade la columna nullable `correo_enviado_en` si falta. Para una instalación nueva, la 01 ya incluye esa columna. No vuelvas a ejecutar la 01 sobre tu instalación únicamente para habilitar los correos.

Verificación actual y vista previa del mensaje: `verificacion/CORREO_PEDIDOS.md`. Las pruebas anteriores de controles permanecen en `verificacion/CONTROLES_CIELO.md`.
