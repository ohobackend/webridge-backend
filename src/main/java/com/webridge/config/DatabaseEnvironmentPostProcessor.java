package com.webridge.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class DatabaseEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String dbUrl = environment.getProperty("DATABASE_URL");
        if (dbUrl == null || dbUrl.isBlank()) {
            dbUrl = environment.getProperty("DATABASE_PUBLIC_URL");
        }

        Map<String, Object> props = new HashMap<>();

        if (dbUrl != null && (dbUrl.startsWith("postgres://") || dbUrl.startsWith("postgresql://"))) {
            try {
                String normalized = dbUrl.replaceFirst("^postgres://", "postgresql://");
                URI uri = URI.create(normalized);
                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                String path = uri.getPath();

                String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
                props.put("spring.datasource.url", jdbcUrl);

                if (uri.getUserInfo() != null) {
                    String[] userParts = uri.getUserInfo().split(":", 2);
                    props.put("spring.datasource.username", URLDecoder.decode(userParts[0], StandardCharsets.UTF_8));
                    if (userParts.length > 1) {
                        props.put("spring.datasource.password", URLDecoder.decode(userParts[1], StandardCharsets.UTF_8));
                    }
                }
            } catch (Exception ignored) {
            }
        }

        String redisUrl = environment.getProperty("REDIS_URL");
        if (redisUrl != null && !redisUrl.isBlank()) {
            props.put("spring.data.redis.url", redisUrl);
        }

        if (!props.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource("railwayPostgresRedisSource", props));
        }
    }
}
