package ica.joyas.maricielojoyas.controller;

import ica.joyas.maricielojoyas.dao.IReporteDAO;
import ica.joyas.maricielojoyas.repositorio.*;
import ica.joyas.maricielojoyas.validacion.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppControllerReporteTests {

    private IReporteDAO reporteDAO;
    private AppController controller;

    @BeforeEach
    void setUp() {
        reporteDAO = mock(IReporteDAO.class);
        controller = new AppController(
                mock(UsuarioRepository.class), mock(ClienteRepository.class), mock(CategoriaRepository.class),
                mock(ProductoRepository.class), mock(CuponRepository.class), mock(PedidoRepository.class),
                mock(DetallePedidoRepository.class), mock(PagoRepository.class), mock(ProveedorRepository.class),
                mock(CompraRepository.class), mock(GarantiaRepository.class), mock(ResenaRepository.class),
                mock(PedidoService.class), reporteDAO, mock(AccesoService.class),
                mock(Ubigeos.class), mock(AvisosPedido.class), mock(GoogleAuthService.class));
    }

    @Test
    void saludDelegaConsultaDeBaseDeDatosAlDao() {
        when(reporteDAO.obtenerBaseDatosActual()).thenReturn("postgres");

        Map<String, Object> respuesta = controller.salud();

        assertEquals(Map.of("ok", true, "database", "postgres"), respuesta);
        verify(reporteDAO).obtenerBaseDatosActual();
    }

    @Test
    void reporteDelegaAlDaoYConservaEstructura() {
        Map<String, Object> esperado = Map.of(
                "days", 30,
                "summary", Map.of("revenue", new BigDecimal("120.00"), "orders", 2));
        when(reporteDAO.obtenerReporteVentas(30)).thenReturn(esperado);

        Map<String, Object> respuesta = controller.reporte(30);

        assertSame(esperado, respuesta);
        verify(reporteDAO).obtenerReporteVentas(30);
    }

    @Test
    void reporteMantieneValidacionHttp400ParaPeriodosInvalidos() {
        ResponseStatusException menor = assertThrows(ResponseStatusException.class, () -> controller.reporte(0));
        ResponseStatusException mayor = assertThrows(ResponseStatusException.class, () -> controller.reporte(366));

        assertEquals(HttpStatus.BAD_REQUEST, menor.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, mayor.getStatusCode());
        verifyNoInteractions(reporteDAO);
    }

    @Test
    void segmentosDelegaAlDao() {
        List<Map<String, Object>> esperado = List.of(
                Map.of("email", "clienta@example.test", "orders", 1, "spent", new BigDecimal("50.00"), "segment", "Nueva compradora"));
        when(reporteDAO.obtenerSegmentosClientes()).thenReturn(esperado);

        List<Map<String, Object>> respuesta = controller.segmentos();

        assertSame(esperado, respuesta);
        verify(reporteDAO).obtenerSegmentosClientes();
    }
}
