package com.shinhan.eclipse.ledger.app.api;

import com.shinhan.eclipse.common.response.ApiResponse;
import com.shinhan.eclipse.ledger.subscription.SubscriptionFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal/subscriptions")
@RequiredArgsConstructor
public class InternalSubscriptionController {

    private final SubscriptionFacade subscriptionFacade;

    @GetMapping("/exists")
    public ResponseEntity<ApiResponse<Map<String, Object>>> exists(
            @RequestParam Long userId, @RequestParam Long ipoId) {
        boolean alreadySubscribed = subscriptionFacade.isAlreadySubscribed(userId, ipoId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("alreadySubscribed", alreadySubscribed)));
    }
}
