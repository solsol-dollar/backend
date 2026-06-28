package com.shinhan.eclipse.service.card;

public interface CardService {
    CardTransactionSummary getMonthlySummary(Long userId, int year, int month);
}