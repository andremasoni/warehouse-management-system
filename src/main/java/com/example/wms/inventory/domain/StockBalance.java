package com.example.wms.inventory.domain;

import com.example.wms.shared.domain.ConflictException;
import java.util.Objects;
import java.util.UUID;

public final class StockBalance {

    private final UUID id;
    private final UUID warehouseId;
    private final UUID productId;
    private long onHand;
    private long reserved;
    private long version;

    private StockBalance(UUID id, UUID warehouseId, UUID productId, long onHand, long reserved, long version) {
        this.id = Objects.requireNonNull(id, "Balance id is required");
        this.warehouseId = Objects.requireNonNull(warehouseId, "Warehouse id is required");
        this.productId = Objects.requireNonNull(productId, "Product id is required");
        if (onHand < 0 || reserved < 0 || reserved > onHand) {
            throw new IllegalArgumentException("Invalid stock balance");
        }
        this.onHand = onHand;
        this.reserved = reserved;
        this.version = version;
    }

    public static StockBalance empty(UUID id, UUID warehouseId, UUID productId) {
        return new StockBalance(id, warehouseId, productId, 0, 0, 0);
    }

    public static StockBalance restore(
            UUID id, UUID warehouseId, UUID productId, long onHand, long reserved, long version) {
        return new StockBalance(id, warehouseId, productId, onHand, reserved, version);
    }

    public void receive(long quantity) {
        requirePositive(quantity);
        onHand = Math.addExact(onHand, quantity);
    }

    public void issueAvailable(long quantity) {
        requirePositive(quantity);
        if (available() < quantity) {
            throw new ConflictException("inventory.insufficient_stock", "Insufficient available stock");
        }
        onHand -= quantity;
    }

    public void reserve(long quantity) {
        requirePositive(quantity);
        if (available() < quantity) {
            throw new ConflictException("inventory.insufficient_stock", "Insufficient available stock");
        }
        reserved = Math.addExact(reserved, quantity);
    }

    public void release(long quantity) {
        requirePositive(quantity);
        if (reserved < quantity) {
            throw new ConflictException("inventory.invalid_release", "Cannot release more than the reserved stock");
        }
        reserved -= quantity;
    }

    public void consumeReserved(long quantity) {
        requirePositive(quantity);
        if (reserved < quantity) {
            throw new ConflictException("inventory.invalid_issue", "Cannot consume more than the reserved stock");
        }
        reserved -= quantity;
        onHand -= quantity;
    }

    public long available() { return onHand - reserved; }
    public UUID id() { return id; }
    public UUID warehouseId() { return warehouseId; }
    public UUID productId() { return productId; }
    public long onHand() { return onHand; }
    public long reserved() { return reserved; }
    public long version() { return version; }

    private static void requirePositive(long quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
    }
}
