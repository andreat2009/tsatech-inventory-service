package com.newproject.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.newproject.inventory.domain.InventoryItem;
import com.newproject.inventory.events.EventPublisher;
import com.newproject.inventory.repository.InventoryRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryServiceReservationLifecycleTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private EventPublisher eventPublisher;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(inventoryRepository, eventPublisher);
        when(inventoryRepository.save(any(InventoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void releaseReservationReturnsStockToOnHand() {
        InventoryItem item = inventoryItem(1008L, 0, 3);
        when(inventoryRepository.findByProductId(1008L)).thenReturn(Optional.of(item));

        inventoryService.releaseReservationFromOrderItem(1008L, 2);

        assertThat(item.getOnHand()).isEqualTo(2);
        assertThat(item.getReserved()).isEqualTo(1);
        verify(eventPublisher).publish(org.mockito.ArgumentMatchers.eq("STOCK_RELEASED"), org.mockito.ArgumentMatchers.eq("inventory"), any(), any());
    }

    @Test
    void commitReservationKeepsSoldStockReducedButClearsReserve() {
        InventoryItem item = inventoryItem(1008L, 5, 3);
        when(inventoryRepository.findByProductId(1008L)).thenReturn(Optional.of(item));

        inventoryService.commitReservationFromOrderItem(1008L, 2);

        assertThat(item.getOnHand()).isEqualTo(5);
        assertThat(item.getReserved()).isEqualTo(1);
        verify(eventPublisher).publish(org.mockito.ArgumentMatchers.eq("STOCK_COMMITTED"), org.mockito.ArgumentMatchers.eq("inventory"), any(), any());
    }

    private InventoryItem inventoryItem(Long productId, int onHand, int reserved) {
        InventoryItem item = new InventoryItem();
        item.setId(1L);
        item.setProductId(productId);
        item.setOnHand(onHand);
        item.setReserved(reserved);
        return item;
    }
}
