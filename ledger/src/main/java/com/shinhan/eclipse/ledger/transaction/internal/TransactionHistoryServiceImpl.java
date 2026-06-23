package com.shinhan.eclipse.ledger.transaction.internal;

import com.shinhan.eclipse.domain.account.FinancialAccount;
import com.shinhan.eclipse.domain.transaction.FxExchangeTransaction;
import com.shinhan.eclipse.domain.transaction.TransferTransaction;
import com.shinhan.eclipse.ledger.transaction.TransactionHistoryItem;
import com.shinhan.eclipse.ledger.transaction.TransactionHistoryItem.AccountInfo;
import com.shinhan.eclipse.ledger.transaction.TransactionHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
class TransactionHistoryServiceImpl implements TransactionHistoryService {

    private final TxTransferRepository transferRepository;
    private final TxFxRepository fxRepository;
    private final TxAccountRepository accountRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TransactionHistoryItem> getHistory(Long userId, List<Long> accountIds, String filter) {
        List<TransferTransaction> transfers;
        List<FxExchangeTransaction> fxList;

        switch (filter.toUpperCase()) {
            case "IN"       -> { transfers = transferRepository.findIncoming(userId, accountIds);   fxList = List.of(); }
            case "OUT"      -> { transfers = transferRepository.findOutgoing(userId, accountIds);   fxList = List.of(); }
            case "CARD"     -> { transfers = transferRepository.findCard(userId, accountIds);       fxList = List.of(); }
            case "EXCHANGE" -> { transfers = List.of(); fxList = fxRepository.findAllByAccounts(userId, accountIds); }
            default         -> { transfers = transferRepository.findAllByAccounts(userId, accountIds); fxList = fxRepository.findAllByAccounts(userId, accountIds); }
        }

        // 관련된 모든 accountId 수집 후 한 번에 조회
        Set<Long> relatedIds = new HashSet<>(accountIds);
        transfers.forEach(t -> { if (t.getFromAccountId() != null) relatedIds.add(t.getFromAccountId()); if (t.getToAccountId() != null) relatedIds.add(t.getToAccountId()); });
        fxList.forEach(t -> { if (t.getFromAccountId() != null) relatedIds.add(t.getFromAccountId()); if (t.getToAccountId() != null) relatedIds.add(t.getToAccountId()); });

        Map<Long, FinancialAccount> accountMap = accountRepository.findAllByIdIn(new ArrayList<>(relatedIds))
                .stream().collect(Collectors.toMap(FinancialAccount::getId, a -> a));

        List<TransactionHistoryItem> result = new ArrayList<>();
        transfers.forEach(t -> result.add(toItem(t, accountIds, accountMap)));
        fxList.forEach(t -> result.add(toFxItem(t, accountMap)));
        result.sort(Comparator.comparing(TransactionHistoryItem::executedAt).reversed());

        return result;
    }

    private TransactionHistoryItem toItem(TransferTransaction t, List<Long> accountIds, Map<Long, FinancialAccount> accountMap) {
        String type;
        if ("CARD".equals(t.getTransferType())) {
            type = "CARD";
        } else if (t.getToAccountId() != null && accountIds.contains(t.getToAccountId())) {
            type = "IN";
        } else {
            type = "OUT";
        }
        return TransactionHistoryItem.ofTransfer(
                t.getId(), type,
                t.getAmount(), t.getCurrency(),
                t.getTransferStatus(),
                t.getCompletedAt() != null ? t.getCompletedAt() : t.getRequestedAt(),
                toAccountInfo(t.getFromAccountId(), accountMap),
                toAccountInfo(t.getToAccountId(), accountMap)
        );
    }

    private TransactionHistoryItem toFxItem(FxExchangeTransaction t, Map<Long, FinancialAccount> accountMap) {
        return TransactionHistoryItem.ofExchange(
                t.getId(),
                t.getFromCurrency(), t.getToCurrency(),
                t.getExchangeRate(),
                t.getSourceAmount(), t.getTargetAmount(),
                t.getExchangeStatus(),
                t.getCompletedAt() != null ? t.getCompletedAt() : t.getRequestedAt(),
                toAccountInfo(t.getFromAccountId(), accountMap),
                toAccountInfo(t.getToAccountId(), accountMap)
        );
    }

    private AccountInfo toAccountInfo(Long accountId, Map<Long, FinancialAccount> accountMap) {
        if (accountId == null) return null;
        FinancialAccount a = accountMap.get(accountId);
        if (a == null) return new AccountInfo(accountId, null, null, null);
        return new AccountInfo(a.getId(), a.getAccountName(), a.getAccountNumberMasked(), a.getInstitutionName());
    }
}
