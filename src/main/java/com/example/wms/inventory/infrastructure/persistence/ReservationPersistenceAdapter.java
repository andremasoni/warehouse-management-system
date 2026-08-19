package com.example.wms.inventory.infrastructure.persistence;

import com.example.wms.inventory.application.ReservationRepository;
import com.example.wms.inventory.domain.Reservation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ReservationPersistenceAdapter implements ReservationRepository {
    private final SpringDataReservationRepository repository;

    public ReservationPersistenceAdapter(SpringDataReservationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Reservation save(Reservation reservation) {
        return toDomain(repository.save(new ReservationJpaEntity(reservation.id(), reservation.warehouseId(),
                reservation.productId(), reservation.quantity(), reservation.externalReference(), reservation.status(),
                reservation.createdAt(), reservation.updatedAt(), reservation.version())));
    }

    @Override
    public Optional<Reservation> findByIdForUpdate(UUID id) {
        return repository.findByIdForUpdate(id).map(ReservationPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<Reservation> findByExternalReference(String externalReference) {
        return repository.findByExternalReference(externalReference).map(ReservationPersistenceAdapter::toDomain);
    }

    private static Reservation toDomain(ReservationJpaEntity entity) {
        return Reservation.restore(entity.getId(), entity.getWarehouseId(), entity.getProductId(), entity.getQuantity(),
                entity.getExternalReference(), entity.getStatus(), entity.getCreatedAt(), entity.getUpdatedAt(),
                entity.getVersion());
    }
}
