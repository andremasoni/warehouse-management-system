package com.example.wms.product.infrastructure.persistence;

import com.example.wms.product.application.ProductRepository;
import com.example.wms.product.domain.Product;
import com.example.wms.product.domain.Sku;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ProductPersistenceAdapter implements ProductRepository {

    private final SpringDataProductRepository repository;

    public ProductPersistenceAdapter(SpringDataProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public Product save(Product product) {
        return toDomain(repository.save(new ProductJpaEntity(
                product.id(), product.sku().value(), product.name(), product.active())));
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return repository.findById(id).map(ProductPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<Product> findBySku(Sku sku) {
        return repository.findBySku(sku.value()).map(ProductPersistenceAdapter::toDomain);
    }

    private static Product toDomain(ProductJpaEntity entity) {
        return Product.restore(entity.getId(), new Sku(entity.getSku()), entity.getName(), entity.isActive());
    }
}
