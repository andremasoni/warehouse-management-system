package com.example.wms.product.application;

import java.util.UUID;

public interface ProductUseCases {

    ProductView create(CreateProductCommand command);

    ProductView get(UUID id);

    boolean existsActive(UUID id);

    record CreateProductCommand(String sku, String name) {}

    record ProductView(UUID id, String sku, String name, boolean active) {}
}
