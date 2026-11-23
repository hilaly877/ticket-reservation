
package com.example.ticketreservation.service;

import com.example.ticketreservation.dto.ReservationRequest;
import com.example.ticketreservation.dto.ReservationResponse;
import com.example.ticketreservation.exception.ApiException;
import com.example.ticketreservation.model.Event;
import com.example.ticketreservation.model.Reservation;
import com.example.ticketreservation.model.ReservationStatus;
import com.example.ticketreservation.model.Role;
import com.example.ticketreservation.repository.EventRepository;
import com.example.ticketreservation.repository.ReservationRepository;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final EventRepository eventRepository;
    private final MongoTemplate mongoTemplate;

    public ReservationService(
            ReservationRepository reservationRepository,
            EventRepository eventRepository,
            MongoTemplate mongoTemplate
    ) {
        this.reservationRepository = reservationRepository;
        this.eventRepository = eventRepository;
        this.mongoTemplate = mongoTemplate;
    }

    // Reserve tickets
    public ReservationResponse reserveTickets(
            ReservationRequest request,
            String username
    ) {
        // Atomically decrease available seats only if enough seats exist
        Query seatQuery = new Query(
                Criteria.where("_id").is(request.eventId())
                        .and("availableSeats").gte(request.seats())
        );

        Update seatUpdate = new Update()
                .inc("availableSeats", -request.seats());

        Event event = mongoTemplate.findAndModify(
                seatQuery,
                seatUpdate,
                FindAndModifyOptions.options().returnNew(true),
                Event.class
        );

        if (event == null) {
            // Check whether the event exists or seats are insufficient
            if (!eventRepository.existsById(request.eventId())) {
                throw new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Event not found"
                );
            }

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Not enough available seats"
            );
        }

        Reservation reservation = new Reservation();

        reservation.setReservationCode(
                "TKT-" + UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase()
        );

        reservation.setCustomerUsername(username);
        reservation.setEventId(event.getId());
        reservation.setSeats(request.seats());
        reservation.setTotalAmount(request.seats() * event.getTicketPrice());
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setCreatedAt(LocalDateTime.now());

        Reservation savedReservation = reservationRepository.save(reservation);

        return mapToResponse(savedReservation);
    }

    // Get current customer's reservations
    public List<ReservationResponse> getMyReservations(String username) {
        return reservationRepository.findByCustomerUsername(username)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // Get all reservations (ADMIN or ORGANIZER)
    public List<ReservationResponse> getAllReservations(
            String username,
            Role role
    ) {
        List<Reservation> reservations;

        // ORGANIZER only sees reservations for their own events
        if (role == Role.ORGANIZER) {
            List<String> organizerEventIds = eventRepository
                    .findByOrganizerUsername(username)
                    .stream()
                    .map(Event::getId)
                    .toList();

            reservations = reservationRepository.findByEventIdIn(organizerEventIds);
        } else {
            reservations = reservationRepository.findAll();
        }

        return reservations.stream()
                .map(this::mapToResponse)
                .toList();
    }

    // Cancel reservation
    public void cancelReservation(
            String reservationId,
            String username,
            Role role
    ) {
        Query cancelQuery = new Query(
                Criteria.where("_id").is(reservationId)
                        .and("status").is(ReservationStatus.CONFIRMED)
        );

        // Customers can cancel only their own reservations
        if (role == Role.CUSTOMER) {
            cancelQuery.addCriteria(
                    Criteria.where("customerUsername").is(username)
            );
        }

        Update cancelUpdate = new Update()
                .set("status", ReservationStatus.CANCELLED);

        Reservation cancelledReservation = mongoTemplate.findAndModify(
                cancelQuery,
                cancelUpdate,
                FindAndModifyOptions.options().returnNew(true),
                Reservation.class
        );

        if (cancelledReservation == null) {
            Reservation existing = reservationRepository
                    .findById(reservationId)
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND,
                            "Reservation not found"
                    ));

            if (role == Role.CUSTOMER
                    && !existing.getCustomerUsername().equals(username)) {
                throw new ApiException(
                        HttpStatus.FORBIDDEN,
                        "You cannot cancel another customer's reservation"
                );
            }

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Reservation is already cancelled"
            );
        }

        // Restore the seats atomically
        mongoTemplate.updateFirst(
                new Query(Criteria.where("_id")
                        .is(cancelledReservation.getEventId())),
                new Update().inc(
                        "availableSeats",
                        cancelledReservation.getSeats()
                ),
                Event.class
        );
    }

    // Convert Reservation to DTO
    private ReservationResponse mapToResponse(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getReservationCode(),
                reservation.getCustomerUsername(),
                reservation.getEventId(),
                reservation.getSeats(),
                reservation.getTotalAmount(),
                reservation.getStatus(),
                reservation.getCreatedAt()
        );
    }
}
