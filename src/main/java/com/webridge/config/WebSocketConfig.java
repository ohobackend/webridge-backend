package com.webridge.config;

import com.webridge.scan.progress.ScanWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Arrays;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ScanWebSocketHandler handler;
    private final WebridgeProperties props;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String allowedOrigins = props.corsAllowedOrigins() != null ? props.corsAllowedOrigins() : "*";
        String[] origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .toArray(String[]::new);

        registry.addHandler(handler, "/ws/scans/{scanId}", "/ws/scans/{scanId}/")
                .setAllowedOrigins(origins);
    }
}
