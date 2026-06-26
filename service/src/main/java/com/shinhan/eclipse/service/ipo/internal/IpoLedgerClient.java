package com.shinhan.eclipse.service.ipo.internal;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ledger-app-subscriptions", url = "${eclipse.ledger.url:http://localhost:8080}")
public interface IpoLedgerClient {

    @GetMapping("/internal/subscriptions/exists")
    InternalApiEnvelope<SubscriptionExistsResponse> exists(@RequestParam("userId") Long userId, @RequestParam("ipoId") Long ipoId);
}
