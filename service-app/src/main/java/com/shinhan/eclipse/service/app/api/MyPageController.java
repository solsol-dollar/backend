package com.shinhan.eclipse.service.app.api;

import com.shinhan.eclipse.auth.AuthUser;
import com.shinhan.eclipse.common.response.ApiResponse;
import com.shinhan.eclipse.service.mypage.MyPageAccountsResponse;
import com.shinhan.eclipse.service.mypage.MyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;

    @GetMapping("/accounts")
    public ResponseEntity<ApiResponse<MyPageAccountsResponse>> getLinkedAccounts(
            @AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity.ok(ApiResponse.success(myPageService.getLinkedAccounts(authUser.userId())));
    }

    @PostMapping("/accounts/deposit")
    public ResponseEntity<ApiResponse<MyPageAccountsResponse.AccountItem>> createDeposit(
            @AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity.ok(ApiResponse.success(myPageService.createDepositAccount(authUser.userId())));
    }

    @PostMapping("/accounts/savings")
    public ResponseEntity<ApiResponse<MyPageAccountsResponse.AccountItem>> createSavings(
            @AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity.ok(ApiResponse.success(myPageService.createSavingsAccount(authUser.userId())));
    }

    @PostMapping("/cards")
    public ResponseEntity<ApiResponse<MyPageAccountsResponse.CardItem>> createCard(
            @AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity.ok(ApiResponse.success(myPageService.createCard(authUser.userId())));
    }
}