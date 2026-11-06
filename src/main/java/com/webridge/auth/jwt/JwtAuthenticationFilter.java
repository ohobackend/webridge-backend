package com.webridge.auth.jwt;

import com.webridge.auth.dto.AuthUser;
import com.webridge.user.entity.User;
import com.webridge.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {
            try {
                Claims claims = jwtProvider.parse(token);
                String tokenType = jwtProvider.getTokenType(claims);
                String jti = jwtProvider.getJti(claims);

                if ("access".equals(tokenType) && !tokenBlacklistService.isBlacklisted(jti)) {
                    Long userId = jwtProvider.getUserId(claims);
                    if (userId != null) {
                        User user = userRepository.findById(userId).orElse(null);
                        if (user != null && user.isActive()) {
                            AuthUser authUser = AuthUser.from(user);
                            var authentication = new UsernamePasswordAuthenticationToken(
                                authUser,
                                null,
                                authUser.getAuthorities()
                            );
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            MDC.put("userId", String.valueOf(userId));
                        }
                    }
                }
            } catch (JwtException e) {
                log.debug("Invalid JWT token: {}", e.getMessage());
            } catch (Exception e) {
                log.warn("Failed to authenticate user via JWT: {}", e.getMessage());
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("userId");
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7).trim();
        }
        return null;
    }
}
