package com.example.wms.product.application;

import com.example.wms.product.domain.Product;
import com.example.wms.product.domain.Sku;
import com.example.wms.shared.application.AccessControl;
import com.example.wms.shared.application.Role;
import com.example.wms.shared.application.TransactionRunner;
import com.example.wms.shared.domain.ConflictException;
import com.example.wms.shared.domain.NotFoundException;
import java.util.UUID;

public final class ProductService implements ProductUseCases {

    private final ProductRepository products;
    private final TransactionRunner transactions;
    private final AccessControl accessControl;

    public ProductService(ProductRepository products, TransactionRunner transactions, AccessControl accessControl) {
        this.products = products;
        this.transactions = transactions;
        this.accessControl = accessControl;
    }

    @Override
    public ProductView create(CreateProductCommand command) {
        accessControl.requireAny(Role.ADMIN);
        return transactions.required(() -> {
            var sku = new Sku(command.sku());
            products.findBySku(sku).ifPresent(existing -> {
                throw new ConflictException("product.sku_already_exists", "A product with this SKU already exists");
            });
            return view(products.save(Product.create(UUID.randomUUID(), sku, command.name())));
        });
    }

    @Override
    public ProductView get(UUID id) {
        accessControl.requireAny(Role.ADMIN, Role.STOCK_OPERATOR, Role.ORDER_OPERATOR, Role.VIEWER);
        return products.findById(id).map(ProductService::view).orElseThrow(() ->
                new NotFoundException("product.not_found", "Product not found"));
    }

    @Override
    public boolean existsActive(UUID id) {
        return products.findById(id).filter(Product::active).isPresent();
    }

    private static ProductView view(Product product) {
        return new ProductView(product.id(), product.sku().value(), product.name(), product.active());
    }
}
