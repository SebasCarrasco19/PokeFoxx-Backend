
package com.pokefoxx.backend.dto;

import java.util.List;

public class CompraRequest {

    private List<DetalleCompraRequest> productos;

    public CompraRequest() {
    }

    public List<DetalleCompraRequest> getProductos() {
        return productos;
    }

    public void setProductos(List<DetalleCompraRequest> productos) {
        this.productos = productos;
    }
}
