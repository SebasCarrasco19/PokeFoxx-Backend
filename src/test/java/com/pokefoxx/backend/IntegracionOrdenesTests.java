package com.pokefoxx.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokefoxx.backend.dto.CompraRequest;
import com.pokefoxx.backend.model.*;
import com.pokefoxx.backend.repository.*;
import com.pokefoxx.backend.security.*;
import com.pokefoxx.backend.service.OrdenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:ordenes;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IntegracionOrdenesTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios;
    @Autowired ProductoRepository productos;
    @Autowired OrdenRepository ordenes;
    @Autowired DetalleOrdenRepository detalles;
    @Autowired OrdenService service;
    @Autowired JwtService jwt;
    @Autowired CustomUserDetailsService userDetails;
    Usuario cliente, otro, vendedor, admin;
    Producto producto;

    @BeforeEach
    void preparar() {
        detalles.deleteAll();
        ordenes.deleteAll();
        productos.deleteAll();
        usuarios.deleteAll();
        cliente = usuario("1", "Cliente");
        otro = usuario("2", "Cliente");
        vendedor = usuario("3", "Vendedor");
        admin = usuario("4", "Administrador");
        producto = producto("PK-1", 5);
    }

    private Usuario usuario(String id, String rol) {
        Usuario u = new Usuario();
        u.setRun("11111111-" + id);
        u.setFirstName("Prueba"); u.setLastName("Ordenes");
        u.setEmail("usuario" + id + "@example.test");
        u.setRole(rol); u.setPassword("solo-prueba");
        return usuarios.saveAndFlush(u);
    }

    private Producto producto(String codigo, int stock) {
        Producto p = new Producto();
        p.setCode(codigo); p.setName(codigo);
        p.setPrice(new BigDecimal("100.00"));
        p.setOriginalPrice(new BigDecimal("200.00")); p.setOnSale(true);
        p.setStock(stock); p.setCriticalStock(2);
        return productos.saveAndFlush(p);
    }

    private String token(Usuario usuario) {
        return "Bearer " + jwt.generarToken(userDetails.loadUserByUsername(usuario.getEmail()));
    }
    private String carrito(long id, int qty) {
        return "{\"productos\":[{\"productId\":" + id + ",\"qty\":" + qty + "}]}";
    }
    private long comprar(Usuario usuario) throws Exception {
        String respuesta = mvc.perform(post("/api/v1/ordenes").header("Authorization", token(usuario))
                .contentType("application/json").content(carrito(producto.getId(), 1)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(respuesta).get("id").asLong();
    }

    @Test
    void compraAgrupaRepetidosCalculaPrecioRealYDescuentaStock() throws Exception {
        String body = """
                {"total":1,"usuarioId":%d,"productos":[
                {"productId":%d,"qty":1,"price":1},{"productId":%d,"qty":2}]}
                """.formatted(otro.getId(), producto.getId(), producto.getId());
        mvc.perform(post("/api/v1/ordenes").header("Authorization", token(cliente))
                .contentType("application/json").content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.total").value(300))
                .andExpect(jsonPath("$.usuarioId").value(cliente.getId()));
        assertEquals(2, productos.findById(producto.getId()).orElseThrow().getStock());
        assertEquals(1, detalles.count());
        var detalle = detalles.findAll().getFirst();
        assertEquals(3, detalle.getCantidad());
        assertEquals(0, detalle.getPrecioUnitario().compareTo(new BigDecimal("100")));
    }

    @Test
    void clienteSoloVeSusOrdenesYPersonalVeTodas() throws Exception {
        long propia = comprar(cliente);
        long ajena = comprar(otro);
        mvc.perform(get("/api/v1/ordenes").header("Authorization", token(cliente)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(propia));
        mvc.perform(get("/api/v1/ordenes/" + propia).header("Authorization", token(cliente)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.productos[0].cantidad").value(1))
                .andExpect(jsonPath("$.usuario.password").doesNotExist());
        mvc.perform(get("/api/v1/ordenes/" + ajena).header("Authorization", token(cliente)))
                .andExpect(status().isForbidden());
        for (Usuario personal : new Usuario[]{vendedor, admin}) {
            mvc.perform(get("/api/v1/ordenes").header("Authorization", token(personal)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
            mvc.perform(get("/api/v1/ordenes/" + propia).header("Authorization", token(personal)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void rutasExigenJwtYCompraSoloCliente() throws Exception {
        for (String ruta : new String[]{"/api/v1/ordenes", "/api/v1/ordenes/1"}) {
            mvc.perform(get(ruta)).andExpect(status().isForbidden());
            mvc.perform(get(ruta).header("Authorization", "Bearer invalido"))
                    .andExpect(status().isForbidden());
            String refresh = jwt.generarRefreshToken(userDetails.loadUserByUsername(cliente.getEmail()));
            mvc.perform(get(ruta).header("Authorization", "Bearer " + refresh))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/v1/ordenes").contentType("application/json")
                .content(carrito(producto.getId(), 1))).andExpect(status().isForbidden());
        for (Usuario personal : new Usuario[]{vendedor, admin}) {
            mvc.perform(post("/api/v1/ordenes").header("Authorization", token(personal))
                    .contentType("application/json").content(carrito(producto.getId(), 1)))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(delete("/api/v1/ordenes/1").header("Authorization", token(admin)))
                .andExpect(status().isForbidden());
        assertEquals(0, ordenes.count());
    }

    @Test
    void carritosInvalidosNoCreanOrdenNiDescuentanStock() throws Exception {
        String repetidos = "{\"productos\":[{\"productId\":" + producto.getId()
                + ",\"qty\":2147483647},{\"productId\":" + producto.getId() + ",\"qty\":1}]}";
        for (String body : new String[]{"{}", "{\"productos\":[]}", "{\"productos\":[null]}",
                carrito(producto.getId(), 0), carrito(producto.getId(), -1),
                carrito(producto.getId(), 6), carrito(Long.MAX_VALUE, 1), repetidos}) {
            mvc.perform(post("/api/v1/ordenes").header("Authorization", token(cliente))
                    .contentType("application/json").content(body)).andExpect(status().isBadRequest());
        }
        assertEquals(0, ordenes.count()); assertEquals(0, detalles.count());
        assertEquals(5, productos.findById(producto.getId()).orElseThrow().getStock());
    }

    @Test
    void falloEnSegundoProductoNoDejaCompraParcial() throws Exception {
        Producto agotado = producto("PK-2", 0);
        String body = "{\"productos\":[{\"productId\":" + producto.getId()
                + ",\"qty\":2},{\"productId\":" + agotado.getId() + ",\"qty\":1}]}";
        mvc.perform(post("/api/v1/ordenes").header("Authorization", token(cliente))
                .contentType("application/json").content(body)).andExpect(status().isBadRequest());
        assertEquals(0, ordenes.count()); assertEquals(0, detalles.count());
        assertEquals(5, productos.findById(producto.getId()).orElseThrow().getStock());
    }

    @Test
    void impideEliminarProductoOUsuarioConCompras() throws Exception {
        comprar(cliente);
        mvc.perform(delete("/api/productos/" + producto.getId()).header("Authorization", token(admin)))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/usuarios/" + cliente.getId()).header("Authorization", token(admin)))
                .andExpect(status().isConflict());
        assertTrue(productos.existsById(producto.getId()));
        assertTrue(usuarios.existsById(cliente.getId()));
        assertEquals(1, ordenes.count()); assertEquals(1, detalles.count());
        mvc.perform(delete("/api/usuarios/" + otro.getId()).header("Authorization", token(admin)))
                .andExpect(status().isNoContent());
        Producto libre = producto("LIBRE", 1);
        mvc.perform(delete("/api/productos/" + libre.getId()).header("Authorization", token(admin)))
                .andExpect(status().isNoContent());
    }

    @Test
    void dosComprasSimultaneasNoSobrevendenLaUltimaUnidad() throws Exception {
        producto.setStock(1); productos.saveAndFlush(producto);
        CompraRequest compra = json.readValue(carrito(producto.getId(), 1), CompraRequest.class);
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> intento = () -> {
                assertTrue(salida.await(10, TimeUnit.SECONDS));
                try { service.registrarCompra(compra, cliente); return true; }
                catch (IllegalArgumentException e) {
                    assertEquals("Stock insuficiente", e.getMessage()); return false;
                }
            };
            Future<Boolean> a = executor.submit(intento);
            Future<Boolean> b = executor.submit(intento);
            salida.countDown();
            int exitos = (a.get(15, TimeUnit.SECONDS) ? 1 : 0) + (b.get(15, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, exitos);
            assertEquals(0, productos.findById(producto.getId()).orElseThrow().getStock());
            assertEquals(1, ordenes.count()); assertEquals(1, detalles.count());
        } finally { executor.shutdownNow(); }
    }
}
