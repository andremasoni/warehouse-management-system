package com.example.wms.inventory.infrastructure.persistence;

import com.example.wms.inventory.application.InventoryRepository;
import com.example.wms.inventory.domain.StockBalance;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class InventoryPersistenceAdapter implements InventoryRepository {
    private final SpringDataStockBalanceRepository repository;
    private final JdbcTemplate jdbcTemplate;

    public InventoryPersistenceAdapter(SpringDataStockBalanceRepository repository, JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public StockBalance lockOrCreate(UUID warehouseId, UUID productId) {
        jdbcTemplate.update("""
                insert into stock_balances (id, warehouse_id, product_id, on_hand, reserved, version)
                values (?, ?, ?, 0, 0, 0)
                on conflict (warehouse_id, product_id) do nothing
                """, UUID.randomUUID(), warehouseId, productId);
        return repository.findForUpdate(warehouseId, productId)
                .map(InventoryPersistenceAdapter::toDomain)
                .orElseThrow(() -> new IllegalStateException("Could not lock stock balance"));
    }

    @Override
    public Optional<StockBalance> find(UUID warehouseId, UUID productId) {
        return repository.findByWarehouseIdAndProductId(warehouseId, productId)
                .map(InventoryPersistenceAdapter::toDomain);
    }

    @Override
    public StockBalance save(StockBalance balance) {
        return toDomain(repository.save(new StockBalanceJpaEntity(balance.id(), balance.warehouseId(),
                balance.productId(), balance.onHand(), balance.reserved(), balance.version())));
    }

    private static StockBalance toDomain(StockBalanceJpaEntity entity) {
        return StockBalance.restore(entity.getId(), entity.getWarehouseId(), entity.getProductId(),
                entity.getOnHand(), entity.getReserved(), entity.getVersion());
    }
}
