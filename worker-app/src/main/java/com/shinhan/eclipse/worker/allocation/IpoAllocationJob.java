package com.shinhan.eclipse.worker.allocation;

import com.shinhan.eclipse.domain.ipo.Ipo;
import com.shinhan.eclipse.domain.subscription.IpoSubscription;
import com.shinhan.eclipse.worker.allocation.dto.AllocationApplicant;
import com.shinhan.eclipse.worker.allocation.dto.AllocationResult;
import com.shinhan.eclipse.worker.allocation.repository.WorkerIpoRepository;
import com.shinhan.eclipse.worker.allocation.repository.WorkerIpoSubscriptionRepository;
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
 * 상장일 배정 배치. 정규장 개장 후 1~3시간 이내 배정결과가 나온다는 요구사항을 매일 11:00(KST)
 * 1회 실행으로 단순화해 흉내낸다 — 실제 증권사 배정 통보 연동 전까지의 MOCK 구현.
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

    @Scheduled(cron = "0 0 11 * * *", zone = "Asia/Seoul")
    public void run() {
        LocalDate today = LocalDate.now();
        List<Ipo> listingTodayIpos = ipoRepository.findByListingDate(today);
        if (listingTodayIpos.isEmpty()) {
            return;
        }

        log.info("IpoAllocationJob 시작: 대상 IPO {}건", listingTodayIpos.size());
        int totalAllocated = 0;
        for (Ipo ipo : listingTodayIpos) {
            totalAllocated += allocateForIpo(ipo);
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
            subscription.allocate(result.finalAllocated(), ratePercent);
        }

        log.info("IPO 배정 완료: ipoId={}, ticker={}, 청약 {}건", ipo.getId(), ipo.getTicker(), subscriptions.size());
        return subscriptions.size();
    }
}
