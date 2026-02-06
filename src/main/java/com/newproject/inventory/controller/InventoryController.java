package com.newproject.inventory.controller;

import com.newproject.inventory.dto.InventoryRequest;
import com.newproject.inventory.dto.InventoryResponse;
import com.newproject.inventory.service.InventoryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public List<InventoryResponse> list() {
        return inventoryService.list();
    }

    @GetMapping("/{productId}")
    public InventoryResponse get(@PathVariable Long productId) {
        return inventoryService.get(productId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryResponse create(@Valid @RequestBody InventoryRequest request) {
        return inventoryService.create(request);
    }

    @PutMapping("/{productId}")
    public InventoryResponse update(@PathVariable Long productId, @Valid @RequestBody InventoryRequest request) {
        return inventoryService.update(productId, request);
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long productId) {
        inventoryService.delete(productId);
    }
}
