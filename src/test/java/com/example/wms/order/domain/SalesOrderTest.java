package com.example.wms.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.wms.shared.domain.ConflictException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SalesOrderTest {

    @Test
    void orderCanBeConfirmedOnlyAfterEveryItemIsReserved() {
        var item = OrderItem.create(UUID.randomUUID(), 3);
        var order = SalesOrder.create(UUID.randomUUID(), UUID.randomUUID(), "order-1", List.of(item), Instant.now());

        assertThatThrownBy(() -> order.markReserved(Instant.now()))
                .isInstanceOf(IllegalStateException.class);

        item.attachReservation(UUID.randomUUID());
        order.markReserved(Instant.now());
        order.confirm(Instant.now());

        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void cancelledOrderCannotBeConfirmed() {
        var item = OrderItem.create(UUID.randomUUID(), 1);
        item.attachReservation(UUID.randomUUID());
        var order = SalesOrder.create(UUID.randomUUID(), UUID.randomUUID(), "order-2", List.of(item), Instant.now());
        order.markReserved(Instant.now());
        order.cancel(Instant.now());

        assertThatThrownBy(() -> order.confirm(Instant.now()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void duplicateProductsAreRejected() {
        var productId = UUID.randomUUID();

        assertThatThrownBy(() -> SalesOrder.create(UUID.randomUUID(), UUID.randomUUID(), "order-3",
                List.of(OrderItem.create(productId, 1), OrderItem.create(productId, 2)), Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate products");
    }
}
