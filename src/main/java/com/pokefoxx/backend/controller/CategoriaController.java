package com.pokefoxx.backend.controller;

import com.pokefoxx.backend.model.Categoria;
import com.pokefoxx.backend.service.CategoriaService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    // Listar todas las categorias
    @GetMapping
    public List<Categoria> listarCategorias() {
        return categoriaService.listarCategorias();
    }

    // Buscar categoria por ID
    @GetMapping("/{id}")
    public ResponseEntity<Categoria> buscarCategoria(
            @PathVariable Long id) {

        return categoriaService.buscarCategoriaPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Crear categoria
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