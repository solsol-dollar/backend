package com.shinhan.eclipse.service.securities.internal;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ledger-app", url = "${eclipse.ledger.url:http://localhost:8080}")
public interface LedgerApiClient {

    @PostMapping("/internal/balance/adjust")
    void adjust(@RequestBody LedgerAdjustRequest request);
}
