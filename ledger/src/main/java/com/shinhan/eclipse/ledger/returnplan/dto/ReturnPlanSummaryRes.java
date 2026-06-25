package com.shinhan.eclipse.ledger.returnplan.dto;

import com.shinhan.eclipse.domain.ipo.Ipo;
import com.shinhan.eclipse.domain.returnplan.ReturnPlan;
import com.shinhan.eclipse.domain.returnplan.ReturnPlanAllocation;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

/** 리턴플랜 대시보드 요약 (명세 외 추가). */
@Getter
@Builder
public class ReturnPlanSummaryRes {
    private final int totalPlanCount;
    private final int executedPlanCount;
    private final BigDecimal totalRefundAmount;
    private final BigDecimal securitiesAmount;
    private final BigDecimal savingsAmount;
    private final BigDecimal accountAmount;
    private final NextIpoInfo nextIpoInfo;

    public static ReturnPlanSummaryRes of(List<ReturnPlan> plans,
                                           Function<Long, List<ReturnPlanAllocation>> allocationsLoader,
                                           Ipo nextUpcomingIpo) {
        BigDecimal totalRefundAmount = plans.stream()
                .map(ReturnPlan::getTotalRefundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long executedCount = plans.stream().filter(ReturnPlan::isExecuted).count();

        BigDecimal securities = BigDecimal.ZERO;
        BigDecimal savings = BigDecimal.ZERO;
        BigDecimal account = BigDecimal.ZERO;

        for (ReturnPlan plan : plans) {
            for (ReturnPlanAllocation allocation : allocationsLoader.apply(plan.getId())) {
                switch (allocation.getDestinationType()) {
                    case "SECURITIES" -> securities = securities.add(allocation.getAllocationAmount());
                    case "SAVINGS" -> savings = savings.add(allocation.getAllocationAmount());
                    case "DEPOSIT" -> account = account.add(allocation.getAllocationAmount());
                    default -> { }
                }
            }
        }

        return ReturnPlanSummaryRes.builder()
                .totalPlanCount(plans.size())
                .executedPlanCount((int) executedCount)
                .totalRefundAmount(totalRefundAmount)
                .securitiesAmount(securities)
                .savingsAmount(savings)
                .accountAmount(account)
                .nextIpoInfo(nextUpcomingIpo == null ? null : NextIpoInfo.from(nextUpcomingIpo))
                .build();
    }

    @Getter
    @Builder
    public static class NextIpoInfo {
        private final Long ipoId;
        private final String ticker;
        private final LocalDate subscriptionStartDate;

        public static NextIpoInfo from(Ipo ipo) {
            return NextIpoInfo.builder()
                    .ipoId(ipo.getId())
                    .ticker(ipo.getTicker())
                    .subscriptionStartDate(ipo.getSubscriptionStartDate())
                    .build();
        }
    }
}
