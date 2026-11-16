package ica.joyas.maricielojoyas.repositorio;

import ica.joyas.maricielojoyas.modelo.ProductoTalla;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductoTallaRepository extends JpaRepository<ProductoTalla, Long> {
    List<ProductoTalla> findByProductoId(Long productoId);
    List<ProductoTalla> findAllByOrderByProductoIdAscIdAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select pt from ProductoTalla pt where pt.producto.id = :productoId and pt.talla = :talla")
    Optional<ProductoTalla> bloquearPorProductoYTalla(
            @Param("productoId") Long productoId,
            @Param("talla") String talla);
}
