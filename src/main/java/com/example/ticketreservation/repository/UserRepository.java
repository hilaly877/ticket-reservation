package com.example.ticketreservation.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.example.ticketreservation.model.AppUser;
import java.util.Optional;

public interface UserRepository extends MongoRepository<AppUser, String> {
    Optional<AppUser> findByUsername(String username);
    boolean existsByUsername(String username);
}
