package com.shinhan.eclipse.ledger.app.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ServiceAppClientConfig {

    @Bean
    @Qualifier("serviceAppRestClient")
    public RestClient serviceAppRestClient(@Value("${service-app.url:http://localhost:8081}") String serviceAppUrl) {
        return RestClient.builder().baseUrl(serviceAppUrl).build();
    }
}
