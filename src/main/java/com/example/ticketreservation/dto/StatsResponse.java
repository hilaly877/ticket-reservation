package com.example.ticketreservation.dto;

public record StatsResponse(
    long totalUsers,
    long totalEvents,
    long totalReservations,
    long confirmedReservations,
    long cancelledReservations
) {}
