package com.dhruthi.usercrud.store;

import com.dhruthi.usercrud.model.User;
import com.dhruthi.usercrud.model.UserNotFoundException;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// This class implementation allows the CRUD operations on User objects in memory, without requiring external databases.
public class InMemoryUserStore implements UserStore {

    // ConcurrentHashMap to store the users in memory
    private final ConcurrentHashMap<UUID, User> users;

    public InMemoryUserStore() {
        this.users = new ConcurrentHashMap<>();
    }

    @Override
    // Create a new user with a generated UUID and store it in the map
    public User create(User user) {
        UUID newId = UUID.randomUUID();
        User newUser = new User(newId, user.name(), user.email());
        users.put(newId, newUser);
        return newUser;
    }

    @Override
    // Finding the user by their UUID
    public Optional<User> findById(UUID id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    // Update the user details if they exist, if not found throw UserNotFoundException
    public User update(UUID id, User user) throws UserNotFoundException {
        User updatedUser = users.computeIfPresent(id, (key, existing) ->
            new User(id, user.name(), user.email())
        );

        if (updatedUser == null) {
            throw new UserNotFoundException(id);
        }

        return updatedUser;
    }

    @Override
    // Delete the user with their UUID, if not found throw UserNotFoundException
    public void delete(UUID id) throws UserNotFoundException {
        User removed = users.remove(id);
        if (removed == null) {
            throw new UserNotFoundException(id);
        }
    }

    @Override
    // Return all users as a collection
    public Collection<User> findAll() {
        return users.values();
    }
}