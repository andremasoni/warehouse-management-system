package com.example.wms.inventory.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataStockBalanceRepository extends JpaRepository<StockBalanceJpaEntity, UUID> {
    Optional<StockBalanceJpaEntity> findByWarehouseIdAndProductId(UUID warehouseId, UUID productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from StockBalanceJpaEntity b where b.warehouseId = :warehouseId and b.productId = :productId")
    Optional<StockBalanceJpaEntity> findForUpdate(@Param("warehouseId") UUID warehouseId,
                                                   @Param("productId") UUID productId);
}
