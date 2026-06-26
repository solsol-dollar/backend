package com.shinhan.eclipse.worker.allocation;

import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.domain.account.BalanceHold;
import com.shinhan.eclipse.domain.account.FinancialAccount;
import com.shinhan.eclipse.domain.ipo.Ipo;
import com.shinhan.eclipse.domain.notification.Notification;
import com.shinhan.eclipse.domain.subscription.IpoSubscription;
import com.shinhan.eclipse.worker.allocation.dto.AllocationApplicant;
import com.shinhan.eclipse.worker.allocation.dto.AllocationResult;
import com.shinhan.eclipse.worker.allocation.repository.WorkerBalanceHoldRepository;
import com.shinhan.eclipse.worker.allocation.repository.WorkerFinancialAccountRepository;
import com.shinhan.eclipse.worker.allocation.repository.WorkerIpoRepository;
import com.shinhan.eclipse.worker.allocation.repository.WorkerIpoSubscriptionRepository;
import com.shinhan.eclipse.worker.allocation.repository.WorkerNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 상장일 배정 배치. "상장일 개장 1시간 전에 배정 결과 발표"라는 요구사항을 매일 21:30(KST) 1회
 * 실행으로 단순화해 흉내낸다 — 실제 증권사 배정 통보 연동 전까지의 MOCK 구현.
 *
 * <p>미국 정규장 개장(9:30 AM ET)은 한국시간으로 22:30(EDT)~23:30(EST)이라, 21:30 KST는
 * 서머타임 여부와 무관하게 항상 개장 1시간 이상 전이다(EDT 기준 정확히 1시간 전, EST 기준 2시간 전).
 *
 * <p>listingDate가 오늘인 IPO들을 찾아, 그 IPO에 대해 CONFIRMED 상태이고 아직 배정 결과가 없는
 * 청약들을 모아 {@link IpoAllocationEngine}으로 배정한 뒤 각 청약에 반영한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IpoAllocationJob {

    private final WorkerIpoRepository ipoRepository;
    private final WorkerIpoSubscriptionRepository subscriptionRepository;
    private final WorkerNotificationRepository notificationRepository;
    private final WorkerBalanceHoldRepository balanceHoldRepository;
    private final WorkerFinancialAccountRepository financialAccountRepository;

    @Scheduled(cron = "0 30 21 * * *", zone = "Asia/Seoul")
    public void run() {
        LocalDate today = LocalDate.now();
        List<Ipo> listingTodayIpos = ipoRepository.findByListingDate(today);
        if (listingTodayIpos.isEmpty()) {
            return;
        }

        log.info("IpoAllocationJob 시작: 대상 IPO {}건", listingTodayIpos.size());
        int totalAllocated = 0;
        for (Ipo ipo : listingTodayIpos) {
            try {
                totalAllocated += allocateForIpo(ipo);
            } catch (Exception e) {
                log.error("IPO 배정 실패: ipoId={}, ticker={}", ipo.getId(), ipo.getTicker(), e);
            }
        }
        log.info("IpoAllocationJob 완료: 청약 {}건 배정", totalAllocated);
    }

    @Transactional
    int allocateForIpo(Ipo ipo) {
        List<IpoSubscription> subscriptions = subscriptionRepository
                .findByIpoIdAndSubscriptionStatusAndResultStatusIsNull(ipo.getId(), "CONFIRMED");
        if (subscriptions.isEmpty()) {
            return 0;
        }

        List<AllocationApplicant> applicants = subscriptions.stream()
                .map(s -> new AllocationApplicant(s.getId(), s.getRequestedShares()))
                .toList();

        List<AllocationResult> results = IpoAllocationEngine.allocate(applicants);
        Map<Long, IpoSubscription> bySubscriptionId = subscriptions.stream()
                .collect(Collectors.toMap(IpoSubscription::getId, s -> s));

        for (AllocationResult result : results) {
            IpoSubscription subscription = bySubscriptionId.get(result.customerId());
            BigDecimal ratePercent = result.allocationRate().multiply(BigDecimal.valueOf(100));

            BalanceHold hold = balanceHoldRepository.findBySubscriptionId(subscription.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                            "BalanceHold 없음: subscriptionId=" + subscription.getId()));
            FinancialAccount account = financialAccountRepository.findById(hold.getAccountId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

            subscription.allocate(result.finalAllocated(), ratePercent, hold.getAmount());

            account.settleReserved(subscription.getAllocatedAmount());
            account.releaseReserved(subscription.getRefundAmount());
            if (result.finalAllocated() > 0) {
                hold.settle();
            } else {
                hold.release();
            }

            notificationRepository.save(Notification.create(
                    subscription.getUserId(),
                    "IPO_ALLOCATION",
                    "IPO 배정 결과가 나왔어요.",
                    ipo.getCompanyName() + " 배정 결과를 지금 확인해보세요.",
                    "IPO", ipo.getId()
            ));
        }

        log.info("IPO 배정 완료: ipoId={}, ticker={}, 청약 {}건", ipo.getId(), ipo.getTicker(), subscriptions.size());
        return subscriptions.size();
    }
}
