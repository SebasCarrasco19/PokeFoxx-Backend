
package com.pokefoxx.backend.repository;

import com.pokefoxx.backend.model.DetalleOrden;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DetalleOrdenRepository
        extends JpaRepository<DetalleOrden, Long> {

    // Buscar los productos de una orden
    List<DetalleOrden> findByOrden_Id(Long ordenId);
}
