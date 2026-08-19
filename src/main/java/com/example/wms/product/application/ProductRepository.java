package com.example.wms.product.application;

import com.example.wms.product.domain.Product;
import com.example.wms.product.domain.Sku;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(UUID id);

    Optional<Product> findBySku(Sku sku);
}
