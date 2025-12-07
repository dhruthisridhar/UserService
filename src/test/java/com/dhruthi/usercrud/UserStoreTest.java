package com.dhruthi.usercrud;

import com.dhruthi.usercrud.model.User;
import com.dhruthi.usercrud.model.UserNotFoundException;
import com.dhruthi.usercrud.store.InMemoryUserStore;
import com.dhruthi.usercrud.store.UserStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

class UserStoreTest {

    private UserStore userStore;

    @BeforeEach
    void setUp() {
        userStore = new InMemoryUserStore();
    }

    // === Positive Cases ===

    // Test to verify user creation with a UUID
    @Test
    void shouldCreateUserWithUUID() {
        User user = new User(UUID.randomUUID(), "John Doe", "john@example.com");
        User created = userStore.create(user);

        assertNotNull(created.id());
        assertEquals("John Doe", created.name());
        assertEquals("john@example.com", created.email());
    }

    // Test to verify finding a user by their UUID
    @Test
    void shouldFindUserById() {
        User user = new User(UUID.randomUUID(), "Jane Smith", "jane@example.com");
        User created = userStore.create(user);

        User found = userStore.findById(created.id()).orElseThrow();

        assertEquals(created.id(), found.id());
        assertEquals("Jane Smith", found.name());
    }

    // Test to verify updating a user's details
    @Test
    void shouldUpdateUser() throws UserNotFoundException {
        User user = new User(UUID.randomUUID(), "Bob", "bob@example.com");
        User created = userStore.create(user);

        User updated = new User(created.id(), "Bob", "newemail@example.com");
        userStore.update(created.id(), updated);

        User found = userStore.findById(created.id()).orElseThrow();
        assertEquals("newemail@example.com", found.email());
    }

    // Test to verify deleting a user by their UUID 
    @Test
    void shouldDeleteUser() throws UserNotFoundException {
        User user = new User(UUID.randomUUID(), "Alice", "alice@example.com");
        User created = userStore.create(user);

        userStore.delete(created.id());

        assertTrue(userStore.findById(created.id()).isEmpty());
    }

    // === Negative Cases ===

    // Test to verify exception is thrown when updating a non-existent user
    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        UUID nonExistentId = UUID.randomUUID();

        assertThrows(UserNotFoundException.class, () ->
            userStore.update(nonExistentId, new User(nonExistentId, "Test", "test@example.com"))
        );
    }

    // Test to verify exception is thrown when deleting a non-existent user
    @Test
    void shouldThrowExceptionWhenDeletingNonExistentUser() {
        UUID nonExistentId = UUID.randomUUID();

        assertThrows(UserNotFoundException.class, () ->
            userStore.delete(nonExistentId)
        );
    }

    // === Concurrency Tests (Task 2: Thread-safety) ===

    // Test to verify thread-safe user creation
    @Test
    void shouldHandleConcurrentCreates() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    User user = new User(UUID.randomUUID(), "User" + index, "user" + index + "@example.com");
                    userStore.create(user);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(threadCount, userStore.findAll().size());
    }

    // Test to verify thread-safe user updates 
    @Test
    void shouldHandleConcurrentUpdates() throws InterruptedException, UserNotFoundException {
        User user = new User(UUID.randomUUID(), "Test User", "test@example.com");
        User created = userStore.create(user);

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    User updated = new User(created.id(), "Test User", "email" + index + "@example.com");
                    userStore.update(created.id(), updated);
                } catch (UserNotFoundException e) {
                    fail("User should exist");
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // User should still exist with one of the updated emails
        User finalUser = userStore.findById(created.id()).orElseThrow();
        assertNotNull(finalUser);
        assertTrue(finalUser.email().startsWith("email"));
    }
}
