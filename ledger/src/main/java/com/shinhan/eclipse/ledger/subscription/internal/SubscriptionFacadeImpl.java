package com.shinhan.eclipse.ledger.subscription.internal;

import com.shinhan.eclipse.domain.subscription.IpoSubscription;
import com.shinhan.eclipse.ledger.subscription.SubscriptionFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
class SubscriptionFacadeImpl implements SubscriptionFacade {

    private final IpoSubscriptionRepository subscriptionRepository;

    @Override
    @Transactional
    public IpoSubscription requestSubscription(Long userId, Long ipoId, Long securitiesAccountId, Integer shares, BigDecimal offerPrice) {
        // TODO: 잔액 검증 → 청약 생성
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    @Transactional
    public IpoSubscription confirmSubscription(Long subscriptionId, Long userId) {
        // TODO: 잔액 재검증(REQ-05-92) → 비관적 락 → 차감 → CONFIRMED → SubscriptionConfirmedEvent 발행
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public List<IpoSubscription> getSubscriptions(Long userId) {
        // TODO: 구현
        throw new UnsupportedOperationException("TODO");
    }
}
