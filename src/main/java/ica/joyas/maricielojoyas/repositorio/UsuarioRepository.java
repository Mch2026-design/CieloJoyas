package ica.joyas.maricielojoyas.repositorio;

import ica.joyas.maricielojoyas.modelo.Usuario;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmailIgnoreCaseAndActivoTrue(String email);
    Optional<Usuario> findByEmailIgnoreCase(String email);
    boolean existsByRol(String rol);
}
