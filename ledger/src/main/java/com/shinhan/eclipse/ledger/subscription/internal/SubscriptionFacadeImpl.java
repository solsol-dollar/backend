package com.shinhan.eclipse.ledger.subscription.internal;

import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.domain.account.BalanceHold;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
class SubscriptionFacadeImpl implements SubscriptionFacade {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionFacadeImpl.class);

    private final IpoSubscriptionRepository subscriptionRepository;
    private final IpoRepository ipoRepository;
    private final AccountLinkService accountLinkService;
    private final BalanceHoldRepository balanceHoldRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    @Transactional
    public IpoSubscription requestSubscription(Long userId, Long ipoId, Long securitiesAccountId,
                                                BigDecimal subscriptionAmount, BigDecimal offerPrice) {
        Ipo ipo = ipoRepository.findById(ipoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        validateSubscriptionPeriod(ipo, true);
        validateOfferPrice(ipo, offerPrice);

        if (ipo.getMinimumSubscriptionAmount() != null
                && subscriptionAmount.compareTo(ipo.getMinimumSubscriptionAmount()) < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "청약신청금액은 최소 " + ipo.getMinimumSubscriptionAmount() + " 이상이어야 합니다.");
        }

        int shares = subscriptionAmount.divide(offerPrice, 0, RoundingMode.DOWN).intValue();
        if (shares < 1) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "청약신청금액이 공모가 1주 가격보다 작습니다.");
        }

        IpoSubscription draft = IpoSubscription.request(userId, ipoId, securitiesAccountId, shares, offerPrice, subscriptionAmount);

        // 신청 시점에 실제 현금은 그대로 두고 reservedBalance만 잠근다 (즉시 출금 아님 - 홀딩 모델).
        accountLinkService.reserve(userId, securitiesAccountId, draft.getSubscriptionAmount());
        FinancialAccount account = accountLinkService.lockAccount(userId, securitiesAccountId);

        IpoSubscription saved = subscriptionRepository.save(draft);
        balanceHoldRepository.save(BalanceHold.lock(account.getId(), saved.getId(), draft.getSubscriptionAmount()));

        log.info("청약 신청 생성(홀딩): subscriptionId={}, userId={}, ipoId={}, shares={}, holdAmount={}",
                saved.getId(), saved.getUserId(), saved.getIpoId(), shares, draft.getSubscriptionAmount());
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
        validateSubscriptionPeriod(ipo, true);

        // 금액은 requestSubscription 시점에 이미 reserve(홀딩)되어 있으므로, 확정 단계에서는
        // 추가 차감이 없다. 실제 차감(settle)은 배정 결과가 확정될 때 일어난다.
        subscription.confirm();

        eventPublisher.publishEvent(new SubscriptionConfirmedEvent(
                subscription.getId(), userId, subscription.getIpoId(), subscription.getSubscriptionAmount()));

        log.info("청약 확정: subscriptionId={}, userId={}", subscription.getId(), userId);
        return subscription;
    }

    @Override
    @Transactional
    public IpoSubscription cancelSubscription(Long subscriptionId, Long userId) {
        IpoSubscription subscription = subscriptionRepository.findByIdAndUserIdForUpdate(subscriptionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        if (!subscription.isRequested()) {
            throw new BusinessException(ErrorCode.SUBSCRIPTION_PERIOD_INVALID);
        }

        Ipo ipo = ipoRepository.findById(subscription.getIpoId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        validateCancellationPeriod(ipo);

        // LOCKED 상태인 hold가 있을 때만 계좌 잠금 해제 + hold 해제를 수행한다.
        balanceHoldRepository.findBySubscriptionId(subscription.getId())
                .filter(BalanceHold::isLocked)
                .ifPresent(hold -> {
                    accountLinkService.releaseReserved(userId, subscription.getSecuritiesAccountId(), subscription.getSubscriptionAmount());
                    hold.release();
                });

        subscription.cancel();
        log.info("청약 취소: subscriptionId={}, userId={}", subscription.getId(), userId);
        return subscription;
    }

    @Override
    @Transactional(readOnly = true)
    public List<IpoSubscription> getSubscriptions(Long userId, Long ipoId, String status, LocalDate from, LocalDate to) {
        String normalizedStatus = StringUtils.hasText(status) ? status : null;
        LocalDateTime fromDateTime = from == null ? null : from.atStartOfDay();
        LocalDateTime toDateTime = to == null ? null : to.atTime(LocalTime.MAX);
        return subscriptionRepository.search(userId, ipoId, normalizedStatus, fromDateTime, toDateTime);
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
        List<Ipo> upcoming = ipoRepository.findUpcomingOrderByStartDate(LocalDate.now(), PageRequest.of(0, 1));
        return upcoming.stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAlreadySubscribed(Long userId, Long ipoId) {
        return subscriptionRepository.existsByUserIdAndIpoIdAndSubscriptionStatusIn(
                userId, ipoId, List.of("REQUESTED", "CONFIRMED"));
    }

    private static final LocalTime SUBSCRIPTION_OPEN  = LocalTime.of(9, 0);
    private static final LocalTime SUBSCRIPTION_CLOSE = LocalTime.of(17, 0);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /** 취소 가능 구간: subscriptionStartDate 09:00 ~ subscriptionEndDate 17:00 (KST) */
    private void validateCancellationPeriod(Ipo ipo) {
        LocalDateTime now = LocalDateTime.now(clock.withZone(KST));
        LocalDateTime start = ipo.getSubscriptionStartDate().atTime(SUBSCRIPTION_OPEN);
        LocalDateTime end   = ipo.getSubscriptionEndDate().atTime(SUBSCRIPTION_CLOSE);
        if (now.isBefore(start) || !now.isBefore(end)) {
            throw new BusinessException(ErrorCode.SUBSCRIPTION_PERIOD_INVALID,
                    "청약 취소는 신청 시작일 09:00 ~ 마감일 17:00(KST) 사이에만 가능합니다.");
        }
    }

    private void validateSubscriptionPeriod(Ipo ipo) {
        validateSubscriptionPeriod(ipo, false);
    }

    private void validateSubscriptionPeriod(Ipo ipo, boolean checkTime) {
        LocalDateTime now = LocalDateTime.now(clock.withZone(KST));
        LocalDate today = now.toLocalDate();
        if (today.isBefore(ipo.getSubscriptionStartDate()) || today.isAfter(ipo.getSubscriptionEndDate())) {
            throw new BusinessException(ErrorCode.SUBSCRIPTION_PERIOD_INVALID);
        }
        if (checkTime) {
            LocalTime time = now.toLocalTime();
            if (time.isBefore(SUBSCRIPTION_OPEN) || !time.isBefore(SUBSCRIPTION_CLOSE)) {
                throw new BusinessException(ErrorCode.SUBSCRIPTION_PERIOD_INVALID, "청약 신청은 09:00 ~ 17:00(KST)에만 가능합니다.");
            }
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
