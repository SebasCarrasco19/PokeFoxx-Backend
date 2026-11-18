
package com.pokefoxx.backend.controller;

import com.pokefoxx.backend.dto.CompraRequest;
import com.pokefoxx.backend.model.Orden;
import com.pokefoxx.backend.model.Usuario;
import com.pokefoxx.backend.repository.UsuarioRepository;
import com.pokefoxx.backend.service.OrdenService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ordenes")
public class OrdenController {

    private final OrdenService ordenService;
    private final UsuarioRepository usuarioRepository;

    public OrdenController(
            OrdenService ordenService,
            UsuarioRepository usuarioRepository) {

        this.ordenService = ordenService;
        this.usuarioRepository = usuarioRepository;
    }

    // Registrar una compra del cliente autenticado
    @PostMapping
    public ResponseEntity<?> registrarCompra(
            @RequestBody CompraRequest compra,
            Authentication authentication) {

        // Comprobar que sea un cliente autenticado
        if (authentication == null ||
                authentication.getAuthorities().stream()
                    .noneMatch(autoridad ->
                        autoridad.getAuthority()
                            .equals("ROLE_CLIENTE"))) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "error", "No tienes permiso para comprar"));
        }

        // Obtener correo desde la autenticación JWT
        String email = authentication.getName();

        // Buscar el usuario real en MySQL
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElse(null);

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "error", "Usuario no encontrado"));
        }

        try {
            // Registrar la compra
            Orden orden = ordenService.registrarCompra(
                    compra, usuario);

            // Devolver solamente los datos de la orden
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "id", orden.getId(),
                            "fecha", orden.getFecha(),
                            "estado", orden.getEstado(),
                            "total", orden.getTotal()));

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
