package com.shinhan.eclipse.service.app.api;

import com.shinhan.eclipse.auth.AuthUser;
import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.common.response.ApiResponse;
import com.shinhan.eclipse.domain.user.User;
import com.shinhan.eclipse.service.app.auth.internal.UserRepository;
import com.shinhan.eclipse.service.mypage.MyPageAccountsResponse;
import com.shinhan.eclipse.service.mypage.MyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;
    private final UserRepository userRepository;

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

    @PostMapping("/investment-profile")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> completeInvestmentDiagnosis(
            @AuthenticationPrincipal AuthUser authUser) {
        User user = userRepository.findById(authUser.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        if ("COMPLETED".equals(user.getInvestmentStatus())) {
            return ResponseEntity.ok(ApiResponse.success(null));
        }
        user.completeInvestmentDiagnosis();
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}