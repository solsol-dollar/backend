package com.shinhan.eclipse.service.securities.internal;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ls.securities")
class LsProperties {

    private String baseUrl = "https://openapi.ls-sec.co.kr:8080";
    private String wsUrl   = "wss://openapi.ls-sec.co.kr:9443/websocket";
    private String appKey  = "";
    private String appSecret = "";

    boolean isConfigured() {
        return appKey != null && !appKey.isBlank()
            && appSecret != null && !appSecret.isBlank();
    }
}
