package com.dhruthi.usercrud.model;

import java.util.UUID;

public class UserNotFoundException extends Exception {

    //Stores the UUID of the user that couldn't be found and marked final for immutability
    private final UUID userId;

    //Constructor that accepts a UUID and initializes the exception message wit it
    public UserNotFoundException(UUID userId) {
        super("User with id " + userId + " not found");
        this.userId = userId;
    }
    
    //Overloaded constructor that accepts a UUID and a custom message
    public UserNotFoundException(UUID userId, String message) {
        super(message);
        this.userId = userId;
    }

    //Getter method to retrieve the userId
    public UUID getUserId() {
        return userId;
    }
}
