
package com.pokefoxx.backend.controller;

import com.pokefoxx.backend.model.Usuario;
import com.pokefoxx.backend.security.JwtService;
import com.pokefoxx.backend.service.UsuarioService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioService usuarioService;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UsuarioService usuarioService) {

        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.usuarioService = usuarioService;
    }

    // Iniciar sesión
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody Map<String, String> loginRequest) {

        String email = loginRequest.get("email");
        String password = loginRequest.get("password");

        if (email == null || email.isBlank()
                || password == null || password.isBlank()) {

            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "Debes ingresar correo y contraseña"));
        }

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

            String token = jwtService.generarToken(userDetails);

            return ResponseEntity.ok(
                    Map.of("token", token)
            );

        } catch (AuthenticationException e) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error",
                            "Correo o contraseña incorrectos"));
        }
    }

    // Registrar usuario
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
                    .body(Map.of("error",
                            "Debes completar los campos obligatorios"));
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
                    .body(Map.of("error", e.getMessage()));
        }
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
    ) {}
}
