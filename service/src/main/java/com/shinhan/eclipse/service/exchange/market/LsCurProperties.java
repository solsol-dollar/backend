package com.shinhan.eclipse.service.exchange.market;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ls.securities")
public class LsCurProperties {

    private String baseUrl   = "https://openapi.ls-sec.co.kr:8080";
    private String wsUrl     = "wss://openapi.ls-sec.co.kr:29443/websocket";
    private String appKey    = "";
    private String appSecret = "";

    public boolean isConfigured() {
        return appKey != null && !appKey.isBlank();
    }
}
