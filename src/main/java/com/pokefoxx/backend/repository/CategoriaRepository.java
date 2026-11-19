
package com.pokefoxx.backend.repository;

import com.pokefoxx.backend.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoriaRepository
        extends JpaRepository<Categoria, Long> {

    // Buscar categoria por nombre
    Optional<Categoria> findByNameIgnoreCase(String name);

    // Comprobar si ya existe una categoria
    boolean existsByNameIgnoreCase(String name);

    // Comprobar duplicados al actualizar
    boolean existsByNameIgnoreCaseAndIdNot(
            String name,
            Long id
    );
}
