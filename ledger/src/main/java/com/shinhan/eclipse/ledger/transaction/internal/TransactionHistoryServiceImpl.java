package com.shinhan.eclipse.ledger.transaction.internal;

import com.shinhan.eclipse.domain.account.BalanceHold;
import com.shinhan.eclipse.domain.account.CardTransaction;
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

@Service
@RequiredArgsConstructor
class TransactionHistoryServiceImpl implements TransactionHistoryService {

    private final TxTransferRepository transferRepository;
    private final TxFxRepository fxRepository;
    private final TxAccountRepository accountRepository;
    private final TxBalanceHoldRepository balanceHoldRepository;
    private final TxCardTransactionRepository cardTransactionRepository;

    private static final int PAGE_SIZE = 10;

    @Override
    @Transactional(readOnly = true)
    public TransactionHistoryService.TransactionPage getHistory(Long userId, List<Long> accountIds, String filter, int page) {
        List<TransferTransaction> transfers;
        List<FxExchangeTransaction> fxList;
        List<Object[]> holdRows;

        List<CardTransaction> cardList;
        String normalizedFilter = (filter == null) ? "ALL" : filter.toUpperCase();
        switch (normalizedFilter) {
            case "IN" -> {
                transfers = transferRepository.findIncoming(userId, accountIds);
                cardList = List.of();
                fxList = List.of();
                holdRows = balanceHoldRepository.findIncomingWithCompanyName(accountIds);
            }
            case "OUT" -> {
                transfers = transferRepository.findOutgoing(userId, accountIds);
                cardList = List.of();
                fxList = List.of();
                holdRows = balanceHoldRepository.findOutgoingWithCompanyName(accountIds);
            }
            case "CARD" -> {
                transfers = List.of();
                cardList = cardTransactionRepository.findAllByUserIdAndAccountIds(userId, accountIds);
                fxList = List.of();
                holdRows = List.of();
            }
            case "EXCHANGE" -> {
                transfers = List.of();
                cardList = List.of();
                fxList = fxRepository.findAllByAccounts(userId, accountIds);
                holdRows = List.of();
            }
            default -> {
                transfers = transferRepository.findAllByAccounts(userId, accountIds);
                cardList = cardTransactionRepository.findAllByUserIdAndAccountIds(userId, accountIds);
                fxList = fxRepository.findAllByAccounts(userId, accountIds);
                holdRows = balanceHoldRepository.findWithCompanyNameByAccountIds(accountIds);
            }
        }

        // 관련된 모든 accountId 수집 후 한 번에 조회
        Set<Long> relatedIds = new HashSet<>(accountIds);
        transfers.forEach(t -> { if (t.getFromAccountId() != null) relatedIds.add(t.getFromAccountId()); if (t.getToAccountId() != null) relatedIds.add(t.getToAccountId()); });
        fxList.forEach(t -> { if (t.getFromAccountId() != null) relatedIds.add(t.getFromAccountId()); if (t.getToAccountId() != null) relatedIds.add(t.getToAccountId()); });

        Map<Long, FinancialAccount> accountMap = accountRepository.findAllByIdIn(new ArrayList<>(relatedIds))
                .stream().collect(Collectors.toMap(FinancialAccount::getId, a -> a));

        List<TransactionHistoryItem> result = new ArrayList<>();
        transfers.forEach(t -> result.add(toItem(t, accountIds, accountMap)));
        cardList.forEach(c -> result.add(toCardItem(c)));
        fxList.forEach(t -> result.add(toFxItem(t, accountMap)));
        holdRows.forEach(row -> result.add(toHoldItem((BalanceHold) row[0], (String) row[1])));
        result.sort(Comparator.comparing(TransactionHistoryItem::executedAt).reversed());

        int from = page * PAGE_SIZE;
        if (from >= result.size()) {
            return new TransactionHistoryService.TransactionPage(List.of(), page, PAGE_SIZE, false);
        }
        int to = Math.min(from + PAGE_SIZE, result.size());
        boolean hasNext = to < result.size();

        return new TransactionHistoryService.TransactionPage(result.subList(from, to), page, PAGE_SIZE, hasNext);
    }

    private TransactionHistoryItem toItem(TransferTransaction t, List<Long> accountIds, Map<Long, FinancialAccount> accountMap) {
        String type;
        boolean fromMine = t.getFromAccountId() != null && accountIds.contains(t.getFromAccountId());
        boolean toMine   = t.getToAccountId()   != null && accountIds.contains(t.getToAccountId());
        if ("CARD".equals(t.getTransferType())) {
            return TransactionHistoryItem.ofCard(
                    t.getId(),
                    t.getAmount(), t.getCurrency(),
                    t.getTransferStatus(),
                    t.getCompletedAt() != null ? t.getCompletedAt() : t.getRequestedAt(),
                    toAccountInfo(t.getFromAccountId(), accountMap),
                    null
            );
        } else if (toMine && !fromMine) {
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

    private TransactionHistoryItem toCardItem(CardTransaction c) {
        return TransactionHistoryItem.ofCard(
                c.getId(),
                c.getAmount(), c.getCurrency(),
                "COMPLETED",
                c.getTransactedAt(),
                null,
                c.getMerchantName()
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

    private TransactionHistoryItem toHoldItem(BalanceHold hold, String companyName) {
        boolean isOut = "LOCKED".equals(hold.getHoldStatus()) || "SETTLED".equals(hold.getHoldStatus());
        String type = isOut ? "IPO_SUBSCRIPTION" : "IPO_SUBSCRIPTION_CANCEL";
        String description = isOut ? companyName + " IPO 청약" : companyName + " IPO 청약 취소";
        return TransactionHistoryItem.ofIpoHold(
                hold.getId(), type, hold.getAmount(), "COMPLETED", hold.getCreatedAt(), description);
    }

    private AccountInfo toAccountInfo(Long accountId, Map<Long, FinancialAccount> accountMap) {
        if (accountId == null) return null;
        FinancialAccount a = accountMap.get(accountId);
        if (a == null) return new AccountInfo(accountId, null, null, null);
        return new AccountInfo(a.getId(), a.getAccountName(), a.getAccountNumber(), a.getInstitutionName());
    }
}
