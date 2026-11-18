
package com.pokefoxx.backend.dto;

public class DetalleCompraRequest {

    private Long productId;
    private Integer qty;

    public DetalleCompraRequest() {
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }
}
