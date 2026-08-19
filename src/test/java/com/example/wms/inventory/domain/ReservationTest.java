package com.example.wms.inventory.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.wms.shared.domain.ConflictException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReservationTest {

    @Test
    void releasedReservationCannotBeConsumed() {
        var reservation = Reservation.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                2, "test-reservation", Instant.parse("2026-01-01T00:00:00Z"));
        reservation.release(Instant.parse("2026-01-01T00:01:00Z"));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.RELEASED);
        assertThatThrownBy(() -> reservation.consume(Instant.parse("2026-01-01T00:02:00Z")))
                .isInstanceOf(ConflictException.class);
    }
}
