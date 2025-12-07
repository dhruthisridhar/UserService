package com.dhruthi.usercrud.model;

import java.util.UUID;

public record User(UUID id, String name, String email) {

    // This code runs everytime a new User is created
    public User {
        // Validate inputs
        if (id == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("User name cannot be null or empty");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("User email cannot be null or empty");
        }

        // Trim inputs
        name = name.trim();
        email = email.trim();
    }
}
