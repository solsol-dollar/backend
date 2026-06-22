package com.shinhan.eclipse.service.exchange.internal;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "eximbank")
class ExchangeProperties {

    /** 한국수출입은행 API 기본 URL */
    private String baseUrl = "https://oapi.koreaexim.go.kr";

    /** 발급받은 인증 키 */
    private String authKey = "";

}