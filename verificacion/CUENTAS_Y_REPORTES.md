# Cielo Joyas: cuentas, administración y ajustes visuales

Comprobación realizada el 28 de octubre de 2026 sobre PostgreSQL temporal. Las cuentas y compras ficticias de las pruebas se crearon exclusivamente en las bases `cielo_cuentas_pruebas` y `cielo_inicio_pruebas`, en el puerto 55439.

## Resultado

| Comprobación | Resultado |
|---|---|
| Compilación Maven y prueba de contexto Spring Boot con validación del esquema | Correcto, 1 prueba sin fallos |
| API de tienda, pedidos, inventario, pagos y concurrencia | 32 comprobaciones correctas |
| Registro, sesiones, roles, historial privado, CRUD y reportes | 32 comprobaciones correctas |
| SQL inicial ejecutado en una base nueva | Correcto, exactamente 12 tablas |
| Actualización de cuentas sobre el esquema existente | Correcta, sin tablas adicionales |
| Accesibilidad automática WCAG 2 A/AA en login, registro y panel | Sin infracciones detectadas |
| Sintaxis de los 6 archivos JavaScript | Correcta |

Las auditorías automáticas no sustituyen la revisión visual. El contraste sobre fotografías con degradado figura como comprobación manual pendiente en axe; se revisó visualmente el texto blanco sobre el sombreado de la fotografía.

## Flujos revisados en Chrome

- Botón de acceso junto al carrito; icono con nombre accesible en móvil.
- Login y registro con las mismas fuentes, paleta y fotografía de joyería del catálogo.
- Resumen de errores enfocable, contraseñas que no coinciden, mostrar y ocultar contraseña y normalización de un celular pegado con `+51`.
- Registro de cliente que termina en «Mi cuenta», cierre de sesión y entrada administrativa al panel.
- Primera configuración desde el enlace del login, sobre una base sin cuentas. La cuenta creada abre el panel y sus 8 productos iniciales.
- Conservación del carrito entre páginas y después del registro. Autocompletado de nombres, correo y celular en checkout.
- Cupón CIELO10 calculado por Java y conservado al cambiar de página; se vuelve a validar con la API.
- Carrito vacío centrado, filas del catálogo separadas y descuento adaptable a 320 y 375 píxeles.
- Login comprobado también a 768, 1024 y 1440 píxeles de ancho, y en horizontal a 812 × 375. Panel comprobado a 320, 375 y 1440 píxeles. Sin desbordamiento horizontal de la página; la tabla de pedidos tiene su propio desplazamiento.
- Sin errores de JavaScript en los flujos revisados.

## Evidencias

- `api-cielo.txt`: resultados de tienda e inventario.
- `cuentas-cielo.txt`: resultados de sesiones, roles, productos y reportes.
- `maven-cielo-cuentas.txt`: compilación y prueba con PostgreSQL.
- `sql-cielo-nueva-instalacion.txt`: ejecución del SQL inicial.
- `a11y-login-cielo.json`, `a11y-registro-cielo.json`, `a11y-admin-cielo.json`: auditorías automáticas.
- `cielo-login-escritorio.png`, `cielo-registro-escritorio.png`, `cielo-login-movil.png`, `cielo-registro-movil.png`: acceso y registro.
- `cielo-carrito-vacio.png`, `cielo-carrito-movil.png`, `cielo-carrito-320.png`, `cielo-checkout-320.png`: carrito y compra.
- `cielo-admin-escritorio.png`, `cielo-admin-movil.png`, `cielo-admin-productos.png`, `cielo-primer-admin.png`: administración.

## Para trabajar en la base local

La base local `maricielojoyas` recibió la actualización de roles, la relación de pedidos con su cuenta y el cambio del cupón y las etiquetas a Cielo. No se crearon cuentas ni compras de prueba allí. Reinicia la aplicación de VS Code con F5 y recarga el navegador con Cmd + Shift + R. Abre `/login.html` y elige «Configurar administrador» para definir tu propia cuenta inicial.

La carpeta y el paquete Java conservan su nombre técnico `maricielojoyas`; la marca de las páginas, correos visibles, mensajes, etiquetas y cupón es Cielo Joyas. Los documentos anteriores del curso permanecen identificados como históricos.
