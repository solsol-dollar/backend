package com.shinhan.eclipse.worker.candle.kis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

/**
 * Worker-app 전용 KIS REST 클라이언트.
 * HHDFS76240000 (일/주/월봉 배치 적재) 전용으로 사용한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkerKisRestClient {

    private static final String EXCD_NAS = "NAS";

    private final WorkerKisProperties   props;
    private final WorkerKisTokenManager tokenManager;

    private WebClient client() {
        return WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader("content-type", "application/json; charset=utf-8")
                .defaultHeader("appkey",    props.getAppKey())
                .defaultHeader("appsecret", props.getAppSecret())
                .defaultHeader("custtype",  "P")
                .build();
    }

    /**
     * HHDFS76240000 — 해외주식 기간별시세 조회.
     *
     * @param symb  종목코드 (예: "AAPL")
     * @param gubn  주기 구분 (0=일, 1=주, 2=월)
     * @param bymd  조회 기준일 (YYYYMMDD) — 해당 날짜부터 과거 최대 100건 반환
     * @param keyb  연속 조회 키 (최초 요청 시 "")
     */
    public Optional<KisDailyPriceResponse> getDailyCandles(String symb, String gubn,
                                                            String bymd, String keyb) {
        String uri = UriComponentsBuilder
                .fromPath("/uapi/overseas-price/v1/quotations/dailyprice")
                .queryParam("AUTH",  "")
                .queryParam("EXCD",  EXCD_NAS)
                .queryParam("SYMB",  symb)
                .queryParam("GUBN",  gubn)
                .queryParam("BYMD",  bymd)
                .queryParam("MODP",  "1")
                .queryParam("KEYB",  keyb != null ? keyb : "")
                .build()
                .toUriString();

        try {
            KisDailyPriceResponse result = client()
                    .get()
                    .uri(uri)
                    .header("authorization", "Bearer " + tokenManager.getAccessToken())
                    .header("tr_id", "HHDFS76240000")
                    .retrieve()
                    .bodyToMono(KisDailyPriceResponse.class)
                    .block();

            if (result != null && !result.isSuccess()) {
                log.warn("KIS 기간별시세 조회 실패 [{}] rt_cd={} msg={}", symb, result.getRtCd(), result.getMsg1());
                return Optional.empty();
            }
            return Optional.ofNullable(result);
        } catch (WebClientResponseException e) {
            log.warn("KIS 기간별시세 HTTP 오류 [{}] status={} body={}", symb, e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 401) tokenManager.invalidate();
            return Optional.empty();
        } catch (Exception e) {
            log.warn("KIS 기간별시세 조회 오류 [{}]: {}", symb, e.getMessage());
            return Optional.empty();
        }
    }
}
