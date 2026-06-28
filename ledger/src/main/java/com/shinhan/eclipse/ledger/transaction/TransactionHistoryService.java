package com.shinhan.eclipse.ledger.transaction;

import java.util.List;

public interface TransactionHistoryService {
    TransactionPage getHistory(Long userId, List<Long> accountIds, String filter, int page);

    record TransactionPage(
            List<TransactionHistoryItem> items,
            int page,
            int size,
            boolean hasNext
    ) {}
}