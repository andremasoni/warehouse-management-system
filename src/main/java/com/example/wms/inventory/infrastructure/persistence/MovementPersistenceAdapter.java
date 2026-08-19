package com.example.wms.inventory.infrastructure.persistence;

import com.example.wms.inventory.application.MovementRepository;
import com.example.wms.inventory.domain.StockMovement;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class MovementPersistenceAdapter implements MovementRepository {
    private final SpringDataMovementRepository repository;

    public MovementPersistenceAdapter(SpringDataMovementRepository repository) {
        this.repository = repository;
    }

    @Override
    public StockMovement save(StockMovement movement) {
        return toDomain(repository.save(new StockMovementJpaEntity(movement.id(), movement.warehouseId(),
                movement.productId(), movement.reservationId(), movement.type(), movement.quantity(),
                movement.externalReference(), movement.occurredAt())));
    }

    @Override
    public Optional<StockMovement> findByExternalReference(String externalReference) {
        return repository.findByExternalReference(externalReference).map(MovementPersistenceAdapter::toDomain);
    }

    @Override
    public MovementPage find(UUID warehouseId, UUID productId, int page, int size) {
        var result = repository.findFiltered(warehouseId, productId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt")));
        return new MovementPage(result.getContent().stream().map(MovementPersistenceAdapter::toDomain).toList(),
                result.getTotalElements(), result.getTotalPages());
    }

    private static StockMovement toDomain(StockMovementJpaEntity entity) {
        return new StockMovement(entity.getId(), entity.getWarehouseId(), entity.getProductId(),
                entity.getReservationId(), entity.getType(), entity.getQuantity(), entity.getExternalReference(),
                entity.getOccurredAt());
    }
}
