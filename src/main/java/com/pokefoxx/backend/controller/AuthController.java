
package com.pokefoxx.backend.controller;

import com.pokefoxx.backend.model.Usuario;
import com.pokefoxx.backend.security.JwtService;
import com.pokefoxx.backend.service.UsuarioService;

import io.jsonwebtoken.JwtException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(
    name = "Autenticación y Registro",
    description = "Registro de usuarios, inicio de sesión y renovación de tokens JWT."
)
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioService usuarioService;
    private final UserDetailsService userDetailsService;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UsuarioService usuarioService,
            UserDetailsService userDetailsService) {

        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.usuarioService = usuarioService;
        this.userDetailsService = userDetailsService;
    }

    // Iniciar sesion
    @Operation(
        summary = "Iniciar sesión",
        description = "Acceso público. Valida el correo y la contraseña del usuario. Si las credenciales son correctas, devuelve un token JWT de acceso y un refresh token."
    )
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody Map<String, String> loginRequest) {

        String email = loginRequest.get("email");
        String password = loginRequest.get("password");

        if (email == null || email.isBlank()
                || password == null || password.isBlank()) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            "Debes ingresar correo y contraseña"
                    ));
        }

        email = email.trim().toLowerCase(Locale.ROOT);

        try {

            Authentication authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    email,
                                    password
                            )
                    );

            UserDetails userDetails =
                    (UserDetails) authentication.getPrincipal();

            String token =
                    jwtService.generarToken(userDetails);

            String refreshToken =
                    jwtService.generarRefreshToken(userDetails);

            return ResponseEntity.ok(
                    Map.of(
                            "token", token,
                            "refreshToken", refreshToken
                    )
            );

        } catch (AuthenticationException e) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "error",
                            "Correo o contraseña incorrectos"
                    ));
        }
    }

    // Renovar token JWT
    @Operation(
        summary = "Renovar token JWT",
        description = "Acceso público. Recibe un refresh token válido y devuelve un nuevo token de acceso sin necesidad de iniciar sesión nuevamente."
    )
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @RequestBody RefreshRequest request) {

        if (request == null
                || request.refreshToken() == null
                || request.refreshToken().isBlank()) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            "Debes enviar el refresh token"
                    ));
        }

        try {

            String email =
                    jwtService.extraerEmail(
                            request.refreshToken()
                    );

            UserDetails userDetails =
                    userDetailsService.loadUserByUsername(email);

            if (!jwtService.validarRefreshToken(
                    request.refreshToken(),
                    userDetails)) {

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "error",
                                "Refresh token no valido"
                        ));
            }

            String nuevoToken =
                    jwtService.generarToken(userDetails);

            return ResponseEntity.ok(
                    Map.of(
                            "token",
                            nuevoToken
                    )
            );

        } catch (JwtException | IllegalArgumentException e) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "error",
                            "Refresh token invalido o expirado"
                    ));
        }
    }

    // Registrar usuario
    @Operation(
        summary = "Registrar nuevo usuario",
        description = "Acceso público. Registra un nuevo usuario con rol Cliente. Valida los datos obligatorios y devuelve la información del usuario registrado sin mostrar su contraseña."
    )
    @PostMapping("/register")
    public ResponseEntity<?> registrar(
            @RequestBody RegistroRequest request) {

        if (request == null ||
                request.run() == null ||
                request.firstName() == null ||
                request.lastName() == null ||
                request.email() == null ||
                request.password() == null) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            "Debes completar los campos obligatorios"
                    ));
        }

        try {

            Usuario usuario = new Usuario();

            usuario.setRun(request.run());
            usuario.setFirstName(request.firstName());
            usuario.setLastName(request.lastName());
            usuario.setEmail(request.email());
            usuario.setPassword(request.password());

            usuario.setBirthDate(request.birthDate());
            usuario.setRegion(request.region());
            usuario.setCommune(request.commune());
            usuario.setAddress(request.address());

            Usuario registrado =
                    usuarioService.registrarUsuario(usuario);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "mensaje", "Usuario registrado correctamente",
                            "id", registrado.getId(),
                            "run", registrado.getRun(),
                            "firstName", registrado.getFirstName(),
                            "lastName", registrado.getLastName(),
                            "email", registrado.getEmail(),
                            "role", registrado.getRole()
                    ));

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            e.getMessage()
                    ));
        }
    }

    // Datos que recibe el refresh
    public record RefreshRequest(
            String refreshToken
    ) {
    }

    // Datos que recibe el registro
    public record RegistroRequest(
            String run,
            String firstName,
            String lastName,
            String email,
            LocalDate birthDate,
            String region,
            String commune,
            String address,
            String password
    ) {
    }
}
