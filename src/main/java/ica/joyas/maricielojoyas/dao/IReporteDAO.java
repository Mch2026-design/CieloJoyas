package ica.joyas.maricielojoyas.dao;

import java.util.List;
import java.util.Map;

/** Contrato DAO para consultas SQL de reportes, analítica y diagnóstico de base de datos. */
public interface IReporteDAO {

    /** Retorna el nombre de la base de datos activa según el motor SQL. */
    String obtenerBaseDatosActual();

    /** Construye el reporte agregado de ventas y pedidos recientes para los últimos {@code days} días. */
    Map<String, Object> obtenerReporteVentas(int days);

    /** Obtiene la segmentación de clientes suscritas según sus pedidos confirmados. */
    List<Map<String, Object>> obtenerSegmentosClientes();
}
