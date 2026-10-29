# Estados, reseñas editables y contacto fijo

Comprobado el 29 de octubre de 2026 en `/Users/flavio/Documents/VScode/maricielojoyas`.

## Cambios

- Selector de estado con texto y color: pendiente beige, pagado verde, cancelado rojo; preparación y envío conservan verde. El color deriva del estado confirmado en el servidor y se conserva al recargar.
- Botón Editar en cada reseña de Posventa. Ventana con texto, foto actual, carga JPG/PNG, vista previa, conservar foto actual, Cancelar y Guardar. Conserva autora, pieza, origen, verificación y publicación.
- PUT multipart `/api/admin/reviews/{id}` reservado a ADMIN, protegido con CSRF. Validación de texto de 10 a 3000 caracteres y límites de fotografía existentes. Rechaza archivos vacíos, falsos o demasiado grandes. El bloqueo de fila serializa edición y moderación. Solo elimina una imagen local anterior después de guardar correctamente; una foto oculta sigue protegida.
- Contacto fijo para clientes con sesión en las páginas de tienda, con SVG de WhatsApp en tonos de Cielo y número 900 934 669. No aparece en cuenta, administración o para visitantes; se oculta durante las ventanas de compra. Enlace directo al número peruano, sin envío automático de mensajes.

## Comprobaciones

Se utilizó exclusivamente PostgreSQL temporal, puerto 55439, base `cielo_editar_resenas`. No se cambiaron pedidos, reseñas ni cuentas en la base local de la tienda.

| Prueba | Resultado |
|---|---:|
| Compra, stock y moderación existentes | 32 correctas |
| Reseñas verificadas, fotos y propiedad | 38 correctas |
| Edición administrativa, permisos y sustitución de imagen | 24 correctas |
| Navegador: colores, edición y contacto fijo | 30 correctas |
| Contexto Java | 1 correcta |

La prueba de navegador confirmó colores diferentes al pasar de pendiente a pagado y cancelado, editor con foto y guardado visible en la tienda, foco dentro del diálogo y ausencia de errores JavaScript. Comprobó el editor a 1440 × 900, 375 × 812 y 812 × 375, y el contacto a 1440, 375 y 320 píxeles. El contacto mantuvo su posición al desplazar y apareció en todas las páginas permitidas, excluyendo perfil, visitantes y administrador.

Se revisaron las capturas `contacto-whatsapp-1440.png`, `editar-resena-375.png`; también se generaron `contacto-whatsapp-375.png`, `editar-resena-1440.png` y `estados-colores.png`.

Resultados: `api-editar-base.txt`, `api-editar-resenas-seguridad.txt`, `api-editar-resenas.txt`, `navegador-estados-edicion-contacto.json`, `java-editar-resenas.txt`. Se compiló el JAR final y se comprobó que contiene los mismos recursos que el proyecto.

El grafo documental respalda reseñas con fotos en RF-03 (Tabla 4, página 14) y WhatsApp para contacto en RF-07 (Tabla 4, página 14). La implementación y sus resultados se comprobaron en el código y pruebas anteriores; no se infirieron del documento.

No hay migración nueva ni dependencias Java nuevas. Reinicia Java con F5 y recarga el navegador con Cmd + Shift + R.
