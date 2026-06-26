package com.shinhan.eclipse.service.exchange.internal;

import com.shinhan.eclipse.common.redis.exchange.ExchangeRateInfo;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * 한국수출입은행 환율 API 클라이언트.
 * API 호출 실패(네트워크 오류, 4xx/5xx) 시 Optional.empty()를 반환하여
 * 호출자가 캐시 폴백을 처리할 수 있도록 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class ExchangeRateApiClient {

    private static final String            DATA_TYPE      = "AP01";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final ZoneId            KST            = ZoneId.of("Asia/Seoul");
    private static final Duration          TIMEOUT        = Duration.ofSeconds(10);

    private final ExchangeProperties props;
    private WebClient webClient;

    // 수출입은행은 한국 공인인증서(KISA) 사용 — JVM 기본 truststore 미포함으로 SSL 검증 우회
    @PostConstruct
    void init() {
        try {
            SslContext sslContext = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
            HttpClient httpClient = HttpClient.create()
                    .responseTimeout(TIMEOUT)
                    .secure(spec -> spec.sslContext(sslContext));
            this.webClient = WebClient.builder()
                    .baseUrl(props.getBaseUrl())
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .build();
        } catch (SSLException e) {
            throw new IllegalStateException("WebClient SSL 설정 실패", e);
        }
    }

    /**
     * 전체 통화 목록 환율 조회. 일일 제한 1회 소모.
     */
    Optional<List<ExchangeRateInfo>> fetchAll() {
        return fetchAll(LocalDate.now(KST));
    }

    Optional<List<ExchangeRateInfo>> fetchAll(LocalDate date) {
        try {
            List<ExchangeRateApiDto> dtos = webClient
                    .get()
                    .uri(uri -> uri
                            .path("/site/program/financial/exchangeJSON")
                            .queryParam("authkey", props.getAuthKey())
                            .queryParam("searchdate", date.format(DATE_FORMATTER))
                            .queryParam("data", DATA_TYPE)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<ExchangeRateApiDto>>() {})
                    .block(TIMEOUT);

            if (dtos == null || dtos.isEmpty()) {
                log.warn("수출입은행 API 응답이 비어 있습니다 (주말·공휴일 가능성) date={}", date);
                return Optional.empty();
            }

            List<ExchangeRateInfo> rates = dtos.stream()
                    .filter(ExchangeRateApiDto::isSuccess)
                    .map(ExchangeRateApiDto::toInfo)
                    .toList();

            if (rates.isEmpty()) {
                int firstResult = dtos.getFirst().getResult();
                log.warn("수출입은행 API 오류 코드: {} date={}", firstResult, date);
                return Optional.empty();
            }

            return Optional.of(rates);

        } catch (WebClientException e) {
            log.error("수출입은행 API 네트워크 오류: {}", e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("수출입은행 API 호출 중 예외 발생: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 특정 통화 환율 조회 (전체 호출 후 필터링 — API가 단일 통화 조회를 미지원).
     */
    Optional<ExchangeRateInfo> fetchOne(String currencyCode) {
        return fetchAll().flatMap(list -> list.stream()
                .filter(r -> currencyCode.equalsIgnoreCase(r.currencyCode()))
                .findFirst());
    }

    Optional<ExchangeRateInfo> fetchOne(String currencyCode, LocalDate date) {
        return fetchAll(date).flatMap(list -> list.stream()
                .filter(r -> currencyCode.equalsIgnoreCase(r.currencyCode()))
                .findFirst());
    }
}
