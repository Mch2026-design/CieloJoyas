package ica.joyas.maricielojoyas.validacion;

import ica.joyas.maricielojoyas.modelo.*;
import ica.joyas.maricielojoyas.repositorio.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PedidoTallaGrabadoTests {

    private PedidoService servicio(Producto producto) {
        ProductoRepository productos = mock(ProductoRepository.class);
        PedidoRepository pedidos = mock(PedidoRepository.class);
        DetallePedidoRepository detalles = mock(DetallePedidoRepository.class);
        PagoRepository pagos = mock(PagoRepository.class);
        ClienteRepository clientes = mock(ClienteRepository.class);
        CuponRepository cupones = mock(CuponRepository.class);
        CompraRepository compras = mock(CompraRepository.class);
        ProveedorRepository proveedores = mock(ProveedorRepository.class);
        Ubigeos ubigeos = mock(Ubigeos.class);

        when(pedidos.findByClaveSolicitud(any())).thenReturn(Optional.empty());
        when(productos.bloquearPorId(1L)).thenReturn(Optional.of(producto));
        when(ubigeos.buscar("150101")).thenReturn(new String[]{"150101", "Lima", "Lima", "Lima"});
        return new PedidoService(productos, pedidos, detalles, pagos, clientes, cupones, compras, proveedores, ubigeos);
    }

    private Producto anillo(boolean admiteGrabado) {
        Categoria cat = new Categoria();
        cat.setNombre("Anillos");
        Producto p = new Producto();
        p.setId(1L);
        p.setNombre("Anillo Prueba");
        p.setPrecio(new BigDecimal("49.00"));
        p.setStock(10);
        p.setActivo(true);
        p.setCategoria(cat);
        p.setRequiereTalla(true);
        p.setAdmiteGrabado(admiteGrabado);
        p.setPersonalizable(true);
        return p;
    }

    private Map<String, Object> pedidoBase(String size, String engraving) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("email", "ana@cielo.pe");
        datos.put("requestId", UUID.randomUUID().toString());
        datos.put("firstName", "Ana");
        datos.put("lastName", "Lopez");
        datos.put("phone", "987654321");
        datos.put("documentType", "dni");
        datos.put("documentId", "12345678");
        datos.put("addressLine", "Av. Larco 123");
        datos.put("ubigeo", "150101");
        datos.put("paymentMethod", "yape");
        datos.put("items", List.of(Map.of(
                "productId", 1,
                "quantity", 1,
                "size", size,
                "engraving", engraving
        )));
        return datos;
    }

    @Test
    void anilloExigeTallaSoloNumerosYRechazaGrabadoNoPermitido() {
        PedidoService s = servicio(anillo(false));

        assertThrows(ResponseStatusException.class, () -> s.crear(pedidoBase("", ""), null));
        assertThrows(ResponseStatusException.class, () -> s.crear(pedidoBase("M7", ""), null));
        assertThrows(ResponseStatusException.class, () -> s.crear(pedidoBase("7", "CIELO"), null));
    }

    @Test
    void grabadoSoloAceptaMayusculasYMaximo16Caracteres() {
        PedidoService s = servicio(anillo(true));

        assertThrows(ResponseStatusException.class, () -> s.crear(pedidoBase("7", "cielo"), null));
        assertThrows(ResponseStatusException.class, () -> s.crear(pedidoBase("7", "CIELO1"), null));
        assertThrows(ResponseStatusException.class, () -> s.crear(pedidoBase("7", "ABCDEFGHIJKLMNOPQ"), null));
    }

    @Test
    void confirmarYCancelarPedidoDescuentaYReponeStockDeLaTalla() {
        ProductoRepository productos = mock(ProductoRepository.class);
        PedidoRepository pedidos = mock(PedidoRepository.class);
        DetallePedidoRepository detalles = mock(DetallePedidoRepository.class);
        PagoRepository pagos = mock(PagoRepository.class);
        ClienteRepository clientes = mock(ClienteRepository.class);
        CuponRepository cupones = mock(CuponRepository.class);
        CompraRepository compras = mock(CompraRepository.class);
        ProveedorRepository proveedores = mock(ProveedorRepository.class);
        Ubigeos ubigeos = mock(Ubigeos.class);
        ProductoTallaRepository productoTallas = mock(ProductoTallaRepository.class);
        EntityManager em = mock(EntityManager.class);

        PedidoService service = new PedidoService(
                productos, pedidos, detalles, pagos, clientes, cupones, compras, proveedores, ubigeos);
        ReflectionTestUtils.setField(service, "productoTallas", productoTallas);
        ReflectionTestUtils.setField(service, "entityManager", em);

        Producto ring = anillo(false);
        ring.setStock(10);
        ProductoTalla pt6 = new ProductoTalla(ring, "6", 4);

        Pedido pedido = new Pedido();
        pedido.setId(99L);
        pedido.setEstado("pendiente");

        Pago pago = new Pago();
        pago.setPedido(pedido);
        pago.setMetodo("tarjeta");
        pago.setEstado("pendiente");

        DetallePedido det = new DetallePedido();
        det.setPedido(pedido);
        det.setProducto(ring);
        det.setCantidad(2);
        det.setTalla("6");

        when(pedidos.bloquearPorId(99L)).thenReturn(Optional.of(pedido));
        when(pagos.findByPedidoId(99L)).thenReturn(Optional.of(pago));
        when(detalles.findByPedidoIdOrderById(99L)).thenReturn(List.of(det));
        when(productos.bloquearPorId(1L)).thenReturn(Optional.of(ring));
        when(productoTallas.bloquearPorProductoYTalla(1L, "6")).thenReturn(Optional.of(pt6));
        when(pedidos.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        service.cambiarEstado(99L, "pagado");
        assertEquals(2, pt6.getStock());
        assertEquals(8, ring.getStock());
        assertEquals("confirmado", pago.getEstado());

        service.cambiarEstado(99L, "cancelado");
        assertEquals(4, pt6.getStock());
        assertEquals(10, ring.getStock());
        assertEquals("reembolsado", pago.getEstado());
    }
}
