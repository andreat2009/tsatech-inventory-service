package com.newproject.inventory.repository;

import com.newproject.inventory.domain.InventoryItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<InventoryItem, Long> {
    Optional<InventoryItem> findByProductIdAndVariantKey(Long productId, String variantKey);
    List<InventoryItem> findAllByProductId(Long productId);
}
