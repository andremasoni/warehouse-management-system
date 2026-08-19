package com.example.wms.product.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SkuTest {

    @Test
    void normalizesSku() {
        assertThat(new Sku("  abc-123 ").value()).isEqualTo("ABC-123");
    }

    @Test
    void rejectsInvalidSku() {
        assertThatThrownBy(() -> new Sku("a b"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
