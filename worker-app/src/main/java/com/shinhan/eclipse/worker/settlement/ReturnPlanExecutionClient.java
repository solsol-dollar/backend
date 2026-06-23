package com.shinhan.eclipse.worker.settlement;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** ledger-app의 내부 전용 리턴 플랜 실행 API를 호출하는 클라이언트. */
@Component
public class ReturnPlanExecutionClient {

    private final RestClient restClient;

    public ReturnPlanExecutionClient(@Value("${eclipse.ledger.url:http://localhost:8080}") String ledgerBaseUrl) {
        this.restClient = RestClient.builder().baseUrl(ledgerBaseUrl).build();
    }

    public void execute(Long returnPlanId) {
        restClient.put()
                .uri("/internal/return-plans/{id}/execute", returnPlanId)
                .retrieve()
                .toBodilessEntity();
    }
}
