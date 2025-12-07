package com.dhruthi.usercrud.store;

import com.dhruthi.usercrud.model.User;
import com.dhruthi.usercrud.model.UserNotFoundException;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

// Interface defining CRUD operations for User storage
public interface UserStore {

    User create(User user);

    Optional<User> findById(UUID id);

    User update(UUID id, User user) throws UserNotFoundException;

    void delete(UUID id) throws UserNotFoundException;

    Collection<User> findAll();
}
