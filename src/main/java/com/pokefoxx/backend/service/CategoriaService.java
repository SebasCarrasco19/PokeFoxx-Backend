package com.pokefoxx.backend.service;

import com.pokefoxx.backend.model.Categoria;
import com.pokefoxx.backend.repository.CategoriaRepository;
import com.pokefoxx.backend.repository.ProductoRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;

    public CategoriaService(
            CategoriaRepository categoriaRepository,
            ProductoRepository productoRepository) {

        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
    }

    // Listar todas las categorias
    public List<Categoria> listarCategorias() {
        return categoriaRepository.findAll();
    }

    // Buscar categoria por ID
    public Optional<Categoria> buscarCategoriaPorId(Long id) {
        return categoriaRepository.findById(id);
    }

    // Crear o actualizar categoria
    public Categoria guardarCategoria(Categoria categoria) {

        validarCategoria(categoria);

        String nombre = categoria.getName().trim();

        // Comprobar si el nombre ya existe
        boolean existe;

        if (categoria.getId() == null) {
            existe = categoriaRepository
                    .existsByNameIgnoreCase(nombre);
        } else {
            existe = categoriaRepository
                    .existsByNameIgnoreCaseAndIdNot(
                            nombre,
                            categoria.getId()
                    );
        }

        if (existe) {
            throw new IllegalArgumentException(
                    "Ya existe una categoria con ese nombre"
            );
        }

        categoria.setName(nombre);

        return categoriaRepository.save(categoria);
    }

    // Validar datos de la categoria
    private void validarCategoria(Categoria categoria) {

        if (categoria == null) {
            throw new IllegalArgumentException(
                    "Debes ingresar los datos de la categoria"
            );
        }

        if (categoria.getName() == null
                || categoria.getName().isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre de la categoria es obligatorio"
            );
        }

        if (categoria.getName().trim().length() > 100) {
            throw new IllegalArgumentException(
                    "El nombre supera los 100 caracteres"
            );
        }

        if (categoria.getDescription() != null
                && categoria.getDescription().length() > 500) {
            throw new IllegalArgumentException(
                    "La descripcion supera los 500 caracteres"
            );
        }
    }

    // Eliminar categoria
    public void eliminarCategoria(Long id) {

        if (!productoRepository.findByCategoria_Id(id).isEmpty()) {
            throw new IllegalArgumentException(
                    "No puedes eliminar una categoria con productos asociados");
        }

        categoriaRepository.deleteById(id);
    }
}