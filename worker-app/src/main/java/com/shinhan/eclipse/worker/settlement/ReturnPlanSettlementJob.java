package com.shinhan.eclipse.worker.settlement;

import com.shinhan.eclipse.domain.ipo.Ipo;
import com.shinhan.eclipse.domain.returnplan.ReturnPlan;
import com.shinhan.eclipse.domain.subscription.IpoSubscription;
import com.shinhan.eclipse.worker.allocation.repository.WorkerIpoRepository;
import com.shinhan.eclipse.worker.allocation.repository.WorkerIpoSubscriptionRepository;
import com.shinhan.eclipse.worker.settlement.repository.WorkerReturnPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 환불일(상장일+1일) 정산 배치. "DRAFT" 상태인 리턴 플랜 중 출처 청약의 IPO refundDate가 오늘인
 * 것들을 찾아 ledger-app의 내부 실행 API({@code PUT /internal/return-plans/{id}/execute})를 호출한다.
 *
 * <p>매일 한국시간 21:00 1회 실행. 리턴 플랜 수정 마감(20:00, {@code ReturnPlanFacadeImpl.EDIT_CUTOFF_TIME})
 * 보다 1시간 뒤로 잡아, 사용자가 환불일 당일 저녁까지 비율을 조정할 시간을 충분히 준 다음 실행한다.
 * 실행 자체(기본값 적용, 계좌 적립, 락)는 ledger-app 쪽 비즈니스 로직을 그대로 재사용하므로 여기서는
 * "오늘 실행해야 할 대상을 찾아 호출"하는 역할만 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReturnPlanSettlementJob {

    private static final String DRAFT = "DRAFT";

    private final WorkerReturnPlanRepository returnPlanRepository;
    private final WorkerIpoSubscriptionRepository subscriptionRepository;
    private final WorkerIpoRepository ipoRepository;
    private final ReturnPlanExecutionClient executionClient;

    @Scheduled(cron = "0 0 21 * * *", zone = "Asia/Seoul")
    public void run() {
        LocalDate today = LocalDate.now();
        List<ReturnPlan> draftPlans = returnPlanRepository.findByPlanStatus(DRAFT);
        if (draftPlans.isEmpty()) {
            return;
        }

        List<ReturnPlan> targets = draftPlans.stream()
                .filter(plan -> isRefundDateToday(plan, today))
                .toList();
        if (targets.isEmpty()) {
            return;
        }

        log.info("ReturnPlanSettlementJob 시작: 대상 {}건", targets.size());
        int executed = 0;
        for (ReturnPlan plan : targets) {
            try {
                executionClient.execute(plan.getId());
                executed++;
            } catch (Exception e) {
                log.error("리턴 플랜 정산 실행 실패: returnPlanId={}", plan.getId(), e);
            }
        }
        log.info("ReturnPlanSettlementJob 완료: {}건 실행", executed);
    }

    private boolean isRefundDateToday(ReturnPlan plan, LocalDate today) {
        Optional<IpoSubscription> subscription = subscriptionRepository.findById(plan.getSubscriptionId());
        if (subscription.isEmpty()) {
            return false;
        }
        Optional<Ipo> ipo = ipoRepository.findById(subscription.get().getIpoId());
        return ipo.map(Ipo::getRefundDate).filter(today::equals).isPresent();
    }
}
