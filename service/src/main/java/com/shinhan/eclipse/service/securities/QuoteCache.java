package com.shinhan.eclipse.service.securities;

import java.util.Optional;

public interface QuoteCache {
    void put(String ticker, QuoteSnapshot snapshot);
    Optional<QuoteSnapshot> get(String ticker);
}
