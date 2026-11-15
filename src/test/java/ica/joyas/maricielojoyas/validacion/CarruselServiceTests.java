package ica.joyas.maricielojoyas.validacion;

import ica.joyas.maricielojoyas.modelo.Producto;
import ica.joyas.maricielojoyas.repositorio.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CarruselServiceTests {

    private List<Producto> productosActivos(int cantidad) {
        List<Producto> lista = new ArrayList<>();
        for (long i = 1; i <= cantidad; i++) {
            Producto p = new Producto();
            p.setId(i);
            p.setActivo(true);
            lista.add(p);
        }
        return lista;
    }

    @Test
    void guardarRechazaMenosDelMinimoDeProductos() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ProductoRepository repo = mock(ProductoRepository.class);
        when(repo.findAll()).thenReturn(productosActivos(12));
        CarruselService service = new CarruselService(jdbc, repo);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.guardar(Map.of("productIds", List.of(1, 2, 3), "speedSeconds", 40, "direction", "left")));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void guardarRechazaVelocidadODireccionInvalida() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ProductoRepository repo = mock(ProductoRepository.class);
        when(repo.findAll()).thenReturn(productosActivos(12));
        CarruselService service = new CarruselService(jdbc, repo);

        List<Integer> ocho = List.of(1, 2, 3, 4, 5, 6, 7, 8);
        assertThrows(ResponseStatusException.class,
                () -> service.guardar(Map.of("productIds", ocho, "speedSeconds", 5, "direction", "left")));
        assertThrows(ResponseStatusException.class,
                () -> service.guardar(Map.of("productIds", ocho, "speedSeconds", 45, "direction", "up")));
    }

    @Test
    void guardarValidoPersisteEnBaseDeDatos() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ProductoRepository repo = mock(ProductoRepository.class);
        when(repo.findAll()).thenReturn(productosActivos(12));
        when(jdbc.queryForList(anyString())).thenReturn(List.of(Map.of(
                "id", 1L,
                "productos_ids", "1,2,3,4,5,6,7,8",
                "velocidad_segundos", 30,
                "direccion", "right")));

        CarruselService service = new CarruselService(jdbc, repo);
        Map<String, Object> res = service.guardar(Map.of(
                "carousels", List.of(Map.of(
                        "productIds", List.of(1, 2, 3, 4, 5, 6, 7, 8),
                        "speedSeconds", 30,
                        "direction", "right"))));

        assertEquals(8, ((List<?>) res.get("productIds")).size());
        assertEquals(30, res.get("speedSeconds"));
        assertEquals("right", res.get("direction"));
        verify(jdbc).update(anyString(), eq(1L), eq("1,2,3,4,5,6,7,8"), eq(30), eq("right"));
    }

    @Test
    void guardarRechazaMasDeCincoCarruseles() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ProductoRepository repo = mock(ProductoRepository.class);
        when(repo.findAll()).thenReturn(productosActivos(12));
        CarruselService service = new CarruselService(jdbc, repo);

        Map<String, Object> uno = Map.of("productIds", List.of(1, 2, 3, 4, 5, 6, 7, 8), "speedSeconds", 55, "direction", "left");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.guardar(Map.of("carousels", List.of(uno, uno, uno, uno, uno, uno))));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}
