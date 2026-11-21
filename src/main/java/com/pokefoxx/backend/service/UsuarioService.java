package com.pokefoxx.backend.service;

import com.pokefoxx.backend.model.Usuario;
import com.pokefoxx.backend.repository.UsuarioRepository;
import com.pokefoxx.backend.repository.OrdenRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrdenRepository ordenRepository;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            OrdenRepository ordenRepository) {

        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.ordenRepository = ordenRepository;
    }

    // Listar todos los usuarios
    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    // Buscar usuario por ID
    public Optional<Usuario> buscarUsuarioPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    // Buscar usuario por correo
    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    // Registrar usuario
    public Usuario registrarUsuario(Usuario usuario) {

        if (usuario == null ||
                usuario.getRun() == null ||
                usuario.getFirstName() == null ||
                usuario.getLastName() == null ||
                usuario.getEmail() == null ||
                usuario.getPassword() == null ||
                usuario.getRun().isBlank() ||
                usuario.getFirstName().isBlank() ||
                usuario.getLastName().isBlank() ||
                usuario.getEmail().isBlank() ||
                usuario.getPassword().isBlank()) {

            throw new IllegalArgumentException(
                    "Debes completar todos los campos obligatorios"
            );
        }

        String email = usuario.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        String run = usuario.getRun().trim();

        if (usuarioRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "El correo ya está registrado"
            );
        }

        if (usuarioRepository.existsByRun(run)) {
            throw new IllegalArgumentException(
                    "El RUN ya está registrado"
            );
        }

        usuario.setId(null);
        usuario.setEmail(email);
        usuario.setRun(run);

        usuario.setFirstName(usuario.getFirstName().trim());
        usuario.setLastName(usuario.getLastName().trim());

        // Encriptar contraseña
        usuario.setPassword(
                passwordEncoder.encode(usuario.getPassword())
        );

        // Todo registro público comienza como Cliente
        usuario.setRole("Cliente");

        return usuarioRepository.save(usuario);
    }

    // Actualizar usuario
    public Usuario actualizarUsuario(Long id, Usuario datos) {

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Usuario no encontrado"));

        if (datos.getEmail() != null) {

            String email = datos.getEmail()
                    .trim()
                    .toLowerCase(Locale.ROOT);

            if (email.isBlank()) {
                throw new IllegalArgumentException(
                        "El correo no puede estar vacío"
                );
            }

            if (!email.equals(usuario.getEmail()) &&
                    usuarioRepository.existsByEmail(email)) {

                throw new IllegalArgumentException(
                        "El correo ya está registrado"
                );
            }

            usuario.setEmail(email);
        }

        if (datos.getRun() != null) {

            String run = datos.getRun().trim();

            if (run.isBlank()) {
                throw new IllegalArgumentException(
                        "El RUN no puede estar vacío"
                );
            }

            if (!run.equals(usuario.getRun()) &&
                    usuarioRepository.existsByRun(run)) {

                throw new IllegalArgumentException(
                        "El RUN ya está registrado"
                );
            }

            usuario.setRun(run);
        }

        if (datos.getFirstName() != null) {
            usuario.setFirstName(datos.getFirstName());
        }

        if (datos.getLastName() != null) {
            usuario.setLastName(datos.getLastName());
        }

        if (datos.getBirthDate() != null) {
            usuario.setBirthDate(datos.getBirthDate());
        }

        if (datos.getRegion() != null) {
            usuario.setRegion(datos.getRegion());
        }

        if (datos.getCommune() != null) {
            usuario.setCommune(datos.getCommune());
        }

        if (datos.getAddress() != null) {
            usuario.setAddress(datos.getAddress());
        }

        return usuarioRepository.save(usuario);
    }

    // Cambiar rol de usuario
    public Usuario cambiarRol(Long id, String nuevoRol) {

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Usuario no encontrado"));

        if (nuevoRol == null || nuevoRol.isBlank()) {
            throw new IllegalArgumentException(
                    "El rol es obligatorio"
            );
        }

        String rolNormalizado;

        switch (nuevoRol.trim().toLowerCase(Locale.ROOT)) {

            case "cliente":
                rolNormalizado = "Cliente";
                break;

            case "vendedor":
                rolNormalizado = "Vendedor";
                break;

            case "administrador":
                rolNormalizado = "Administrador";
                break;

            default:
                throw new IllegalArgumentException(
                        "Rol no válido"
                );
        }

        usuario.setRole(rolNormalizado);

        return usuarioRepository.save(usuario);
    }

    // Eliminar usuario
    public void eliminarUsuario(Long id) {

        if (!usuarioRepository.existsById(id)) {
            throw new RuntimeException(
                    "Usuario no encontrado"
            );
        }

        if (ordenRepository.existsByUsuario_Id(id)) {
            throw new IllegalArgumentException("No puedes eliminar un usuario con ordenes asociadas");
        }
        usuarioRepository.deleteById(id);
    }
}
