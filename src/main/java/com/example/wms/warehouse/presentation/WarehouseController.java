package com.example.wms.warehouse.presentation;

import com.example.wms.warehouse.application.WarehouseUseCases;
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
@RequestMapping("/api/warehouses")
public class WarehouseController {
    private final WarehouseUseCases useCases;

    public WarehouseController(WarehouseUseCases useCases) {
        this.useCases = useCases;
    }

    @PostMapping
    public ResponseEntity<WarehouseUseCases.WarehouseView> create(@Valid @RequestBody CreateWarehouseRequest request) {
        var created = useCases.create(new WarehouseUseCases.CreateWarehouseCommand(request.code(), request.name()));
        return ResponseEntity.created(URI.create("/api/warehouses/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public WarehouseUseCases.WarehouseView get(@PathVariable UUID id) {
        return useCases.get(id);
    }

    public record CreateWarehouseRequest(
            @NotBlank @Size(max = 30) String code,
            @NotBlank @Size(max = 120) String name) {}
}
