package com.example.wms.order.application;

import com.example.wms.order.domain.OrderStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OrderUseCases {
    OrderView create(CreateOrderCommand command);
    OrderView get(UUID id);
    OrderView confirm(UUID id);
    OrderView cancel(UUID id);

    record CreateOrderCommand(UUID warehouseId, String externalReference, List<ItemCommand> items) {}
    record ItemCommand(UUID productId, long quantity) {}
    record ItemView(UUID id, UUID productId, long quantity, UUID reservationId) {}
    record OrderView(UUID id, UUID warehouseId, String externalReference, OrderStatus status,
                     List<ItemView> items, Instant createdAt, Instant updatedAt) {}
}
