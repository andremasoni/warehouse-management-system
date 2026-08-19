package com.example.wms.inventory.domain;

import com.example.wms.shared.domain.ConflictException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Reservation {

    private final UUID id;
    private final UUID warehouseId;
    private final UUID productId;
    private final long quantity;
    private final String externalReference;
    private final Instant createdAt;
    private ReservationStatus status;
    private Instant updatedAt;
    private long version;

    private Reservation(UUID id, UUID warehouseId, UUID productId, long quantity, String externalReference,
                        ReservationStatus status, Instant createdAt, Instant updatedAt, long version) {
        this.id = Objects.requireNonNull(id, "Reservation id is required");
        this.warehouseId = Objects.requireNonNull(warehouseId, "Warehouse id is required");
        this.productId = Objects.requireNonNull(productId, "Product id is required");
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be greater than zero");
        if (externalReference == null || externalReference.isBlank() || externalReference.length() > 100) {
            throw new IllegalArgumentException("External reference must have 1 to 100 characters");
        }
        this.quantity = quantity;
        this.externalReference = externalReference.trim();
        this.status = Objects.requireNonNull(status, "Reservation status is required");
        this.createdAt = Objects.requireNonNull(createdAt, "Creation time is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Update time is required");
        this.version = version;
    }

    public static Reservation create(UUID id, UUID warehouseId, UUID productId, long quantity,
                                     String externalReference, Instant now) {
        return new Reservation(id, warehouseId, productId, quantity, externalReference,
                ReservationStatus.ACTIVE, now, now, 0);
    }

    public static Reservation restore(UUID id, UUID warehouseId, UUID productId, long quantity,
                                      String externalReference, ReservationStatus status,
                                      Instant createdAt, Instant updatedAt, long version) {
        return new Reservation(id, warehouseId, productId, quantity, externalReference,
                status, createdAt, updatedAt, version);
    }

    public void release(Instant now) {
        requireActive();
        status = ReservationStatus.RELEASED;
        updatedAt = now;
    }

    public void consume(Instant now) {
        requireActive();
        status = ReservationStatus.CONSUMED;
        updatedAt = now;
    }

    private void requireActive() {
        if (status != ReservationStatus.ACTIVE) {
            throw new ConflictException("reservation.not_active", "Reservation is no longer active");
        }
    }

    public UUID id() { return id; }
    public UUID warehouseId() { return warehouseId; }
    public UUID productId() { return productId; }
    public long quantity() { return quantity; }
    public String externalReference() { return externalReference; }
    public ReservationStatus status() { return status; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public long version() { return version; }
}
