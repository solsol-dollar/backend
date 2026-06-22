package com.shinhan.eclipse.auth.filter;

import com.shinhan.eclipse.auth.AuthUser;
import com.shinhan.eclipse.auth.jwt.JwtTokenVerifier;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenVerifier verifier;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                AuthUser user = verifier.verify(header.substring(7));
                var auth = new UsernamePasswordAuthenticationToken(
                        user, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.role())));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ignored) {
                // 유효하지 않은 토큰 → SecurityContext 비워둠 → 인증 필요 엔드포인트에서 403 반환
            }
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
}