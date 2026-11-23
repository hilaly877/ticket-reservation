
package com.example.ticketreservation.service;

import com.example.ticketreservation.dto.AdminCreateUserRequest;
import com.example.ticketreservation.dto.StatsResponse;
import com.example.ticketreservation.dto.UserResponse;
import com.example.ticketreservation.exception.ApiException;
import com.example.ticketreservation.model.AppUser;
import com.example.ticketreservation.model.Role;
import com.example.ticketreservation.model.Reservation;
import com.example.ticketreservation.model.ReservationStatus;
import com.example.ticketreservation.repository.EventRepository;
import com.example.ticketreservation.repository.ReservationRepository;
import com.example.ticketreservation.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final ReservationRepository reservationRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(
            UserRepository userRepository,
            EventRepository eventRepository,
            ReservationRepository reservationRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.reservationRepository = reservationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ADMIN: Create ADMIN or ORGANIZER account
    public UserResponse createUser(AdminCreateUserRequest request) {

        if (userRepository.existsByUsername(request.username())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Username is already taken"
            );
        }

        if (request.role() == Role.CUSTOMER) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Admin can only create ADMIN or ORGANIZER accounts"
            );
        }

        AppUser user = new AppUser();

        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setRole(request.role());

        AppUser savedUser = userRepository.save(user);

        return UserResponse.from(savedUser);
    }

    // ADMIN: View all registered users
    public List<UserResponse> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    // ADMIN: View system statistics
    public StatsResponse getStats() {

        long totalUsers = userRepository.count();
        long totalEvents = eventRepository.count();
        long totalReservations = reservationRepository.count();
        long confirmedReservations = reservationRepository.countByStatus(ReservationStatus.CONFIRMED);
        long cancelledReservations = reservationRepository.countByStatus(ReservationStatus.CANCELLED);

        return new StatsResponse(
                totalUsers,
                totalEvents,
                totalReservations,
                confirmedReservations,
                cancelledReservations
        );
    }
}
