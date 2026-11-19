
package com.pokefoxx.backend.controller;

import com.pokefoxx.backend.model.Producto;
import com.pokefoxx.backend.service.ProductoService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    // Listar todos los productos
    @GetMapping
    public List<Producto> listarProductos() {
        return productoService.listarProductos();
    }

    // Buscar producto por ID
    @GetMapping("/{id}")
    public ResponseEntity<Producto> buscarProducto(
            @PathVariable Long id) {

        return productoService.buscarProductoPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Buscar productos por categoria
    @GetMapping("/categoria/{categoriaId}")
    public ResponseEntity<?> listarProductosPorCategoria(
            @PathVariable Long categoriaId) {

        try {

            return ResponseEntity.ok(
                    productoService.listarProductosPorCategoria(categoriaId)
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Crear producto
    @PostMapping
    public ResponseEntity<?> crearProducto(
            @RequestBody Producto producto) {

        try {

            producto.setId(null);

            Producto nuevoProducto =
                    productoService.guardarProducto(producto);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(nuevoProducto);

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Actualizar producto
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarProducto(
            @PathVariable Long id,
            @RequestBody Producto producto) {

        if (productoService.buscarProductoPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {

            producto.setId(id);

            Producto actualizado =
                    productoService.guardarProducto(producto);

            return ResponseEntity.ok(actualizado);

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Eliminar producto
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProducto(
            @PathVariable Long id) {

        if (productoService.buscarProductoPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        productoService.eliminarProducto(id);

        return ResponseEntity.noContent().build();
    }
}
