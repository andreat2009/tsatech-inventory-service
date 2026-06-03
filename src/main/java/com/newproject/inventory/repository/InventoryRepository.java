package com.newproject.inventory.repository;

import com.newproject.inventory.domain.InventoryItem;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<InventoryItem, Long> {
    Optional<InventoryItem> findByProductIdAndVariantKey(Long productId, String variantKey);
    List<InventoryItem> findAllByProductId(Long productId);

    /**
     * Riserva atomica e condizionale: decrementa onHand e incrementa reserved solo se c'è
     * capienza sufficiente. Una sola istruzione SQL → niente race tra checkout concorrenti
     * (no overselling). Ritorna il numero di righe aggiornate: 0 = riga inesistente oppure
     * stock insufficiente.
     */
    @Modifying(clearAutomatically = true)
    @Query("update InventoryItem i set i.onHand = i.onHand - :quantity, "
        + "i.reserved = i.reserved + :quantity, i.updatedAt = :now "
        + "where i.productId = :productId and i.variantKey = :variantKey and i.onHand >= :quantity")
    int reserveIfAvailable(@Param("productId") Long productId,
                           @Param("variantKey") String variantKey,
                           @Param("quantity") int quantity,
                           @Param("now") OffsetDateTime now);
}
