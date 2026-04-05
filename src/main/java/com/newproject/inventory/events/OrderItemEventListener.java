package com.newproject.inventory.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newproject.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderItemEventListener {
    private static final Logger logger = LoggerFactory.getLogger(OrderItemEventListener.class);

    private final ObjectMapper objectMapper;
    private final InventoryService inventoryService;

    public OrderItemEventListener(ObjectMapper objectMapper, InventoryService inventoryService) {
        this.objectMapper = objectMapper;
        this.inventoryService = inventoryService;
    }

    @KafkaListener(topics = "${inventory.order-events.topic:order.events}", groupId = "${spring.application.name}-order-events")
    public void onOrderEvent(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            String eventType = root.path("eventType").asText("");
            if (!"ORDER_ITEM_ADDED".equals(eventType)
                && !"ORDER_ITEM_COMMITTED".equals(eventType)
                && !"ORDER_ITEM_RELEASED".equals(eventType)) {
                return;
            }

            JsonNode itemPayload = root.path("payload");
            Long productId = asLong(itemPayload.path("productId"));
            Integer quantity = asInt(itemPayload.path("quantity"));

            if (productId == null || quantity == null || quantity <= 0) {
                logger.warn("Skipping order item event with invalid payload: {}", payload);
                return;
            }

            switch (eventType) {
                case "ORDER_ITEM_ADDED" -> inventoryService.reserveFromOrderItem(productId, quantity);
                case "ORDER_ITEM_COMMITTED" -> inventoryService.commitReservationFromOrderItem(productId, quantity);
                case "ORDER_ITEM_RELEASED" -> inventoryService.releaseReservationFromOrderItem(productId, quantity);
                default -> {
                    return;
                }
            }
        } catch (Exception ex) {
            logger.warn("Unable to process order item event: {}", ex.getMessage());
        }
    }

    private Long asLong(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isIntegralNumber()) {
            return node.asLong();
        }
        if (node.isTextual()) {
            try {
                return Long.parseLong(node.asText());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Integer asInt(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isIntegralNumber()) {
            return node.asInt();
        }
        if (node.isTextual()) {
            try {
                return Integer.parseInt(node.asText());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
