package com.newproject.inventory.service;

import com.newproject.inventory.domain.InventoryItem;
import com.newproject.inventory.dto.InventoryRequest;
import com.newproject.inventory.dto.InventoryResponse;
import com.newproject.inventory.dto.StockLineRequest;
import com.newproject.inventory.events.EventPublisher;
import com.newproject.inventory.exception.BadRequestException;
import com.newproject.inventory.exception.InsufficientStockException;
import com.newproject.inventory.exception.NotFoundException;
import com.newproject.inventory.repository.InventoryRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
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
        String variantKey = normalizeVariantKey(request.getVariantKey());
        inventoryRepository.findByProductIdAndVariantKey(request.getProductId(), variantKey)
            .ifPresent(existing -> { throw new BadRequestException("Inventory already exists for product scope"); });

        InventoryItem item = new InventoryItem();
        applyRequest(item, request, request.getProductId(), variantKey);
        InventoryItem saved = inventoryRepository.save(item);
        eventPublisher.publish("STOCK_CREATED", "inventory", saved.getId().toString(), toResponse(saved));
        return toResponse(saved);
    }

    @Transactional
    public InventoryResponse update(Long productId, InventoryRequest request) {
        return updateScoped(productId, "", request);
    }

    @Transactional
    public InventoryResponse updateVariant(Long productId, String variantKey, InventoryRequest request) {
        return updateScoped(productId, variantKey, request);
    }

    @Transactional(readOnly = true)
    public InventoryResponse get(Long productId) {
        return getScoped(productId, "");
    }

    @Transactional(readOnly = true)
    public InventoryResponse getVariant(Long productId, String variantKey) {
        return getScoped(productId, variantKey);
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> list() {
        return inventoryRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long productId) {
        deleteScoped(productId, "");
    }

    @Transactional
    public void deleteVariant(Long productId, String variantKey) {
        deleteScoped(productId, variantKey);
    }

    @Transactional
    public void commitReservationFromOrderItem(Long productId, String variantKey, int quantity) {
        if (quantity <= 0) {
            return;
        }
        InventoryItem item = findRequired(productId, variantKey);
        int reserved = item.getReserved() != null ? item.getReserved() : 0;
        item.setReserved(Math.max(0, reserved - quantity));
        item.setUpdatedAt(OffsetDateTime.now());
        InventoryItem saved = inventoryRepository.save(item);
        eventPublisher.publish("STOCK_COMMITTED", "inventory", saved.getId().toString(), toResponse(saved));
    }

    @Transactional
    public void releaseReservationFromOrderItem(Long productId, String variantKey, int quantity) {
        if (quantity <= 0) {
            return;
        }
        InventoryItem item = findOrCreate(productId, variantKey);
        int onHand = item.getOnHand() != null ? item.getOnHand() : 0;
        int reserved = item.getReserved() != null ? item.getReserved() : 0;
        item.setOnHand(onHand + quantity);
        item.setReserved(Math.max(0, reserved - quantity));
        item.setUpdatedAt(OffsetDateTime.now());
        InventoryItem saved = inventoryRepository.save(item);
        eventPublisher.publish("STOCK_RELEASED", "inventory", saved.getId().toString(), toResponse(saved));
    }

    /**
     * Riserva sincrona dell'intero carrello (all-or-nothing). Per ogni riga TRACCIATA
     * (esiste una riga inventario) usa una UPDATE atomica con guardia di capienza: se anche
     * una sola riga non ha stock sufficiente l'intera transazione viene annullata e si
     * solleva {@link InsufficientStockException} (HTTP 409). Le righe NON tracciate (prodotti
     * senza inventario gestito) sono lasciate passare senza decremento.
     */
    @Transactional
    public void reserve(List<StockLineRequest> lines) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        List<StockLineRequest> reservedLines = new ArrayList<>();
        List<String> insufficient = new ArrayList<>();

        for (StockLineRequest line : lines) {
            if (line == null || line.getProductId() == null) {
                continue;
            }
            int quantity = line.getQuantity() != null ? line.getQuantity() : 0;
            if (quantity <= 0) {
                continue;
            }
            String variantKey = normalizeVariantKey(line.getVariantKey());
            int updated = inventoryRepository.reserveIfAvailable(line.getProductId(), variantKey, quantity, now);
            if (updated > 0) {
                reservedLines.add(new StockLineRequest(line.getProductId(), variantKey, quantity));
            } else if (inventoryRepository.findByProductIdAndVariantKey(line.getProductId(), variantKey).isPresent()) {
                // riga tracciata ma capienza insufficiente
                insufficient.add(describeLine(line.getProductId(), variantKey));
            }
            // altrimenti: prodotto non tracciato → consentito senza riservare
        }

        if (!insufficient.isEmpty()) {
            // rollback dell'intera transazione (anche delle righe già riservate sopra)
            throw new InsufficientStockException("Insufficient stock for: " + String.join(", ", insufficient));
        }

        for (StockLineRequest reserved : reservedLines) {
            inventoryRepository.findByProductIdAndVariantKey(reserved.getProductId(), reserved.getVariantKey())
                .ifPresent(item -> eventPublisher.publish("STOCK_RESERVED", "inventory", item.getId().toString(), toResponse(item)));
        }
    }

    /**
     * Rilascio di compensazione di una riserva sincrona (es. checkout abortito prima della
     * creazione dell'ordine). Restituisce le quantità a onHand. Salta le righe non tracciate
     * e non crea mai righe inventario fantasma.
     */
    @Transactional
    public void release(List<StockLineRequest> lines) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        for (StockLineRequest line : lines) {
            if (line == null || line.getProductId() == null) {
                continue;
            }
            int quantity = line.getQuantity() != null ? line.getQuantity() : 0;
            if (quantity <= 0) {
                continue;
            }
            String variantKey = normalizeVariantKey(line.getVariantKey());
            inventoryRepository.findByProductIdAndVariantKey(line.getProductId(), variantKey).ifPresent(item -> {
                int onHand = item.getOnHand() != null ? item.getOnHand() : 0;
                int reserved = item.getReserved() != null ? item.getReserved() : 0;
                // SECURITY (H6): si puo' rilasciare SOLO cio' che e' effettivamente riservato.
                // L'endpoint /release e' pubblico (serve al checkout guest per compensare una
                // riserva abortita). Senza questo cap un anonimo poteva chiamare /release senza
                // alcuna reservation e gonfiare onHand all'infinito (phantom stock -> oversell).
                // Con reserved==0 il rilascio e' un no-op.
                int releasable = Math.min(quantity, reserved);
                if (releasable <= 0) {
                    return;
                }
                item.setOnHand(onHand + releasable);
                item.setReserved(reserved - releasable);
                item.setUpdatedAt(now);
                InventoryItem saved = inventoryRepository.save(item);
                eventPublisher.publish("STOCK_RELEASED", "inventory", saved.getId().toString(), toResponse(saved));
            });
        }
    }

    private String describeLine(Long productId, String variantKey) {
        return variantKey == null || variantKey.isEmpty()
            ? "product " + productId
            : "product " + productId + " [" + variantKey + "]";
    }

    private InventoryResponse updateScoped(Long productId, String variantKey, InventoryRequest request) {
        InventoryItem item = findRequired(productId, variantKey);
        applyRequest(item, request, productId, normalizeVariantKey(variantKey));
        InventoryItem saved = inventoryRepository.save(item);
        eventPublisher.publish("STOCK_ADJUSTED", "inventory", saved.getId().toString(), toResponse(saved));
        return toResponse(saved);
    }

    private InventoryResponse getScoped(Long productId, String variantKey) {
        return toResponse(findRequired(productId, variantKey));
    }

    private void deleteScoped(Long productId, String variantKey) {
        InventoryItem item = findRequired(productId, variantKey);
        inventoryRepository.delete(item);
        eventPublisher.publish("STOCK_DELETED", "inventory", item.getId().toString(), null);
    }

    private InventoryItem findRequired(Long productId, String variantKey) {
        return inventoryRepository.findByProductIdAndVariantKey(productId, normalizeVariantKey(variantKey))
            .orElseThrow(() -> new NotFoundException("Inventory item not found"));
    }

    private InventoryItem findOrCreate(Long productId, String variantKey) {
        String normalizedVariantKey = normalizeVariantKey(variantKey);
        return inventoryRepository.findByProductIdAndVariantKey(productId, normalizedVariantKey)
            .orElseGet(() -> {
                InventoryItem created = new InventoryItem();
                created.setProductId(productId);
                created.setVariantKey(normalizedVariantKey);
                created.setOnHand(0);
                created.setReserved(0);
                created.setUpdatedAt(OffsetDateTime.now());
                return created;
            });
    }

    private void applyRequest(InventoryItem item, InventoryRequest request, Long productId, String variantKey) {
        item.setProductId(productId);
        item.setVariantKey(variantKey);
        item.setOnHand(request.getOnHand());
        item.setReserved(request.getReserved());
        item.setUpdatedAt(OffsetDateTime.now());
    }

    private InventoryResponse toResponse(InventoryItem item) {
        InventoryResponse response = new InventoryResponse();
        response.setId(item.getId());
        response.setProductId(item.getProductId());
        response.setVariantKey(item.getVariantKey());
        response.setOnHand(item.getOnHand());
        response.setReserved(item.getReserved());
        response.setUpdatedAt(item.getUpdatedAt());
        return response;
    }

    private String normalizeVariantKey(String variantKey) {
        if (variantKey == null) {
            return "";
        }
        String trimmed = variantKey.trim();
        return trimmed.isEmpty() ? "" : trimmed;
    }
}
