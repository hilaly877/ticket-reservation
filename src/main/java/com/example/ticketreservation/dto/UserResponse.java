package com.example.ticketreservation.dto;

import com.example.ticketreservation.model.AppUser;
import com.example.ticketreservation.model.Role;

public record UserResponse(
    String id,
    String username,
    String fullName,
    Role role
) {
    public static UserResponse from(AppUser user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getFullName(),
            user.getRole()
        );
    }
}
