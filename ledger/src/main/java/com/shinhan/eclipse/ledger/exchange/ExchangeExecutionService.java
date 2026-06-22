package com.shinhan.eclipse.ledger.exchange;

public interface ExchangeExecutionService {
    ExchangeResult execute(ExchangeRequest request, Long userId);
}