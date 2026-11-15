
package com.pokefoxx.backend.service;

import com.pokefoxx.backend.model.Usuario;
import com.pokefoxx.backend.repository.UsuarioRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder) {

        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
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

        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            throw new IllegalArgumentException("El correo ya está registrado");
        }

        if (usuarioRepository.existsByRun(usuario.getRun())) {
            throw new IllegalArgumentException("El RUN ya está registrado");
        }

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

        if (datos.getEmail() != null &&
                !datos.getEmail().equals(usuario.getEmail()) &&
                usuarioRepository.existsByEmail(datos.getEmail())) {
            throw new IllegalArgumentException("El correo ya está registrado");
        }

        if (datos.getRun() != null &&
                !datos.getRun().equals(usuario.getRun()) &&
                usuarioRepository.existsByRun(datos.getRun())) {
            throw new IllegalArgumentException("El RUN ya está registrado");
        }

        if (datos.getRun() != null) usuario.setRun(datos.getRun());
        if (datos.getFirstName() != null) usuario.setFirstName(datos.getFirstName());
        if (datos.getLastName() != null) usuario.setLastName(datos.getLastName());
        if (datos.getEmail() != null) usuario.setEmail(datos.getEmail());
        if (datos.getBirthDate() != null) usuario.setBirthDate(datos.getBirthDate());
        if (datos.getRegion() != null) usuario.setRegion(datos.getRegion());
        if (datos.getCommune() != null) usuario.setCommune(datos.getCommune());
        if (datos.getAddress() != null) usuario.setAddress(datos.getAddress());

        return usuarioRepository.save(usuario);
    }

    // Eliminar usuario
    public void eliminarUsuario(Long id) {

        if (!usuarioRepository.existsById(id)) {
            throw new RuntimeException("Usuario no encontrado");
        }

        usuarioRepository.deleteById(id);
    }
}
