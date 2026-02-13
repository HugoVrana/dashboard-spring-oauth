package com.dashboard.oauth.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Epic;
import net.datafaker.Faker;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import java.time.Duration;

@Tag("integration")
@Epic("Integration Tests")
@SpringBootTest
@AutoConfigureMockMvc
public abstract class BaseIntegrationTest {

    // 2. Define the container with a specific Log Message Wait Strategy
    @ServiceConnection
    static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .waitingFor(Wait.forLogMessage("(?i).*waiting for connections.*\\n", 1))
            .withStartupTimeout(Duration.ofMinutes(2)); // Give CI more time to start

    // 3. Start the container manually in a static block
    static {
        mongoDBContainer.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected static final Faker faker = new Faker();
}
