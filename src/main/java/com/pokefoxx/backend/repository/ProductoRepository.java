<<<<<<< HEAD

package com.pokefoxx.backend.repository;

import com.pokefoxx.backend.model.Producto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface ProductoRepository
        extends JpaRepository<Producto, Long> {

    // Bloquear un producto mientras se procesa una compra
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Producto p WHERE p.id = :id")
    Optional<Producto> buscarParaCompra(@Param("id") Long id);
}
=======
package com.pokefoxx.backend.repository;

import com.pokefoxx.backend.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

}
>>>>>>> origin/main
