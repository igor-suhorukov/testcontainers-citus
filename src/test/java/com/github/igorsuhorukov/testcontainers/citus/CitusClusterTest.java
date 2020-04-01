package com.github.igorsuhorukov.testcontainers.citus;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
public class CitusClusterTest {

    private static final String CITUS_SERVICE = "master_1";
    private static final int CITUS_PORT = 5432;

    @Container
    private DockerComposeContainer container = new DockerComposeContainer(new File(
                CitusClusterTest.class.getResource("/docker-compose.yml").getFile()))
            .withExposedService(CITUS_SERVICE, CITUS_PORT,
                    Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(60)));

    @Test
    void setup() throws Exception{
        try (Connection connection = DriverManager.getConnection(getConnectionUrl(), "postgres", "")){
            try (Statement statement = connection.createStatement()){
                try (ResultSet resultSet = statement.executeQuery("SELECT * FROM master_get_active_worker_nodes()")){
                    boolean isNotEmpty=false;
                    while (resultSet.next()){
                        String node = resultSet.getString(1);
                        assertThat(node).contains("_worker_");
                        isNotEmpty=true;
                    }
                    assertThat(isNotEmpty).isTrue();
                }
            }
        }
    }

    private String getConnectionUrl() {
        Integer port = container.getServicePort(CITUS_SERVICE, CITUS_PORT);
        String serviceHost = container.getServiceHost(CITUS_SERVICE, CITUS_PORT);
        return String.format("jdbc:postgresql://%s:%d/", serviceHost, port);
    }
}
