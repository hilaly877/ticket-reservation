package com.example.ticketreservation.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter 
@Setter 
@NoArgsConstructor 
@Document(collection = "users")
public class AppUser {
    @Id 
    private String id;
    @Indexed(unique = true)
    private String username;
    private String password;
    private String fullName;
    private Role role;
}
