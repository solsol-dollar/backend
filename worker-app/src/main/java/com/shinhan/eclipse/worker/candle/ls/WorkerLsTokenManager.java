package com.shinhan.eclipse.worker.candle.ls;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkerLsTokenManager {

    private final WorkerLsProperties props;

    private record TokenInfo(String token, Instant expiry) {
        boolean isValid() {
            return Instant.now().isBefore(expiry.minusSeconds(30));
        }
    }

    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expireIn
    ) {}

    private final AtomicReference<TokenInfo> cached = new AtomicReference<>();

    public String getAccessToken() {
        TokenInfo info = cached.get();
        if (info != null && info.isValid()) return info.token();
        return refresh();
    }

    private synchronized String refresh() {
        TokenInfo info = cached.get();
        if (info != null && info.isValid()) return info.token();

        log.info("Worker LS 증권 OAuth2 토큰 갱신 중...");

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type",   "client_credentials");
        form.add("appkey",       props.getAppKey());
        form.add("appsecretkey", props.getAppSecret());
        form.add("scope",        "oob");

        TokenResponse resp = WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .build()
                .post()
                .uri("/oauth2/token")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .block();

        if (resp == null || resp.accessToken() == null) {
            throw new IllegalStateException("Worker LS 증권 토큰 발급 실패");
        }

        TokenInfo newInfo = new TokenInfo(
                resp.accessToken(),
                Instant.now().plusSeconds(resp.expireIn())
        );
        cached.set(newInfo);
        log.info("Worker LS 증권 토큰 발급 완료 (만료: {}초 후)", resp.expireIn());
        return newInfo.token();
    }

    public void invalidate() {
        cached.set(null);
    }
}
