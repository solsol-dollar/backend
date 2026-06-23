package com.shinhan.eclipse.ledger.accountlink;

import com.shinhan.eclipse.common.resolver.UserHeader;
import com.shinhan.eclipse.common.response.ApiResponse;
import com.shinhan.eclipse.domain.account.FinancialAccount;
import com.shinhan.eclipse.ledger.accountlink.dto.AccountRes;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 계좌 잔액/예금주명 조회 (명세 외 추가) — 청약 신청 화면 진입 시 필요. */
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountLinkService accountLinkService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountRes>>> getAccounts(@UserHeader Long userId) {
        String holderName = accountLinkService.getAccountHolderName(userId);
        List<AccountRes> accounts = accountLinkService.getLinkedAccounts(userId).stream()
                .map(account -> AccountRes.of(account, holderName))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<ApiResponse<AccountRes>> getAccount(
            @UserHeader Long userId,
            @PathVariable("accountId") Long accountId) {
        FinancialAccount account = accountLinkService.getLinkedAccount(userId, accountId);
        String holderName = accountLinkService.getAccountHolderName(userId);
        return ResponseEntity.ok(ApiResponse.success(AccountRes.of(account, holderName)));
    }
}
