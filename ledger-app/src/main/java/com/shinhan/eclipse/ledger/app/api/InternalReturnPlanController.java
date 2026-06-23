package com.shinhan.eclipse.ledger.app.api;

import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.common.response.ApiResponse;
import com.shinhan.eclipse.domain.returnplan.ReturnPlan;
import com.shinhan.eclipse.ledger.returnplan.ReturnPlanFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/**
 * 환불일 정산 배치(worker-app)가 호출하는 내부 전용 API. 사용자 액션이 아니라 시스템 트리거이므로
 * userId 스코프 없이 동작하고, 비율 합이 100이 아니면 SECURITIES 100%로 기본값을 적용한 뒤 실행한다.
 *
 * <p>{@code /internal/**} 경로는 네트워크 격리(방화벽/내부망)만으로 보호되고 있었는데, 그 격리가
 * 깨지면 누구나 호출할 수 있어서 공유 비밀키 헤더로 한 번 더 검증한다.
 */
@RestController
@RequestMapping("/internal/return-plans")
@RequiredArgsConstructor
public class InternalReturnPlanController {

    private static final String API_KEY_HEADER = "X-Internal-Api-Key";

    private final ReturnPlanFacade returnPlanFacade;

    @Value("${eclipse.internal.api-key:}")
    private String expectedApiKey;

    @PutMapping("/{returnPlanId}/execute")
    public ResponseEntity<ApiResponse<Map<String, Object>>> execute(
            @PathVariable("returnPlanId") Long returnPlanId,
            @RequestHeader(name = API_KEY_HEADER, required = false) String apiKey) {
        validateApiKey(apiKey);
        ReturnPlan plan = returnPlanFacade.executeReturnPlan(returnPlanId);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "returnPlanId", plan.getId(),
                "planStatus", plan.getPlanStatus(),
                "executedAt", plan.getExecutedAt())));
    }

    private void validateApiKey(String apiKey) {
        if (!StringUtils.hasText(expectedApiKey) || !StringUtils.hasText(apiKey)
                || !MessageDigest.isEqual(expectedApiKey.getBytes(StandardCharsets.UTF_8), apiKey.getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "내부 API 키가 유효하지 않습니다.");
        }
    }
}
