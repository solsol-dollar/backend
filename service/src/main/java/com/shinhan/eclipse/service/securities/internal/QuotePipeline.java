package com.shinhan.eclipse.service.securities.internal;

import com.shinhan.eclipse.service.securities.QuoteCache;
import com.shinhan.eclipse.service.securities.QuoteReceivedEvent;
import com.shinhan.eclipse.service.securities.QuoteSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class QuotePipeline {

    private static final String TOPIC_PREFIX = "/topic/quotes/";

    private final QuoteCache             quoteCache;
    private final SimpMessagingTemplate  messagingTemplate;

    @Async
    @EventListener
    public void onQuoteReceived(QuoteReceivedEvent event) {
        QuoteSnapshot snapshot = QuoteSnapshot.from(event);
        quoteCache.put(event.ticker(), snapshot);
        messagingTemplate.convertAndSend(TOPIC_PREFIX + event.ticker(), snapshot);
        log.debug("시세 브로드캐스트: {} = {}", event.ticker(), event.price());
    }
}
