package com.example.ticketreservation.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.example.ticketreservation.model.Event;

public interface EventRepository extends MongoRepository<Event, String> {
    List<Event> findByOrganizerUsername(String organizerUsername);
}
