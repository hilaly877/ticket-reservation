package com.example.ticketreservation.service;

import com.example.ticketreservation.dto.EventRequest;
import com.example.ticketreservation.dto.EventResponse;
import com.example.ticketreservation.exception.ApiException;
import com.example.ticketreservation.model.Event;
import com.example.ticketreservation.model.Role;
import com.example.ticketreservation.repository.EventRepository;
import com.example.ticketreservation.repository.ReservationRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final ReservationRepository reservationRepository;

    public EventService(
            EventRepository eventRepository,
            ReservationRepository reservationRepository
    ) {
        this.eventRepository = eventRepository;
        this.reservationRepository = reservationRepository;
    }

    // Create a new event
    public EventResponse createEvent(
            EventRequest request,
            String username
    ) {
        Event event = new Event();

        event.setName(request.name());
        event.setDescription(request.description());
        event.setVenue(request.venue());
        event.setEventDate(request.eventDate());
        event.setTotalSeats(request.totalSeats());
        event.setAvailableSeats(request.totalSeats());
        event.setTicketPrice(request.ticketPrice());
        event.setOrganizerUsername(username);

        Event savedEvent = eventRepository.save(event);

        return mapToResponse(savedEvent);
    }

    // Get all events
    public List<EventResponse> getAllEvents() {
        return eventRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // Get event by ID
    public EventResponse getEventById(String eventId) {
        Event event = findEventById(eventId);

        return mapToResponse(event);
    }

    // Update event
    public EventResponse updateEvent(
            String eventId,
            EventRequest request,
            String username,
            Role role
    ) {
        Event event = findEventById(eventId);

        // Organizers can only update their own events
        if (role == Role.ORGANIZER
                && !event.getOrganizerUsername().equals(username)) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "You can only update your own events"
            );
        }

        // Calculate already booked seats
        int bookedSeats = event.getTotalSeats() - event.getAvailableSeats();

        // Prevent reducing capacity below already booked seats
        if (request.totalSeats() < bookedSeats) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Total seats cannot be less than already reserved seats"
            );
        }

        event.setName(request.name());
        event.setDescription(request.description());
        event.setVenue(request.venue());
        event.setEventDate(request.eventDate());
        event.setTotalSeats(request.totalSeats());
        event.setAvailableSeats(request.totalSeats() - bookedSeats);
        event.setTicketPrice(request.ticketPrice());

        Event updatedEvent = eventRepository.save(event);

        return mapToResponse(updatedEvent);
    }

// Delete event (ADMIN can delete any, ORGANIZER can delete own)
public void deleteEvent(
        String eventId,
        String username,
        Role role
) {
    Event event = findEventById(eventId);

    // Organizers can only delete their own events
    if (role == Role.ORGANIZER
            && !event.getOrganizerUsername().equals(username)) {
        throw new ApiException(
                HttpStatus.FORBIDDEN,
                "You can only delete your own events"
        );
    }

    // Prevent deletion if reservations exist
    if (reservationRepository.existsByEventId(eventId)) {
        throw new ApiException(
                HttpStatus.CONFLICT,
                "Cannot delete an event that has reservations"
        );
    }

    eventRepository.delete(event);
}

    // Find event or throw 404
    private Event findEventById(String eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Event not found"
                ));
    }

    // Convert Event entity into EventResponse DTO
    private EventResponse mapToResponse(Event event) {
        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getVenue(),
                event.getEventDate(),
                event.getTotalSeats(),
                event.getAvailableSeats(),
                event.getTicketPrice(),
                event.getOrganizerUsername()
        );
    }
}
