package com.newproject.inventory.dto;

import jakarta.validation.constraints.NotNull;

public class InventoryRequest {
    @NotNull
    private Long productId;

    @NotNull
    private Integer onHand;

    @NotNull
    private Integer reserved;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getOnHand() {
        return onHand;
    }

    public void setOnHand(Integer onHand) {
        this.onHand = onHand;
    }

    public Integer getReserved() {
        return reserved;
    }

    public void setReserved(Integer reserved) {
        this.reserved = reserved;
    }
}
