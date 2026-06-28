package com.shinhan.eclipse.worker.allocation;

import com.shinhan.eclipse.domain.holding.Holding;
import com.shinhan.eclipse.domain.holding.HoldingLot;
import com.shinhan.eclipse.domain.ipo.Ipo;
import com.shinhan.eclipse.domain.notification.Notification;
import com.shinhan.eclipse.domain.subscription.IpoSubscription;
import com.shinhan.eclipse.worker.allocation.repository.WorkerHoldingLotRepository;
import com.shinhan.eclipse.worker.allocation.repository.WorkerHoldingRepository;
import com.shinhan.eclipse.worker.allocation.repository.WorkerIpoRepository;
import com.shinhan.eclipse.worker.allocation.repository.WorkerIpoSubscriptionRepository;
import com.shinhan.eclipse.worker.allocation.repository.WorkerNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * 상장일 개장(22:30 KST, EDT 기준 9:30 AM ET)에 배정된 주식을 보유 계좌에 입고시킨다.
 * IpoAllocationJob(21:30 KST)이 먼저 실행돼 resultStatus = "COMPLETED"를 기록한 뒤
 * 이 Job이 실행된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IpoListingJob {

    private final WorkerIpoRepository ipoRepository;
    private final WorkerIpoSubscriptionRepository subscriptionRepository;
    private final WorkerHoldingRepository holdingRepository;
    private final WorkerHoldingLotRepository holdingLotRepository;
    private final WorkerNotificationRepository notificationRepository;

    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    private boolean isNewYorkInDst() {
        return NEW_YORK.getRules().isDaylightSavings(Instant.now());
    }

    /** EDT(써머타임): 개장 = 22:30 KST (9:30 AM ET) */
    @Scheduled(cron = "0 30 22 * * *", zone = "Asia/Seoul")
    public void runEdt() {
        if (isNewYorkInDst()) run();
    }

    /** EST(겨울): 개장 = 23:30 KST (9:30 AM ET) */
    @Scheduled(cron = "0 30 23 * * *", zone = "Asia/Seoul")
    public void runEst() {
        if (!isNewYorkInDst()) run();
    }

    void run() {
        LocalDate today = LocalDate.now();
        List<Ipo> listingTodayIpos = ipoRepository.findByListingDate(today);
        if (listingTodayIpos.isEmpty()) {
            return;
        }

        log.info("IpoListingJob 시작: 대상 IPO {}건", listingTodayIpos.size());
        int totalDelivered = 0;
        for (Ipo ipo : listingTodayIpos) {
            if (ipo.getProductId() == null) {
                log.warn("IpoListingJob: productId 없음 — 건너뜀 ipoId={}, ticker={}", ipo.getId(), ipo.getTicker());
                continue;
            }
            try {
                totalDelivered += deliverForIpo(ipo);
            } catch (Exception e) {
                log.error("IpoListingJob 입고 실패: ipoId={}, ticker={}", ipo.getId(), ipo.getTicker(), e);
            }
        }
        log.info("IpoListingJob 완료: {}건 입고", totalDelivered);
    }

    @Transactional
    int deliverForIpo(Ipo ipo) {
        List<IpoSubscription> subscriptions = subscriptionRepository
                .findByIpoIdAndResultStatusAndAllocatedSharesGreaterThan(ipo.getId(), "COMPLETED", 0);
        if (subscriptions.isEmpty()) {
            return 0;
        }

        for (IpoSubscription subscription : subscriptions) {
            Long userId = subscription.getUserId();
            Long productId = ipo.getProductId();
            int shares = subscription.getAllocatedShares();

            // 재실행 방어: 이미 입고된 청약은 건너뜀 (holding_lots unique 제약과 이중 방어)
            if (holdingLotRepository.existsBySourceTypeAndSourceId("IPO_ALLOCATION", subscription.getId())) {
                log.warn("IpoListingJob: 이미 입고된 청약 — 건너뜀 subscriptionId={}", subscription.getId());
                continue;
            }

            Holding holding = holdingRepository
                    .findByUserIdAndProductIdForUpdate(userId, productId)
                    .orElse(null);

            if (holding == null) {
                holding = Holding.create(userId, productId, shares, subscription.getOfferPrice());
                holding = holdingRepository.save(holding);
            } else {
                holding.addBuy(shares, subscription.getOfferPrice());
            }

            holdingLotRepository.save(
                    HoldingLot.ofIpoAllocation(holding.getId(), userId, productId,
                            subscription.getId(), shares, subscription.getOfferPrice()));

            notificationRepository.save(Notification.create(
                    userId,
                    "IPO_LISTING",
                    ipo.getCompanyName() + " 주식이 입고됐어요.",
                    shares + "주가 내 계좌에 추가됐습니다.",
                    "IPO", ipo.getId()
            ));
        }

        log.info("IpoListingJob 입고 완료: ipoId={}, ticker={}, {}건", ipo.getId(), ipo.getTicker(), subscriptions.size());
        return subscriptions.size();
    }
}
