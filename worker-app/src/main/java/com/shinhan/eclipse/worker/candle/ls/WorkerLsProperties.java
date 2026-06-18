package com.shinhan.eclipse.worker.candle.ls;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ls.securities")
public class WorkerLsProperties {

    private String baseUrl    = "https://openapi.ls-sec.co.kr:8080";
    private String appKey     = "";
    private String appSecret  = "";

    public boolean isConfigured() {
        return appKey != null && !appKey.isBlank()
            && appSecret != null && !appSecret.isBlank();
    }
}
