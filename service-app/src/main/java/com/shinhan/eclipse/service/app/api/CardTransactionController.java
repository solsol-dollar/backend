package com.shinhan.eclipse.service.app.api;

import com.shinhan.eclipse.auth.AuthUser;
import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.common.response.ApiResponse;
import com.shinhan.eclipse.service.card.CardService;
import com.shinhan.eclipse.service.card.CardTransactionSummary;
import com.shinhan.eclipse.service.card.CardTransactionsByCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/card/transactions")
@RequiredArgsConstructor
public class CardTransactionController {

    private final CardService cardService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<CardTransactionSummary>> getMonthlySummary(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        LocalDate now = LocalDate.now();
        int targetYear  = (year  != null) ? year  : now.getYear();
        int targetMonth = (month != null) ? month : now.getMonthValue();

        if (targetMonth < 1 || targetMonth > 12) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "month는 1~12 사이여야 합니다.");
        }

        return ResponseEntity.ok(ApiResponse.success(
                cardService.getMonthlySummary(authUser.userId(), targetYear, targetMonth)));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<CardTransactionsByCategory>> getByCategory(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String category) {

        LocalDate now = LocalDate.now();
        int targetYear  = (year  != null) ? year  : now.getYear();
        int targetMonth = (month != null) ? month : now.getMonthValue();

        if (targetMonth < 1 || targetMonth > 12) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "month는 1~12 사이여야 합니다.");
        }

        return ResponseEntity.ok(ApiResponse.success(
                cardService.getByCategory(authUser.userId(), targetYear, targetMonth, category)));
    }
}