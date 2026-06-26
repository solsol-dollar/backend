package com.shinhan.eclipse.service.card.internal;

import com.shinhan.eclipse.common.redis.exchange.ExchangeRateInfo;
import com.shinhan.eclipse.domain.account.CardTransaction;
import com.shinhan.eclipse.service.card.CardService;
import com.shinhan.eclipse.service.card.CardTransactionSummary;
import com.shinhan.eclipse.service.card.CardTransactionSummary.*;
import com.shinhan.eclipse.service.exchange.ExchangeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class CardServiceImpl implements CardService {

    private final CardTransactionRepository txRepository;
    private final MerchantCategoryClassifier classifier;
    private final ExchangeService exchangeService;

    @Override
    @Transactional(readOnly = true)
    public CardTransactionSummary getMonthlySummary(Long userId, int year, int month) {
        LocalDateTime from = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime to   = from.plusMonths(1);

        List<CardTransaction> monthly = txRepository
                .findByUserIdAndTransactedAtBetweenOrderByTransactedAtDesc(userId, from, to);

        int totalCount = monthly.size();
        BigDecimal totalAmount = monthly.stream()
                .map(CardTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 카테고리별 집계
        List<CategorySpend> byCategory = monthly.stream()
                .collect(Collectors.groupingBy(CardTransaction::getCategory))
                .entrySet().stream()
                .map(e -> new CategorySpend(
                        e.getKey(),
                        e.getValue().stream().map(CardTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
                        e.getValue().size()
                ))
                .sorted(Comparator.comparing(CategorySpend::amount).reversed())
                .toList();

        // 이번 달 최대 소비
        TransactionItem topSpend = monthly.stream()
                .max(Comparator.comparing(CardTransaction::getAmount))
                .map(this::toItem)
                .orElse(null);

        // 외화 직접 결제 절약 금액
        FxSavings fxSavings = calculateFxSavings(monthly);

        // 정기 결제 감지 (최근 3개월 기준)
        LocalDateTime threeMonthsAgo = from.minusMonths(2);
        List<CardTransaction> recent = txRepository
                .findByUserIdAndTransactedAtAfterOrderByTransactedAtAsc(userId, threeMonthsAgo);
        List<RecurringPayment> recurringPayments = detectRecurring(recent);

        List<TransactionItem> transactions = monthly.stream().map(this::toItem).toList();

        return new CardTransactionSummary(
                totalCount, totalAmount, "USD",
                fxSavings, byCategory, topSpend, recurringPayments, transactions
        );
    }

    private FxSavings calculateFxSavings(List<CardTransaction> txList) {
        // 결제 시점 환율이 저장된 건만 계산
        BigDecimal savingsKrw = txList.stream()
                .filter(t -> t.getTtsAtTime() != null && t.getBaseRateAtTime() != null)
                .map(t -> t.getAmount().multiply(t.getTtsAtTime().subtract(t.getBaseRateAtTime())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 현재 기준환율로 USD 환산
        BigDecimal savingsUsd = null;
        try {
            BigDecimal currentBaseRate = exchangeService.getExchangeRate("USD").baseRate();
            if (currentBaseRate != null && currentBaseRate.compareTo(BigDecimal.ZERO) > 0) {
                savingsUsd = savingsKrw.divide(currentBaseRate, 0, RoundingMode.HALF_UP);
            }
        } catch (Exception ignored) {}

        return new FxSavings(savingsKrw.setScale(0, RoundingMode.HALF_UP), savingsUsd);
    }

    private List<RecurringPayment> detectRecurring(List<CardTransaction> txList) {
        // 가맹점별 그룹핑
        Map<String, List<CardTransaction>> byMerchant = txList.stream()
                .collect(Collectors.groupingBy(CardTransaction::getMerchantName));

        List<RecurringPayment> result = new ArrayList<>();

        for (Map.Entry<String, List<CardTransaction>> entry : byMerchant.entrySet()) {
            String merchantName = entry.getKey();
            List<CardTransaction> txs = entry.getValue();

            // 월별 등장 여부 확인 — 2개월 이상 연속 등장해야 정기 결제로 판단
            Set<String> months = txs.stream()
                    .map(t -> t.getTransactedAt().getYear() + "-" + t.getTransactedAt().getMonthValue())
                    .collect(Collectors.toSet());
            if (months.size() < 2) continue;

            // 금액 분산 확인 (평균 대비 ±10% 이내)
            BigDecimal avg = txs.stream().map(CardTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(txs.size()), 4, RoundingMode.HALF_UP);

            boolean amountStable = txs.stream().allMatch(t -> {
                BigDecimal diff = t.getAmount().subtract(avg).abs();
                BigDecimal threshold = avg.multiply(BigDecimal.valueOf(0.1));
                return diff.compareTo(threshold) <= 0;
            });
            if (!amountStable) continue;

            // 결제일 분산 확인 (평균 결제일 기준 ±5일 이내)
            double avgDay = txs.stream()
                    .mapToInt(t -> t.getTransactedAt().getDayOfMonth())
                    .average().orElse(0);
            boolean dayStable = txs.stream()
                    .allMatch(t -> Math.abs(t.getTransactedAt().getDayOfMonth() - avgDay) <= 5);
            if (!dayStable) continue;

            result.add(new RecurringPayment(
                    merchantName,
                    txs.getFirst().getCategory(),
                    avg,
                    (int) Math.round(avgDay)
            ));
        }

        return result;
    }

    private TransactionItem toItem(CardTransaction tx) {
        return new TransactionItem(
                tx.getId(), tx.getMerchantName(), tx.getCategory(),
                tx.getAmount(), tx.getCurrency(), tx.getTransactedAt()
        );
    }
}