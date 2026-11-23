package com.example.ticketreservation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ReservationRequest(
    @NotBlank(message = "Event ID is required")
    String eventId,

    @Min(value =1, message = "You must reserve at least 1 seat")
    int seats
) {}
