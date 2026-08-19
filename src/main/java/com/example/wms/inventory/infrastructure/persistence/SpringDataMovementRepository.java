package com.example.wms.inventory.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataMovementRepository extends JpaRepository<StockMovementJpaEntity, UUID> {
    Optional<StockMovementJpaEntity> findByExternalReference(String externalReference);

    @Query("select m from StockMovementJpaEntity m " +
            "where (:warehouseId is null or m.warehouseId = :warehouseId) " +
            "and (:productId is null or m.productId = :productId)")
    Page<StockMovementJpaEntity> findFiltered(@Param("warehouseId") UUID warehouseId,
                                               @Param("productId") UUID productId,
                                               Pageable pageable);
}
