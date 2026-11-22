
package com.pokefoxx.backend.controller;

import com.pokefoxx.backend.model.Producto;
import com.pokefoxx.backend.service.ProductoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/productos")
@Tag(
    name = "Gestión de Productos",
    description = "Consulta y administración del catálogo de productos de PokeFoxx."
)
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    // Listar todos los productos
    @Operation(
        summary = "Listar todos los productos",
        description = "Acceso público. Obtiene todos los productos registrados en el catálogo."
    )
    @GetMapping
    public List<Producto> listarProductos() {
        return productoService.listarProductos();
    }

    // Listar productos en oferta
    @Operation(
        summary = "Consultar productos en oferta",
        description = "Acceso público. Obtiene los productos que actualmente están marcados como ofertas."
    )
    @GetMapping("/ofertas")
    public List<Producto> listarOfertas() {
        return productoService.listarOfertas();
    }

    // Consultar productos con stock critico
    @Operation(
        summary = "Consultar productos con stock crítico",
        description = "Solo Administrador. Obtiene los productos que tienen existencias iguales o inferiores a su nivel de stock crítico."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/stock-critico")
    public List<Producto> listarStockCritico() {
        return productoService.listarStockCritico();
    }

    // Buscar producto por ID
    @Operation(
        summary = "Buscar producto por ID",
        description = "Acceso público. Consulta la información de un producto utilizando su identificador."
    )
    @GetMapping("/{id}")
    public ResponseEntity<Producto> buscarProducto(
            @PathVariable Long id) {

        return productoService.buscarProductoPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Buscar productos por categoria
    @Operation(
        summary = "Filtrar productos por categoría",
        description = "Acceso público. Obtiene los productos asociados a una categoría determinada."
    )
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
    @Operation(
        summary = "Crear producto",
        description = "Solo Administrador. Registra un nuevo producto y aplica las validaciones correspondientes."
    )
    @SecurityRequirement(name = "bearerAuth")
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
    @Operation(
        summary = "Actualizar producto",
        description = "Solo Administrador. Modifica la información de un producto existente, incluyendo precio, stock y otros datos."
    )
    @SecurityRequirement(name = "bearerAuth")
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
    @Operation(
        summary = "Eliminar producto",
        description = "Solo Administrador. Elimina un producto si no tiene compras asociadas. Si tiene historial de compras, devuelve 409 Conflict."
    )
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarProducto(
            @PathVariable Long id) {

        if (productoService.buscarProductoPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            productoService.eliminarProducto(id);
            return ResponseEntity.noContent().build();

        } catch (IllegalArgumentException e) {

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));

        } catch (org.springframework.dao.DataIntegrityViolationException e) {

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "error",
                            "No puedes eliminar un producto con datos asociados"
                    ));
        }
    }
}
