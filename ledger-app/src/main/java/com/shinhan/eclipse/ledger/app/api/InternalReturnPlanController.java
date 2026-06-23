package com.shinhan.eclipse.ledger.app.api;

import com.shinhan.eclipse.common.response.ApiResponse;
import com.shinhan.eclipse.domain.returnplan.ReturnPlan;
import com.shinhan.eclipse.ledger.returnplan.ReturnPlanFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 환불일 정산 배치(worker-app)가 호출하는 내부 전용 API. 사용자 액션이 아니라 시스템 트리거이므로
 * userId 스코프 없이 동작하고, 비율 합이 100이 아니면 SECURITIES 100%로 기본값을 적용한 뒤 실행한다.
 */
@RestController
@RequestMapping("/internal/return-plans")
@RequiredArgsConstructor
public class InternalReturnPlanController {

    private final ReturnPlanFacade returnPlanFacade;

    @PutMapping("/{returnPlanId}/execute")
    public ResponseEntity<ApiResponse<Map<String, Object>>> execute(@PathVariable("returnPlanId") Long returnPlanId) {
        ReturnPlan plan = returnPlanFacade.executeReturnPlan(returnPlanId);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "returnPlanId", plan.getId(),
                "planStatus", plan.getPlanStatus(),
                "executedAt", plan.getExecutedAt())));
    }
}
