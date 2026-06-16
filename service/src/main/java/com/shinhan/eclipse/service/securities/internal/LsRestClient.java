package com.shinhan.eclipse.service.securities.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
class LsRestClient {

    private static final String EXCHCD_NASDAQ = "82";
    private static final String EXCHCD_NYSE   = "81";

    private final LsProperties    props;
    private final LsTokenManager  tokenManager;

    private WebClient client() {
        return WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader("Content-Type", "application/json;charset=UTF-8")
                .defaultHeader("tr_cont", "N")
                .defaultHeader("tr_cont_key", "")
                .defaultHeader("mac_address", "")
                .build();
    }

    private <T> Optional<T> post(String uri, String trCd, Map<String, Object> body, Class<T> type) {
        try {
            T result = client()
                    .post()
                    .uri(uri)
                    .header("Authorization", "Bearer " + tokenManager.getAccessToken())
                    .header("tr_cd", trCd)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(type)
                    .block();
            return Optional.ofNullable(result);
        } catch (WebClientResponseException e) {
            log.warn("LS API 호출 실패 [{}] status={} body={}", trCd, e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 401) tokenManager.invalidate();
            return Optional.empty();
        } catch (Exception e) {
            log.warn("LS API 호출 오류 [{}]: {}", trCd, e.getMessage());
            return Optional.empty();
        }
    }

    private String exchcdOf(String exchangeName) {
        if (exchangeName != null && exchangeName.contains("NYSE")) return EXCHCD_NYSE;
        return EXCHCD_NASDAQ;
    }

    private String keysymbol(String exchcd, String ticker) {
        return exchcd + ticker;
    }

    /** g3101 — 현재가 조회 */
    Optional<LsCurrentPriceDto> getCurrentPrice(String ticker, String exchangeName) {
        String exchcd = exchcdOf(exchangeName);
        Map<String, Object> body = Map.of(
                "g3101InBlock", Map.of(
                        "delaygb",   "R",
                        "keysymbol", keysymbol(exchcd, ticker),
                        "exchcd",    exchcd,
                        "symbol",    ticker
                )
        );
        return post("/overseas-stock/market-data", "g3101", body, LsCurrentPriceDto.class)
                .filter(LsCurrentPriceDto::isSuccess);
    }

    /** g3104 — 종목정보 조회 */
    Optional<LsStockInfoDto> getStockInfo(String ticker, String exchangeName) {
        String exchcd = exchcdOf(exchangeName);
        Map<String, Object> body = Map.of(
                "g3104InBlock", Map.of(
                        "delaygb",   "R",
                        "keysymbol", keysymbol(exchcd, ticker),
                        "exchcd",    exchcd,
                        "symbol",    ticker
                )
        );
        return post("/overseas-stock/market-data", "g3104", body, LsStockInfoDto.class)
                .filter(LsStockInfoDto::isSuccess);
    }

    /** g3106 — 현재가 + 10호가 조회 */
    Optional<LsOrderBookDto> getOrderBook(String ticker, String exchangeName) {
        String exchcd = exchcdOf(exchangeName);
        Map<String, Object> body = Map.of(
                "g3106InBlock", Map.of(
                        "delaygb",   "R",
                        "keysymbol", keysymbol(exchcd, ticker),
                        "exchcd",    exchcd,
                        "symbol",    ticker
                )
        );
        return post("/overseas-stock/market-data", "g3106", body, LsOrderBookDto.class)
                .filter(LsOrderBookDto::isSuccess);
    }

    /** g3190 — 마스터 조회 (전체 페이지 수집) */
    List<LsMasterDto.Item> getAllMasterItems(String natcode, String exgubun) {
        List<LsMasterDto.Item> all = new ArrayList<>();
        String ctsValue = "";
        String trCont   = "N";

        do {
            final String finalCts  = ctsValue;
            final String finalCont = trCont;

            Map<String, Object> body = Map.of(
                    "g3190InBlock", Map.of(
                            "delaygb",   "R",
                            "natcode",   natcode,
                            "exgubun",   exgubun,
                            "readcnt",   100,
                            "cts_value", finalCts
                    )
            );

            try {
                LsMasterDto resp = client()
                        .post()
                        .uri("/overseas-stock/market-data")
                        .header("Authorization", "Bearer " + tokenManager.getAccessToken())
                        .header("tr_cd", "g3190")
                        .header("tr_cont", finalCont)
                        .header("tr_cont_key", finalCts)
                        .header("mac_address", "")
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(LsMasterDto.class)
                        .block();

                if (resp == null || !resp.isSuccess() || resp.getItems() == null) break;

                all.addAll(resp.getItems());
                log.debug("g3190 수집: {} 건 (누적 {})", resp.getItems().size(), all.size());

                if (!resp.hasMore()) break;
                ctsValue = resp.getHeader().getCtsValue();
                trCont   = "Y";

                Thread.sleep(200); // 연속 호출 딜레이

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("g3190 마스터 조회 오류: {}", e.getMessage());
                break;
            }
        } while (true);

        log.info("g3190 마스터 조회 완료: 총 {} 건 (natcode={}, exgubun={})", all.size(), natcode, exgubun);
        return all;
    }
}
