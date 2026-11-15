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

/** Configuración persistente de hasta 5 carruseles continuos en la portada. */
@Service
public class CarruselService {

    public static final int MIN_PRODUCTOS = 8;
    public static final int MAX_CARRUSELES = 5;
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

    private Set<Long> idsActivos() {
        return productos.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .map(Producto::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<Long> completarMinimo(List<Long> ids, Set<Long> activos) {
        List<Long> res = new ArrayList<>(ids);
        if (res.size() < MIN_PRODUCTOS) {
            for (Long id : activos) {
                if (!res.contains(id)) res.add(id);
                if (res.size() >= 20) break;
            }
        }
        return res;
    }

    public Map<String, Object> obtener() {
        asegurarTabla();
        Set<Long> activos = idsActivos();

        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, productos_ids, velocidad_segundos, direccion FROM configuracion_carrusel ORDER BY id");

        List<Map<String, Object>> carousels = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            long id = row.get("id") instanceof Number n ? n.longValue() : carousels.size() + 1L;
            if (id < 1 || id > MAX_CARRUSELES) continue;
            List<Long> ids = new ArrayList<>();
            String rawIds = row.get("productos_ids") instanceof String s ? s : "";
            for (String parte : rawIds.split(",")) {
                String limpia = parte.trim();
                if (limpia.isEmpty()) continue;
                try {
                    long pid = Long.parseLong(limpia);
                    if (activos.contains(pid) && !ids.contains(pid)) ids.add(pid);
                } catch (NumberFormatException ignored) {}
            }
            ids = completarMinimo(ids, activos);
            int velocidad = row.get("velocidad_segundos") instanceof Number n
                    ? Math.max(10, Math.min(180, n.intValue())) : DEFAULT_VELOCIDAD;
            String direccion = row.get("direccion") instanceof String d && ("left".equals(d) || "right".equals(d))
                    ? d : DEFAULT_DIRECCION;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", id);
            item.put("productIds", ids);
            item.put("speedSeconds", velocidad);
            item.put("direction", direccion);
            carousels.add(item);
        }

        if (carousels.isEmpty()) {
            Map<String, Object> primero = new LinkedHashMap<>();
            primero.put("id", 1L);
            primero.put("productIds", completarMinimo(List.of(), activos));
            primero.put("speedSeconds", DEFAULT_VELOCIDAD);
            primero.put("direction", DEFAULT_DIRECCION);
            carousels.add(primero);
        }

        Map<String, Object> first = carousels.get(0);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("minProducts", MIN_PRODUCTOS);
        out.put("maxCarousels", MAX_CARRUSELES);
        out.put("carousels", carousels);
        out.put("productIds", first.get("productIds"));
        out.put("speedSeconds", first.get("speedSeconds"));
        out.put("direction", first.get("direction"));
        return out;
    }

    @Transactional
    public Map<String, Object> guardar(Map<String, Object> datos) {
        asegurarTabla();
        Set<Long> activos = idsActivos();

        List<Map<String, Object>> entrada = new ArrayList<>();
        if (datos.get("carousels") instanceof Collection<?> lista) {
            for (Object obj : lista) {
                if (obj instanceof Map<?, ?> m) {
                    Map<String, Object> copy = new LinkedHashMap<>();
                    m.forEach((k, v) -> { if (k != null) copy.put(k.toString(), v); });
                    entrada.add(copy);
                }
            }
        } else {
            entrada.add(datos);
        }

        if (entrada.isEmpty() || entrada.size() > MAX_CARRUSELES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Puedes configurar entre 1 y " + MAX_CARRUSELES + " carruseles.");
        }

        List<Object[]> validados = new ArrayList<>();
        for (int i = 0; i < entrada.size(); i++) {
            Map<String, Object> item = entrada.get(i);
            int num = i + 1;
            if (!(item.get("productIds") instanceof Collection<?> rawList)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Envía la lista de productos del Carrusel " + num + ".");
            }
            List<Long> ids = new ArrayList<>();
            for (Object idObj : rawList) {
                if (idObj == null) continue;
                try {
                    long pid = idObj instanceof Number n ? n.longValue() : Long.parseLong(idObj.toString().trim());
                    if (activos.contains(pid) && !ids.contains(pid)) ids.add(pid);
                } catch (NumberFormatException ignored) {}
            }
            if (ids.size() < MIN_PRODUCTOS) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El Carrusel " + num + " necesita al menos " + MIN_PRODUCTOS + " productos activos.");
            }

            int velocidad;
            Object rawVel = item.get("speedSeconds");
            if (rawVel instanceof Number n) {
                velocidad = n.intValue();
            } else if (rawVel instanceof String s && !s.isBlank()) {
                try { velocidad = Integer.parseInt(s.trim()); }
                catch (NumberFormatException ex) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Velocidad inválida en Carrusel " + num + ".");
                }
            } else {
                velocidad = DEFAULT_VELOCIDAD;
            }
            if (velocidad < 10 || velocidad > 180) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La velocidad del Carrusel " + num + " debe estar entre 10 y 180 segundos.");
            }

            String direccion = item.get("direction") instanceof String d
                    ? d.trim().toLowerCase(Locale.ROOT) : DEFAULT_DIRECCION;
            if (!Set.of("left", "right").contains(direccion)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La dirección del Carrusel " + num + " debe ser izquierda (left) o derecha (right).");
            }

            String joined = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
            validados.add(new Object[] { (long) num, joined, velocidad, direccion });
        }

        jdbc.update("DELETE FROM configuracion_carrusel WHERE id > ?", validados.size());
        for (Object[] row : validados) {
            jdbc.update("""
                INSERT INTO configuracion_carrusel (id, productos_ids, velocidad_segundos, direccion, actualizado_en)
                VALUES (?, ?, ?, ?, NOW())
                ON CONFLICT (id) DO UPDATE SET
                    productos_ids = EXCLUDED.productos_ids,
                    velocidad_segundos = EXCLUDED.velocidad_segundos,
                    direccion = EXCLUDED.direccion,
                    actualizado_en = NOW()
                """, row[0], row[1], row[2], row[3]);
        }

        return obtener();
    }
}
