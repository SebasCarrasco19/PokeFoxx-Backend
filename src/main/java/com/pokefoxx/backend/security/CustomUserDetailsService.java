
package com.pokefoxx.backend.security;

import com.pokefoxx.backend.model.Usuario;
import com.pokefoxx.backend.repository.UsuarioRepository;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Usuario no encontrado: " + email
                        )
                );

        String rol = usuario.getRole().trim().toUpperCase(Locale.ROOT);

        return User.builder()
                .username(usuario.getEmail())
                .password(usuario.getPassword())
                .roles(rol)
                .build();
    }
}
