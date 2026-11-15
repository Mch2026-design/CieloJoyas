package ica.joyas.maricielojoyas.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Implementación JDBC del DAO de reportes sobre PostgreSQL manteniendo compatibilidad con AppController. */
@Repository
public class ReporteJdbcDAOImpl implements IReporteDAO {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final List<String> ESTADOS_ORDENADOS =
            List.of("pendiente", "pagado", "preparando", "enviado", "entregado", "cancelado");
    private static final Set<String> ESTADOS_CONFIRMADOS =
            Set.of("pagado", "preparando", "enviado", "entregado");
    private static final String FILTRO_ESTADOS_CONFIRMADOS =
            "('pagado', 'preparando', 'enviado', 'entregado')";

    private final JdbcTemplate jdbc;

    public ReporteJdbcDAOImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public String obtenerBaseDatosActual() {
        return jdbc.queryForObject("SELECT current_database()", String.class);
    }

    @Override
    public Map<String, Object> obtenerReporteVentas(int days) {
        if (days < 1 || days > 365) {
            throw new IllegalArgumentException("El periodo debe estar entre 1 y 365 días.");
        }
        OffsetDateTime desde = LocalDate.now(LIMA).minusDays(days - 1L).atStartOfDay(LIMA).toOffsetDateTime();
        SqlParameterValue parametroDesde = parametroFecha(desde);

        List<Map<String, Object>> recientes = consultarPedidosRecientes(parametroDesde);
        List<Map<String, Object>> resumenPagos = consultarResumenPagos(parametroDesde);
        List<Map<String, Object>> diario = consultarVentasDiarias(parametroDesde);
        List<Map<String, Object>> popular = consultarProductosPopulares(parametroDesde);

        BigDecimal ingreso = BigDecimal.ZERO;
        long totalVentasConfirmadas = 0L;
        List<Map<String, Object>> porMedioPago = new ArrayList<>();
        for (Map<String, Object> fila : resumenPagos) {
            BigDecimal monto = (BigDecimal) fila.get("revenue");
            long cantidad = ((Number) fila.get("ordersCount")).longValue();
            ingreso = ingreso.add(monto);
            totalVentasConfirmadas += cantidad;
            porMedioPago.add(vista("method", fila.get("method"), "revenue", monto));
        }

        BigDecimal promedio = totalVentasConfirmadas == 0
                ? BigDecimal.ZERO
                : ingreso.divide(BigDecimal.valueOf(totalVentasConfirmadas), 2, RoundingMode.HALF_UP);

        long clientesUnicos = recientes.stream()
                .map(p -> (String) p.get("email"))
                .distinct()
                .count();

        return vista(
                "days", days,
                "byStatus", construirResumenEstados(recientes),
                "byPayment", porMedioPago,
                "summary", vista(
                        "revenue", ingreso,
                        "orders", recientes.size(),
                        "customers", clientesUnicos,
                        "average", promedio),
                "daily", diario,
                "popular", popular,
                "recent", recientes);
    }

    @Override
    public List<Map<String, Object>> obtenerSegmentosClientes() {
        String sql = """
                SELECT c.email,
                       COUNT(p.id) AS orders_count,
                       COALESCE(SUM(p.total), 0) AS spent
                FROM clientes c
                LEFT JOIN pedidos p
                       ON p.cliente_id = c.id
                      AND p.estado IN ('pagado', 'preparando', 'enviado', 'entregado')
                WHERE c.suscrito = TRUE
                GROUP BY c.id, c.email
                ORDER BY c.id ASC
                """;
        return jdbc.query(sql, (rs, rowNum) -> {
            int ordenes = rs.getInt("orders_count");
            BigDecimal gastado = ordenes == 0 ? BigDecimal.ZERO : valorMonetario(rs.getBigDecimal("spent"));
            String segmento = ordenes == 0 ? "Suscrita" : ordenes >= 3 ? "Recurrente" : "Nueva compradora";
            return vista("email", rs.getString("email"), "orders", ordenes, "spent", gastado, "segment", segmento);
        });
    }

    private List<Map<String, Object>> consultarPedidosRecientes(SqlParameterValue parametroDesde) {
        String sql = """
                SELECT p.id, p.nombres, p.apellidos, p.email, p.telefono,
                       p.direccion, p.distrito, p.provincia, p.departamento,
                       p.estado, p.creado_en, p.pagado_en,
                       p.subtotal, p.descuento, p.costo_envio, p.total,
                       p.mensaje_regalo, p.correo_enviado_en,
                       pg.metodo AS metodo_pago, pg.estado AS estado_pago
                FROM pedidos p
                JOIN pagos pg ON pg.pedido_id = p.id
                WHERE p.creado_en > ?
                ORDER BY p.id DESC
                """;
        List<Map<String, Object>> recientes = jdbc.query(sql, (rs, rowNum) -> mapearPedido(rs), parametroDesde);
        if (recientes.isEmpty()) {
            return recientes;
        }
        List<Long> pedidoIds = recientes.stream().map(p -> (Long) p.get("id")).toList();
        Map<Long, List<Map<String, Object>>> itemsPorPedido = cargarDetallesPorPedido(pedidoIds);
        for (Map<String, Object> pedido : recientes) {
            Long id = (Long) pedido.get("id");
            pedido.put("items", itemsPorPedido.getOrDefault(id, List.of()));
        }
        return recientes;
    }

    private Map<Long, List<Map<String, Object>>> cargarDetallesPorPedido(List<Long> pedidoIds) {
        String placeholders = String.join(", ", Collections.nCopies(pedidoIds.size(), "?"));
        String sql = """
                SELECT d.pedido_id, d.nombre_producto, d.cantidad, d.precio_unitario, d.grabado, d.talla
                FROM detalle_pedidos d
                WHERE d.pedido_id IN (%s)
                ORDER BY d.pedido_id DESC, d.id ASC
                """.formatted(placeholders);
        Map<Long, List<Map<String, Object>>> agrupado = new LinkedHashMap<>();
        jdbc.query(sql, rs -> {
            Long pedidoId = rs.getLong("pedido_id");
            agrupado.computeIfAbsent(pedidoId, k -> new ArrayList<>()).add(vista(
                    "name", rs.getString("nombre_producto"),
                    "quantity", rs.getInt("cantidad"),
                    "price", rs.getBigDecimal("precio_unitario"),
                    "engraving", rs.getString("grabado"),
                    "size", rs.getString("talla")));
        }, pedidoIds.toArray());
        return agrupado;
    }

    private List<Map<String, Object>> consultarResumenPagos(SqlParameterValue parametroDesde) {
        String sql = """
                SELECT pg.metodo AS method,
                       COUNT(*) AS orders_count,
                       COALESCE(SUM(p.total), 0) AS revenue
                FROM pedidos p
                JOIN pagos pg ON pg.pedido_id = p.id
                WHERE p.estado IN %s
                  AND p.pagado_en IS NOT NULL
                  AND p.pagado_en >= ?
                GROUP BY pg.metodo
                ORDER BY pg.metodo ASC
                """.formatted(FILTRO_ESTADOS_CONFIRMADOS);
        return jdbc.query(sql, (rs, rowNum) -> vista(
                "method", rs.getString("method"),
                "ordersCount", rs.getLong("orders_count"),
                "revenue", valorMonetario(rs.getBigDecimal("revenue"))), parametroDesde);
    }

    private List<Map<String, Object>> consultarVentasDiarias(SqlParameterValue parametroDesde) {
        String sql = """
                SELECT TO_CHAR(p.pagado_en AT TIME ZONE 'America/Lima', 'YYYY-MM-DD') AS day,
                       COALESCE(SUM(p.total), 0) AS revenue
                FROM pedidos p
                WHERE p.estado IN %s
                  AND p.pagado_en IS NOT NULL
                  AND p.pagado_en >= ?
                GROUP BY TO_CHAR(p.pagado_en AT TIME ZONE 'America/Lima', 'YYYY-MM-DD')
                ORDER BY day ASC
                """.formatted(FILTRO_ESTADOS_CONFIRMADOS);
        return jdbc.query(sql, (rs, rowNum) -> vista(
                "day", rs.getString("day"),
                "revenue", valorMonetario(rs.getBigDecimal("revenue"))), parametroDesde);
    }

    private List<Map<String, Object>> consultarProductosPopulares(SqlParameterValue parametroDesde) {
        String sql = """
                SELECT MIN(d.nombre_producto) AS name,
                       COALESCE(SUM(d.cantidad), 0) AS quantity,
                       COALESCE(SUM(d.importe), 0) AS revenue
                FROM detalle_pedidos d
                JOIN pedidos p ON p.id = d.pedido_id
                WHERE p.estado IN %s
                  AND p.pagado_en IS NOT NULL
                  AND p.pagado_en >= ?
                GROUP BY d.producto_id
                ORDER BY quantity DESC, d.producto_id ASC
                LIMIT 8
                """.formatted(FILTRO_ESTADOS_CONFIRMADOS);
        return jdbc.query(sql, (rs, rowNum) -> vista(
                "name", rs.getString("name"),
                "quantity", rs.getInt("quantity"),
                "revenue", valorMonetario(rs.getBigDecimal("revenue"))), parametroDesde);
    }

    private List<Map<String, Object>> construirResumenEstados(List<Map<String, Object>> recientes) {
        List<Map<String, Object>> estados = new ArrayList<>();
        for (String estado : ESTADOS_ORDENADOS) {
            long conteo = recientes.stream().filter(p -> estado.equals(p.get("status"))).count();
            estados.add(vista("status", estado, "count", conteo));
        }
        return estados;
    }

    private Map<String, Object> mapearPedido(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        String nombres = rs.getString("nombres");
        String apellidos = rs.getString("apellidos");
        String estado = rs.getString("estado");
        String estadoPago = rs.getString("estado_pago");
        boolean certificadoDisponible = ESTADOS_CONFIRMADOS.contains(estado) && "confirmado".equals(estadoPago);
        return vista(
                "id", id,
                "customer", nombres + " " + apellidos,
                "firstName", nombres,
                "lastName", apellidos,
                "email", rs.getString("email"),
                "phone", rs.getString("telefono"),
                "address", rs.getString("direccion"),
                "district", rs.getString("distrito"),
                "province", rs.getString("provincia"),
                "department", rs.getString("departamento"),
                "status", estado,
                "createdAt", leerOffsetDateTime(rs, "creado_en"),
                "paidAt", leerOffsetDateTime(rs, "pagado_en"),
                "subtotal", rs.getBigDecimal("subtotal"),
                "discount", rs.getBigDecimal("descuento"),
                "shipping", rs.getBigDecimal("costo_envio"),
                "total", rs.getBigDecimal("total"),
                "giftMessage", rs.getString("mensaje_regalo"),
                "paymentMethod", rs.getString("metodo_pago"),
                "paymentStatus", estadoPago,
                "emailSent", leerOffsetDateTime(rs, "correo_enviado_en") != null,
                "certificateAvailable", certificadoDisponible,
                "certificateUrl", certificadoDisponible ? "/api/orders/" + id + "/certificate" : null);
    }

    private static SqlParameterValue parametroFecha(OffsetDateTime fecha) {
        return new SqlParameterValue(Types.TIMESTAMP_WITH_TIMEZONE, fecha);
    }

    private static OffsetDateTime leerOffsetDateTime(ResultSet rs, String columna) throws SQLException {
        OffsetDateTime odt = rs.getObject(columna, OffsetDateTime.class);
        if (odt != null) {
            return odt;
        }
        Timestamp ts = rs.getTimestamp(columna);
        return ts == null ? null : ts.toInstant().atOffset(ZoneOffset.UTC);
    }

    private static BigDecimal valorMonetario(BigDecimal monto) {
        return monto == null ? BigDecimal.ZERO : monto;
    }

    private static Map<String, Object> vista(Object... pares) {
        Map<String, Object> resultado = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) {
            resultado.put((String) pares[i], pares[i + 1]);
        }
        return resultado;
    }
}
