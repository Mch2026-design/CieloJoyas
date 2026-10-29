package ica.joyas.maricielojoyas.repositorio;

import ica.joyas.maricielojoyas.modelo.DetallePedido;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Long> {
    List<DetallePedido> findByPedidoIdOrderById(Long pedidoId);
    boolean existsByPedidoIdAndProductoId(Long pedidoId, Long productoId);
}
