package com.shinhan.eclipse.service.securities.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
class KisRestClient {

    private static final String EXCD_NAS = "NAS";
    private static final String EXCD_NYS = "NYS";

    private final KisProperties   props;
    private final KisTokenManager tokenManager;

    private WebClient client() {
        return WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader("content-type", "application/json; charset=utf-8")
                .defaultHeader("appkey",    props.getAppKey())
                .defaultHeader("appsecret", props.getAppSecret())
                .defaultHeader("custtype",  "P")
                .build();
    }

    private String excdOf(String exchangeName) {
        if (exchangeName != null && exchangeName.contains("NYSE")) return EXCD_NYS;
        return EXCD_NAS;
    }

    /**
     * HHDFS76200200 — 해외주식 현재가상세.
     */
    Optional<KisQuoteDto.PriceDetailResponse> getCurrentPrice(String ticker, String exchangeName) {
        String excd = excdOf(exchangeName);
        String uri = UriComponentsBuilder
                .fromPath("/uapi/overseas-price/v1/quotations/price-detail")
                .queryParam("AUTH", "")
                .queryParam("EXCD", excd)
                .queryParam("SYMB", ticker)
                .build()
                .toUriString();

        try {
            KisQuoteDto.PriceDetailResponse result = client()
                    .get()
                    .uri(uri)
                    .header("authorization", "Bearer " + tokenManager.getAccessToken())
                    .header("tr_id", "HHDFS76200200")
                    .retrieve()
                    .bodyToMono(KisQuoteDto.PriceDetailResponse.class)
                    .block();

            if (result != null && !result.isSuccess()) {
                log.warn("KIS 현재가 조회 실패 [{}] rt_cd={} msg={}", ticker, result.getRtCd(), result.getMsg1());
                return Optional.empty();
            }
            return Optional.ofNullable(result);
        } catch (WebClientResponseException e) {
            log.warn("KIS 현재가 HTTP 오류 [{}] status={} body={}", ticker, e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 401) tokenManager.invalidate();
            return Optional.empty();
        } catch (Exception e) {
            log.warn("KIS 현재가 조회 오류 [{}]: {}", ticker, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * HHDFS76200100 — 해외주식 현재가 호가 (10호가).
     */
    Optional<KisQuoteDto.AskingPriceResponse> getOrderBook(String ticker, String exchangeName) {
        String excd = excdOf(exchangeName);
        String uri = UriComponentsBuilder
                .fromPath("/uapi/overseas-price/v1/quotations/inquire-asking-price")
                .queryParam("AUTH", "")
                .queryParam("EXCD", excd)
                .queryParam("SYMB", ticker)
                .build()
                .toUriString();

        try {
            KisQuoteDto.AskingPriceResponse result = client()
                    .get()
                    .uri(uri)
                    .header("authorization", "Bearer " + tokenManager.getAccessToken())
                    .header("tr_id", "HHDFS76200100")
                    .retrieve()
                    .bodyToMono(KisQuoteDto.AskingPriceResponse.class)
                    .block();

            if (result != null && !result.isSuccess()) {
                log.warn("KIS 호가 조회 실패 [{}] rt_cd={} msg={}", ticker, result.getRtCd(), result.getMsg1());
                return Optional.empty();
            }
            return Optional.ofNullable(result);
        } catch (WebClientResponseException e) {
            log.warn("KIS 호가 HTTP 오류 [{}] status={} body={}", ticker, e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 401) tokenManager.invalidate();
            return Optional.empty();
        } catch (Exception e) {
            log.warn("KIS 호가 조회 오류 [{}]: {}", ticker, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * HHDFS76950200 — 해외주식 분봉조회 (5분봉).
     * PINC=1(전일포함), NREC=120(최대 건수).
     */
    Optional<KisChartDto.MinuteChartResponse> getMinuteCandles(String ticker, String exchangeName) {
        String excd = excdOf(exchangeName);
        String uri = UriComponentsBuilder
                .fromPath("/uapi/overseas-price/v1/quotations/inquire-time-itemchartprice")
                .queryParam("AUTH",  "")
                .queryParam("EXCD",  excd)
                .queryParam("SYMB",  ticker)
                .queryParam("NMIN",  "5")
                .queryParam("PINC",  "1")
                .queryParam("NEXT",  "")
                .queryParam("NREC",  "120")
                .queryParam("FILL",  "")
                .queryParam("KEYB",  "")
                .build()
                .toUriString();

        try {
            KisChartDto.MinuteChartResponse result = client()
                    .get()
                    .uri(uri)
                    .header("authorization", "Bearer " + tokenManager.getAccessToken())
                    .header("tr_id", "HHDFS76950200")
                    .retrieve()
                    .bodyToMono(KisChartDto.MinuteChartResponse.class)
                    .block();

            if (result != null && !result.isSuccess()) {
                log.warn("KIS 분봉 조회 실패 [{}] rt_cd={} msg={}", ticker, result.getRtCd(), result.getMsg1());
                return Optional.empty();
            }
            return Optional.ofNullable(result);
        } catch (WebClientResponseException e) {
            log.warn("KIS 분봉 HTTP 오류 [{}] status={} body={}", ticker, e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 401) tokenManager.invalidate();
            return Optional.empty();
        } catch (Exception e) {
            log.warn("KIS 분봉 조회 오류 [{}]: {}", ticker, e.getMessage());
            return Optional.empty();
        }
    }
}
