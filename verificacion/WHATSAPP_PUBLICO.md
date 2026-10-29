# WhatsApp público en todas las secciones

Corregido el 29 de octubre de 2026.

El contacto estaba condicionado al rol CLIENTE y a una sesión válida. Ahora un script independiente, `whatsapp.js`, crea el mismo contacto sin consultar la sesión. Todos los HTML con contenido cargan este script; los documentos antiguos que redirigen llevan a una página que lo incluye. Solo se excluye `cuenta.html`, tanto por identificador de página como por ruta. Se retiró la condición de rol y la regla CSS que lo ocultaba según ventanas abiertas. Se conserva el diseño existente y el enlace al número 900 934 669.

Se añaden versiones a los recursos en el HTML para evitar reutilizar los anteriores del navegador. No hay cambios Java ni migraciones nuevas.

## Verificación

28 comprobaciones de navegador correctas en servidor temporal: diez entradas públicas, enlace y pestaña nueva, posición fija y encuadre a 1440/375/320 píxeles, perfil excluido para cliente, vuelta a tienda, seis secciones administrativas y contacto visible con la API bloqueada. Sin errores JavaScript. Resultados: `whatsapp-publico.json`, `whatsapp-publico.txt`; capturas `whatsapp-publico-1440.png` y `whatsapp-publico-375.png`.

Además, nueve comprobaciones directamente sobre la tienda local de puerto 8080 confirmaron el contacto visible sin iniciar sesión en Inicio, Novedades, Catálogo, Personaliza, Reseñas, Contacto, Ofertas, Login y Registro. Solo se hicieron lecturas en esa tienda; no se registraron cuentas ni pedidos. Resultado: `whatsapp-tienda-local.txt`.

Se comprobó que el JAR final incluye el script independiente, los HTML actualizados y el CSS correspondiente. Para cargarlo en una pestaña que ya estaba abierta, recargar con Cmd + Shift + R.
