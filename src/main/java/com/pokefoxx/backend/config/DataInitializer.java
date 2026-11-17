
package com.pokefoxx.backend.config;

import com.pokefoxx.backend.model.Usuario;
import com.pokefoxx.backend.repository.UsuarioRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.admin.run:11111111-1}")
    private String adminRun;

    public DataInitializer(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder) {

        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {

        // Comprobar que existan las credenciales
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            System.out.println("Credenciales del administrador no configuradas.");
            return;
        }

        // Comprobar si el administrador ya existe
        if (usuarioRepository.existsByEmail(adminEmail)) {
            System.out.println("El administrador ya existe.");
            return;
        }

        // Evitar duplicar el RUN
        if (usuarioRepository.existsByRun(adminRun)) {
            System.out.println("El RUN de prueba ya esta registrado.");
            return;
        }

        // Crear administrador de prueba
        Usuario admin = new Usuario();

        admin.setRun(adminRun);
        admin.setFirstName("Administrador");
        admin.setLastName("PokeFoxx");
        admin.setEmail(adminEmail);
        admin.setRole("Administrador");

        // Encriptar contrasena con BCrypt
        admin.setPassword(
                passwordEncoder.encode(adminPassword)
        );

        usuarioRepository.save(admin);

        System.out.println("======================================");
        System.out.println("ADMINISTRADOR CREADO EXITOSAMENTE");
        System.out.println("Correo: " + adminEmail);
        System.out.println("======================================");
    }
}
