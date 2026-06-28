package com.shinhan.eclipse.ledger.app.api;

import com.shinhan.eclipse.auth.AuthUser;
import com.shinhan.eclipse.common.response.ApiResponse;
import com.shinhan.eclipse.ledger.transaction.TransactionHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionHistoryService transactionHistoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<TransactionHistoryService.TransactionPage>> getHistory(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam List<Long> accountId,
            @RequestParam(defaultValue = "ALL") String filter,
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(ApiResponse.success(
                transactionHistoryService.getHistory(authUser.userId(), accountId, filter, page)));
    }
}