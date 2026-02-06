package com.newproject.inventory.service;

import com.newproject.inventory.domain.InventoryItem;
import com.newproject.inventory.dto.InventoryRequest;
import com.newproject.inventory.dto.InventoryResponse;
import com.newproject.inventory.events.EventPublisher;
import com.newproject.inventory.exception.BadRequestException;
import com.newproject.inventory.exception.NotFoundException;
import com.newproject.inventory.repository.InventoryRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final EventPublisher eventPublisher;

    public InventoryService(InventoryRepository inventoryRepository, EventPublisher eventPublisher) {
        this.inventoryRepository = inventoryRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public InventoryResponse create(InventoryRequest request) {
        inventoryRepository.findByProductId(request.getProductId())
            .ifPresent(existing -> { throw new BadRequestException("Inventory already exists for product"); });

        InventoryItem item = new InventoryItem();
        item.setProductId(request.getProductId());
        item.setOnHand(request.getOnHand());
        item.setReserved(request.getReserved());
        item.setUpdatedAt(OffsetDateTime.now());

        InventoryItem saved = inventoryRepository.save(item);
        eventPublisher.publish("STOCK_CREATED", "inventory", saved.getId().toString(), toResponse(saved));
        return toResponse(saved);
    }

    @Transactional
    public InventoryResponse update(Long productId, InventoryRequest request) {
        InventoryItem item = inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new NotFoundException("Inventory item not found"));

        item.setOnHand(request.getOnHand());
        item.setReserved(request.getReserved());
        item.setUpdatedAt(OffsetDateTime.now());

        InventoryItem saved = inventoryRepository.save(item);
        eventPublisher.publish("STOCK_ADJUSTED", "inventory", saved.getId().toString(), toResponse(saved));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public InventoryResponse get(Long productId) {
        InventoryItem item = inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new NotFoundException("Inventory item not found"));
        return toResponse(item);
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> list() {
        return inventoryRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long productId) {
        InventoryItem item = inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new NotFoundException("Inventory item not found"));
        inventoryRepository.delete(item);
        eventPublisher.publish("STOCK_DELETED", "inventory", item.getId().toString(), null);
    }

    private InventoryResponse toResponse(InventoryItem item) {
        InventoryResponse response = new InventoryResponse();
        response.setId(item.getId());
        response.setProductId(item.getProductId());
        response.setOnHand(item.getOnHand());
        response.setReserved(item.getReserved());
        response.setUpdatedAt(item.getUpdatedAt());
        return response;
    }
}
