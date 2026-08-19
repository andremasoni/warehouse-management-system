package com.example.wms.order.application;

import com.example.wms.order.domain.SalesOrder;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {
    SalesOrder save(SalesOrder order);
    Optional<SalesOrder> findById(UUID id);
    Optional<SalesOrder> findByIdForUpdate(UUID id);
    Optional<SalesOrder> findByExternalReference(String externalReference);
}
