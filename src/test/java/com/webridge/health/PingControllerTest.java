package com.webridge.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("schematest")
class PingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/ping/ 호출 시 200 OK와 pong 메시지, X-Request-ID 헤더를 반환해야 한다")
    void pingWithTrailingSlash() throws Exception {
        mockMvc.perform(get("/api/v1/ping/").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(header().exists("X-Request-ID"))
            .andExpect(jsonPath("$.message").value("pong"))
            .andExpect(jsonPath("$.server").value("WEBridge-api"))
            .andExpect(jsonPath("$.version").value("1.0.0"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("GET /api/v1/ping 호출 시 (슬래시 없이)도 200 OK를 반환해야 한다")
    void pingWithoutTrailingSlash() throws Exception {
        mockMvc.perform(get("/api/v1/ping").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(header().exists("X-Request-ID"))
            .andExpect(jsonPath("$.message").value("pong"))
            .andExpect(jsonPath("$.server").value("WEBridge-api"))
            .andExpect(jsonPath("$.version").value("1.0.0"));
    }

    @Test
    @DisplayName("GET /api/v1/health/ 호출 시 200 OK와 healthy 상태를 반환해야 한다")
    void healthCheck() throws Exception {
        mockMvc.perform(get("/api/v1/health/").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("healthy"))
            .andExpect(jsonPath("$.service").value("WEBridge-drf"));
    }
}
