package com.shinhan.eclipse.service.securities.internal;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kis.securities")
class KisProperties {

    private String baseUrl    = "https://openapi.koreainvestment.com:9443";
    private String appKey     = "";
    private String appSecret  = "";

    boolean isConfigured() {
        return appKey != null && !appKey.isBlank()
            && appSecret != null && !appSecret.isBlank();
    }
}
