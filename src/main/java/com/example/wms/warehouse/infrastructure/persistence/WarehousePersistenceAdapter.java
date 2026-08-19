package com.example.wms.warehouse.infrastructure.persistence;

import com.example.wms.warehouse.application.WarehouseRepository;
import com.example.wms.warehouse.domain.Warehouse;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class WarehousePersistenceAdapter implements WarehouseRepository {
    private final SpringDataWarehouseRepository repository;

    public WarehousePersistenceAdapter(SpringDataWarehouseRepository repository) {
        this.repository = repository;
    }

    @Override
    public Warehouse save(Warehouse warehouse) {
        return toDomain(repository.save(new WarehouseJpaEntity(
                warehouse.id(), warehouse.code(), warehouse.name(), warehouse.active())));
    }

    @Override
    public Optional<Warehouse> findById(UUID id) {
        return repository.findById(id).map(WarehousePersistenceAdapter::toDomain);
    }

    @Override
    public Optional<Warehouse> findByCode(String code) {
        return repository.findByCode(code).map(WarehousePersistenceAdapter::toDomain);
    }

    private static Warehouse toDomain(WarehouseJpaEntity entity) {
        return Warehouse.restore(entity.getId(), entity.getCode(), entity.getName(), entity.isActive());
    }
}
