package com.example.wms.inventory.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record StockMovement(
        UUID id,
        UUID warehouseId,
        UUID productId,
        UUID reservationId,
        MovementType type,
        long quantity,
        String externalReference,
        Instant occurredAt) {

    public StockMovement {
        Objects.requireNonNull(id, "Movement id is required");
        Objects.requireNonNull(warehouseId, "Warehouse id is required");
        Objects.requireNonNull(productId, "Product id is required");
        Objects.requireNonNull(type, "Movement type is required");
        Objects.requireNonNull(occurredAt, "Occurrence time is required");
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be greater than zero");
        if (externalReference == null || externalReference.isBlank() || externalReference.length() > 120) {
            throw new IllegalArgumentException("External reference must have 1 to 120 characters");
        }
        externalReference = externalReference.trim();
    }
}
