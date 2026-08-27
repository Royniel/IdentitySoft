package com.identity.identitysoft;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// Shared base for any test that needs a real Spring application context — without this, a plain
// @SpringBootTest falls back to the datasource URL in application.properties (localhost:5432),
// which only "works" on a machine that happens to have a local Postgres running for other
// reasons. That's exactly what happened here: green locally, failing on a clean CI runner.
@Testcontainers
public abstract class AbstractPostgresIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
}
