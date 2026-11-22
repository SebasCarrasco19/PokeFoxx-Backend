
package com.pokefoxx.backend.controller;

import com.pokefoxx.backend.dto.CompraRequest;
import com.pokefoxx.backend.model.DetalleOrden;
import com.pokefoxx.backend.model.Orden;
import com.pokefoxx.backend.model.Usuario;
import com.pokefoxx.backend.repository.DetalleOrdenRepository;
import com.pokefoxx.backend.repository.UsuarioRepository;
import com.pokefoxx.backend.service.OrdenService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/ordenes")
@Tag(
    name = "Órdenes y Compras",
    description = "Registro de compras y consulta de órdenes según los permisos de cada usuario."
)
@SecurityRequirement(name = "bearerAuth")
public class OrdenController {

    private final OrdenService ordenService;
    private final UsuarioRepository usuarioRepository;
    private final DetalleOrdenRepository detalleOrdenRepository;

    public OrdenController(
            OrdenService ordenService,
            UsuarioRepository usuarioRepository,
            DetalleOrdenRepository detalleOrdenRepository) {

        this.ordenService = ordenService;
        this.usuarioRepository = usuarioRepository;
        this.detalleOrdenRepository = detalleOrdenRepository;
    }

    // Buscar usuario autenticado
    private Optional<Usuario> obtenerUsuario(Authentication auth) {

        if (auth == null) {
            return Optional.empty();
        }

        return usuarioRepository.findByEmail(auth.getName());
    }

    // Comprobar permisos
    private boolean tieneRol(Authentication auth, String rol) {

        return auth != null && auth.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals(rol));
    }

    // Preparar respuesta sin exponer contraseña del usuario
    private Map<String, Object> respuestaOrden(Orden orden) {

        Map<String, Object> respuesta = new LinkedHashMap<>();

        respuesta.put("id", orden.getId());
        respuesta.put("usuarioId", orden.getUsuario().getId());
        respuesta.put("fecha", orden.getFecha());
        respuesta.put("estado", orden.getEstado());
        respuesta.put("total", orden.getTotal());

        return respuesta;
    }

    // Crear compra
    @Operation(
        summary = "Registrar una compra",
        description = "Solo Cliente. Registra una compra utilizando los productos y cantidades enviados. El backend valida el stock, calcula el total y descuenta las unidades compradas."
    )
    @PostMapping
    public ResponseEntity<?> registrarCompra(
            @RequestBody CompraRequest compra,
            Authentication auth) {

        if (!tieneRol(auth, "ROLE_CLIENTE")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Acceso denegado"));
        }

        Optional<Usuario> usuario = obtenerUsuario(auth);

        if (usuario.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no encontrado"));
        }

        try {
            Orden orden = ordenService.registrarCompra(
                    compra, usuario.get());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(respuestaOrden(orden));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Listar órdenes según el rol
    @Operation(
        summary = "Listar órdenes",
        description = "Cliente: consulta únicamente sus propias compras. Vendedor y Administrador: pueden consultar todas las órdenes registradas."
    )
    @GetMapping
    public ResponseEntity<?> listarOrdenes(Authentication auth) {

        Optional<Usuario> usuario = obtenerUsuario(auth);

        if (usuario.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no encontrado"));
        }

        List<Orden> ordenes = ordenService.listarOrdenes();

        boolean esAdministrador =
                tieneRol(auth, "ROLE_ADMINISTRADOR");

        boolean esVendedor =
                tieneRol(auth, "ROLE_VENDEDOR");

        boolean esCliente =
                tieneRol(auth, "ROLE_CLIENTE");

        if (!esAdministrador && !esVendedor && !esCliente) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Acceso denegado"));
        }

        List<Map<String, Object>> resultado = new ArrayList<>();

        for (Orden orden : ordenes) {

            // Los clientes solo pueden ver sus propias órdenes
            if (esCliente && !esAdministrador && !esVendedor &&
                    !orden.getUsuario().getId()
                            .equals(usuario.get().getId())) {
                continue;
            }

            resultado.add(respuestaOrden(orden));
        }

        return ResponseEntity.ok(resultado);
    }

    // Consultar una orden y sus productos
    @Operation(
        summary = "Consultar detalle de una orden",
        description = "Obtiene una orden y sus productos, cantidades, precios y subtotales. El Cliente solo puede consultar sus propias órdenes. Vendedor y Administrador pueden consultar cualquier orden."
    )
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerOrden(
            @PathVariable Long id,
            Authentication auth) {

        Optional<Usuario> usuario = obtenerUsuario(auth);

        if (usuario.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no encontrado"));
        }

        boolean esAdministrador =
                tieneRol(auth, "ROLE_ADMINISTRADOR");

        boolean esVendedor =
                tieneRol(auth, "ROLE_VENDEDOR");

        boolean esCliente =
                tieneRol(auth, "ROLE_CLIENTE");

        if (!esAdministrador && !esVendedor && !esCliente) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Acceso denegado"));
        }

        Optional<Orden> ordenBuscada =
                ordenService.buscarOrdenPorId(id);

        if (ordenBuscada.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Orden no encontrada"));
        }

        Orden orden = ordenBuscada.get();

        // Impedir que un cliente consulte compras ajenas
        if (!esAdministrador && !esVendedor &&
                !orden.getUsuario().getId()
                        .equals(usuario.get().getId())) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Acceso denegado"));
        }

        List<DetalleOrden> detalles =
                detalleOrdenRepository.findByOrden_Id(id);

        List<Map<String, Object>> productos = new ArrayList<>();

        for (DetalleOrden detalle : detalles) {

            Map<String, Object> item = new LinkedHashMap<>();

            item.put("productoId", detalle.getProducto().getId());
            item.put("nombre", detalle.getProducto().getName());
            item.put("cantidad", detalle.getCantidad());
            item.put("precioUnitario", detalle.getPrecioUnitario());
            item.put("subtotal", detalle.getSubtotal());

            productos.add(item);
        }

        Map<String, Object> respuesta = respuestaOrden(orden);
        respuesta.put("productos", productos);

        return ResponseEntity.ok(respuesta);
    }
}
