package com.shinhan.eclipse.service.securities.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
class KisTokenManager {

    private final KisProperties props;

    private record TokenInfo(String token, Instant expiry) {
        boolean isValid() {
            return Instant.now().isBefore(expiry.minusSeconds(30));
        }
    }

    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in")   long expiresIn
    ) {}

    private final AtomicReference<TokenInfo> cached = new AtomicReference<>();

    String getAccessToken() {
        TokenInfo info = cached.get();
        if (info != null && info.isValid()) return info.token();
        return refresh();
    }

    private synchronized String refresh() {
        TokenInfo info = cached.get();
        if (info != null && info.isValid()) return info.token();

        log.info("KIS OAuth2 토큰 갱신 중...");

        TokenResponse resp = WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .build()
                .post()
                .uri("/oauth2/tokenP")
                .header("Content-Type", "application/json")
                .bodyValue(Map.of(
                        "grant_type", "client_credentials",
                        "appkey",     props.getAppKey(),
                        "appsecret",  props.getAppSecret()
                ))
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .block();

        if (resp == null || resp.accessToken() == null) {
            throw new IllegalStateException("KIS 토큰 발급 실패");
        }

        TokenInfo newInfo = new TokenInfo(
                resp.accessToken(),
                Instant.now().plusSeconds(resp.expiresIn())
        );
        cached.set(newInfo);
        log.info("KIS 토큰 발급 완료 (만료: {}초 후)", resp.expiresIn());
        return newInfo.token();
    }

    void invalidate() {
        cached.set(null);
    }
}
