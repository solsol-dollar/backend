package com.shinhan.eclipse.service.exchange.market;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component("lsExchangeTokenManager")
@RequiredArgsConstructor
public class LsTokenManager {

    private final LsCurProperties          props;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper              objectMapper;

    private final AtomicReference<String> token = new AtomicReference<>("");

    @PostConstruct
    void init() {
        if (!props.isConfigured()) {
            log.warn("[LS토큰] appKey 미설정 — LS 웹소켓 비활성화");
            return;
        }
        refreshToken();
    }

    @Scheduled(cron = "0 5 7 * * MON-FRI", zone = "Asia/Seoul")
    void scheduledRefresh() {
        if (!props.isConfigured()) return;
        log.info("[LS토큰] 일일 갱신");
        refreshToken();
    }

    public String getToken() {
        return token.get();
    }

    private void refreshToken() {
        try {
            String body = "grant_type=client_credentials"
                    + "&appkey="       + URLEncoder.encode(props.getAppKey(),    StandardCharsets.UTF_8)
                    + "&appsecretkey=" + URLEncoder.encode(props.getAppSecret(), StandardCharsets.UTF_8)
                    + "&scope=oob";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(props.getBaseUrl() + "/oauth2/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("[LS토큰] {} 응답 본문: {}", response.statusCode(), response.body());
                return;
            }

            LsTokenResponse resp = objectMapper.readValue(response.body(), LsTokenResponse.class);
            if (resp.accessToken() != null && !resp.accessToken().isBlank()) {
                token.set(resp.accessToken());
                log.info("[LS토큰] 갱신 완료");
                eventPublisher.publishEvent(new LsTokenRefreshedEvent(this, resp.accessToken()));
            } else {
                log.warn("[LS토큰] 응답에 토큰 없음: {}", response.body());
            }
        } catch (Exception e) {
            log.error("[LS토큰] 갱신 실패: {}", e.getMessage());
        }
    }

    record LsTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type")   String tokenType,
            @JsonProperty("expires_in")   Long   expiresIn
    ) {}
}
