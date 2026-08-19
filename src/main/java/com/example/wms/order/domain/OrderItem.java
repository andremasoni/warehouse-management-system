package com.example.wms.order.domain;

import java.util.Objects;
import java.util.UUID;

public final class OrderItem {
    private final UUID id;
    private final UUID productId;
    private final long quantity;
    private UUID reservationId;

    private OrderItem(UUID id, UUID productId, long quantity, UUID reservationId) {
        this.id = Objects.requireNonNull(id, "Order item id is required");
        this.productId = Objects.requireNonNull(productId, "Product id is required");
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be greater than zero");
        this.quantity = quantity;
        this.reservationId = reservationId;
    }

    public static OrderItem create(UUID productId, long quantity) {
        return new OrderItem(UUID.randomUUID(), productId, quantity, null);
    }

    public static OrderItem restore(UUID id, UUID productId, long quantity, UUID reservationId) {
        return new OrderItem(id, productId, quantity, reservationId);
    }

    public void attachReservation(UUID reservationId) {
        if (this.reservationId != null) throw new IllegalStateException("Item already has a reservation");
        this.reservationId = Objects.requireNonNull(reservationId, "Reservation id is required");
    }

    public UUID id() { return id; }
    public UUID productId() { return productId; }
    public long quantity() { return quantity; }
    public UUID reservationId() { return reservationId; }
}
