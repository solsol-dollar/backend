package com.shinhan.eclipse.worker.settlement;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/** ledger-app의 내부 전용 리턴 플랜 실행 API를 호출하는 클라이언트. */
@Component
public class ReturnPlanExecutionClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private final RestClient restClient;
    private final String apiKey;

    public ReturnPlanExecutionClient(@Value("${eclipse.ledger.url:http://localhost:8080}") String ledgerBaseUrl,
                                      @Value("${eclipse.internal.api-key:}") String apiKey) {
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(
                ClientHttpRequestFactorySettings.DEFAULTS
                        .withConnectTimeout(CONNECT_TIMEOUT)
                        .withReadTimeout(READ_TIMEOUT));
        this.restClient = RestClient.builder().baseUrl(ledgerBaseUrl).requestFactory(requestFactory).build();
        this.apiKey = apiKey;
    }

    public void execute(Long returnPlanId) {
        restClient.put()
                .uri("/internal/return-plans/{id}/execute", returnPlanId)
                .header("X-Internal-Api-Key", apiKey)
                .retrieve()
                .toBodilessEntity();
    }
}
