package com.shinhan.eclipse.ledger.returnplan;

import com.shinhan.eclipse.domain.returnplan.ReturnPlan;
import com.shinhan.eclipse.domain.returnplan.ReturnPlanAllocation;
import com.shinhan.eclipse.domain.returnplan.ReturnPlanPreset;
import com.shinhan.eclipse.ledger.returnplan.dto.AllocationItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ReturnPlanFacade {
    ReturnPlan createReturnPlan(Long userId, Long subscriptionId);
    ReturnPlan getReturnPlan(Long returnPlanId, Long userId);
    List<ReturnPlanAllocation> getAllocations(Long returnPlanId);

    /** 여러 플랜의 분배 내역을 returnPlanId 기준으로 묶어서 한 번에 가져온다 (대시보드 요약 등 N+1 회피용). */
    Map<Long, List<ReturnPlanAllocation>> getAllocationsByPlanIds(List<Long> returnPlanIds);

    ReturnPlan updateRatios(Long returnPlanId, Long userId, List<AllocationItem> items);

    /** 조회 조건(기간/상태) 필터를 DB 단에서 적용한 뒤 페이지네이션해서 반환한다. */
    Page<ReturnPlan> getReturnPlans(Long userId, LocalDate from, LocalDate to, String status, Pageable pageable);

    /** 대시보드 요약처럼 페이지네이션 없이 전체가 필요한 경우. */
    List<ReturnPlan> getAllReturnPlans(Long userId);

    /** ALLOC-002 의 hasReturnPlan 플래그 계산용. */
    boolean existsBySubscriptionId(Long subscriptionId);

    /** 분배 프리셋 목록 (return_plan_presets 테이블 기반, 명세 외 추가). */
    List<ReturnPlanPreset> getPresets();

    /** 프리셋 비율을 그대로 적용 (명세 외 추가). */
    ReturnPlan applyPreset(Long returnPlanId, Long userId, String presetCode);

    /**
     * 환불일 배치가 호출하는 분배 실행 (명세 외 추가). 사용자 액션이 아니라 시스템 트리거이므로 userId 스코프가 없다.
     * 비율 합이 100이 아니면 SECURITIES 100%로 기본값을 적용한 뒤 실행한다.
     */
    ReturnPlan executeReturnPlan(Long returnPlanId);
}
