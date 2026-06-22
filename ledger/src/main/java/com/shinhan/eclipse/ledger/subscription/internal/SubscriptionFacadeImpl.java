package com.shinhan.eclipse.ledger.subscription.internal;

import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.domain.account.FinancialAccount;
import com.shinhan.eclipse.domain.ipo.Ipo;
import com.shinhan.eclipse.domain.subscription.IpoSubscription;
import java.util.Optional;
import com.shinhan.eclipse.ledger.accountlink.AccountLinkService;
import com.shinhan.eclipse.ledger.event.SubscriptionConfirmedEvent;
import com.shinhan.eclipse.ledger.subscription.SubscriptionFacade;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
class SubscriptionFacadeImpl implements SubscriptionFacade {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionFacadeImpl.class);

    private final IpoSubscriptionRepository subscriptionRepository;
    private final IpoRepository ipoRepository;
    private final AccountLinkService accountLinkService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public IpoSubscription requestSubscription(IpoSubscription draft) {
        Ipo ipo = ipoRepository.findById(draft.getIpoId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        validateSubscriptionPeriod(ipo);
        validateOfferPrice(ipo, draft.getOfferPrice());

        FinancialAccount account = accountLinkService.getLinkedAccount(draft.getUserId(), draft.getSecuritiesAccountId());
        if (!account.hasSufficientBalance(draft.getSubscriptionAmount())) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        IpoSubscription saved = subscriptionRepository.save(draft);
        log.info("청약 신청 생성: subscriptionId={}, userId={}, ipoId={}", saved.getId(), saved.getUserId(), saved.getIpoId());
        return saved;
    }

    @Override
    @Transactional
    public IpoSubscription confirmSubscription(Long subscriptionId, Long userId) {
        // 동일 청약 동시 확정(REQ-05-93-01) 방지: 상태 체크 전에 청약 row 자체를 잠가서,
        // 두 번째 요청은 첫 번째가 커밋(CONFIRMED)된 뒤에야 락을 얻고 isRequested()==false로 막힌다.
        IpoSubscription subscription = subscriptionRepository.findByIdAndUserIdForUpdate(subscriptionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        if (!subscription.isRequested()) {
            throw new BusinessException(ErrorCode.SUBSCRIPTION_CONFLICT);
        }

        Ipo ipo = ipoRepository.findById(subscription.getIpoId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        validateSubscriptionPeriod(ipo);

        // 잔액 재검증(REQ-05-92-01): 계좌도 락을 잡아 다른 청약의 동시 확정으로 인한 잔액 변동을 반영
        FinancialAccount account = accountLinkService.lockAccount(userId, subscription.getSecuritiesAccountId());
        if (!account.hasSufficientBalance(subscription.getSubscriptionAmount())) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        accountLinkService.deduct(userId, account.getId(), subscription.getSubscriptionAmount());
        subscription.confirm();

        eventPublisher.publishEvent(new SubscriptionConfirmedEvent(
                subscription.getId(), userId, subscription.getIpoId(), subscription.getSubscriptionAmount()));

        log.info("청약 확정: subscriptionId={}, userId={}", subscription.getId(), userId);
        return subscription;
    }

    @Override
    @Transactional
    public void cancelSubscription(Long subscriptionId, Long userId) {
        IpoSubscription subscription = subscriptionRepository.findByIdAndUserIdForUpdate(subscriptionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        if (!subscription.isRequested()) {
            throw new BusinessException(ErrorCode.SUBSCRIPTION_PERIOD_INVALID);
        }

        Ipo ipo = ipoRepository.findById(subscription.getIpoId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        validateSubscriptionPeriod(ipo);

        subscription.cancel();
        log.info("청약 취소: subscriptionId={}, userId={}", subscription.getId(), userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IpoSubscription> getSubscriptions(Long userId, Long ipoId, String status) {
        boolean hasIpoId = ipoId != null;
        boolean hasStatus = StringUtils.hasText(status);

        if (hasIpoId && hasStatus) {
            return subscriptionRepository.findByUserIdAndIpoIdAndSubscriptionStatus(userId, ipoId, status);
        }
        if (hasIpoId) {
            return subscriptionRepository.findByUserIdAndIpoId(userId, ipoId);
        }
        if (hasStatus) {
            return subscriptionRepository.findByUserIdAndSubscriptionStatus(userId, status);
        }
        return subscriptionRepository.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public IpoSubscription getSubscriptionResult(Long subscriptionResultId, Long userId) {
        return subscriptionRepository.findByIdAndUserId(subscriptionResultId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<IpoSubscription> getSubscriptionResults(Long userId, Long subscriptionId) {
        if (subscriptionId != null) {
            return subscriptionRepository.findByIdAndUserId(subscriptionId, userId)
                    .map(List::of)
                    .orElse(List.of());
        }
        return subscriptionRepository.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Ipo getIpo(Long ipoId) {
        return ipoRepository.findById(ipoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Ipo> findNextUpcomingIpo() {
        return ipoRepository.findFirstByIpoStatusOrderBySubscriptionStartDateAsc("UPCOMING");
    }

    private void validateSubscriptionPeriod(Ipo ipo) {
        LocalDate today = LocalDate.now();
        if (today.isBefore(ipo.getSubscriptionStartDate()) || today.isAfter(ipo.getSubscriptionEndDate())) {
            throw new BusinessException(ErrorCode.SUBSCRIPTION_PERIOD_INVALID);
        }
    }

    /** 클라이언트가 보낸 offerPrice가 서버 기준 공모가와 일치하는지 검증 (변조 방지). */
    private void validateOfferPrice(Ipo ipo, BigDecimal offerPrice) {
        if (ipo.getConfirmedOfferPrice() != null) {
            if (offerPrice.compareTo(ipo.getConfirmedOfferPrice()) != 0) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "공모가가 일치하지 않습니다.");
            }
            return;
        }
        if (ipo.getOfferPriceMin() != null && offerPrice.compareTo(ipo.getOfferPriceMin()) < 0
                || ipo.getOfferPriceMax() != null && offerPrice.compareTo(ipo.getOfferPriceMax()) > 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "공모가가 희망 범위를 벗어났습니다.");
        }
    }
}
