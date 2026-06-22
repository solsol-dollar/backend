package com.shinhan.eclipse.ledger.app.client;

import com.shinhan.eclipse.ledger.allocation.QuoteClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;

/** service-app(:8081)의 종목 상세(SEC-002) API를 호출해 실시간 시세를 가져온다. */
@Component
public class SecuritiesQuoteClient implements QuoteClient {

    private static final Logger log = LoggerFactory.getLogger(SecuritiesQuoteClient.class);

    private final RestClient serviceAppRestClient;

    public SecuritiesQuoteClient(@Qualifier("serviceAppRestClient") RestClient serviceAppRestClient) {
        this.serviceAppRestClient = serviceAppRestClient;
    }

    @Override
    public BigDecimal getCurrentPrice(Long productId) {
        try {
            RemoteApiResponse<ProductPriceDto> response = serviceAppRestClient.get()
                    .uri("/api/v1/securities/products/{id}", productId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return response == null || response.data() == null ? null : response.data().price();
        } catch (RestClientException e) {
            log.warn("시세 조회 실패: productId={}, message={}", productId, e.getMessage());
            return null;
        }
    }

    private record ProductPriceDto(BigDecimal price) {}

    private record RemoteApiResponse<T>(String code, String message, T data) {}
}
