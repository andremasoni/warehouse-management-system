package com.example.wms.inventory.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.wms.shared.domain.ConflictException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StockBalanceTest {

    @Test
    void reservesOnlyAvailableStock() {
        var balance = emptyBalance();
        balance.receive(10);

        balance.reserve(7);

        assertThat(balance.onHand()).isEqualTo(10);
        assertThat(balance.reserved()).isEqualTo(7);
        assertThat(balance.available()).isEqualTo(3);
    }

    @Test
    void rejectsReservationAboveAvailability() {
        var balance = emptyBalance();
        balance.receive(5);

        assertThatThrownBy(() -> balance.reserve(6))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Insufficient available stock");
    }

    @Test
    void consumingReservationReducesOnHandAndReservedTogether() {
        var balance = emptyBalance();
        balance.receive(10);
        balance.reserve(4);

        balance.consumeReserved(4);

        assertThat(balance.onHand()).isEqualTo(6);
        assertThat(balance.reserved()).isZero();
        assertThat(balance.available()).isEqualTo(6);
    }

    @Test
    void manualIssueCannotUseReservedUnits() {
        var balance = emptyBalance();
        balance.receive(10);
        balance.reserve(8);

        assertThatThrownBy(() -> balance.issueAvailable(3))
                .isInstanceOf(ConflictException.class);
    }

    private static StockBalance emptyBalance() {
        return StockBalance.empty(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    }
}
