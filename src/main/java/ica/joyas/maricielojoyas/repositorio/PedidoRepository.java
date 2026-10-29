package ica.joyas.maricielojoyas.repositorio;

import ica.joyas.maricielojoyas.modelo.Pedido;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByUsuarioIdOrderByIdDesc(Long usuarioId);
    Optional<Pedido> findByClaveSolicitud(String claveSolicitud);
    List<Pedido> findByCreadoEnAfterOrderByIdDesc(OffsetDateTime desde);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.id = :id")
    Optional<Pedido> bloquearPorId(@Param("id") Long id);
}
