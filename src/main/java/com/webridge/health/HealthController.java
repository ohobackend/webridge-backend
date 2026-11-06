package com.webridge.health;

import com.webridge.health.dto.PingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Health", description = "서버 상태 확인 API")
public class HealthController {

    @GetMapping({"/ping", "/ping/"})
    @Operation(summary = "Ping API", description = "서버 상태를 확인하는 간단한 ping-pong API입니다.")
    public ResponseEntity<PingResponse> ping() {
        return ResponseEntity.ok(PingResponse.ok());
    }

    @GetMapping({"/health", "/health/"})
    @Operation(summary = "헬스체크 API (GET)", description = "서버의 상태를 확인합니다.")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "WEBridge-drf",
            "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        ));
    }
}
