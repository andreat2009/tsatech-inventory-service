package com.newproject.inventory.dto;

public class StockLineRequest {
    private Long productId;
    private String variantKey;
    private Integer quantity;

    public StockLineRequest() {
    }

    public StockLineRequest(Long productId, String variantKey, Integer quantity) {
        this.productId = productId;
        this.variantKey = variantKey;
        this.quantity = quantity;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getVariantKey() { return variantKey; }
    public void setVariantKey(String variantKey) { this.variantKey = variantKey; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
