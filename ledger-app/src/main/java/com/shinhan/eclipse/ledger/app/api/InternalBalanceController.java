package com.shinhan.eclipse.ledger.app.api;

import com.shinhan.eclipse.common.response.ApiResponse;
import com.shinhan.eclipse.ledger.balance.BalanceAdjustFacade;
import com.shinhan.eclipse.ledger.balance.BalanceAdjustRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/internal/balance")
@RequiredArgsConstructor
public class InternalBalanceController {

    private final BalanceAdjustFacade balanceAdjustFacade;

    @PostMapping("/adjust")
    public ResponseEntity<ApiResponse<Map<String, Object>>> adjust(
            @RequestBody BalanceAdjustRequest request) {
        BigDecimal balanceAfter = balanceAdjustFacade.adjust(request);
        return ResponseEntity.ok(ApiResponse.success(Map.of("balanceAfter", balanceAfter)));
    }
}
