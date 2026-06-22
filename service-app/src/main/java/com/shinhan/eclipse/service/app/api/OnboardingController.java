package com.shinhan.eclipse.service.app.api;

import com.shinhan.eclipse.auth.AuthUser;
import com.shinhan.eclipse.common.response.ApiResponse;
import com.shinhan.eclipse.service.app.auth.dto.OnboardingAccountsResponse;
import com.shinhan.eclipse.service.app.auth.service.OnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @GetMapping("/accounts")
    public ResponseEntity<ApiResponse<OnboardingAccountsResponse>> getAvailableAccounts(
            @AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity.ok(ApiResponse.success(
                onboardingService.getAvailableAccounts(authUser.userId())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> complete(@AuthenticationPrincipal AuthUser authUser) {
        onboardingService.complete(authUser.userId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}