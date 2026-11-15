package ica.joyas.maricielojoyas.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameterValue;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReporteJdbcDAOImplTests {

    private JdbcTemplate jdbc;
    private ReporteJdbcDAOImpl dao;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        dao = new ReporteJdbcDAOImpl(jdbc);
    }

    @Test
    void obtenerBaseDatosActualConsultaMotorSql() {
        when(jdbc.queryForObject("SELECT current_database()", String.class)).thenReturn("postgres");

        assertEquals("postgres", dao.obtenerBaseDatosActual());
        verify(jdbc).queryForObject("SELECT current_database()", String.class);
    }

    @Test
    void obtenerReporteVentasRechazaPeriodosFueraDeRango() {
        assertThrows(IllegalArgumentException.class, () -> dao.obtenerReporteVentas(0));
        assertThrows(IllegalArgumentException.class, () -> dao.obtenerReporteVentas(366));
        verifyNoInteractions(jdbc);
    }

    @Test
    @SuppressWarnings("unchecked")
    void obtenerReporteVentasSinDatosRetornaTotalesEnCeroYSeisEstados() {
        when(jdbc.query(anyString(), any(RowMapper.class), any(SqlParameterValue.class)))
                .thenReturn(List.of());

        Map<String, Object> reporte = dao.obtenerReporteVentas(30);

        assertEquals(30, reporte.get("days"));
        Map<String, Object> summary = (Map<String, Object>) reporte.get("summary");
        assertEquals(BigDecimal.ZERO, summary.get("revenue"));
        assertEquals(0, summary.get("orders"));
        assertEquals(0L, summary.get("customers"));
        assertEquals(BigDecimal.ZERO, summary.get("average"));

        List<Map<String, Object>> byStatus = (List<Map<String, Object>>) reporte.get("byStatus");
        assertEquals(6, byStatus.size());
        assertTrue(byStatus.stream().allMatch(s -> Long.valueOf(0L).equals(s.get("count"))));
        assertTrue(((List<?>) reporte.get("byPayment")).isEmpty());
        assertTrue(((List<?>) reporte.get("daily")).isEmpty());
        assertTrue(((List<?>) reporte.get("popular")).isEmpty());
        assertTrue(((List<?>) reporte.get("recent")).isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void obtenerReporteVentasMapeaPedidosDetallesPagosYMetricas() throws SQLException {
        OffsetDateTime ahora = OffsetDateTime.of(2026, 11, 14, 15, 30, 0, 0, ZoneOffset.UTC);
        ResultSet rsPedido = mock(ResultSet.class);
        when(rsPedido.getLong("id")).thenReturn(15L);
        when(rsPedido.getString("nombres")).thenReturn("Lucía");
        when(rsPedido.getString("apellidos")).thenReturn("Paredes");
        when(rsPedido.getString("email")).thenReturn("lucia@example.test");
        when(rsPedido.getString("telefono")).thenReturn("987654321");
        when(rsPedido.getString("direccion")).thenReturn("Av. San Martín 120");
        when(rsPedido.getString("distrito")).thenReturn("Ica");
        when(rsPedido.getString("provincia")).thenReturn("Ica");
        when(rsPedido.getString("departamento")).thenReturn("Ica");
        when(rsPedido.getString("estado")).thenReturn("pagado");
        when(rsPedido.getObject("creado_en", OffsetDateTime.class)).thenReturn(ahora);
        when(rsPedido.getObject("pagado_en", OffsetDateTime.class)).thenReturn(null);
        when(rsPedido.getTimestamp("pagado_en")).thenReturn(Timestamp.from(Instant.parse("2026-11-14T16:00:00Z")));
        when(rsPedido.getObject("correo_enviado_en", OffsetDateTime.class)).thenReturn(ahora);
        when(rsPedido.getBigDecimal("subtotal")).thenReturn(new BigDecimal("100.00"));
        when(rsPedido.getBigDecimal("descuento")).thenReturn(new BigDecimal("10.00"));
        when(rsPedido.getBigDecimal("costo_envio")).thenReturn(BigDecimal.ZERO);
        when(rsPedido.getBigDecimal("total")).thenReturn(new BigDecimal("90.00"));
        when(rsPedido.getString("mensaje_regalo")).thenReturn("Feliz cumpleaños");
        when(rsPedido.getString("metodo_pago")).thenReturn("yape");
        when(rsPedido.getString("estado_pago")).thenReturn("confirmado");

        ResultSet rsDetalle = mock(ResultSet.class);
        when(rsDetalle.getLong("pedido_id")).thenReturn(15L);
        when(rsDetalle.getString("nombre_producto")).thenReturn("Pulsera Estrellas");
        when(rsDetalle.getInt("cantidad")).thenReturn(2);
        when(rsDetalle.getBigDecimal("precio_unitario")).thenReturn(new BigDecimal("50.00"));
        when(rsDetalle.getString("grabado")).thenReturn("L");
        when(rsDetalle.getString("talla")).thenReturn("18 cm");

        ResultSet rsPago = mock(ResultSet.class);
        when(rsPago.getString("method")).thenReturn("yape");
        when(rsPago.getLong("orders_count")).thenReturn(2L);
        when(rsPago.getBigDecimal("revenue")).thenReturn(new BigDecimal("150.00"));

        ResultSet rsDiario = mock(ResultSet.class);
        when(rsDiario.getString("day")).thenReturn("2026-11-14");
        when(rsDiario.getBigDecimal("revenue")).thenReturn(new BigDecimal("150.00"));

        ResultSet rsPopular = mock(ResultSet.class);
        when(rsPopular.getString("name")).thenReturn("Pulsera Estrellas");
        when(rsPopular.getInt("quantity")).thenReturn(2);
        when(rsPopular.getBigDecimal("revenue")).thenReturn(new BigDecimal("100.00"));

        when(jdbc.query(anyString(), any(RowMapper.class), any(SqlParameterValue.class)))
                .thenAnswer(inv -> {
                    String sql = inv.getArgument(0);
                    RowMapper<Map<String, Object>> mapper = inv.getArgument(1);
                    if (sql.contains("FROM pedidos p") && sql.contains("p.creado_en > ?")) {
                        return List.of(mapper.mapRow(rsPedido, 0));
                    }
                    if (sql.contains("GROUP BY pg.metodo")) {
                        return List.of(mapper.mapRow(rsPago, 0));
                    }
                    if (sql.contains("TO_CHAR(p.pagado_en")) {
                        return List.of(mapper.mapRow(rsDiario, 0));
                    }
                    if (sql.contains("FROM detalle_pedidos d")) {
                        return List.of(mapper.mapRow(rsPopular, 0));
                    }
                    return List.of();
                });

        doAnswer(inv -> {
            RowCallbackHandler handler = inv.getArgument(1);
            handler.processRow(rsDetalle);
            return null;
        }).when(jdbc).query(contains("FROM detalle_pedidos d"), any(RowCallbackHandler.class), any(Object[].class));

        Map<String, Object> reporte = dao.obtenerReporteVentas(7);

        assertEquals(7, reporte.get("days"));
        Map<String, Object> summary = (Map<String, Object>) reporte.get("summary");
        assertEquals(new BigDecimal("150.00"), summary.get("revenue"));
        assertEquals(1, summary.get("orders"));
        assertEquals(1L, summary.get("customers"));
        assertEquals(new BigDecimal("75.00"), summary.get("average"));

        List<Map<String, Object>> recent = (List<Map<String, Object>>) reporte.get("recent");
        assertEquals(1, recent.size());
        Map<String, Object> orden = recent.get(0);
        assertEquals("Lucía Paredes", orden.get("customer"));
        assertEquals(true, orden.get("emailSent"));
        assertEquals(true, orden.get("certificateAvailable"));
        assertEquals("/api/orders/15/certificate", orden.get("certificateUrl"));
        List<Map<String, Object>> items = (List<Map<String, Object>>) orden.get("items");
        assertEquals(1, items.size());
        assertEquals("Pulsera Estrellas", items.get(0).get("name"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void obtenerSegmentosClientesClasificaSuscritasYNuevasYRecurrentes() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("email")).thenReturn("suscrita@example.test", "nueva@example.test", "fiel@example.test");
        when(rs.getInt("orders_count")).thenReturn(0, 1, 3);
        when(rs.getBigDecimal("spent")).thenReturn(new BigDecimal("65.00"), new BigDecimal("240.00"));

        when(jdbc.query(contains("FROM clientes c"), any(RowMapper.class))).thenAnswer(inv -> {
            RowMapper<Map<String, Object>> mapper = inv.getArgument(1);
            return List.of(mapper.mapRow(rs, 0), mapper.mapRow(rs, 1), mapper.mapRow(rs, 2));
        });

        List<Map<String, Object>> segmentos = dao.obtenerSegmentosClientes();

        assertEquals(3, segmentos.size());
        assertEquals("Suscrita", segmentos.get(0).get("segment"));
        assertEquals(BigDecimal.ZERO, segmentos.get(0).get("spent"));
        assertEquals("Nueva compradora", segmentos.get(1).get("segment"));
        assertEquals("Recurrente", segmentos.get(2).get("segment"));
        assertEquals(new BigDecimal("240.00"), segmentos.get(2).get("spent"));
    }
}
