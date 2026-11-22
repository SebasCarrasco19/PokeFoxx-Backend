
package com.pokefoxx.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokefoxx.backend.model.Usuario;
import com.pokefoxx.backend.repository.UsuarioRepository;
import com.pokefoxx.backend.security.CustomUserDetailsService;
import com.pokefoxx.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class IntegracionSeguridadCatalogoTests {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios;
    @Autowired CustomUserDetailsService userDetails;
    @Autowired JwtService jwt;

    @Test
    void consultasPublicasYRecursosProtegidos() throws Exception {
        for (String ruta : new String[]{
                "/api/v1/productos",
                "/api/v1/productos/ofertas",
                "/api/v1/categorias"
        }) {
            mvc.perform(get(ruta)).andExpect(status().isOk());
        }

        for (String ruta : new String[]{
                "/api/v1/productos/stock-critico",
                "/api/v1/usuarios",
                "/api/v1/usuarios/me"
        }) {
            mvc.perform(get(ruta)).andExpect(status().isForbidden());
        }

        mvc.perform(post("/api/v1/categorias")
                .contentType("application/json")
                .content("{\"name\":\"Cartas\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void clienteNoPuedeAdministrarCatalogoNiUsuarios() throws Exception {
        verificarRestricciones();
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    void vendedorNoPuedeAdministrarCatalogoNiUsuarios() throws Exception {
        verificarRestricciones();
    }

    private void verificarRestricciones() throws Exception {
        mvc.perform(get("/api/v1/productos/stock-critico"))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/v1/usuarios"))
                .andExpect(status().isForbidden());

        for (String ruta : new String[]{
                "/api/v1/productos",
                "/api/v1/categorias"
        }) {
            mvc.perform(post(ruta)
                    .contentType("application/json")
                    .content("{}"))
                    .andExpect(status().isForbidden());

            mvc.perform(put(ruta + "/1")
                    .contentType("application/json")
                    .content("{}"))
                    .andExpect(status().isForbidden());

            mvc.perform(delete(ruta + "/1"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void administradorGestionaCatalogoConValidacionesYRelacion() throws Exception {
        String categoria = mvc.perform(post("/api/v1/categorias")
                .contentType("application/json")
                .content("{\"name\":\"Cartas\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long categoriaId = json.readTree(categoria).get("id").asLong();

        mvc.perform(post("/api/v1/categorias")
                .contentType("application/json")
                .content("{\"name\":\"cartas\"}"))
                .andExpect(status().isBadRequest());

        String payload = """
                {"code":"PK-1","name":"Pikachu","price":100,"originalPrice":200,
                 "onSale":true,"stock":2,"criticalStock":3,"categoria":{"id":%d}}
                """.formatted(categoriaId);

        String producto = mvc.perform(post("/api/v1/productos")
                .contentType("application/json")
                .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long productoId = json.readTree(producto).get("id").asLong();

        mvc.perform(post("/api/v1/productos")
                .contentType("application/json")
                .content(payload))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/v1/productos/categoria/" + categoriaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("PK-1"));

        mvc.perform(get("/api/v1/productos/ofertas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("PK-1"));

        mvc.perform(get("/api/v1/productos/stock-critico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("PK-1"));

        mvc.perform(delete("/api/v1/categorias/" + categoriaId))
                .andExpect(status().isBadRequest());

        mvc.perform(delete("/api/v1/productos/" + productoId))
                .andExpect(status().isNoContent());

        mvc.perform(delete("/api/v1/categorias/" + categoriaId))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/usuarios"))
                .andExpect(status().isOk());
    }

    @Test
    void jwtAutenticaPerfilPeroRefreshNoAutorizaAcceso() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setRun("11111111-1");
        usuario.setFirstName("Prueba");
        usuario.setLastName("Integracion");
        usuario.setEmail("integracion@example.test");
        usuario.setRole("Cliente");
        usuario.setPassword("solo-prueba");

        usuarios.saveAndFlush(usuario);

        var details = userDetails.loadUserByUsername(usuario.getEmail());

        mvc.perform(get("/api/v1/usuarios/me")
                .header("Authorization", "Bearer " + jwt.generarToken(details)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(usuario.getEmail()))
                .andExpect(jsonPath("$.password").doesNotExist());

        mvc.perform(get("/api/v1/usuarios/me")
                .header("Authorization", "Bearer " + jwt.generarRefreshToken(details)))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/v1/usuarios/me")
                .header("Authorization", "Bearer invalido"))
                .andExpect(status().isForbidden());
    }
}
