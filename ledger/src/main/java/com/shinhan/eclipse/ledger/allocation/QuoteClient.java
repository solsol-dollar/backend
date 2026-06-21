package com.shinhan.eclipse.ledger.allocation;

import java.math.BigDecimal;

/** 실시간 시세 조회. ledger-app에서 service-app의 종목 상세 API를 호출하는 구현체를 주입한다. */
public interface QuoteClient {
    /** @param productId investment_products.id (Ipo.productId) */
    BigDecimal getCurrentPrice(Long productId);
}
