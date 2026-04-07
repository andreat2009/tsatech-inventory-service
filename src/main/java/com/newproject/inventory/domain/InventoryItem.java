package com.newproject.inventory.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "inventory_item")
public class InventoryItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "variant_key", nullable = false, length = 128)
    private String variantKey = "";

    @Column(name = "on_hand", nullable = false)
    private Integer onHand;

    @Column(nullable = false)
    private Integer reserved;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getVariantKey() { return variantKey; }
    public void setVariantKey(String variantKey) { this.variantKey = variantKey; }
    public Integer getOnHand() { return onHand; }
    public void setOnHand(Integer onHand) { this.onHand = onHand; }
    public Integer getReserved() { return reserved; }
    public void setReserved(Integer reserved) { this.reserved = reserved; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
