package com.shinhan.eclipse.service.securities.internal;

import com.shinhan.eclipse.service.securities.QuoteCache;
import com.shinhan.eclipse.service.securities.QuoteReceivedEvent;
import com.shinhan.eclipse.service.securities.QuoteSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QuotePipelineTest {

    @Mock QuoteCache            quoteCache;
    @Mock SimpMessagingTemplate messagingTemplate;

    @InjectMocks QuotePipeline pipeline;

    @Test
    void 이벤트_수신_시_Redis에_캐시하고_STOMP로_브로드캐스트한다() {
        QuoteReceivedEvent event = QuoteReceivedEvent.of(
                "TSLA",
                new BigDecimal("250.50"),
                new BigDecimal("3.20"),
                new BigDecimal("1.29"),
                9876543L,
                "2"
        );

        pipeline.onQuoteReceived(event);

        // Redis 캐시 저장 검증
        ArgumentCaptor<QuoteSnapshot> snapshotCaptor = ArgumentCaptor.forClass(QuoteSnapshot.class);
        verify(quoteCache).put(eq("TSLA"), snapshotCaptor.capture());
        QuoteSnapshot saved = snapshotCaptor.getValue();
        assertThat(saved.ticker()).isEqualTo("TSLA");
        assertThat(saved.price()).isEqualByComparingTo("250.50");
        assertThat(saved.volume()).isEqualTo(9876543L);

        // STOMP 브로드캐스트 검증
        ArgumentCaptor<QuoteSnapshot> broadcastCaptor = ArgumentCaptor.forClass(QuoteSnapshot.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/quotes/TSLA"),
                broadcastCaptor.capture()
        );
        assertThat(broadcastCaptor.getValue().ticker()).isEqualTo("TSLA");
    }

    @Test
    void STOMP_토픽_경로가_ticker별로_구분된다() {
        QuoteReceivedEvent event = QuoteReceivedEvent.of(
                "AAPL", BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, 0L, "3"
        );

        pipeline.onQuoteReceived(event);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/quotes/AAPL"),
                org.mockito.ArgumentMatchers.any(QuoteSnapshot.class)
        );
    }
}
