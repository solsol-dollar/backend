package com.shinhan.eclipse.ledger.subscription;

import com.shinhan.eclipse.domain.ipo.Ipo;
import com.shinhan.eclipse.domain.subscription.IpoSubscription;
import java.util.List;
import java.util.Optional;

public interface SubscriptionFacade {
    /** draft 는 IpoSubscription.request(...) 로 만든, 아직 저장되지 않은 엔티티 */
    IpoSubscription requestSubscription(IpoSubscription draft);
    IpoSubscription confirmSubscription(Long subscriptionId, Long userId);
    void cancelSubscription(Long subscriptionId, Long userId);
    List<IpoSubscription> getSubscriptions(Long userId, Long ipoId, String status);

    /** 배정 결과 단건 조회 (본인 소유). 없으면 NOT_FOUND. */
    IpoSubscription getSubscriptionResult(Long subscriptionResultId, Long userId);

    /** 배정 결과 목록 조회. subscriptionId가 주어지면 해당 건만 필터링. */
    List<IpoSubscription> getSubscriptionResults(Long userId, Long subscriptionId);

    Ipo getIpo(Long ipoId);

    /** 청약 시작일이 가장 빠른 예정(UPCOMING) IPO. */
    Optional<Ipo> findNextUpcomingIpo();
}
