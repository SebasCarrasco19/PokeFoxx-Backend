
package com.pokefoxx.backend.config;

import com.pokefoxx.backend.model.Categoria;
import com.pokefoxx.backend.model.Producto;
import com.pokefoxx.backend.repository.CategoriaRepository;
import com.pokefoxx.backend.repository.ProductoRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@Profile("!test")
public class CatalogoInitializer implements CommandLineRunner {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;

    public CatalogoInitializer(
            ProductoRepository productoRepository,
            CategoriaRepository categoriaRepository) {

        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {

        // Crear las categorias si no existen
        Categoria cartas = obtenerOCrearCategoria("Cartas individuales");
        Categoria sobres = obtenerOCrearCategoria("Sobres sellados");
        Categoria mazos = obtenerOCrearCategoria("Mazos");
        Categoria accesorios = obtenerOCrearCategoria("Accesorios");
        Categoria cajas = obtenerOCrearCategoria("Cajas");

        // Agregar los productos iniciales que falten
        agregarProducto(
                "TCG001",
                "Ascended Heroes Booster Pack - ME: Ascended Heroes (ASC)",
                "Booster de Pokémon TCG de la expansión Ascended Heroes.",
                7990, 9990, true, "Pokémon",
                8, 2, sobres,
                "/assets/img/sobre-pk.png"
        );

        agregarProducto(
                "TCG002",
                "Donquixote Doflamingo",
                "Carta de One Piece Card Game.",
                7500, 10000, false, "One Piece",
                5, 2, cartas,
                "/assets/img/doffy.png"
        );

        agregarProducto(
                "TCG003",
                "Monkey.D.Luffy (OP05-119)",
                "Special Card OP09.",
                600000, 800000, true, "One Piece",
                10, 3, cartas,
                "/assets/img/Monkey.D.Luffy (OP05-119).jpg"
        );

        agregarProducto(
                "TCG004",
                "Pikachu - 042/128 - ME: 30th Celebration (30C)",
                "Carta Pokémon Pikachu de colección.",
                4990, 4990, false, "Pokémon",
                24, 5, cartas,
                "/assets/img/pikachu.png"
        );

        agregarProducto(
                "TCG005",
                "Starter Deck",
                "Mazo inicial preparado para comenzar a jugar.",
                18990, 21990, true, "One Piece",
                6, 2, mazos,
                "/assets/img/starter-pack.png"
        );

        agregarProducto(
                "TCG006",
                "Protectores de mazos: Magic: The Gathering (24)",
                "Pack de protectores para mantener las cartas en buen estado.",
                6990, 6990, false, "Magic: The Gathering",
                30, 5, accesorios,
                "/assets/img/magic.png"
        );

        agregarProducto(
                "TCG007",
                "One Piece TCG: Booster Display (OP13) CARRYING ON HIS WILL",
                "Caja de boosters One Piece Card Game OP13.",
                499999, 499999, false, "One Piece",
                6, 2, cajas,
                "/assets/img/CajaOP.jpg"
        );

        agregarProducto(
                "TCG008",
                "Sanji The Worlds Strongest Warriors",
                "Carta de colección One Piece.",
                250000, 350000, true, "One Piece",
                3, 1, cartas,
                "/assets/img/SanjiElbaph.jpg"
        );

        agregarProducto(
                "TCG009",
                "Captain America, Steve Rogers (0835)",
                "Carta Marvel Super Heroes Commander.",
                6000000, 6000000, false, "Magic: The Gathering",
                1, 1, cartas,
                "/assets/img/SteveRogers.webp"
        );

        agregarProducto(
                "TCG010",
                "Pokemon TCG - League Battle Deck Mega Lucario ex - Inglés",
                "Mazo de 60 cartas listo para jugar.",
                37990, 42990, false, "Pokémon",
                6, 2, mazos,
                "/assets/img/lucario.webp"
        );

        agregarProducto(
                "TCG011",
                "Jace, the Mind Sculptor",
                "Legendary Planeswalker — Jace de Double Masters.",
                17000, 20500, true, "Magic: The Gathering",
                1, 1, cartas,
                "/assets/img/jace.webp"
        );

        agregarProducto(
                "TCG012",
                "Protectores de mazos: VOL. 1 KAIDO",
                "Pack de protectores para mantener las cartas en buen estado.",
                6000, 10000, false, "One Piece",
                30, 5, accesorios,
                "/assets/img/kaido.webp"
        );

        System.out.println("Catalogo inicial de PokeFoxx verificado.");
    }

    private Categoria obtenerOCrearCategoria(String nombre) {

        return categoriaRepository.findByNameIgnoreCase(nombre)
                .orElseGet(() -> {

                    Categoria categoria = new Categoria();
                    categoria.setName(nombre);
                    categoria.setActive(true);

                    return categoriaRepository.save(categoria);
                });
    }

    private void agregarProducto(
            String code,
            String name,
            String description,
            long price,
            long originalPrice,
            boolean onSale,
            String game,
            int stock,
            int criticalStock,
            Categoria categoria,
            String image) {

        // No duplicar ni sobrescribir productos existentes
        if (productoRepository.existsByCodeIgnoreCase(code)) {
            return;
        }

        Producto producto = new Producto();

        producto.setCode(code);
        producto.setName(name);
        producto.setDescription(description);
        producto.setPrice(BigDecimal.valueOf(price));
        producto.setOriginalPrice(BigDecimal.valueOf(originalPrice));
        producto.setOnSale(onSale);
        producto.setGame(game);
        producto.setStock(stock);
        producto.setCriticalStock(criticalStock);
        producto.setCategoria(categoria);
        producto.setImage(image);

        productoRepository.save(producto);

        System.out.println("Producto inicial agregado: " + code);
    }
}
