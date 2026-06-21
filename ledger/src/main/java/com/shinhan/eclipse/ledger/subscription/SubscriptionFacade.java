package com.shinhan.eclipse.ledger.subscription;

import com.shinhan.eclipse.domain.subscription.IpoSubscription;
import java.util.List;

public interface SubscriptionFacade {
    /** draft 는 IpoSubscription.request(...) 로 만든, 아직 저장되지 않은 엔티티 */
    IpoSubscription requestSubscription(IpoSubscription draft);
    IpoSubscription confirmSubscription(Long subscriptionId, Long userId);
    void cancelSubscription(Long subscriptionId, Long userId);
    List<IpoSubscription> getSubscriptions(Long userId, Long ipoId, String status);
}
