
package com.pokefoxx.backend.controller;

import com.pokefoxx.backend.model.Categoria;
import com.pokefoxx.backend.service.CategoriaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/categorias")
@Tag(
    name = "Gestión de Categorías",
    description = "Consulta y administración de las categorías de productos de PokeFoxx."
)
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    // Listar todas las categorias
    @Operation(
        summary = "Listar todas las categorías",
        description = "Acceso público. Obtiene todas las categorías registradas en el sistema."
    )
    @GetMapping
    public List<Categoria> listarCategorias() {
        return categoriaService.listarCategorias();
    }

    // Buscar categoria por ID
    @Operation(
        summary = "Buscar categoría por ID",
        description = "Acceso público. Consulta una categoría utilizando su identificador."
    )
    @GetMapping("/{id}")
    public ResponseEntity<Categoria> buscarCategoria(
            @PathVariable Long id) {

        return categoriaService.buscarCategoriaPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Crear categoria
    @Operation(
        summary = "Crear categoría",
        description = "Solo Administrador. Registra una nueva categoría y valida que no exista otra con el mismo nombre."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<?> crearCategoria(
            @RequestBody Categoria categoria) {

        try {

            categoria.setId(null);

            Categoria nuevaCategoria =
                    categoriaService.guardarCategoria(categoria);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(nuevaCategoria);

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Actualizar categoria
    @Operation(
        summary = "Actualizar categoría",
        description = "Solo Administrador. Modifica los datos de una categoría existente, aplicando las validaciones correspondientes."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarCategoria(
            @PathVariable Long id,
            @RequestBody Categoria categoria) {

        if (categoriaService.buscarCategoriaPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {

            categoria.setId(id);

            Categoria actualizada =
                    categoriaService.guardarCategoria(categoria);

            return ResponseEntity.ok(actualizada);

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Eliminar categoria
    @Operation(
        summary = "Eliminar categoría",
        description = "Solo Administrador. Elimina una categoría si no tiene productos asociados. Si contiene productos, la eliminación es rechazada."
    )
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarCategoria(
            @PathVariable Long id) {

        if (categoriaService.buscarCategoriaPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {

            categoriaService.eliminarCategoria(id);

            return ResponseEntity.noContent().build();

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
