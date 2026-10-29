package ica.joyas.maricielojoyas.repositorio;

import ica.joyas.maricielojoyas.modelo.Cliente;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByEmailIgnoreCase(String email);
    @Modifying
    @Query(value = "INSERT INTO clientes (nombres, apellidos, email, telefono, suscrito) VALUES (:nombres, :apellidos, :email, :telefono, :suscrito) ON CONFLICT (email) DO UPDATE SET suscrito = clientes.suscrito OR EXCLUDED.suscrito, nombres = CASE WHEN clientes.nombres = '' THEN EXCLUDED.nombres ELSE clientes.nombres END, apellidos = CASE WHEN clientes.apellidos = '' THEN EXCLUDED.apellidos ELSE clientes.apellidos END, telefono = COALESCE(clientes.telefono, EXCLUDED.telefono)", nativeQuery = true)
    void registrarSiFalta(@Param("nombres") String nombres, @Param("apellidos") String apellidos, @Param("email") String email, @Param("telefono") String telefono, @Param("suscrito") boolean suscrito);
}
