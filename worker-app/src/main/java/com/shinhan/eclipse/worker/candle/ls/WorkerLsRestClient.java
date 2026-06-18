package com.shinhan.eclipse.worker.candle.ls;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;
import java.util.Optional;

/**
 * Worker-app 전용 LS증권 REST 클라이언트.
 * g3204 (일/주/월봉 배치 적재) 전용으로만 사용한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkerLsRestClient {

    private static final String EXCHCD_NASDAQ = "82";

    private final WorkerLsProperties   props;
    private final WorkerLsTokenManager tokenManager;

    private WebClient client() {
        return WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .build();
    }

    /**
     * g3204 — 일/주/월봉 조회 (연속조회 포함).
     *
     * @param exchcd   거래소 코드 (보통 "82"=NASDAQ)
     * @param symbol   종목 코드 (예: "AAPL")
     * @param gubun    2=일봉, 3=주봉, 4=월봉
     * @param sdate    조회 시작일 (YYYYMMDD)
     * @param edate    조회 종료일 (YYYYMMDD)
     * @param ctsDate  연속조회 키 (최초 요청 시 "")
     * @param ctsInfo  연속조회 정보 (최초 요청 시 "")
     */
    public Optional<G3204Response> getDailyCandles(String exchcd, String symbol,
                                                    String gubun, String sdate, String edate,
                                                    String ctsDate, String ctsInfo) {
        boolean isContinue = ctsDate != null && !ctsDate.isBlank();
        String keysymbol = buildKeysymbol(exchcd, symbol);

        Map<String, Object> inBlock = new java.util.LinkedHashMap<>();
        inBlock.put("sujung",    "Y");
        inBlock.put("delaygb",   "0");
        inBlock.put("keysymbol", keysymbol);
        inBlock.put("exchcd",    exchcd);
        inBlock.put("symbol",    symbol);
        inBlock.put("gubun",     gubun);
        inBlock.put("qrycnt",    500);
        inBlock.put("comp_yn",   "N");
        inBlock.put("sdate",     sdate);
        inBlock.put("edate",     edate);
        inBlock.put("cts_date",  ctsDate != null ? ctsDate : "");
        inBlock.put("cts_info",  ctsInfo  != null ? ctsInfo  : "");
        Map<String, Object> body = Map.of("g3204InBlock", inBlock);

        try {
            G3204Response[] holder = new G3204Response[1];
            String[] trContHolder  = new String[]{""};

            client()
                    .post()
                    .uri("/overseas-stock/market-data")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + tokenManager.getAccessToken())
                    .header("tr_cd",       "g3204")
                    .header("tr_cont",     isContinue ? "Y" : "N")
                    .header("tr_cont_key", ctsDate != null ? ctsDate : "")
                    .header("mac_address", "")
                    .bodyValue(body)
                    .exchangeToMono(resp -> {
                        String trCont = resp.headers().asHttpHeaders().getFirst("tr_cont");
                        trContHolder[0] = trCont != null ? trCont : "";
                        return resp.bodyToMono(G3204Response.class);
                    })
                    .doOnNext(r -> {
                        holder[0] = r;
                        if (r != null) r.setTrCont(trContHolder[0]);
                    })
                    .block();

            return Optional.ofNullable(holder[0]);

        } catch (WebClientResponseException e) {
            log.warn("LS g3204 호출 실패 status={} body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 401) tokenManager.invalidate();
            return Optional.empty();
        } catch (Exception e) {
            log.warn("LS g3204 호출 오류: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** keysymbol 생성 (exchcd + symbol, 20자리 우측 공백 패딩) */
    private String buildKeysymbol(String exchcd, String symbol) {
        return String.format("%-20s", exchcd + symbol);
    }
}
