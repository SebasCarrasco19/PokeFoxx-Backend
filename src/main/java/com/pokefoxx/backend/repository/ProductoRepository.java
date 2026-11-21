
package com.pokefoxx.backend.repository;

import com.pokefoxx.backend.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    // Comprobar si existe un codigo de producto
    boolean existsByCodeIgnoreCase(String code);

    // Comprobar si el codigo pertenece a otro producto
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    // Buscar productos por categoria
    List<Producto> findByCategoria_Id(Long categoriaId);
}
