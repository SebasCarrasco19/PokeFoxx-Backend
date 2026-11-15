
package com.pokefoxx.backend.service;

import com.pokefoxx.backend.model.Producto;
import com.pokefoxx.backend.repository.ProductoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;

    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    // Listar todos los productos
    public List<Producto> listarProductos() {
        return productoRepository.findAll();
    }

    // Buscar producto por ID
    public Optional<Producto> buscarProductoPorId(Long id) {
        return productoRepository.findById(id);
    }

    // Guardar o actualizar producto
    public Producto guardarProducto(Producto producto) {
        return productoRepository.save(producto);
    }

    // Eliminar producto
    public void eliminarProducto(Long id) {
        productoRepository.deleteById(id);
    }
}
