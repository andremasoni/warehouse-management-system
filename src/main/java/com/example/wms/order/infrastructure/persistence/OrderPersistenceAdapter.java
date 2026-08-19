package com.example.wms.order.infrastructure.persistence;

import com.example.wms.order.application.OrderRepository;
import com.example.wms.order.domain.OrderItem;
import com.example.wms.order.domain.SalesOrder;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OrderPersistenceAdapter implements OrderRepository {
    private final SpringDataOrderRepository repository;

    public OrderPersistenceAdapter(SpringDataOrderRepository repository) {
        this.repository = repository;
    }

    @Override
    public SalesOrder save(SalesOrder order) {
        var entity = new OrderJpaEntity(order.id(), order.warehouseId(), order.externalReference(), order.status(),
                order.createdAt(), order.updatedAt(), order.version());
        order.items().forEach(item -> entity.addItem(new OrderItemJpaEntity(
                item.id(), item.productId(), item.quantity(), item.reservationId())));
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<SalesOrder> findById(UUID id) {
        return repository.findById(id).map(OrderPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<SalesOrder> findByIdForUpdate(UUID id) {
        return repository.findByIdForUpdate(id).map(OrderPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<SalesOrder> findByExternalReference(String externalReference) {
        return repository.findByExternalReference(externalReference).map(OrderPersistenceAdapter::toDomain);
    }

    private static SalesOrder toDomain(OrderJpaEntity entity) {
        var items = entity.getItems().stream().map(item -> OrderItem.restore(item.getId(), item.getProductId(),
                item.getQuantity(), item.getReservationId())).toList();
        return SalesOrder.restore(entity.getId(), entity.getWarehouseId(), entity.getExternalReference(), items,
                entity.getStatus(), entity.getCreatedAt(), entity.getUpdatedAt(), entity.getVersion());
    }
}
