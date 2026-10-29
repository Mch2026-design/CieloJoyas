# Ajustes de acceso, cuenta y administración

29 de octubre de 2026. Proyecto activo: `/Users/flavio/Documents/VScode/maricielojoyas`.

## Cambios

- Login y registro conservan la fotografía y el estilo de Cielo, con espacios ajustados a la altura disponible. La imagen acompaña el formulario y mantiene su proporción mediante recorte.
- El registro pide nombres, apellidos, correo y contraseña. Java admite el registro sin celular ni confirmación de contraseña, y conserva el mínimo de 8 caracteres. La configuración inicial del administrador conserva su confirmación.
- Clientes autenticados y recién registrados llegan a `index.html`. El nombre de la cabecera abre su historial. Para administradores, abre el resumen administrativo.
- La cabecera muestra solo el primer nombre. El cierre de sesión es un icono junto al carrito en las ocho páginas de tienda y en todos los apartados administrativos.
- Cuenta y administración tienen contenidos centrados, márgenes coherentes y controles adaptados a móvil.
- `admin.html` contiene el resumen. Pedidos, productos, reportes, compras, posventa y promociones tienen HTML independientes. Cada documento contiene sus propios controles y carga los datos correspondientes.
- La navegación administrativa permanece arriba. En móvil se desliza horizontalmente y muestra el apartado actual. Cambiar de apartado abre otra página desde el inicio.
- Todos los HTML administrativos exigen el rol `ADMIN` desde Java. Visitantes van al login y clientes al inicio.
- Se corrigió el título de Contacto a 320 px y se limitó el selector de piezas al ancho disponible.

## Verificación

Las operaciones de prueba usaron exclusivamente `cielo_navegacion_pruebas`, en PostgreSQL temporal del puerto 55439. No se crearon cuentas ni compras ficticias en la base local de la tienda.

- API: 32 comprobaciones correctas (`api-navegacion.txt`).
- Cuentas y roles: 54 comprobaciones correctas, incluidas las siete páginas administrativas para visitantes, clientes y administradores (`cuentas-navegacion.txt`).
- Maven: prueba de contexto Spring Boot sin fallos (`maven-navegacion.txt`).
- JavaScript: sintaxis correcta. Los 18 HTML tienen identificadores únicos y enlaces locales válidos.
- Chrome: login y registro revisados a 1440 × 900, 1366 × 768, 1024 × 768, 768 × 1024, 390 × 844, 375 × 667 y 320 × 568. En esos tamaños no hay desplazamiento vertical ni horizontal. Evidencia: `acceso-dimensiones.json` y capturas `ajuste-login-*`, `ajuste-registro-*`.
- En horizontal a 812 × 375, o cuando errores y zoom requieren más espacio, el contenido conserva desplazamiento vertical para mantener accesibles todos los campos. No se ocultan campos ni se deshabilita el scroll para disimular un desbordamiento.
- Chrome: 125 comprobaciones correctas de navegación, formularios y sesiones, sin errores JavaScript. Se revisaron los siete HTML administrativos a 1440, 1024, 390 y 320 px, y las ocho cabeceras de tienda a 320 px. Resultados en `flujos-navegacion.json`. La revisión usa los archivos estáticos actuales sobre la API real de pruebas.

## Uso

Reinicia Spring Boot desde VS Code para cargar los cambios de Java y recarga con Cmd + Shift + R. No hay migraciones nuevas ni cambios en las 12 tablas.
