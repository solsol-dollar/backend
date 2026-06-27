package com.shinhan.eclipse.ledger.subscription;

import com.shinhan.eclipse.domain.ipo.Ipo;
import com.shinhan.eclipse.domain.subscription.IpoSubscription;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SubscriptionFacade {
    /**
     * 사용자가 입력한 청약신청금액(USD)을 서버에서 주수로 환산해 청약을 생성한다.
     * shares = floor(subscriptionAmount / offerPrice) — 환산 후 실제 청약금액은 shares * offerPrice로
     * 재계산되므로 사용자가 입력한 금액보다 같거나 약간 작을 수 있다 (명세 외 변경).
     */
    IpoSubscription requestSubscription(Long userId, Long ipoId, Long securitiesAccountId,
                                         BigDecimal subscriptionAmount, BigDecimal offerPrice);
    IpoSubscription confirmSubscription(Long subscriptionId, Long userId);

    /** 취소된 청약을 반환한다 (응답에 환불금액/환불계좌를 채우기 위해 명세 외 변경). */
    IpoSubscription cancelSubscription(Long subscriptionId, Long userId);

    /** from/to(청약일 기준)는 명세 외 추가 — 조회 조건 설정 모달용. */
    List<IpoSubscription> getSubscriptions(Long userId, Long ipoId, String status, LocalDate from, LocalDate to);

    /** 배정 결과 단건 조회 (본인 소유). 없으면 NOT_FOUND. */
    IpoSubscription getSubscriptionResult(Long subscriptionResultId, Long userId);

    /** 배정 결과 목록 조회. subscriptionId가 주어지면 해당 건만 필터링. */
    List<IpoSubscription> getSubscriptionResults(Long userId, Long subscriptionId);

    Ipo getIpo(Long ipoId);

    /** 청약 시작일이 가장 빠른 예정(UPCOMING) IPO. */
    Optional<Ipo> findNextUpcomingIpo();

    /** 해당 사용자가 ipoId 종목을 이미 청약(REQUESTED/CONFIRMED)했는지 확인. */
    boolean isAlreadySubscribed(Long userId, Long ipoId);
}
