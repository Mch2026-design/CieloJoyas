# Verificación de la entrega

28 de octubre de 2026, hora de Lima.

## Java y PostgreSQL

- Maven Wrapper: compilación, prueba de arranque y generación del JAR completadas.
- Spring Boot detectó los 12 repositorios y validó los 12 modelos contra el SQL, usando `ddl-auto=validate` durante la prueba. La configuración entregada conserva `update`.
- Migración ejecutada en PostgreSQL 18.4 temporal: exactamente 12 tablas, 6 categorías, 8 productos y el cupón inicial.
- La migración se ejecutó dos veces sin duplicar sus datos iniciales.
- **32 comprobaciones de operaciones aprobadas**, documentadas en `api.txt`.
- Incluyen cálculo del total en Java, cupón, reintento sin duplicados, dos grabados diferentes, existencias, pago pendiente/confirmado, compra a proveedor, garantía, reseña y moderación.
- Dos confirmaciones simultáneas sobre la última unidad dieron 200 y 409: solo una se confirmó. También se comprobó la reposición al cancelar y el rollback completo si falla una línea del pedido.

Las pruebas usaron una instancia creada para este trabajo en el puerto 55439. No se ejecutó la migración ni se modificaron registros en el servidor personal del usuario.

## Frontend

- Las ocho páginas cargaron: inicio, catálogo, novedades, personalización, nosotras, contacto, ofertas y administración.
- Navegación real desde el menú a `catalogo.html` y `contacto.html`.
- Sin errores de JavaScript observados en esas páginas.
- Búsqueda «Collar»: dos resultados. Orden ascendente: S/ 78.00 y S/ 95.00.
- Categoría Collares con precios de 80 a 100: un resultado. El filtro se conservó al recargar mediante la URL.
- El carrito conservó una pieza al navegar desde catálogo a contacto.
- Se completó un pedido ficticio desde el checkout móvil y se verificó su confirmación y aparición en administración.
- Sin desbordamiento horizontal observado en las vistas comprobadas: 375 px, 500 px, 812 px y escritorio de 1440 px. Se revisó también la preferencia de movimiento reducido.
- Los escaneos automáticos de catálogo y administración con etiquetas WCAG 2 A/AA terminaron sin infracciones detectadas. Sus comprobaciones incompletas requieren revisión manual; esto no equivale a una certificación completa.

Los datos, ventas y correos de las capturas son ficticios y pertenecen solo a la base temporal.

## Ubicaciones

Se corrigieron caracteres dañados en el catálogo de ubicaciones de la referencia. El nombre Vizcatán del Ene se contrastó con la [información oficial de su municipalidad](https://www.gob.pe/institucion/munivizcatandelene/informes-publicaciones). El JSON del navegador y el archivo utilizado por Java conservan los mismos códigos de ubigeo.

## Alcance final solicitado

La capa de seguridad añadida se retiró. Se conservan modelos, repositorios, AppController y operaciones del backend. El panel local de clase tiene acceso directo. Solo se usan las tres dependencias de producción del proyecto original.

Para ejecutar con la base personal hay que crear el esquema desde el SQL y configurar su contraseña. Las pruebas temporales no sustituyen esa configuración.
