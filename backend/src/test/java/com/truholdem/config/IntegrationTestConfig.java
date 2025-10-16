package com.truholdem.config;

import com.truholdem.TestConstants;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.mockito.Mockito.mock;


@TestConfiguration
@Profile("integration")
public class IntegrationTestConfig {

    
    
    
    
    
    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>(
            DockerImageName.parse(TestConstants.POSTGRES_IMAGE))
        .withDatabaseName("truholdem_test")
        .withUsername("test")
        .withPassword("test")
        .withStartupTimeout(Duration.ofSeconds(60))
        .withReuse(true);  
    
    
    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> redisContainer = new GenericContainer<>(
            DockerImageName.parse(TestConstants.REDIS_IMAGE))
        .withExposedPorts(6379)
        .waitingFor(Wait.forListeningPort())
        .withStartupTimeout(Duration.ofSeconds(30))
        .withReuse(true);

    
    
    
    
    static {
        
        
        postgresContainer.start();
        redisContainer.start();
        
        
        System.setProperty("spring.datasource.url", postgresContainer.getJdbcUrl());
        System.setProperty("spring.datasource.username", postgresContainer.getUsername());
        System.setProperty("spring.datasource.password", postgresContainer.getPassword());
        System.setProperty("spring.datasource.driver-class-name", "org.postgresql.Driver");
        
        
        System.setProperty("spring.data.redis.host", redisContainer.getHost());
        System.setProperty("spring.data.redis.port", 
            String.valueOf(redisContainer.getMappedPort(6379)));
    }

    
    
    
    
    
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisContainer.getHost());
        config.setPort(redisContainer.getMappedPort(6379));
        return new LettuceConnectionFactory(config);
    }
    
    
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }

    
    
    
    
    
    @Bean
    @Primary
    public SimpMessagingTemplate simpMessagingTemplate() {
        return mock(SimpMessagingTemplate.class);
    }
    
    
    @Bean
    @Primary
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    
    
    
    
    
    public static String getPostgresJdbcUrl() {
        return postgresContainer.getJdbcUrl();
    }
    
    
    public static String getRedisHost() {
        return redisContainer.getHost();
    }
    
    
    public static int getRedisPort() {
        return redisContainer.getMappedPort(6379);
    }
    
    
    public static boolean areContainersHealthy() {
        return postgresContainer.isRunning() && redisContainer.isRunning();
    }
    
    
    public static String getContainerDiagnostics() {
        return String.format(
            """
            === Testcontainers Diagnostics ===
            PostgreSQL:
              - Running: %s
              - JDBC URL: %s
              - Username: test
            Redis:
              - Running: %s
              - Host: %s
              - Port: %d
            """,
            postgresContainer.isRunning(),
            postgresContainer.getJdbcUrl(),
            redisContainer.isRunning(),
            redisContainer.getHost(),
            redisContainer.getMappedPort(6379)
        );
    }
}
