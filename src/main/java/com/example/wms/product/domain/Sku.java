package com.example.wms.product.domain;

import java.util.Locale;
import java.util.regex.Pattern;

public record Sku(String value) {

    private static final Pattern VALID_FORMAT = Pattern.compile("[A-Z0-9][A-Z0-9._-]{2,49}");

    public Sku {
        if (value == null) {
            throw new IllegalArgumentException("SKU is required");
        }
        value = value.trim().toUpperCase(Locale.ROOT);
        if (!VALID_FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("SKU must have 3 to 50 letters, numbers, dots, hyphens or underscores");
        }
    }
}
