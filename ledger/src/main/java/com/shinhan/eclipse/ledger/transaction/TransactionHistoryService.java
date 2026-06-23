package com.shinhan.eclipse.ledger.transaction;

import java.util.List;

public interface TransactionHistoryService {
    List<TransactionHistoryItem> getHistory(Long userId, List<Long> accountIds, String filter);
}