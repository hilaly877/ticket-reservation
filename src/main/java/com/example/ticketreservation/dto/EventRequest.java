package com.example.ticketreservation.dto;

import java.time.LocalDateTime;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record EventRequest(

    @NotBlank (message = "Event name is required")
    String name,

    @NotBlank(message = "Event description is required")
    String description,

    @NotBlank(message = "Venue is required")
    String venue,

    @NotNull(message = "Event date is required")
    @Future(message = "Event date must be in the future")
    LocalDateTime eventDate,

    @Min(value = 1, message = "Total seats must be at least 1")
    int totalSeats,
    
    @PositiveOrZero(message = "Ticket price cannot be negative")
    double ticketPrice
) {}
