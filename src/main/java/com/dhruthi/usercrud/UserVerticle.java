package com.dhruthi.usercrud;

import com.dhruthi.usercrud.model.User;
import com.dhruthi.usercrud.model.UserNotFoundException;
import com.dhruthi.usercrud.store.InMemoryUserStore;
import com.dhruthi.usercrud.store.UserStore;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.regex.Pattern;


// This verticle sets up the HTTP server and defines the RESTful API endpoints for User CRUD operations
public class UserVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(UserVerticle.class);
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final int PORT = 8080;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@]+@[^@]+\\.[^@]+$");

    private final UserStore userStore;

    // Constructor to initialize the UserStore, defaulting to InMemoryUserStore
    public UserVerticle(UserStore userStore) {
        this.userStore = userStore;
    }

    public UserVerticle() {
        this(new InMemoryUserStore());
    }

    @Override
    // Start the verticle and set up the HTTP server with routes
    public void start(Promise<Void> startPromise) {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        router.post("/users").handler(this::createUser);
        router.get("/users/:id").handler(this::getUserById);
        router.put("/users/:id/email").handler(this::updateUserEmail);
        router.delete("/users/:id").handler(this::deleteUser);

        router.errorHandler(500, this::handleError);

        HttpServer server = vertx.createHttpServer();
        server.requestHandler(router)
            .listen(PORT)
            .onSuccess(http -> {
                logger.info("HTTP server started on port {}", PORT);
                startPromise.complete();
            })
            .onFailure(error -> {
                logger.error("Failed to start HTTP server", error);
                startPromise.fail(error);
            });
    }

    // Hanlder to create a new User
    private void createUser(RoutingContext ctx) {
        try {
            JsonObject body = ctx.body().asJsonObject();
            if (body == null) {
                sendError(ctx, 400, "Request body must be valid JSON");
                return;
            }

            String name = body.getString("name");
            String email = body.getString("email");

            if (name == null || name.trim().isEmpty()) {
                sendError(ctx, 400, "Name is required and cannot be empty");
                return;
            }

            if (email == null || email.trim().isEmpty()) {
                sendError(ctx, 400, "Email is required and cannot be empty");
                return;
            }

            if (!isValidEmail(email)) {
                sendError(ctx, 400, "Invalid email format");
                return;
            }

            User user = new User(UUID.randomUUID(), name.trim(), email.trim());
            User created = userStore.create(user);

            JsonObject response = toJson(created);

            ctx.response()
                .setStatusCode(201)
                .putHeader("Content-Type", CONTENT_TYPE_JSON)
                .putHeader("Location", "/users/" + created.id())
                .end(response.encode());

            logger.info("Created user: {}", created.id());

        } catch (Exception e) {
            logger.error("Error creating user", e);
            sendError(ctx, 500, "Internal server error");
        }
    }

    // Handler to get a User by their UUID
    private void getUserById(RoutingContext ctx) {
        try {
            UUID id = parseUUID(ctx.pathParam("id"));
            if (id == null) {
                sendError(ctx, 400, "Invalid UUID format");
                return;
            }

            User user = userStore.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", CONTENT_TYPE_JSON)
                .end(toJson(user).encode());

        } catch (UserNotFoundException e) {
            sendError(ctx, 404, e.getMessage());
        } catch (Exception e) {
            logger.error("Error retrieving user", e);
            sendError(ctx, 500, "Internal server error");
        }
    }

    // Handler to update a User's email by their UUID
    private void updateUserEmail(RoutingContext ctx) {
        try {
            UUID id = parseUUID(ctx.pathParam("id"));
            if (id == null) {
                sendError(ctx, 400, "Invalid UUID format");
                return;
            }

            JsonObject body = ctx.body().asJsonObject();
            if (body == null) {
                sendError(ctx, 400, "Request body must be valid JSON");
                return;
            }

            String email = body.getString("email");
            if (email == null || email.trim().isEmpty()) {
                sendError(ctx, 400, "Email is required and cannot be empty");
                return;
            }

            if (!isValidEmail(email)) {
                sendError(ctx, 400, "Invalid email format");
                return;
            }

            User existing = userStore.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

            User updated = new User(id, existing.name(), email.trim());
            userStore.update(id, updated);

            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", CONTENT_TYPE_JSON)
                .end(toJson(updated).encode());

            logger.info("Updated email for user: {}", id);

        } catch (UserNotFoundException e) {
            sendError(ctx, 404, e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating user email", e);
            sendError(ctx, 500, "Internal server error");
        }
    }

    // Handler to delete a User by their UUID
    private void deleteUser(RoutingContext ctx) {
        try {
            UUID id = parseUUID(ctx.pathParam("id"));
            if (id == null) {
                sendError(ctx, 400, "Invalid UUID format");
                return;
            }

            userStore.delete(id);

            ctx.response()
                .setStatusCode(204)
                .end();

            logger.info("Deleted user: {}", id);

        } catch (UserNotFoundException e) {
            sendError(ctx, 404, e.getMessage());
        } catch (Exception e) {
            logger.error("Error deleting user", e);
            sendError(ctx, 500, "Internal server error");
        }
    }

    private UUID parseUUID(String uuidString) {
        try {
            return UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    private JsonObject toJson(User user) {
        return new JsonObject()
            .put("id", user.id().toString())
            .put("name", user.name())
            .put("email", user.email());
    }

    // Helper method to send error responses in JSON format
    private void sendError(RoutingContext ctx, int statusCode, String message) {
        JsonObject error = new JsonObject()
            .put("error", message)
            .put("status", statusCode);

        ctx.response()
            .setStatusCode(statusCode)
            .putHeader("Content-Type", CONTENT_TYPE_JSON)
            .end(error.encode());
    }

    // Global error handler for unhandled exceptions
    private void handleError(RoutingContext ctx) {
        Throwable failure = ctx.failure();
        logger.error("Unhandled error in request", failure);
        sendError(ctx, 500, "Internal server error");
    }
}
