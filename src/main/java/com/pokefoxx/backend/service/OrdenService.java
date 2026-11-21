
package com.pokefoxx.backend.service;

import com.pokefoxx.backend.dto.CompraRequest;
import com.pokefoxx.backend.dto.DetalleCompraRequest;
import com.pokefoxx.backend.model.DetalleOrden;
import com.pokefoxx.backend.model.Orden;
import com.pokefoxx.backend.model.Producto;
import com.pokefoxx.backend.model.Usuario;
import com.pokefoxx.backend.repository.DetalleOrdenRepository;
import com.pokefoxx.backend.repository.OrdenRepository;
import com.pokefoxx.backend.repository.ProductoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@Service
public class OrdenService {

    private final OrdenRepository ordenRepository;
    private final DetalleOrdenRepository detalleOrdenRepository;
    private final ProductoRepository productoRepository;

    public OrdenService(
            OrdenRepository ordenRepository,
            DetalleOrdenRepository detalleOrdenRepository,
            ProductoRepository productoRepository) {

        this.ordenRepository = ordenRepository;
        this.detalleOrdenRepository = detalleOrdenRepository;
        this.productoRepository = productoRepository;
    }

    // Listar todas las órdenes
    public List<Orden> listarOrdenes() {
        return ordenRepository.findAll();
    }

    // Buscar una orden por ID
    public Optional<Orden> buscarOrdenPorId(Long id) {
        return ordenRepository.findById(id);
    }

    // Validar stock y calcular subtotal
    public BigDecimal calcularSubtotal(
            Producto producto, int cantidad) {

        if (producto == null) {
            throw new IllegalArgumentException(
                    "El producto no existe");
        }

        if (cantidad <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad debe ser mayor a cero");
        }

        if (producto.getStock() == null ||
                producto.getStock() < cantidad) {
            throw new IllegalArgumentException(
                    "Stock insuficiente");
        }

        if (producto.getPrice() == null ||
                producto.getPrice().signum() < 0) {
            throw new IllegalArgumentException(
                    "Precio inválido");
        }

        return producto.getPrice().multiply(
                BigDecimal.valueOf(cantidad));
    }

    // Validar y agrupar las cantidades del carrito
    private Map<Long, Integer> obtenerCantidades(
            CompraRequest compra) {

        if (compra == null ||
                compra.getProductos() == null ||
                compra.getProductos().isEmpty()) {

            throw new IllegalArgumentException(
                    "El carrito está vacío");
        }

        Map<Long, Integer> cantidades = new TreeMap<>();

        for (DetalleCompraRequest detalle :
                compra.getProductos()) {

            if (detalle == null ||
                    detalle.getProductId() == null ||
                    detalle.getQty() == null ||
                    detalle.getQty() <= 0) {

                throw new IllegalArgumentException(
                        "Detalle de compra inválido");
            }

            try {
                cantidades.merge(
                        detalle.getProductId(),
                        detalle.getQty(),
                        Math::addExact);
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException(
                        "Cantidad demasiado grande");
            }
        }

        return cantidades;
    }

    // Calcular total sin registrar una compra
    public BigDecimal calcularTotalCompra(
            CompraRequest compra) {

        BigDecimal total = BigDecimal.ZERO;

        Map<Long, Integer> cantidades =
                obtenerCantidades(compra);

        for (Map.Entry<Long, Integer> item :
                cantidades.entrySet()) {

            Producto producto = productoRepository
                    .findById(item.getKey())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Producto no encontrado: "
                                    + item.getKey()));

            total = total.add(
                    calcularSubtotal(
                            producto, item.getValue()));
        }

        return total;
    }

    // Registrar compra y proteger el stock
    @Transactional
    public Orden registrarCompra(
            CompraRequest compra, Usuario usuario) {

        if (usuario == null || usuario.getId() == null) {
            throw new IllegalArgumentException(
                    "Usuario inválido");
        }

        Map<Long, Integer> cantidades =
                obtenerCantidades(compra);

        List<DetalleOrden> detalles = new ArrayList<>();

        BigDecimal total = BigDecimal.ZERO;

        // Buscar productos con bloqueo de escritura
        for (Map.Entry<Long, Integer> item :
                cantidades.entrySet()) {

            Producto producto = productoRepository
                    .buscarParaCompra(item.getKey())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Producto no encontrado: "
                                    + item.getKey()));

            int cantidad = item.getValue();

            // Comprobar stock con el producto bloqueado
            BigDecimal subtotal =
                    calcularSubtotal(producto, cantidad);

            DetalleOrden detalle = new DetalleOrden();

            detalle.setProducto(producto);
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitario(producto.getPrice());
            detalle.setSubtotal(subtotal);

            detalles.add(detalle);
            total = total.add(subtotal);
        }

        // Crear la orden
        Orden orden = new Orden();

        orden.setUsuario(usuario);
        orden.setFecha(LocalDateTime.now());
        orden.setEstado("PENDIENTE");
        orden.setTotal(total);

        orden = ordenRepository.save(orden);

        // Guardar detalles y descontar stock
        for (DetalleOrden detalle : detalles) {

            detalle.setOrden(orden);
            detalleOrdenRepository.save(detalle);

            Producto producto = detalle.getProducto();

            int nuevoStock =
                    producto.getStock() -
                    detalle.getCantidad();

            producto.setStock(nuevoStock);

            productoRepository.save(producto);
        }

        // Los cambios se confirman al terminar la transacción
        return orden;
    }
}
