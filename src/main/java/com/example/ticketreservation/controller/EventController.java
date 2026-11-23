
package com.example.ticketreservation.controller;

import com.example.ticketreservation.dto.EventRequest;
import com.example.ticketreservation.dto.EventResponse;
import com.example.ticketreservation.model.Role;
import com.example.ticketreservation.security.SecurityUtils;
import com.example.ticketreservation.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    // Get all events - authenticated users
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    // Get event by ID - authenticated users
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(
            @PathVariable String id
    ) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    // Create event - ADMIN or ORGANIZER
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    @PostMapping
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody EventRequest request,
            Authentication authentication
    ) {
        EventResponse response = eventService.createEvent(
                request,
                authentication.getName()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Update event - ADMIN or ORGANIZER
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable String id,
            @Valid @RequestBody EventRequest request,
            Authentication authentication
    ) {
        Role role = SecurityUtils.getCurrentUserRole(authentication);

        EventResponse response = eventService.updateEvent(
                id,
                request,
                authentication.getName(),
                role
        );

        return ResponseEntity.ok(response);
    }

// Delete event - ADMIN or ORGANIZER
@PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteEvent(
        @PathVariable String id,
        Authentication authentication
) {
    Role role = SecurityUtils.getCurrentUserRole(authentication);

    eventService.deleteEvent(
            id,
            authentication.getName(),
            role
    );

    return ResponseEntity.noContent().build();
}
}
