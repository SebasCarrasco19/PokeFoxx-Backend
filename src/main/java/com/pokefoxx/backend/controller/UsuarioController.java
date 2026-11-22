
package com.pokefoxx.backend.controller;

import com.pokefoxx.backend.model.Usuario;
import com.pokefoxx.backend.service.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(
    name = "Gestión de Usuarios",
    description = "Administración de usuarios, perfiles personales y roles."
)
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // Consultar perfil del usuario autenticado
    @Operation(
        summary = "Consultar mi perfil",
        description = "Permite a un usuario autenticado consultar su información personal sin mostrar la contraseña."
    )
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
    @Operation(
        summary = "Actualizar mi perfil",
        description = "Permite actualizar los datos personales del usuario autenticado. No permite cambiar el correo desde el perfil."
    )
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

        // No permitir cambiar el correo desde el perfil
        if (datos.getEmail() != null
                && !datos.getEmail()
                        .trim()
                        .equalsIgnoreCase(usuario.getEmail())) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            "No puedes cambiar tu correo desde el perfil"
                    ));
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
    @Operation(
        summary = "Listar todos los usuarios",
        description = "Solo Administrador. Obtiene la lista de usuarios registrados sin mostrar sus contraseñas."
    )
    @GetMapping
    public List<PerfilUsuarioResponse> listarUsuarios() {

        return usuarioService.listarUsuarios()
                .stream()
                .map(this::convertirRespuesta)
                .toList();
    }

    // ADMINISTRADOR: buscar usuario por ID
    @Operation(
        summary = "Buscar usuario por ID",
        description = "Solo Administrador. Consulta la información de un usuario mediante su identificador."
    )
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
    @Operation(
        summary = "Cambiar rol de usuario",
        description = "Solo Administrador. Permite cambiar el rol de otro usuario. Un administrador no puede cambiar su propio rol."
    )
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
    @Operation(
        summary = "Actualizar usuario",
        description = "Solo Administrador. Permite modificar la información de un usuario registrado."
    )
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
    @Operation(
        summary = "Eliminar usuario",
        description = "Solo Administrador. Elimina un usuario si no tiene compras asociadas. Un administrador no puede eliminarse a sí mismo."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarUsuario(
            @PathVariable Long id,
            Authentication authentication) {

        Usuario administrador = usuarioService
                .buscarPorEmail(authentication.getName())
                .orElse(null);

        if (administrador == null) {
            return ResponseEntity.notFound().build();
        }

        // Evitar que el administrador se elimine a si mismo
        if (administrador.getId().equals(id)) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            "No puedes eliminar tu propio usuario"
                    ));
        }

        if (usuarioService.buscarUsuarioPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            usuarioService.eliminarUsuario(id);
            return ResponseEntity.noContent().build();

        } catch (IllegalArgumentException e) {

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));

        } catch (org.springframework.dao.DataIntegrityViolationException e) {

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "error",
                            "No puedes eliminar un usuario con datos asociados"
                    ));
        }
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
