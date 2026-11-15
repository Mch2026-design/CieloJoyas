package ica.joyas.maricielojoyas.validacion;

import ica.joyas.maricielojoyas.modelo.Producto;
import ica.joyas.maricielojoyas.repositorio.ProductoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

/** Configuración persistente del carrusel continuo de la portada. */
@Service
public class CarruselService {

    public static final int MIN_PRODUCTOS = 8;
    public static final int DEFAULT_VELOCIDAD = 55;
    public static final String DEFAULT_DIRECCION = "left";

    private final JdbcTemplate jdbc;
    private final ProductoRepository productos;

    public CarruselService(JdbcTemplate jdbc, ProductoRepository productos) {
        this.jdbc = jdbc;
        this.productos = productos;
    }

    private void asegurarTabla() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS configuracion_carrusel (
                id BIGINT PRIMARY KEY,
                productos_ids TEXT NOT NULL,
                velocidad_segundos INTEGER NOT NULL DEFAULT 55,
                direccion VARCHAR(16) NOT NULL DEFAULT 'left',
                actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
            )
            """);
    }

    public Map<String, Object> obtener() {
        asegurarTabla();
        Set<Long> activos = productos.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .map(Producto::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT productos_ids, velocidad_segundos, direccion FROM configuracion_carrusel WHERE id = 1");

        List<Long> ids = new ArrayList<>();
        int velocidad = DEFAULT_VELOCIDAD;
        String direccion = DEFAULT_DIRECCION;

        if (!rows.isEmpty()) {
            Map<String, Object> row = rows.get(0);
            String rawIds = row.get("productos_ids") instanceof String s ? s : "";
            for (String parte : rawIds.split(",")) {
                String limpia = parte.trim();
                if (limpia.isEmpty()) continue;
                try {
                    long id = Long.parseLong(limpia);
                    if (activos.contains(id) && !ids.contains(id)) ids.add(id);
                } catch (NumberFormatException ignored) {}
            }
            if (row.get("velocidad_segundos") instanceof Number n) {
                velocidad = Math.max(10, Math.min(180, n.intValue()));
            }
            if (row.get("direccion") instanceof String d && ("left".equals(d) || "right".equals(d))) {
                direccion = d;
            }
        }

        if (ids.size() < MIN_PRODUCTOS) {
            for (Long id : activos) {
                if (!ids.contains(id)) ids.add(id);
                if (ids.size() >= 20) break;
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("productIds", ids);
        out.put("speedSeconds", velocidad);
        out.put("direction", direccion);
        out.put("minProducts", MIN_PRODUCTOS);
        return out;
    }

    @Transactional
    public Map<String, Object> guardar(Map<String, Object> datos) {
        asegurarTabla();
        if (!(datos.get("productIds") instanceof Collection<?> rawList)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Envía la lista de productos del carrusel.");
        }
        Set<Long> activos = productos.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .map(Producto::getId)
                .collect(Collectors.toSet());

        List<Long> ids = new ArrayList<>();
        for (Object item : rawList) {
            if (item == null) continue;
            try {
                long id = item instanceof Number n ? n.longValue() : Long.parseLong(item.toString().trim());
                if (activos.contains(id) && !ids.contains(id)) ids.add(id);
            } catch (NumberFormatException ignored) {}
        }
        if (ids.size() < MIN_PRODUCTOS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Selecciona al menos " + MIN_PRODUCTOS + " productos activos para el carrusel.");
        }

        int velocidad;
        Object rawVel = datos.get("speedSeconds");
        if (rawVel instanceof Number n) {
            velocidad = n.intValue();
        } else if (rawVel instanceof String s && !s.isBlank()) {
            try { velocidad = Integer.parseInt(s.trim()); }
            catch (NumberFormatException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Velocidad inválida.");
            }
        } else {
            velocidad = DEFAULT_VELOCIDAD;
        }
        if (velocidad < 10 || velocidad > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La velocidad debe estar entre 10 y 180 segundos por vuelta.");
        }

        String direccion = datos.get("direction") instanceof String d ? d.trim().toLowerCase(Locale.ROOT) : DEFAULT_DIRECCION;
        if (!Set.of("left", "right").contains(direccion)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La dirección del carrusel debe ser izquierda (left) o derecha (right).");
        }

        String joined = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        jdbc.update("""
            INSERT INTO configuracion_carrusel (id, productos_ids, velocidad_segundos, direccion, actualizado_en)
            VALUES (1, ?, ?, ?, NOW())
            ON CONFLICT (id) DO UPDATE SET
                productos_ids = EXCLUDED.productos_ids,
                velocidad_segundos = EXCLUDED.velocidad_segundos,
                direccion = EXCLUDED.direccion,
                actualizado_en = NOW()
            """, joined, velocidad, direccion);

        return obtener();
    }
}
