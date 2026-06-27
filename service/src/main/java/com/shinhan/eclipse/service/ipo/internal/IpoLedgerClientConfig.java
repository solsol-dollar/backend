package com.shinhan.eclipse.service.ipo.internal;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(clients = IpoLedgerClient.class)
class IpoLedgerClientConfig {
}
