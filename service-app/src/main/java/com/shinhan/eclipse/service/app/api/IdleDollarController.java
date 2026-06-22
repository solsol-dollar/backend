package com.shinhan.eclipse.service.app.api;

import com.shinhan.eclipse.auth.AuthUser;
import com.shinhan.eclipse.common.response.ApiResponse;
import com.shinhan.eclipse.service.inflow.IdleDollarService;
import com.shinhan.eclipse.service.inflow.IdleDollarStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inflow")
@RequiredArgsConstructor
public class IdleDollarController {

    private final IdleDollarService idleDollarService;

    /** 쉬는 달러 현황 조회 */
    @GetMapping("/idle-status")
    public ResponseEntity<ApiResponse<IdleDollarStatusResponse>> getIdleStatus(
            @AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity.ok(ApiResponse.success(
                idleDollarService.getIdleStatus(authUser.userId())
                        .orElseGet(IdleDollarStatusResponse::none)));
    }
}