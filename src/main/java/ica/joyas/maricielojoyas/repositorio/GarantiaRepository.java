package ica.joyas.maricielojoyas.repositorio;

import ica.joyas.maricielojoyas.modelo.Garantia;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface GarantiaRepository extends JpaRepository<Garantia, Long> {
    List<Garantia> findAllByOrderByIdDesc();
}
