package com.shinhan.eclipse.service.exchange.market;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MarketExchangeRateService {

    private final MarketRateRedisStore redisStore;
    private final SseEmitterRegistry   sseRegistry;

    public Optional<MarketRateData> getCurrent() {
        return redisStore.get();
    }

    public SseEmitter subscribe() {
        return sseRegistry.register();
    }
}
