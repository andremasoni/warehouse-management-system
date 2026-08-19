package com.example.wms.product.presentation;

import com.example.wms.product.application.ProductUseCases;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductUseCases useCases;

    public ProductController(ProductUseCases useCases) {
        this.useCases = useCases;
    }

    @PostMapping
    public ResponseEntity<ProductUseCases.ProductView> create(@Valid @RequestBody CreateProductRequest request) {
        var created = useCases.create(new ProductUseCases.CreateProductCommand(request.sku(), request.name()));
        return ResponseEntity.created(URI.create("/api/products/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public ProductUseCases.ProductView get(@PathVariable UUID id) {
        return useCases.get(id);
    }

    public record CreateProductRequest(
            @NotBlank @Size(max = 50) String sku,
            @NotBlank @Size(max = 160) String name) {}
}
