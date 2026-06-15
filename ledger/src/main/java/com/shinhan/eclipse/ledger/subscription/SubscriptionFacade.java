package com.shinhan.eclipse.ledger.subscription;

import com.shinhan.eclipse.domain.subscription.IpoSubscription;
import java.math.BigDecimal;
import java.util.List;

public interface SubscriptionFacade {
    IpoSubscription requestSubscription(Long userId, Long ipoId, Long securitiesAccountId, Integer shares, BigDecimal offerPrice);
    IpoSubscription confirmSubscription(Long subscriptionId, Long userId);
    List<IpoSubscription> getSubscriptions(Long userId);
}
