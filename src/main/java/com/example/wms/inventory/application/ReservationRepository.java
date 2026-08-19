package com.example.wms.inventory.application;

import com.example.wms.inventory.domain.Reservation;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository {
    Reservation save(Reservation reservation);
    Optional<Reservation> findByIdForUpdate(UUID id);
    Optional<Reservation> findByExternalReference(String externalReference);
}
