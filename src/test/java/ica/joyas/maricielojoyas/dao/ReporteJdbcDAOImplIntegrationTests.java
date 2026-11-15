package ica.joyas.maricielojoyas.dao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de integración de solo lectura para verificar compatibilidad SQL del DAO con PostgreSQL. */
@SpringBootTest
class ReporteJdbcDAOImplIntegrationTests {

    @Autowired
    private IReporteDAO reporteDAO;

    @Test
    @SuppressWarnings("unchecked")
    void consultasDaoSeEjecutanContraBaseDatosRealSinErrores() {
        String db = reporteDAO.obtenerBaseDatosActual();
        assertNotNull(db);
        assertFalse(db.isBlank());

        Map<String, Object> reporte = reporteDAO.obtenerReporteVentas(30);
        assertEquals(30, reporte.get("days"));
        assertNotNull(reporte.get("byStatus"));
        assertNotNull(reporte.get("byPayment"));
        assertNotNull(reporte.get("daily"));
        assertNotNull(reporte.get("popular"));
        assertNotNull(reporte.get("recent"));

        Map<String, Object> summary = (Map<String, Object>) reporte.get("summary");
        assertNotNull(summary);
        assertInstanceOf(BigDecimal.class, summary.get("revenue"));
        assertInstanceOf(BigDecimal.class, summary.get("average"));

        List<Map<String, Object>> segmentos = reporteDAO.obtenerSegmentosClientes();
        assertNotNull(segmentos);
    }
}
