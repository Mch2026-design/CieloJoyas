package ica.joyas.maricielojoyas.service;

import ica.joyas.maricielojoyas.dao.IReporteDAO;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/** Implementación de exportación de reportes Excel (.xlsx) usando Apache POI y IReporteDAO. */
@Service
public class ReporteExcelPoiServiceImpl implements IReporteExcelService {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final Map<String, String> ETIQUETAS_ESTADO = Map.of(
            "pendiente", "Pendientes de pago",
            "pagado", "Pago confirmado",
            "preparando", "En preparación",
            "enviado", "Enviados",
            "entregado", "Entregados",
            "cancelado", "Cancelados");

    private final IReporteDAO reporteDAO;

    public ReporteExcelPoiServiceImpl(IReporteDAO reporteDAO) {
        this.reporteDAO = reporteDAO;
    }

    @Override
    public byte[] exportarPedidosExcel(int days) {
        Map<String, Object> reporte = obtenerDatosValidados(days);
        return generarLibro(workbook -> {
            Estilos estilos = new Estilos(workbook);
            construirHojaPedidos(workbook, estilos, days, listaMapas(reporte.get("recent")));
        });
    }

    @Override
    public byte[] exportarResumenVentasExcel(int days) {
        Map<String, Object> reporte = obtenerDatosValidados(days);
        return generarLibro(workbook -> {
            Estilos estilos = new Estilos(workbook);
            construirHojaResumen(workbook, estilos, days, reporte);
            construirHojaVentasDiarias(workbook, estilos, days, listaMapas(reporte.get("daily")));
            construirHojaProductosPopulares(workbook, estilos, days, listaMapas(reporte.get("popular")));
            construirHojaPedidos(workbook, estilos, days, listaMapas(reporte.get("recent")));
        });
    }

    private Map<String, Object> obtenerDatosValidados(int days) {
        if (days < 1 || days > 365) {
            throw new IllegalArgumentException("El periodo debe estar entre 1 y 365 días.");
        }
        return reporteDAO.obtenerReporteVentas(days);
    }

    private byte[] generarLibro(GeneradorContenido generador) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            generador.escribir(workbook);
            workbook.write(salida);
            return salida.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo generar el libro Excel.", ex);
        }
    }

    private void construirHojaResumen(
            XSSFWorkbook workbook, Estilos estilos, int days, Map<String, Object> reporte) {
        Sheet hoja = workbook.createSheet("Resumen");
        int filaIdx = escribirTitulo(hoja, estilos,
                "Cielo Joyas — Reporte General de Ventas",
                "Periodo analizado: Últimos " + days + " días (hora Lima)");

        Map<String, Object> summary = mapa(reporte.get("summary"));
        filaIdx = escribirEncabezado(hoja, filaIdx, estilos, "Indicador", "Valor");
        escribirParMetrica(hoja, filaIdx++, estilos, "Ingresos confirmados", numeroDecimal(summary.get("revenue")), true);
        escribirParMetrica(hoja, filaIdx++, estilos, "Pedidos registrados", numeroEntero(summary.get("orders")), false);
        escribirParMetrica(hoja, filaIdx++, estilos, "Clientes únicos", numeroEntero(summary.get("customers")), false);
        escribirParMetrica(hoja, filaIdx++, estilos, "Promedio de venta confirmada", numeroDecimal(summary.get("average")), true);

        filaIdx++;
        filaIdx = escribirEncabezado(hoja, filaIdx, estilos, "Estado del pedido", "Cantidad");
        for (Map<String, Object> estado : listaMapas(reporte.get("byStatus"))) {
            String codigo = texto(estado.get("status"));
            Row row = hoja.createRow(filaIdx++);
            crearCeldaTexto(row, 0, ETIQUETAS_ESTADO.getOrDefault(codigo, codigo), estilos.texto);
            crearCeldaEntero(row, 1, numeroEntero(estado.get("count")), estilos.entero);
        }

        filaIdx++;
        filaIdx = escribirEncabezado(hoja, filaIdx, estilos, "Medio de pago confirmado", "Ingresos");
        List<Map<String, Object>> medios = listaMapas(reporte.get("byPayment"));
        if (medios.isEmpty()) {
            escribirFilaVacia(hoja, filaIdx, estilos, "Sin pagos confirmados en el periodo.");
        } else {
            for (Map<String, Object> medio : medios) {
                Row row = hoja.createRow(filaIdx++);
                crearCeldaTexto(row, 0, textoSeguro(medio.get("method")), estilos.texto);
                crearCeldaMoneda(row, 1, numeroDecimal(medio.get("revenue")), estilos.moneda);
            }
        }
        ajustarColumnas(hoja, new int[]{36, 22});
    }

    private void construirHojaVentasDiarias(
            XSSFWorkbook workbook, Estilos estilos, int days, List<Map<String, Object>> diario) {
        Sheet hoja = workbook.createSheet("Ventas por Día");
        int filaIdx = escribirTitulo(hoja, estilos,
                "Cielo Joyas — Ventas Confirmadas por Día",
                "Periodo: Últimos " + days + " días");
        filaIdx = escribirEncabezado(hoja, filaIdx, estilos, "Fecha", "Ingreso Confirmado");

        if (diario.isEmpty()) {
            escribirFilaVacia(hoja, filaIdx, estilos, "No hay ventas confirmadas en los últimos " + days + " días.");
        } else {
            BigDecimal total = BigDecimal.ZERO;
            for (Map<String, Object> dia : diario) {
                BigDecimal ingreso = numeroDecimal(dia.get("revenue"));
                total = total.add(ingreso);
                Row row = hoja.createRow(filaIdx++);
                crearCeldaTexto(row, 0, textoSeguro(dia.get("day")), estilos.texto);
                crearCeldaMoneda(row, 1, ingreso, estilos.moneda);
            }
            Row rowTotal = hoja.createRow(filaIdx);
            crearCeldaTexto(rowTotal, 0, "Total", estilos.totalEtiqueta);
            crearCeldaMoneda(rowTotal, 1, total, estilos.totalMoneda);
        }
        ajustarColumnas(hoja, new int[]{18, 22});
    }

    private void construirHojaProductosPopulares(
            XSSFWorkbook workbook, Estilos estilos, int days, List<Map<String, Object>> populares) {
        Sheet hoja = workbook.createSheet("Productos Vendidos");
        int filaIdx = escribirTitulo(hoja, estilos,
                "Cielo Joyas — Productos Más Vendidos",
                "Periodo: Últimos " + days + " días (importes antes de descuentos)");
        filaIdx = escribirEncabezado(hoja, filaIdx, estilos, "Producto", "Unidades", "Importe");

        if (populares.isEmpty()) {
            escribirFilaVacia(hoja, filaIdx, estilos, "No se registraron productos vendidos en los últimos " + days + " días.");
        } else {
            for (Map<String, Object> item : populares) {
                Row row = hoja.createRow(filaIdx++);
                crearCeldaTexto(row, 0, textoSeguro(item.get("name")), estilos.texto);
                crearCeldaEntero(row, 1, numeroEntero(item.get("quantity")), estilos.entero);
                crearCeldaMoneda(row, 2, numeroDecimal(item.get("revenue")), estilos.moneda);
            }
        }
        ajustarColumnas(hoja, new int[]{36, 14, 20});
    }

    private void construirHojaPedidos(
            XSSFWorkbook workbook, Estilos estilos, int days, List<Map<String, Object>> recientes) {
        Sheet hoja = workbook.createSheet("Pedidos");
        int filaIdx = escribirTitulo(hoja, estilos,
                "Cielo Joyas — Listado de Pedidos",
                "Periodo: Últimos " + days + " días");
        filaIdx = escribirEncabezado(hoja, filaIdx, estilos,
                "ID", "Cliente", "Correo", "Teléfono", "Ubicación", "Fecha Creación",
                "Estado Pedido", "Medio Pago", "Estado Pago", "Subtotal", "Descuento", "Total", "Productos");

        if (recientes.isEmpty()) {
            escribirFilaVacia(hoja, filaIdx, estilos, "No hay pedidos registrados en los últimos " + days + " días.");
        } else {
            BigDecimal sumaTotal = BigDecimal.ZERO;
            for (Map<String, Object> p : recientes) {
                BigDecimal total = numeroDecimal(p.get("total"));
                sumaTotal = sumaTotal.add(total);
                Row row = hoja.createRow(filaIdx++);
                crearCeldaEntero(row, 0, numeroEntero(p.get("id")), estilos.entero);
                crearCeldaTexto(row, 1, textoSeguro(p.get("customer")), estilos.texto);
                crearCeldaTexto(row, 2, textoSeguro(p.get("email")), estilos.texto);
                crearCeldaTexto(row, 3, textoSeguro(p.get("phone")), estilos.texto);
                crearCeldaTexto(row, 4, formatearUbicacion(p), estilos.texto);
                crearCeldaFecha(row, 5, p.get("createdAt"), estilos.fecha, estilos.texto);
                crearCeldaTexto(row, 6, textoSeguro(p.get("status")), estilos.texto);
                crearCeldaTexto(row, 7, textoSeguro(p.get("paymentMethod")), estilos.texto);
                crearCeldaTexto(row, 8, textoSeguro(p.get("paymentStatus")), estilos.texto);
                crearCeldaMoneda(row, 9, numeroDecimal(p.get("subtotal")), estilos.moneda);
                crearCeldaMoneda(row, 10, numeroDecimal(p.get("discount")), estilos.moneda);
                crearCeldaMoneda(row, 11, total, estilos.moneda);
                crearCeldaTexto(row, 12, resumirProductos(listaMapas(p.get("items"))), estilos.texto);
            }
            Row rowTotal = hoja.createRow(filaIdx);
            crearCeldaTexto(rowTotal, 10, "Total", estilos.totalEtiqueta);
            crearCeldaMoneda(rowTotal, 11, sumaTotal, estilos.totalMoneda);
        }
        ajustarColumnas(hoja, new int[]{10, 26, 28, 15, 28, 19, 16, 15, 15, 16, 16, 16, 42});
    }

    private int escribirTitulo(Sheet hoja, Estilos estilos, String titulo, String subtitulo) {
        Row r0 = hoja.createRow(0);
        crearCeldaTexto(r0, 0, titulo, estilos.titulo);
        Row r1 = hoja.createRow(1);
        crearCeldaTexto(r1, 0, subtitulo, estilos.subtitulo);
        return 3;
    }

    private int escribirEncabezado(Sheet hoja, int filaIdx, Estilos estilos, String... columnas) {
        Row header = hoja.createRow(filaIdx);
        header.setHeightInPoints(22);
        for (int i = 0; i < columnas.length; i++) {
            Cell c = header.createCell(i);
            c.setCellValue(columnas[i]);
            c.setCellStyle(estilos.encabezado);
        }
        return filaIdx + 1;
    }

    private void escribirParMetrica(
            Sheet hoja, int filaIdx, Estilos estilos, String etiqueta, Number valor, boolean moneda) {
        Row row = hoja.createRow(filaIdx);
        crearCeldaTexto(row, 0, etiqueta, estilos.texto);
        if (moneda) {
            crearCeldaMoneda(row, 1, (BigDecimal) valor, estilos.moneda);
        } else {
            crearCeldaEntero(row, 1, valor.longValue(), estilos.entero);
        }
    }

    private void escribirFilaVacia(Sheet hoja, int filaIdx, Estilos estilos, String mensaje) {
        Row row = hoja.createRow(filaIdx);
        crearCeldaTexto(row, 0, mensaje, estilos.subtitulo);
    }

    private void ajustarColumnas(Sheet hoja, int[] anchosCaracteres) {
        for (int i = 0; i < anchosCaracteres.length; i++) {
            hoja.setColumnWidth(i, anchosCaracteres[i] * 256);
        }
    }

    private static void crearCeldaTexto(Row row, int col, String valor, CellStyle estilo) {
        Cell cell = row.createCell(col);
        cell.setCellValue(valor == null ? "" : valor);
        cell.setCellStyle(estilo);
    }

    private static void crearCeldaEntero(Row row, int col, long valor, CellStyle estilo) {
        Cell cell = row.createCell(col);
        cell.setCellValue(valor);
        cell.setCellStyle(estilo);
    }

    private static void crearCeldaMoneda(Row row, int col, BigDecimal valor, CellStyle estilo) {
        Cell cell = row.createCell(col);
        cell.setCellValue((valor == null ? BigDecimal.ZERO : valor).doubleValue());
        cell.setCellStyle(estilo);
    }

    private static void crearCeldaFecha(Row row, int col, Object valor, CellStyle estiloFecha, CellStyle estiloTexto) {
        Cell cell = row.createCell(col);
        if (valor instanceof OffsetDateTime odt) {
            LocalDateTime local = odt.atZoneSameInstant(LIMA).toLocalDateTime();
            cell.setCellValue(local);
            cell.setCellStyle(estiloFecha);
        } else {
            cell.setCellValue(valor == null ? "" : textoSeguro(valor));
            cell.setCellStyle(estiloTexto);
        }
    }

    private static String formatearUbicacion(Map<String, Object> pedido) {
        String distrito = texto(pedido.get("district"));
        String provincia = texto(pedido.get("province"));
        String departamento = texto(pedido.get("department"));
        if (distrito.isBlank() && provincia.isBlank() && departamento.isBlank()) {
            return "";
        }
        return sanitizarTextoCelda(distrito + ", " + provincia + ", " + departamento);
    }

    private static String resumirProductos(List<Map<String, Object>> items) {
        if (items.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                sb.append(" | ");
            }
            Map<String, Object> item = items.get(i);
            sb.append(numeroEntero(item.get("quantity")))
              .append("x ")
              .append(texto(item.get("name")));
        }
        return sanitizarTextoCelda(sb.toString());
    }

    /** Neutraliza prefijos de fórmulas (=, +, -, @, tab, CR) en texto libre exportado a Excel. */
    static String sanitizarTextoCelda(String valor) {
        if (valor == null || valor.isEmpty()) {
            return "";
        }
        char primero = valor.charAt(0);
        if (primero == '=' || primero == '+' || primero == '-' || primero == '@'
                || primero == '\t' || primero == '\r') {
            return "'" + valor;
        }
        return valor;
    }

    private static String textoSeguro(Object valor) {
        return sanitizarTextoCelda(texto(valor));
    }

    private static String texto(Object valor) {
        return valor == null ? "" : String.valueOf(valor);
    }

    private static BigDecimal numeroDecimal(Object valor) {
        if (valor instanceof BigDecimal bd) {
            return bd;
        }
        if (valor instanceof Number num) {
            return BigDecimal.valueOf(num.doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private static long numeroEntero(Object valor) {
        return valor instanceof Number num ? num.longValue() : 0L;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapa(Object valor) {
        return valor instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> listaMapas(Object valor) {
        return valor instanceof List<?> l ? (List<Map<String, Object>>) l : List.of();
    }

    @FunctionalInterface
    private interface GeneradorContenido {
        void escribir(XSSFWorkbook workbook);
    }

    /** Paleta de estilos reutilizable por libro para respetar el límite de estilos de Excel. */
    private static final class Estilos {
        final CellStyle titulo;
        final CellStyle subtitulo;
        final CellStyle encabezado;
        final CellStyle texto;
        final CellStyle entero;
        final CellStyle moneda;
        final CellStyle fecha;
        final CellStyle totalEtiqueta;
        final CellStyle totalMoneda;

        Estilos(XSSFWorkbook wb) {
            DataFormat df = wb.createDataFormat();

            Font fTitulo = wb.createFont();
            fTitulo.setBold(true);
            fTitulo.setFontHeightInPoints((short) 13);

            Font fSubtitulo = wb.createFont();
            fSubtitulo.setItalic(true);
            fSubtitulo.setColor(IndexedColors.GREY_50_PERCENT.getIndex());

            Font fEncabezado = wb.createFont();
            fEncabezado.setBold(true);
            fEncabezado.setColor(IndexedColors.WHITE.getIndex());

            Font fNegrita = wb.createFont();
            fNegrita.setBold(true);

            this.titulo = wb.createCellStyle();
            this.titulo.setFont(fTitulo);

            this.subtitulo = wb.createCellStyle();
            this.subtitulo.setFont(fSubtitulo);

            this.encabezado = wb.createCellStyle();
            this.encabezado.setFont(fEncabezado);
            this.encabezado.setFillForegroundColor(IndexedColors.GREY_80_PERCENT.getIndex());
            this.encabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            this.encabezado.setAlignment(HorizontalAlignment.CENTER);
            this.encabezado.setVerticalAlignment(VerticalAlignment.CENTER);
            aplicarBordes(this.encabezado);

            this.texto = wb.createCellStyle();
            this.texto.setVerticalAlignment(VerticalAlignment.CENTER);
            aplicarBordes(this.texto);

            this.entero = wb.createCellStyle();
            this.entero.setDataFormat(df.getFormat("#,##0"));
            this.entero.setAlignment(HorizontalAlignment.RIGHT);
            this.entero.setVerticalAlignment(VerticalAlignment.CENTER);
            aplicarBordes(this.entero);

            this.moneda = wb.createCellStyle();
            this.moneda.setDataFormat(df.getFormat("\"S/ \"#,##0.00"));
            this.moneda.setAlignment(HorizontalAlignment.RIGHT);
            this.moneda.setVerticalAlignment(VerticalAlignment.CENTER);
            aplicarBordes(this.moneda);

            this.fecha = wb.createCellStyle();
            this.fecha.setDataFormat(df.getFormat("dd/mm/yyyy hh:mm"));
            this.fecha.setVerticalAlignment(VerticalAlignment.CENTER);
            aplicarBordes(this.fecha);

            this.totalEtiqueta = wb.createCellStyle();
            this.totalEtiqueta.setFont(fNegrita);
            this.totalEtiqueta.setAlignment(HorizontalAlignment.RIGHT);
            aplicarBordes(this.totalEtiqueta);

            this.totalMoneda = wb.createCellStyle();
            this.totalMoneda.setFont(fNegrita);
            this.totalMoneda.setDataFormat(df.getFormat("\"S/ \"#,##0.00"));
            this.totalMoneda.setAlignment(HorizontalAlignment.RIGHT);
            aplicarBordes(this.totalMoneda);
        }

        private static void aplicarBordes(CellStyle style) {
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
        }
    }
}
