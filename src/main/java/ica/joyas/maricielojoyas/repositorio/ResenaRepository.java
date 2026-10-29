package ica.joyas.maricielojoyas.repositorio;

import ica.joyas.maricielojoyas.modelo.Resena;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ResenaRepository extends JpaRepository<Resena, Long> {
    List<Resena> findByProductoIdAndAprobadaTrueOrderByIdDesc(Long productoId);
    boolean existsByProductoIdAndPedidoId(Long productoId, Long pedidoId);
    List<Resena> findByAprobadaTrueOrderByIdDesc();
    Optional<Resena> findFirstByFotoUrl(String fotoUrl);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Resena r where r.id = :id")
    Optional<Resena> bloquearPorId(@Param("id") Long id);
}
