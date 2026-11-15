package ica.joyas.maricielojoyas.service;

import ica.joyas.maricielojoyas.dao.IReporteDAO;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReporteExcelPoiServiceImplTests {

    private IReporteDAO reporteDAO;
    private ReporteExcelPoiServiceImpl service;

    @BeforeEach
    void setUp() {
        reporteDAO = mock(IReporteDAO.class);
        service = new ReporteExcelPoiServiceImpl(reporteDAO);
    }

    @Test
    void rechazaPeriodosFueraDeRangoSinConsultarDao() {
        assertThrows(IllegalArgumentException.class, () -> service.exportarPedidosExcel(0));
        assertThrows(IllegalArgumentException.class, () -> service.exportarResumenVentasExcel(366));
        verifyNoInteractions(reporteDAO);
    }

    @Test
    void exportarPedidosExcelGeneraLibroValidoConTiposYEncabezados() throws IOException {
        OffsetDateTime fecha = OffsetDateTime.of(2026, 11, 14, 15, 0, 0, 0, ZoneOffset.UTC);
        Map<String, Object> pedido = Map.ofEntries(
                Map.entry("id", 42L),
                Map.entry("customer", "Lucía Paredes"),
                Map.entry("email", "lucia@example.test"),
                Map.entry("phone", "987654321"),
                Map.entry("district", "Ica"),
                Map.entry("province", "Ica"),
                Map.entry("department", "Ica"),
                Map.entry("createdAt", fecha),
                Map.entry("status", "pagado"),
                Map.entry("paymentMethod", "yape"),
                Map.entry("paymentStatus", "confirmado"),
                Map.entry("subtotal", new BigDecimal("110.00")),
                Map.entry("discount", new BigDecimal("10.00")),
                Map.entry("total", new BigDecimal("100.00")),
                Map.entry("items", List.of(
                        Map.of("name", "Pulsera Plata", "quantity", 2))));

        when(reporteDAO.obtenerReporteVentas(30)).thenReturn(Map.of(
                "days", 30,
                "recent", List.of(pedido)));

        byte[] xlsx = service.exportarPedidosExcel(30);
        assertTrue(xlsx.length > 0);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            assertEquals(1, wb.getNumberOfSheets());
            Sheet hoja = wb.getSheet("Pedidos");
            assertNotNull(hoja);

            Row encabezados = hoja.getRow(3);
            assertEquals("ID", encabezados.getCell(0).getStringCellValue());
            assertEquals("Cliente", encabezados.getCell(1).getStringCellValue());
            assertEquals("Total", encabezados.getCell(11).getStringCellValue());
            assertEquals("Productos", encabezados.getCell(12).getStringCellValue());

            Row filaPedido = hoja.getRow(4);
            assertEquals(CellType.NUMERIC, filaPedido.getCell(0).getCellType());
            assertEquals(42d, filaPedido.getCell(0).getNumericCellValue());
            assertEquals("Lucía Paredes", filaPedido.getCell(1).getStringCellValue());
            assertTrue(DateUtil.isCellDateFormatted(filaPedido.getCell(5)));
            assertEquals(CellType.NUMERIC, filaPedido.getCell(11).getCellType());
            assertEquals(100.00d, filaPedido.getCell(11).getNumericCellValue(), 0.001);
            assertEquals("2x Pulsera Plata", filaPedido.getCell(12).getStringCellValue());

            Row filaTotal = hoja.getRow(5);
            assertEquals("Total", filaTotal.getCell(10).getStringCellValue());
            assertEquals(100.00d, filaTotal.getCell(11).getNumericCellValue(), 0.001);
        }
    }

    @Test
    void exportarResumenVentasExcelGeneraCuatroHojasYCalculaTotales() throws IOException {
        when(reporteDAO.obtenerReporteVentas(7)).thenReturn(Map.of(
                "days", 7,
                "summary", Map.of(
                        "revenue", new BigDecimal("250.50"),
                        "orders", 3,
                        "customers", 2L,
                        "average", new BigDecimal("125.25")),
                "byStatus", List.of(
                        Map.of("status", "pendiente", "count", 1L),
                        Map.of("status", "pagado", "count", 2L)),
                "byPayment", List.of(
                        Map.of("method", "yape", "revenue", new BigDecimal("250.50"))),
                "daily", List.of(
                        Map.of("day", "2026-11-13", "revenue", new BigDecimal("100.00")),
                        Map.of("day", "2026-11-14", "revenue", new BigDecimal("150.50"))),
                "popular", List.of(
                        Map.of("name", "Anillo Cielo", "quantity", 4, "revenue", new BigDecimal("260.00"))),
                "recent", List.of()));

        byte[] xlsx = service.exportarResumenVentasExcel(7);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            assertEquals(4, wb.getNumberOfSheets());
            assertNotNull(wb.getSheet("Resumen"));
            assertNotNull(wb.getSheet("Ventas por Día"));
            assertNotNull(wb.getSheet("Productos Vendidos"));
            assertNotNull(wb.getSheet("Pedidos"));

            Sheet resumen = wb.getSheet("Resumen");
            assertEquals("Ingresos confirmados", resumen.getRow(4).getCell(0).getStringCellValue());
            assertEquals(250.50d, resumen.getRow(4).getCell(1).getNumericCellValue(), 0.001);
            assertEquals(3d, resumen.getRow(5).getCell(1).getNumericCellValue(), 0.001);

            Sheet ventasDia = wb.getSheet("Ventas por Día");
            assertEquals("2026-11-13", ventasDia.getRow(4).getCell(0).getStringCellValue());
            assertEquals(100.00d, ventasDia.getRow(4).getCell(1).getNumericCellValue(), 0.001);
            assertEquals("Total", ventasDia.getRow(6).getCell(0).getStringCellValue());
            assertEquals(250.50d, ventasDia.getRow(6).getCell(1).getNumericCellValue(), 0.001);

            Sheet productos = wb.getSheet("Productos Vendidos");
            assertEquals("Anillo Cielo", productos.getRow(4).getCell(0).getStringCellValue());
            assertEquals(4d, productos.getRow(4).getCell(1).getNumericCellValue(), 0.001);
            assertEquals(260.00d, productos.getRow(4).getCell(2).getNumericCellValue(), 0.001);
        }
    }

    @Test
    void manejaCasosSinRegistrosYNeutralizaFormulasPeligrosas() throws IOException {
        when(reporteDAO.obtenerReporteVentas(15)).thenReturn(Map.of(
                "days", 15,
                "summary", Map.of(
                        "revenue", BigDecimal.ZERO,
                        "orders", 0,
                        "customers", 0L,
                        "average", BigDecimal.ZERO),
                "byStatus", List.of(),
                "byPayment", List.of(),
                "daily", List.of(),
                "popular", List.of(),
                "recent", List.of()));

        byte[] xlsxPedidos = service.exportarPedidosExcel(15);
        byte[] xlsxResumen = service.exportarResumenVentasExcel(15);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsxPedidos))) {
            Sheet hoja = wb.getSheet("Pedidos");
            assertEquals("No hay pedidos registrados en los últimos 15 días.", hoja.getRow(4).getCell(0).getStringCellValue());
        }
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsxResumen))) {
            assertEquals("No hay ventas confirmadas en los últimos 15 días.",
                    wb.getSheet("Ventas por Día").getRow(4).getCell(0).getStringCellValue());
            assertEquals("No se registraron productos vendidos en los últimos 15 días.",
                    wb.getSheet("Productos Vendidos").getRow(4).getCell(0).getStringCellValue());
        }

        assertEquals("'=CMD()", ReporteExcelPoiServiceImpl.sanitizarTextoCelda("=CMD()"));
        assertEquals("'+1+1", ReporteExcelPoiServiceImpl.sanitizarTextoCelda("+1+1"));
        assertEquals("'-2", ReporteExcelPoiServiceImpl.sanitizarTextoCelda("-2"));
        assertEquals("'@SUM(A1)", ReporteExcelPoiServiceImpl.sanitizarTextoCelda("@SUM(A1)"));
        assertEquals("María", ReporteExcelPoiServiceImpl.sanitizarTextoCelda("María"));
    }
}
