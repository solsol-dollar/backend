package com.shinhan.eclipse.service.app.api;

import com.shinhan.eclipse.auth.AuthUser;
import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.common.response.ApiResponse;
import com.shinhan.eclipse.service.ipo.*;
import lombok.RequiredArgsConstructor;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ipos")
@RequiredArgsConstructor
public class IpoController {

    private final IpoExplorationService ipoExplorationService;

    /** IPO-001: IPO 목록 조회 */
    @GetMapping
    public ResponseEntity<ApiResponse<IpoListResult>> getIpos(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "false") boolean favoriteOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal AuthUser authUser) {
        if (page < 0) throw new BusinessException(ErrorCode.INVALID_INPUT, "page는 0 이상이어야 합니다.");
        if (size < 1 || size > 100) throw new BusinessException(ErrorCode.INVALID_INPUT, "size는 1~100 사이어야 합니다.");
        if (status != null && !status.equals("OPEN") && !status.equals("UPCOMING") && !status.equals("CLOSED"))
            throw new BusinessException(ErrorCode.INVALID_INPUT, "status는 OPEN, UPCOMING, CLOSED 중 하나여야 합니다.");
        return ResponseEntity.ok(ApiResponse.success(
                ipoExplorationService.getIpos(status, favoriteOnly, authUser.userId(), page, size, keyword)));
    }

    /** IPO-002: IPO 상세 조회 */
    @GetMapping("/{ipoId}")
    public ResponseEntity<ApiResponse<IpoDetailResult>> getIpoDetail(
            @PathVariable Long ipoId,
            @AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity.ok(ApiResponse.success(
                ipoExplorationService.getIpoDetail(ipoId, authUser.userId())));
    }

    /** IPO-007: IPO 뉴스 목록 */
    @GetMapping("/{ipoId}/news")
    public ResponseEntity<ApiResponse<List<IpoNewsItem>>> getIpoNews(
            @PathVariable Long ipoId,
            @RequestParam(defaultValue = "3") int size) {
        if (size < 1 || size > 5) throw new BusinessException(ErrorCode.INVALID_INPUT, "size는 1~5 사이어야 합니다.");
        return ResponseEntity.ok(ApiResponse.success(
                ipoExplorationService.getIpoNews(ipoId, size)));
    }

    /** IPO-007-DETAIL: 뉴스 상세 (content 포함) */
    @GetMapping("/{ipoId}/news/{newsId}")
    public ResponseEntity<ApiResponse<IpoNewsDetailItem>> getIpoNewsDetail(
            @PathVariable Long ipoId,
            @PathVariable Long newsId) {
        return ResponseEntity.ok(ApiResponse.success(
                ipoExplorationService.getIpoNewsDetail(ipoId, newsId)));
    }

    /** IPO-007-TOP: 핵심 뉴스 (상장 전 topNewsIds 2건 + 상장 후 postTopNewsIds 2건) */
    @GetMapping("/{ipoId}/news/top")
    public ResponseEntity<ApiResponse<IpoTopNewsResult>> getTopIpoNews(@PathVariable Long ipoId) {
        return ResponseEntity.ok(ApiResponse.success(
                ipoExplorationService.getTopIpoNews(ipoId)));
    }

    /** IPO 연간 재무 데이터 */
    @GetMapping("/{ipoId}/financials")
    public ResponseEntity<ApiResponse<List<IpoFinancialItem>>> getIpoFinancials(@PathVariable Long ipoId) {
        return ResponseEntity.ok(ApiResponse.success(ipoExplorationService.getIpoFinancials(ipoId)));
    }

    /** IPO-008: IPO 뉴스 스코어 조회 */
    @GetMapping("/{ipoId}/score")
    public ResponseEntity<ApiResponse<IpoScoreResult>> getIpoScore(@PathVariable Long ipoId) {
        return ipoExplorationService.getIpoScore(ipoId)
                .map(ApiResponse::success)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** IPO-003: 찜 추가 */
    @PostMapping("/{ipoId}/favorites")
    public ResponseEntity<ApiResponse<FavoriteIpoResponse>> addFavorite(
            @PathVariable Long ipoId,
            @AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity.ok(ApiResponse.success(
                ipoExplorationService.addFavorite(authUser.userId(), ipoId)));
    }

    /** IPO-004: 찜 삭제 */
    @DeleteMapping("/{ipoId}/favorites")
    public ResponseEntity<ApiResponse<Void>> removeFavorite(
            @PathVariable Long ipoId,
            @AuthenticationPrincipal AuthUser authUser) {
        ipoExplorationService.removeFavorite(authUser.userId(), ipoId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
