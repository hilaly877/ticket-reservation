
package com.example.ticketreservation.controller;

import com.example.ticketreservation.dto.AdminCreateUserRequest;
import com.example.ticketreservation.dto.StatsResponse;
import com.example.ticketreservation.dto.UserResponse;
import com.example.ticketreservation.service.AdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.example.ticketreservation.dto.AdminUpdateUserRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // ADMIN: Create ADMIN or ORGANIZER account
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody AdminCreateUserRequest request) {

        UserResponse response = adminService.createUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // ADMIN: View all registered users
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {

        return ResponseEntity.ok(
                adminService.getAllUsers()
        );
    }

    // ADMIN: View system statistics
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/stats")
    public ResponseEntity<StatsResponse> getStats() {

        return ResponseEntity.ok(
                adminService.getStats()
        );
    }

    // ADMIN: Update a user's full name
@PreAuthorize("hasRole('ADMIN')")
@PutMapping("/users/{userId}")
public ResponseEntity<UserResponse> updateUser(
        @PathVariable String userId,
        @Valid @RequestBody AdminUpdateUserRequest request) {

    UserResponse response =
            adminService.updateUser(userId, request);

    return ResponseEntity.ok(response);
}


}
