package ica.joyas.maricielojoyas.service;

/** Servicio para la generación de reportes administrativos en formato Excel (.xlsx). */
public interface IReporteExcelService {

    /** Genera un libro Excel (.xlsx) con el listado de pedidos del periodo indicado. */
    byte[] exportarPedidosExcel(int days);

    /** Genera un libro Excel (.xlsx) multipestaña con resumen, ventas diarias, productos y pedidos. */
    byte[] exportarResumenVentasExcel(int days);
}
