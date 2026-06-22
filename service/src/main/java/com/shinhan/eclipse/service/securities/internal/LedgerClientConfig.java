package com.shinhan.eclipse.service.securities.internal;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(clients = LedgerApiClient.class)
class LedgerClientConfig {

    @Bean
    LedgerClient ledgerClient(LedgerApiClient apiClient) {
        return new LedgerClient(apiClient);
    }
}
