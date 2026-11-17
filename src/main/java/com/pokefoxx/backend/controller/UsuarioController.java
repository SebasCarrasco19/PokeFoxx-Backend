package com.pokefoxx.backend.controller;

import com.pokefoxx.backend.model.Usuario;
import com.pokefoxx.backend.service.UsuarioService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // Consultar perfil del usuario autenticado
    @GetMapping("/me")
    public ResponseEntity<?> obtenerMiPerfil(
            Authentication authentication) {

        Usuario usuario = usuarioService
                .buscarPorEmail(authentication.getName())
                .orElse(null);

        if (usuario == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                convertirRespuesta(usuario)
        );
    }

    // Actualizar perfil del usuario autenticado
    @PutMapping("/me")
    public ResponseEntity<?> actualizarMiPerfil(
            Authentication authentication,
            @RequestBody Usuario datos) {

        Usuario usuario = usuarioService
                .buscarPorEmail(authentication.getName())
                .orElse(null);

        if (usuario == null) {
            return ResponseEntity.notFound().build();
        }

        try {

            Usuario actualizado =
                    usuarioService.actualizarUsuario(
                            usuario.getId(),
                            datos
                    );

            return ResponseEntity.ok(
                    convertirRespuesta(actualizado)
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "error",
                            e.getMessage()
                    ));
        }
    }

    // ADMINISTRADOR: listar todos los usuarios
    @GetMapping
    public List<PerfilUsuarioResponse> listarUsuarios() {

        return usuarioService.listarUsuarios()
                .stream()
                .map(this::convertirRespuesta)
                .toList();
    }

    // ADMINISTRADOR: buscar usuario por ID
    @GetMapping("/{id}")
    public ResponseEntity<?> buscarUsuario(
            @PathVariable Long id) {

        return usuarioService.buscarUsuarioPorId(id)
                .map(usuario ->
                        ResponseEntity.ok(
                                convertirRespuesta(usuario)
                        ))
                .orElse(
                        ResponseEntity.notFound().build()
                );
    }

    // ADMINISTRADOR: cambiar rol de un usuario
    @PutMapping("/{id}/rol")
    public ResponseEntity<?> cambiarRol(
            @PathVariable Long id,
            @RequestBody CambiarRolRequest request,
            Authentication authentication) {

        Usuario administrador = usuarioService
                .buscarPorEmail(authentication.getName())
                .orElse(null);

        if (administrador == null) {
            return ResponseEntity.notFound().build();
        }

        // Evitar que el administrador cambie su propio rol
        if (administrador.getId().equals(id)) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            "No puedes cambiar tu propio rol"
                    ));
        }

        try {

            Usuario actualizado =
                    usuarioService.cambiarRol(
                            id,
                            request.role()
                    );

            return ResponseEntity.ok(
                    convertirRespuesta(actualizado)
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            e.getMessage()
                    ));

        } catch (RuntimeException e) {

            return ResponseEntity.notFound().build();
        }
    }

    // ADMINISTRADOR: actualizar usuario
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarUsuario(
            @PathVariable Long id,
            @RequestBody Usuario datos) {

        if (usuarioService.buscarUsuarioPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {

            Usuario actualizado =
                    usuarioService.actualizarUsuario(
                            id,
                            datos
                    );

            return ResponseEntity.ok(
                    convertirRespuesta(actualizado)
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "error",
                            e.getMessage()
                    ));
        }
    }

    // ADMINISTRADOR: eliminar usuario
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarUsuario(
            @PathVariable Long id) {

        if (usuarioService.buscarUsuarioPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        usuarioService.eliminarUsuario(id);

        return ResponseEntity.noContent().build();
    }

    // Convertir usuario a respuesta sin contraseña
    private PerfilUsuarioResponse convertirRespuesta(
            Usuario usuario) {

        return new PerfilUsuarioResponse(
                usuario.getId(),
                usuario.getRun(),
                usuario.getFirstName(),
                usuario.getLastName(),
                usuario.getEmail(),
                usuario.getBirthDate(),
                usuario.getRole(),
                usuario.getRegion(),
                usuario.getCommune(),
                usuario.getAddress()
        );
    }

    // DTO para cambiar rol
    public record CambiarRolRequest(
            String role
    ) {
    }

    // DTO de respuesta sin contraseña
    public record PerfilUsuarioResponse(
            Long id,
            String run,
            String firstName,
            String lastName,
            String email,
            LocalDate birthDate,
            String role,
            String region,
            String commune,
            String address
    ) {
    }
}