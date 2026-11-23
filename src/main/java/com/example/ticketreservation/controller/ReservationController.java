
package com.example.ticketreservation.controller;

import com.example.ticketreservation.dto.ReservationRequest;
import com.example.ticketreservation.dto.ReservationResponse;
import com.example.ticketreservation.model.Role;
import com.example.ticketreservation.security.SecurityUtils;
import com.example.ticketreservation.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    // CUSTOMER: Reserve tickets
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping
    public ResponseEntity<ReservationResponse> reserveTickets(
            @Valid @RequestBody ReservationRequest request,
            Authentication authentication) {

        ReservationResponse response =
                reservationService.reserveTickets(
                        request,
                        authentication.getName()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // CUSTOMER: View own reservations
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/my")
    public ResponseEntity<List<ReservationResponse>> getMyReservations(
            Authentication authentication) {

        return ResponseEntity.ok(
                reservationService.getMyReservations(
                        authentication.getName()
                )
        );
    }

    // ADMIN / ORGANIZER: View reservations
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    @GetMapping
    public ResponseEntity<List<ReservationResponse>> getAllReservations(
            Authentication authentication) {

        Role role = SecurityUtils.getCurrentUserRole(authentication);

        return ResponseEntity.ok(
                reservationService.getAllReservations(
                        authentication.getName(),
                        role
                )
        );
    }

    // CUSTOMER: Cancel own reservation
    // ADMIN: Cancel any reservation
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelReservation(
            @PathVariable String id,
            Authentication authentication) {

        Role role = SecurityUtils.getCurrentUserRole(authentication);

        reservationService.cancelReservation(
                id,
                authentication.getName(),
                role
        );

        return ResponseEntity.noContent().build();
    }
}
