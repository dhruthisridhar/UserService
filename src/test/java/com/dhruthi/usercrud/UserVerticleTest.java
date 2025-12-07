package com.dhruthi.usercrud;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(VertxExtension.class)
class UserVerticleTest {

    private static final int TEST_PORT = 8080;
    private WebClient webClient;

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext testContext) {
        webClient = WebClient.create(vertx);
        vertx.deployVerticle(new UserVerticle())
            .onComplete(testContext.succeedingThenComplete());
    }

    @AfterEach
    void tearDown(Vertx vertx, VertxTestContext testContext) {
        vertx.close().onComplete(testContext.succeedingThenComplete());
    }

    // === POST /users ===

    @Test
    void shouldCreateUser(Vertx vertx, VertxTestContext testContext) {
        JsonObject requestBody = new JsonObject()
            .put("name", "John Doe")
            .put("email", "john@example.com");

        webClient.post(TEST_PORT, "localhost", "/users")
            .sendJsonObject(requestBody)
            .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
                assertEquals(201, response.statusCode());
                assertTrue(response.headers().contains("Location"));

                JsonObject body = response.bodyAsJsonObject();
                assertNotNull(body.getString("id"));
                assertEquals("John Doe", body.getString("name"));
                assertEquals("john@example.com", body.getString("email"));

                testContext.completeNow();
            })));
    }

    @Test
    void shouldRejectInvalidEmail(Vertx vertx, VertxTestContext testContext) {
        JsonObject requestBody = new JsonObject()
            .put("name", "John Doe")
            .put("email", "invalid-email");

        webClient.post(TEST_PORT, "localhost", "/users")
            .sendJsonObject(requestBody)
            .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
                assertEquals(400, response.statusCode());
                testContext.completeNow();
            })));
    }

    // === GET /users/:id ===

    @Test
    void shouldGetUserById(Vertx vertx, VertxTestContext testContext) {
        // First create a user
        JsonObject createRequest = new JsonObject()
            .put("name", "Jane Smith")
            .put("email", "jane@example.com");

        webClient.post(TEST_PORT, "localhost", "/users")
            .sendJsonObject(createRequest)
            .compose(createResponse -> {
                String userId = createResponse.bodyAsJsonObject().getString("id");
                return webClient.get(TEST_PORT, "localhost", "/users/" + userId).send();
            })
            .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
                assertEquals(200, response.statusCode());
                JsonObject body = response.bodyAsJsonObject();
                assertEquals("Jane Smith", body.getString("name"));
                testContext.completeNow();
            })));
    }

    @Test
    void shouldReturn404WhenUserNotFound(Vertx vertx, VertxTestContext testContext) {
        UUID nonExistentId = UUID.randomUUID();

        webClient.get(TEST_PORT, "localhost", "/users/" + nonExistentId)
            .send()
            .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
                assertEquals(404, response.statusCode());
                testContext.completeNow();
            })));
    }

    @Test
    void shouldReturn400ForInvalidUUID(Vertx vertx, VertxTestContext testContext) {
        webClient.get(TEST_PORT, "localhost", "/users/invalid-uuid")
            .send()
            .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
                assertEquals(400, response.statusCode());
                testContext.completeNow();
            })));
    }

    // === PUT /users/:id/email ===

    @Test
    void shouldUpdateEmail(Vertx vertx, VertxTestContext testContext) {
        JsonObject createRequest = new JsonObject()
            .put("name", "Bob Jones")
            .put("email", "bob@example.com");

        webClient.post(TEST_PORT, "localhost", "/users")
            .sendJsonObject(createRequest)
            .compose(createResponse -> {
                String userId = createResponse.bodyAsJsonObject().getString("id");
                JsonObject updateRequest = new JsonObject().put("email", "bob.jones@example.com");
                return webClient.put(TEST_PORT, "localhost", "/users/" + userId + "/email")
                    .sendJsonObject(updateRequest);
            })
            .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
                assertEquals(200, response.statusCode());
                JsonObject body = response.bodyAsJsonObject();
                assertEquals("bob.jones@example.com", body.getString("email"));
                testContext.completeNow();
            })));
    }

    // === DELETE /users/:id ===

    @Test
    void shouldDeleteUser(Vertx vertx, VertxTestContext testContext) {
        JsonObject createRequest = new JsonObject()
            .put("name", "Alice Brown")
            .put("email", "alice@example.com");

        webClient.post(TEST_PORT, "localhost", "/users")
            .sendJsonObject(createRequest)
            .compose(createResponse -> {
                String userId = createResponse.bodyAsJsonObject().getString("id");
                return webClient.delete(TEST_PORT, "localhost", "/users/" + userId)
                    .send()
                    .compose(deleteResponse -> {
                        testContext.verify(() -> assertEquals(204, deleteResponse.statusCode()));
                        // Verify user is deleted
                        return webClient.get(TEST_PORT, "localhost", "/users/" + userId).send();
                    });
            })
            .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
                assertEquals(404, response.statusCode());
                testContext.completeNow();
            })));
    }
}
