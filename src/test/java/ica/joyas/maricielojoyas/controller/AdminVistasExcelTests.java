package ica.joyas.maricielojoyas.controller;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/** Verifica la integración de los botones de exportación Excel en las vistas y scripts administrativos. */
class AdminVistasExcelTests {

    @Test
    void adminPedidosConservaCsvYAgregaBotonExportarExcel() throws IOException {
        String html = leerRecurso("static/admin-pedidos.html");
        assertTrue(html.contains("id=\"export-csv\""));
        assertTrue(html.contains("id=\"export-excel-pedidos\""));
        assertTrue(html.contains("Exportar Excel (.xlsx)"));
        assertTrue(html.contains("id=\"period\""));
    }

    @Test
    void adminReportesAgregaBotonExportarExcelYConservaPeriodo() throws IOException {
        String html = leerRecurso("static/admin-reportes.html");
        assertTrue(html.contains("id=\"export-excel-resumen\""));
        assertTrue(html.contains("Exportar Excel (.xlsx)"));
        assertTrue(html.contains("id=\"period\""));
    }

    @Test
    void adminJsConectaBotonesExcelConEndpointsYFiltroDePeriodo() throws IOException {
        String js = leerRecurso("static/admin.js");
        assertTrue(js.contains("#export-excel-pedidos"));
        assertTrue(js.contains("#export-excel-resumen"));
        assertTrue(js.contains("/admin/reportes/excel/pedidos"));
        assertTrue(js.contains("/admin/reportes/excel/resumen"));
        assertTrue(js.contains("$('#period')"));
        assertTrue(js.contains("handleError("));
    }

    private static String leerRecurso(String ruta) throws IOException {
        return new String(new ClassPathResource(ruta).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
