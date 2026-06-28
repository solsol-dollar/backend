package com.shinhan.eclipse.service.app.auth.controller;

import com.shinhan.eclipse.common.response.ApiResponse;
import com.shinhan.eclipse.service.app.auth.dto.SimpleLoginRequest;
import com.shinhan.eclipse.service.app.auth.dto.SimpleLoginResponse;
import com.shinhan.eclipse.service.app.auth.service.AuthService;
import com.shinhan.eclipse.service.app.auth.service.AuthService.LoginResult;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    static final String COOKIE_NAME = "access_token";

    private final AuthService authService;

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    @PostMapping("/simple-login")
    public ResponseEntity<ApiResponse<SimpleLoginResponse>> login(
            @Valid @RequestBody SimpleLoginRequest request,
            HttpServletResponse response) {
        LoginResult result = authService.login(request);

        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(result.token(), result.expiresInSeconds()));

        return ResponseEntity.ok(ApiResponse.success(new SimpleLoginResponse(result.userId(), result.onboardingStatus())));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("", 0));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private String buildCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .sameSite("Lax")
                .build()
                .toString();
    }
}