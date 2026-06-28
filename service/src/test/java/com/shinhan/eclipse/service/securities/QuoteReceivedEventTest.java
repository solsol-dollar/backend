package com.shinhan.eclipse.service.securities;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class QuoteReceivedEventTest {

    @Test
    void of_팩토리_메서드로_생성하면_receivedAt이_설정된다() {
        QuoteReceivedEvent event = QuoteReceivedEvent.of(
                "TSLA",
                new BigDecimal("250.50"),
                new BigDecimal("3.20"),
                new BigDecimal("1.29"),
                1234567L,
                "2"
        );

        assertThat(event.ticker()).isEqualTo("TSLA");
        assertThat(event.price()).isEqualByComparingTo("250.50");
        assertThat(event.volume()).isEqualTo(1234567L);
        assertThat(event.receivedAt()).isNotNull();
    }

    @Test
    void record_동등성_비교() {
        QuoteReceivedEvent e1 = new QuoteReceivedEvent(
                "AAPL", BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, 100L, "2",
                java.time.Instant.EPOCH
        );
        QuoteReceivedEvent e2 = new QuoteReceivedEvent(
                "AAPL", BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, 100L, "2",
                java.time.Instant.EPOCH
        );

        assertThat(e1).isEqualTo(e2);
    }
}
