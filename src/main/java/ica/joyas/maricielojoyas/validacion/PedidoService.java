package ica.joyas.maricielojoyas.validacion;

import ica.joyas.maricielojoyas.modelo.*;
import ica.joyas.maricielojoyas.repositorio.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.LockModeType;
import java.math.*;
import java.time.OffsetDateTime;
import java.util.*;
import static ica.joyas.maricielojoyas.validacion.Reglas.*;

/** Compra y cambios de stock en transacciones, con bloqueo de las filas afectadas. */
@Service
public class PedidoService {
    @Autowired private ProductoRepository productos;
    @Autowired private PedidoRepository pedidos;
    @Autowired private DetallePedidoRepository detalles;
    @Autowired private PagoRepository pagos;
    @Autowired private ClienteRepository clientes;
    @Autowired private CuponRepository cupones;
    @Autowired private CompraRepository compras;
    @Autowired private ProveedorRepository proveedores;
    @Autowired private Ubigeos ubigeos;
    @PersistenceContext private EntityManager entityManager;

    public Cupon validarCupon(String codigo, BigDecimal subtotal) {
        Cupon cupon = cupones.findByCodigo(codigo.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> error("No encontramos ese cupón."));
        if (!cupon.getActivo() || (cupon.getVenceEn() != null && cupon.getVenceEn().isBefore(OffsetDateTime.now())))
            throw error("El cupón ya no está disponible.");
        if (subtotal.compareTo(cupon.getMinimo()) < 0)
            throw error("Este cupón requiere una compra mínima de S/ " + cupon.getMinimo() + ".");
        return cupon;
    }

    @Transactional
    public Map<String, Object> crear(Map<String, Object> datos, Usuario usuario) {
        boolean recordar = bandera(datos, "rememberDelivery");
        if (recordar && usuario == null) throw error("Inicia sesión para recordar tus datos de entrega.");
        String correo = email(datos);
        String clave = texto(datos, "requestId", 36, 36);
        try { UUID.fromString(clave); } catch (IllegalArgumentException e) { throw error("Identificador de solicitud inválido. Actualiza la página."); }
        Optional<Pedido> anterior = pedidos.findByClaveSolicitud(clave);
        if (anterior.isPresent()) {
            if (!anterior.get().getEmail().equals(correo) || (anterior.get().getUsuario() != null && (usuario == null || !anterior.get().getUsuario().getId().equals(usuario.getId())))) throw error("La solicitud ya fue utilizada.");
            return confirmacion(anterior.get());
        }
        if (!(datos.get("items") instanceof List<?> filas) || filas.isEmpty() || filas.size() > 50)
            throw error("Agrega entre 1 y 50 líneas al carrito.");
        List<Map<String, Object>> lineas = new ArrayList<>();
        Map<Long, Integer> cantidades = new TreeMap<>();
        for (Object fila : filas) {
            if (!(fila instanceof Map<?, ?> mapa)) throw error("Revisa los productos del carrito.");
            Map<String, Object> linea = new HashMap<>();
            mapa.forEach((k, v) -> linea.put(String.valueOf(k), v));
            long id = id(linea.get("productId"));
            int cantidad = entero(linea, "quantity", 1, 100);
            cantidades.merge(id, cantidad, Integer::sum); lineas.add(linea);
        }
        Map<Long, Producto> catalogo = new HashMap<>();
        // Orden estable de bloqueo para evitar interbloqueos entre pedidos.
        for (var entrada : cantidades.entrySet()) {
            Producto p = productos.bloquearPorId(entrada.getKey()).orElseThrow(() -> error("Una pieza ya no existe."));
            if (!p.getActivo() || entrada.getValue() > p.getStock())
                throw error("No hay suficientes unidades de " + p.getNombre() + ". Actualiza tu carrito.");
            catalogo.put(p.getId(), p);
        }
        Pedido pedido = new Pedido();
        pedido.setUsuario(usuario);
        pedido.setClaveSolicitud(clave); pedido.setEmail(correo);
        pedido.setNombres(texto(datos, "firstName", 2, 100));
        pedido.setApellidos(texto(datos, "lastName", 2, 100));
        String telefono = texto(datos, "phone", 9, 12).replace(" ", "").replaceFirst("^\\+51", "");
        if (!telefono.matches("9[0-9]{8}")) throw error("Ingresa un celular peruano de 9 dígitos que empiece por 9.");
        pedido.setTelefono(telefono);
        String tipo = opcion(datos, "documentType", Set.of("dni", "ce", "ruc"));
        String documento = texto(datos, "documentId", 8, 12);
        String patron = switch (tipo) { case "dni" -> "[0-9]{8}"; case "ruc" -> "(?:10|15|17|20)[0-9]{9}"; default -> "[A-Za-z0-9]{9,12}"; };
        if (!documento.matches(patron)) throw error("Revisa el número de documento.");
        pedido.setTipoDocumento(tipo); pedido.setNumeroDocumento(documento);
        pedido.setDireccion(texto(datos, "addressLine", 5, 254));
        pedido.setReferencia(nulo(texto(datos, "reference", 0, 254)));
        String[] localidad = ubigeos.buscar(texto(datos, "ubigeo", 6, 6));
        pedido.setUbigeoCodigo(localidad[0]); pedido.setDepartamento(localidad[1]);
        pedido.setProvincia(localidad[2]); pedido.setDistrito(localidad[3]);
        String postal = texto(datos, "postal", 0, 5);
        if (!postal.isBlank() && !postal.matches("[0-9]{5}")) throw error("El código postal debe tener 5 dígitos.");
        pedido.setCodigoPostal(nulo(postal));
        pedido.setMensajeRegalo(nulo(texto(datos, "giftMessage", 0, 180)));
        String metodo = opcion(datos, "paymentMethod", Set.of("yape", "plin", "transferencia", "tarjeta"));
        if (metodo.equals("tarjeta")) throw error("El pago con tarjeta aún no está habilitado. Elige Yape, Plin o transferencia.");
        BigDecimal subtotal = BigDecimal.ZERO;
        List<DetallePedido> items = new ArrayList<>();
        for (var linea : lineas) {
            Producto producto = catalogo.get(id(linea.get("productId")));
            DetallePedido detalle = new DetallePedido();
            detalle.setPedido(pedido); detalle.setProducto(producto); detalle.setNombreProducto(producto.getNombre());
            detalle.setImagenUrl(producto.getImagenUrl()); detalle.setCantidad(entero(linea, "quantity", 1, 100));
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setImporte(producto.getPrecio().multiply(BigDecimal.valueOf(detalle.getCantidad())));
            String grabado = texto(linea, "engraving", 0, 20), talla = texto(linea, "size", 0, 30);
            if (!producto.getPersonalizable() && (!grabado.isBlank() || !talla.isBlank()))
                throw error(producto.getNombre() + " no admite personalización.");
            detalle.setGrabado(nulo(grabado)); detalle.setTalla(nulo(talla));
            detalle.setColor(nulo(texto(linea, "color", 0, 50)));
            items.add(detalle); subtotal = subtotal.add(detalle.getImporte());
        }
        pedido.setSubtotal(dinero(subtotal));
        String codigo = texto(datos, "coupon", 0, 20);
        if (!codigo.isBlank()) {
            Cupon cupon = validarCupon(codigo, subtotal); pedido.setCupon(cupon); pedido.setCodigoCupon(cupon.getCodigo());
            pedido.setDescuento(subtotal.multiply(BigDecimal.valueOf(cupon.getDescuento())).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
        }
        pedido.setTotal(dinero(subtotal.subtract(pedido.getDescuento())));
        clientes.registrarSiFalta(pedido.getNombres(), pedido.getApellidos(), correo, telefono, false);
        pedido.setCliente(clientes.findByEmailIgnoreCase(correo).orElseThrow());
        pedidos.saveAndFlush(pedido); detalles.saveAll(items);
        Pago pago = new Pago(); pago.setPedido(pedido); pago.setMetodo(metodo); pago.setMonto(pedido.getTotal()); pagos.save(pago);
        if (recordar) { Usuario cuenta=entityManager.find(Usuario.class, usuario.getId()); cuenta.getEntregaGuardada().clear(); cuenta.setPedidoRecordadoId(pedido.getId()); }
        // El stock se descuenta al confirmar el pago. El pedido pendiente no es una venta cobrada.
        return confirmacion(pedido);
    }
    public Map<String, Object> confirmacion(Pedido p) {
        return Map.of("id", p.getId(), "total", p.getTotal(), "status", p.getEstado(), "paymentStatus", pagos.findByPedidoId(p.getId()).map(Pago::getEstado).orElse("pendiente"));
    }

    @Transactional
    public Pedido cambiarEstado(Long id, String nuevo) {
        Pedido pedido = pedidos.bloquearPorId(id).orElseThrow(() -> error("Pedido no encontrado."));
        String previo = pedido.getEstado();
        if (previo.equals(nuevo)) return pedido;
        Map<String, Set<String>> permitidos = Map.of(
            "pendiente", Set.of("pagado", "cancelado"), "pagado", Set.of("preparando", "cancelado"),
            "preparando", Set.of("enviado", "cancelado"), "enviado", Set.of("entregado"),
            "entregado", Set.of(), "cancelado", Set.of());
        if (!permitidos.getOrDefault(previo, Set.of()).contains(nuevo))
            throw error("No puedes pasar de " + previo + " a " + nuevo + ".");
        Pago pago = pagos.findByPedidoId(id).orElseThrow();
        if (nuevo.equals("pagado") || (nuevo.equals("cancelado") && pago.getEstado().equals("confirmado"))) {
            Map<Long, Integer> cantidades = new TreeMap<>();
            for (DetallePedido detalle : detalles.findByPedidoIdOrderById(id))
                cantidades.merge(detalle.getProducto().getId(), detalle.getCantidad(), Integer::sum);
            for (var linea : cantidades.entrySet()) {
                Producto producto = productos.bloquearPorId(linea.getKey()).orElseThrow();
                // El detalle pudo cargar el producto antes de esperar el bloqueo.
                // Refrescar evita usar ese stock anterior después de obtener la fila.
                entityManager.refresh(producto, LockModeType.PESSIMISTIC_WRITE);
                if (nuevo.equals("pagado")) {
                    if (producto.getStock() < linea.getValue())
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Stock insuficiente de " + producto.getNombre() + ". No se confirmó el pago.");
                    producto.setStock(producto.getStock() - linea.getValue());
                } else producto.setStock(Math.addExact(producto.getStock(), linea.getValue()));
                productos.save(producto);
            }
            if (nuevo.equals("pagado")) {
                pago.setEstado("confirmado"); pago.setConfirmadoEn(OffsetDateTime.now()); pedido.setPagadoEn(pago.getConfirmadoEn());
            } else pago.setEstado("reembolsado");
            pagos.save(pago);
        } else if (nuevo.equals("cancelado")) { pago.setEstado("rechazado"); pagos.save(pago); }
        pedido.setEstado(nuevo); return pedidos.save(pedido);
    }

    @Transactional
    public Pedido editar(Long id, Map<String, Object> datos) {
        Pedido pedido = pedidos.bloquearPorId(id).orElseThrow(() -> error("Pedido no encontrado."));
        if (Set.of("enviado", "entregado", "cancelado").contains(pedido.getEstado()))
            throw error("Solo puedes editar los datos de entrega antes del envío.");
        pedido.setNombres(texto(datos, "firstName", 2, 100)); pedido.setApellidos(texto(datos, "lastName", 2, 100));
        pedido.setDireccion(texto(datos, "address", 5, 254));
        String telefono = texto(datos, "phone", 9, 9);
        if (!telefono.matches("9[0-9]{8}")) throw error("Celular inválido.");
        pedido.setTelefono(telefono);
        // El correo y documento originales identifican la compra y no se sustituyen desde este formulario.
        return pedidos.save(pedido);
    }

    @Transactional
    public Compra registrarCompra(Map<String, Object> datos) {
        Proveedor proveedor = proveedores.findById(id(datos.get("vendorId"))).orElseThrow(() -> error("Proveedor no encontrado."));
        if (!proveedor.getActivo()) throw error("El proveedor está inactivo.");
        Producto producto = productos.bloquearPorId(id(datos.get("productId"))).orElseThrow(() -> error("Producto no encontrado."));
        Compra compra = new Compra(); compra.setProveedor(proveedor); compra.setProducto(producto);
        compra.setCantidad(entero(datos, "quantity", 1, 1000000)); compra.setCostoUnitario(dinero(datos.get("unitCost")));
        compra.setTotal(dinero(compra.getCostoUnitario().multiply(BigDecimal.valueOf(compra.getCantidad()))));
        compra.setComprobante(nulo(texto(datos, "reference", 0, 100)));
        producto.setStock(Math.addExact(producto.getStock(), compra.getCantidad())); productos.save(producto);
        return compras.save(compra);
    }
}
