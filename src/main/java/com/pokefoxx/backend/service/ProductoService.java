package com.pokefoxx.backend.service;

import com.pokefoxx.backend.model.Producto;
import com.pokefoxx.backend.model.Categoria;
import com.pokefoxx.backend.repository.ProductoRepository;
import com.pokefoxx.backend.repository.CategoriaRepository;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;

    public ProductoService(
            ProductoRepository productoRepository,
            CategoriaRepository categoriaRepository) {

        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    // Listar todos los productos
    public List<Producto> listarProductos() {
        return productoRepository.findAll();
    }

    // Listar productos con oferta activa
    public List<Producto> listarOfertas() {
        return productoRepository.findAll().stream()
                .filter(Producto::isOnSale)
                .toList();
    }

    // El stock es critico si llego al limite configurado
    public List<Producto> listarStockCritico() {
        return productoRepository.findAll().stream()
                .filter(producto -> producto.getStock() != null
                        && producto.getCriticalStock() != null
                        && producto.getStock() <= producto.getCriticalStock())
                .toList();
    }

    // Buscar producto por ID
    public Optional<Producto> buscarProductoPorId(Long id) {
        return productoRepository.findById(id);
    }

    // Buscar productos por categoria
    public List<Producto> listarProductosPorCategoria(Long categoriaId) {

        if (!categoriaRepository.existsById(categoriaId)) {
            throw new IllegalArgumentException(
                    "La categoria seleccionada no existe");
        }

        return productoRepository.findByCategoria_Id(categoriaId);
    }

    // Guardar o actualizar producto
    public Producto guardarProducto(Producto producto) {

        validarProducto(producto);

        producto.setCode(
                producto.getCode().trim().toUpperCase(Locale.ROOT));

        producto.setName(producto.getName().trim());

        // Comprobar codigo duplicado
        boolean codigoExiste;

        if (producto.getId() == null) {
            codigoExiste = productoRepository
                    .existsByCodeIgnoreCase(producto.getCode());
        } else {
            codigoExiste = productoRepository
                    .existsByCodeIgnoreCaseAndIdNot(
                            producto.getCode(),
                            producto.getId());
        }

        if (codigoExiste) {
            throw new IllegalArgumentException(
                    "Ya existe un producto con ese codigo");
        }

        // Validar y obtener categoria existente
        if (producto.getCategoria() != null) {

            Long categoriaId = producto.getCategoria().getId();

            if (categoriaId == null) {
                throw new IllegalArgumentException(
                        "Debes seleccionar una categoria existente");
            }

            Categoria categoria = categoriaRepository
                    .findById(categoriaId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "La categoria seleccionada no existe"));

            producto.setCategoria(categoria);
        }

        return productoRepository.save(producto);
    }

    // Validar datos del producto
    private void validarProducto(Producto producto) {

        if (producto == null) {
            throw new IllegalArgumentException(
                    "Debes ingresar los datos del producto");
        }

        if (producto.getCode() == null
                || producto.getCode().isBlank()) {
            throw new IllegalArgumentException(
                    "El codigo del producto es obligatorio");
        }

        if (producto.getName() == null
                || producto.getName().isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre del producto es obligatorio");
        }

        if (producto.getPrice() == null
                || producto.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "El precio debe ser mayor que cero");
        }

        if (producto.getOriginalPrice() == null
                || producto.getOriginalPrice()
                        .compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "El precio original no puede ser negativo");
        }

        // Una oferta debe tener un precio menor al original
        if (producto.isOnSale()
                && producto.getPrice()
                        .compareTo(producto.getOriginalPrice()) >= 0) {
            throw new IllegalArgumentException(
                    "El precio en oferta debe ser menor al precio original");
        }

        // Sin oferta, ambos precios deben coincidir
        if (!producto.isOnSale()
                && producto.getPrice()
                        .compareTo(producto.getOriginalPrice()) != 0) {
            throw new IllegalArgumentException(
                    "Sin oferta, el precio debe ser igual al precio original");
        }

        if (producto.getStock() != null
                && producto.getStock() < 0) {
            throw new IllegalArgumentException(
                    "El stock no puede ser negativo");
        }

        if (producto.getCriticalStock() != null
                && producto.getCriticalStock() < 0) {
            throw new IllegalArgumentException(
                    "El stock critico no puede ser negativo");
        }

        if (producto.getCode().trim().length() > 20
                || producto.getName().trim().length() > 150) {
            throw new IllegalArgumentException(
                    "El codigo o nombre supera la longitud permitida");
        }

        if (producto.getDescription() != null
                && producto.getDescription().length() > 1000) {
            throw new IllegalArgumentException(
                    "La descripcion supera los 1000 caracteres");
        }
    }

    // Eliminar producto
    public void eliminarProducto(Long id) {
        productoRepository.deleteById(id);
    }
}