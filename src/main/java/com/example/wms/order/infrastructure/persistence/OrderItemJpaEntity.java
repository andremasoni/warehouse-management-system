package com.example.wms.order.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "order_items")
public class OrderItemJpaEntity {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderJpaEntity order;
    @Column(name = "product_id", nullable = false) private UUID productId;
    @Column(nullable = false) private long quantity;
    @Column(name = "reservation_id", nullable = false) private UUID reservationId;

    protected OrderItemJpaEntity() {}

    public OrderItemJpaEntity(UUID id, UUID productId, long quantity, UUID reservationId) {
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
        this.reservationId = reservationId;
    }

    void attach(OrderJpaEntity order) { this.order = order; }
    public UUID getId() { return id; }
    public UUID getProductId() { return productId; }
    public long getQuantity() { return quantity; }
    public UUID getReservationId() { return reservationId; }
}
