package com.shinhan.eclipse.service.ipo.internal;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ledger-app-subscriptions", url = "${eclipse.ledger.url:http://localhost:8080}")
interface IpoLedgerClient {

    @GetMapping("/internal/subscriptions/exists")
    InternalApiEnvelope<SubscriptionExistsResponse> exists(@RequestHeader("X-User-Id") Long userId, @RequestParam("ipoId") Long ipoId);
}
