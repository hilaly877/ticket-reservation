package com.example.ticketreservation.model;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter 
@NoArgsConstructor 
@Document(collection = "reservations")
public class Reservation {
    @Id 
    private String id;
    private String reservationCode;
    private String customerUsername;
    private String eventId;
    private int seats;
    private double totalAmount;
    private ReservationStatus status;
    private LocalDateTime createdAt;
}
