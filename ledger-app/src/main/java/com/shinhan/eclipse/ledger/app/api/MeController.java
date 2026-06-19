package com.shinhan.eclipse.ledger.app.api;

import com.shinhan.eclipse.auth.AuthUser;
import com.shinhan.eclipse.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> me(@AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "userId", authUser.userId(),
                "name", authUser.name(),
                "role", authUser.role()
        )));
    }
}
