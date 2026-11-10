package com.webridge.config;

import com.webridge.auth.jwt.JwtAuthenticationFilter;
import com.webridge.auth.password.DjangoPbkdf2PasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsUtils;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final WebridgeProperties props;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/v1/ping", "/api/v1/ping/**", "/api/v1/health", "/api/v1/health/**").permitAll()
                .requestMatchers(
                    "/api/v1/auth/register", "/api/v1/auth/register/**",
                    "/api/v1/auth/login", "/api/v1/auth/login/**",
                    "/api/v1/auth/social-login", "/api/v1/auth/social-login/**",
                    "/api/v1/auth/password-reset", "/api/v1/auth/password-reset/**",
                    "/api/v1/auth/password-reset-confirm", "/api/v1/auth/password-reset-confirm/**",
                    "/api/v1/auth/token/refresh", "/api/v1/auth/token/refresh/**",
                    "/api/v1/auth/request-email-verification", "/api/v1/auth/request-email-verification/**",
                    "/api/v1/auth/verify-email-link", "/api/v1/auth/verify-email-link/**",
                    "/api/v1/auth/check-email-verification", "/api/v1/auth/check-email-verification/**"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/terms/current", "/api/v1/terms/current/**").permitAll()
                .requestMatchers("/api/schema/**", "/api/docs/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(e -> e.authenticationEntryPoint(
                (req, res, ex) -> {
                    res.setStatus(401);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"detail\":\"자격 인증데이터(authentication credentials)가 제공되지 않았습니다.\"}");
                }
            ))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new DjangoPbkdf2PasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var c = new CorsConfiguration();
        String allowedOrigins = props.corsAllowedOrigins() != null ? props.corsAllowedOrigins() : "http://localhost:3000";
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
        c.setAllowedOriginPatterns(origins);
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("*"));
        c.setAllowCredentials(true);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", c);
        return source;
    }
}
