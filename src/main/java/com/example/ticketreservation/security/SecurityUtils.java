package com.example.ticketreservation.security;

import com.example.ticketreservation.model.Role;
import org.springframework.security.core.Authentication;

/**
 * Reusable utility methods for Spring Security authentication context.
 */
public final class SecurityUtils {

    private SecurityUtils() {
        // Prevent instantiation of utility class
    }

    /**
     * Extracts the application Role from the authenticated user's authorities.
     *
     * @param authentication current authentication object
     * @return the resolved Role enum
     */
    public static Role getCurrentUserRole(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities().isEmpty()) {
            throw new IllegalStateException("No authorities found for current user");
        }

        String authority = authentication.getAuthorities()
                .iterator()
                .next()
                .getAuthority();

        return Role.valueOf(authority.replace("ROLE_", ""));
    }
}
