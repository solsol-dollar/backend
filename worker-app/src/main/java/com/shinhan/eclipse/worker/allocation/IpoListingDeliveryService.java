package com.shinhan.eclipse.worker.allocation;

import com.shinhan.eclipse.domain.holding.Holding;
import com.shinhan.eclipse.domain.holding.HoldingLot;
import com.shinhan.eclipse.domain.ipo.Ipo;
import com.shinhan.eclipse.domain.notification.Notification;
import com.shinhan.eclipse.domain.subscription.IpoSubscription;
import com.shinhan.eclipse.worker.allocation.repository.WorkerHoldingLotRepository;
import com.shinhan.eclipse.worker.allocation.repository.WorkerHoldingRepository;
import com.shinhan.eclipse.worker.allocation.repository.WorkerIpoSubscriptionRepository;
import com.shinhan.eclipse.worker.allocation.repository.WorkerNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * IpoListingJob의 입고 처리를 별도 빈으로 분리해 {@code @Transactional} 프록시를 통해 호출되도록 한다.
 * (같은 빈 내부에서 this.메서드() 로 호출하면 AOP 프록시를 거치지 않아 트랜잭션이 적용되지 않는다.)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IpoListingDeliveryService {

    private final WorkerIpoSubscriptionRepository subscriptionRepository;
    private final WorkerHoldingRepository holdingRepository;
    private final WorkerHoldingLotRepository holdingLotRepository;
    private final WorkerNotificationRepository notificationRepository;

    @Transactional
    public int deliverForIpo(Ipo ipo) {
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

            subscription.deposit();

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
