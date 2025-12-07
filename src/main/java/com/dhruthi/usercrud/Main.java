package com.dhruthi.usercrud;

import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Main class the heart of the application, triggering the Vert.x verticle deployment 
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

// Initializes Vert.x and deploy the UserVerticle and handles success and failures of deployment
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        // Deploy UserVerticle
        vertx.deployVerticle(new UserVerticle())
            .onSuccess(id -> {
                logger.info("UserVerticle deployed successfully with ID: {}", id);
                logger.info("Application started successfully");
                addShutdownHook(vertx);
            })
            .onFailure(error -> {
                logger.error("Failed to deploy UserVerticle", error);
                vertx.close();
                System.exit(1);
            });
    }

    // Gracefull shutdow hook to close Vert.x instance on JVM termination
    private static void addShutdownHook(Vertx vertx) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown signal received, closing Vert.x instance...");
            vertx.close()
                .onSuccess(v -> logger.info("Vert.x instance closed successfully"))
                .onFailure(error -> logger.error("Error closing Vert.x instance", error));
        }));
    }
}
