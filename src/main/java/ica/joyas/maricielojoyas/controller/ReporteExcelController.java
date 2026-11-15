package ica.joyas.maricielojoyas.controller;

import ica.joyas.maricielojoyas.service.IReporteExcelService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static ica.joyas.maricielojoyas.validacion.Reglas.error;

/** Controlador REST para la descarga segura de reportes Excel (.xlsx) de administración. */
@RestController
@RequestMapping("/api/admin/reportes/excel")
public class ReporteExcelController {

    private static final MediaType MEDIA_XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final IReporteExcelService excelService;

    public ReporteExcelController(IReporteExcelService excelService) {
        this.excelService = excelService;
    }

    /** Descarga el listado de pedidos de los últimos {@code days} días en formato .xlsx. */
    @GetMapping("/pedidos")
    public ResponseEntity<byte[]> exportarPedidos(@RequestParam(defaultValue = "30") int days) {
        validarPeriodo(days);
        byte[] archivo = excelService.exportarPedidosExcel(days);
        return respuestaExcel(archivo, "cielo-pedidos-" + days + "d.xlsx");
    }

    /** Descarga el resumen multipestaña de ventas de los últimos {@code days} días en formato .xlsx. */
    @GetMapping("/resumen")
    public ResponseEntity<byte[]> exportarResumen(@RequestParam(defaultValue = "30") int days) {
        validarPeriodo(days);
        byte[] archivo = excelService.exportarResumenVentasExcel(days);
        return respuestaExcel(archivo, "cielo-reporte-ventas-" + days + "d.xlsx");
    }

    private static void validarPeriodo(int days) {
        if (days < 1 || days > 365) {
            throw error("El periodo debe estar entre 1 y 365 días.");
        }
    }

    private static ResponseEntity<byte[]> respuestaExcel(byte[] contenido, String nombreArchivo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MEDIA_XLSX);
        headers.setContentDisposition(ContentDisposition.attachment().filename(nombreArchivo).build());
        headers.setContentLength(contenido.length);
        return ResponseEntity.ok().headers(headers).body(contenido);
    }
}
