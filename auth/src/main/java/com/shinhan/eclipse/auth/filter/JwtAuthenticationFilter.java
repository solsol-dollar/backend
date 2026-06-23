package com.shinhan.eclipse.auth.filter;

import com.shinhan.eclipse.auth.AuthUser;
import com.shinhan.eclipse.auth.jwt.JwtTokenVerifier;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String COOKIE_NAME = "access_token";

    private final JwtTokenVerifier verifier;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = extractToken(request);

        if (token != null) {
            try {
                AuthUser user = verifier.verify(token);
                var auth = new UsernamePasswordAuthenticationToken(
                        user, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.role())));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ignored) {}
        } else {
            // 개발 편의: X-User-Id 헤더로 mock 인증
            String userId = request.getHeader("X-User-Id");
            if (userId != null) {
                try {
                    AuthUser user = new AuthUser(Long.parseLong(userId), "mock-user", "USER");
                    var auth = new UsernamePasswordAuthenticationToken(
                            user, null,
                            List.of(new SimpleGrantedAuthority("ROLE_USER")));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } catch (NumberFormatException ignored) {}
            }
        }
        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        // 1순위: 쿠키 (브라우저 → service-app)
        String fromCookie = extractFromCookie(request);
        if (fromCookie != null) return fromCookie;

        // 2순위: Authorization 헤더 (service-app → ledger-app 서버 간 호출)
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) return header.substring(7);

        return null;
    }

    private String extractFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        return Arrays.stream(cookies)
                .filter(c -> COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}