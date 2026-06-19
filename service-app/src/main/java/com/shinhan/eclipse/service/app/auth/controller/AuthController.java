package com.shinhan.eclipse.service.app.auth.controller;

import com.shinhan.eclipse.common.response.ApiResponse;
import com.shinhan.eclipse.service.app.auth.dto.SimpleLoginRequest;
import com.shinhan.eclipse.service.app.auth.dto.SimpleLoginResponse;
import com.shinhan.eclipse.service.app.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/simple-login")
    public ResponseEntity<ApiResponse<SimpleLoginResponse>> login(@RequestBody SimpleLoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }
}