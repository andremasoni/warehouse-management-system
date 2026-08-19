package com.example.wms.order.domain;

import com.example.wms.shared.domain.ConflictException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class SalesOrder {
    private final UUID id;
    private final UUID warehouseId;
    private final String externalReference;
    private final List<OrderItem> items;
    private final Instant createdAt;
    private OrderStatus status;
    private Instant updatedAt;
    private long version;

    private SalesOrder(UUID id, UUID warehouseId, String externalReference, List<OrderItem> items,
                       OrderStatus status, Instant createdAt, Instant updatedAt, long version) {
        this.id = Objects.requireNonNull(id, "Order id is required");
        this.warehouseId = Objects.requireNonNull(warehouseId, "Warehouse id is required");
        if (externalReference == null || externalReference.isBlank() || externalReference.length() > 100) {
            throw new IllegalArgumentException("External reference must have 1 to 100 characters");
        }
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("Order must contain at least one item");
        var productIds = new HashSet<UUID>();
        if (items.stream().anyMatch(item -> !productIds.add(item.productId()))) {
            throw new IllegalArgumentException("An order cannot contain duplicate products");
        }
        this.externalReference = externalReference.trim();
        this.items = List.copyOf(items);
        this.status = Objects.requireNonNull(status, "Order status is required");
        this.createdAt = Objects.requireNonNull(createdAt, "Creation time is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Update time is required");
        this.version = version;
    }

    public static SalesOrder create(UUID id, UUID warehouseId, String externalReference,
                                    List<OrderItem> items, Instant now) {
        return new SalesOrder(id, warehouseId, externalReference, items, OrderStatus.CREATED, now, now, 0);
    }

    public static SalesOrder restore(UUID id, UUID warehouseId, String externalReference, List<OrderItem> items,
                                     OrderStatus status, Instant createdAt, Instant updatedAt, long version) {
        return new SalesOrder(id, warehouseId, externalReference, items, status, createdAt, updatedAt, version);
    }

    public void markReserved(Instant now) {
        requireStatus(OrderStatus.CREATED);
        if (items.stream().anyMatch(item -> item.reservationId() == null)) {
            throw new IllegalStateException("All items must be reserved");
        }
        status = OrderStatus.RESERVED;
        updatedAt = now;
    }

    public void confirm(Instant now) {
        requireStatus(OrderStatus.RESERVED);
        status = OrderStatus.CONFIRMED;
        updatedAt = now;
    }

    public void cancel(Instant now) {
        requireStatus(OrderStatus.RESERVED);
        status = OrderStatus.CANCELLED;
        updatedAt = now;
    }

    private void requireStatus(OrderStatus expected) {
        if (status != expected) {
            throw new ConflictException("order.invalid_state", "Order is not in the required state: " + expected);
        }
    }

    public UUID id() { return id; }
    public UUID warehouseId() { return warehouseId; }
    public String externalReference() { return externalReference; }
    public List<OrderItem> items() { return items; }
    public OrderStatus status() { return status; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public long version() { return version; }
}
