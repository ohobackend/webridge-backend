package com.webridge.scan.progress;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webridge.scan.entity.AccessibilityScan;
import com.webridge.scan.repository.AccessibilityScanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScanWebSocketHandler extends TextWebSocketHandler {

    private final Map<UUID, Set<WebSocketSession>> groups = new ConcurrentHashMap<>();
    private final AccessibilityScanRepository scanRepo;
    private final ObjectMapper om;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        UUID scanId = extractScanId(session.getUri());
        if (scanId == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        Optional<AccessibilityScan> scanOpt = scanRepo.findById(scanId);
        if (scanOpt.isEmpty()) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        AccessibilityScan scan = scanOpt.get();
        groups.computeIfAbsent(scanId, k -> ConcurrentHashMap.newKeySet()).add(session);

        // Send current progress immediately upon connect
        Map<String, Object> data = Map.of(
                "scan_id", scanId.toString(),
                "status", scan.getStatus().getValue(),
                "progress", scan.getProgress(),
                "message", scan.getStatus().getValue() + " - " + scan.getProgress() + "%"
        );
        send(session, Map.of("type", "scan_progress", "data", data));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        groups.values().forEach(set -> set.remove(session));
    }

    public void broadcast(UUID scanId, Map<String, Object> data) {
        Set<WebSocketSession> sessions = groups.get(scanId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        Map<String, Object> message = Map.of("type", "scan_progress", "data", data);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                send(session, message);
            }
        }
    }

    public void broadcastError(UUID scanId, Map<String, Object> data) {
        Set<WebSocketSession> sessions = groups.get(scanId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        Map<String, Object> message = Map.of("type", "scan_error", "data", data);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                send(session, message);
            }
        }
    }

    private void send(WebSocketSession session, Object payload) {
        try {
            String json = om.writeValueAsString(payload);
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (IOException e) {
            log.warn("Failed to send WebSocket message: {}", e.getMessage());
        }
    }

    private UUID extractScanId(URI uri) {
        if (uri == null) return null;
        String path = uri.getPath();
        if (path == null) return null;
        // /ws/scans/{scanId} or /ws/scans/{scanId}/
        String[] parts = path.split("/");
        for (int i = 0; i < parts.length; i++) {
            if ("scans".equals(parts[i]) && i + 1 < parts.length) {
                try {
                    return UUID.fromString(parts[i + 1]);
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return null;
    }
}
