package com.shinhan.eclipse.worker.candle.kis;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kis.securities")
public class WorkerKisProperties {

    private String baseUrl   = "https://openapi.koreainvestment.com:9443";
    private String appKey    = "";
    private String appSecret = "";

    public boolean isConfigured() {
        return appKey != null && !appKey.isBlank()
            && appSecret != null && !appSecret.isBlank();
    }
}
