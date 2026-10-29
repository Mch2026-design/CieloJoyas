# Ajustes solicitados: perfil, carrito y reseñas

- Acceso: un único error por campo, respetando el validador específico del formulario.
- Carrito: área de productos ampliada; el bloque inferior ocupa como máximo el 46 % de la pantalla y conserva su desplazamiento.
- Reseñas: reemplazo mediante archivo o URL HTTPS, con vista previa y validación en el servidor.
- Cuenta: Mi información permite guardar y editar nombres, apellidos, documento, dirección, referencia, ubicación, código postal y teléfono. La compra usa los datos guardados. Los pedidos anteriores mantienen sus datos originales.
- Pedidos administrativos: el selector de estado permite elegir opciones y usar el teclado, sin introducir texto.

Validación: cinco pruebas Maven correctas, 21 comprobaciones de tests/verificar_recordar.py y flujo Chrome de tests/verificar_ajustes_perfil.cjs correcto. Carrito comprobado en 1440 × 1000, 390 × 844 y 320 × 700. Sin errores JavaScript.

Las pruebas de flujos usaron PostgreSQL temporal, base cielo_editar_resenas y aplicación en el puerto 18085. El contexto de Maven utilizó la configuración local del proyecto.

La tabla usuario_entrega se crea con el esquema automático configurado en el proyecto. Para instalaciones que ejecuten migraciones manuales se incluye sql/migracion_03_informacion_usuario.sql. Reiniciar la aplicación para cargar el backend actualizado.
