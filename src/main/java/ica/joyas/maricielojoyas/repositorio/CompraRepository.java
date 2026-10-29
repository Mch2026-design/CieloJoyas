package ica.joyas.maricielojoyas.repositorio;

import ica.joyas.maricielojoyas.modelo.Compra;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface CompraRepository extends JpaRepository<Compra, Long> {
    List<Compra> findAllByOrderByIdDesc();
}
