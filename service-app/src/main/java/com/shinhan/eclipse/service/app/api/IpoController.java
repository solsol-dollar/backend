package com.shinhan.eclipse.service.app.api;

import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.common.resolver.UserHeader;
import com.shinhan.eclipse.common.response.ApiResponse;
import com.shinhan.eclipse.service.ipo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
            @UserHeader Long userId) {
        if (page < 0) throw new BusinessException(ErrorCode.INVALID_INPUT, "page는 0 이상이어야 합니다.");
        if (size < 1 || size > 100) throw new BusinessException(ErrorCode.INVALID_INPUT, "size는 1~100 사이어야 합니다.");
        return ResponseEntity.ok(ApiResponse.success(
                ipoExplorationService.getIpos(status, favoriteOnly, userId, page, size)));
    }

    /** IPO-002: IPO 상세 조회 */
    @GetMapping("/{ipoId}")
    public ResponseEntity<ApiResponse<IpoDetailResult>> getIpoDetail(
            @PathVariable Long ipoId,
            @UserHeader Long userId) {
        return ResponseEntity.ok(ApiResponse.success(
                ipoExplorationService.getIpoDetail(ipoId, userId)));
    }

    /** IPO-003: 찜 추가 */
    @PostMapping("/{ipoId}/favorites")
    public ResponseEntity<ApiResponse<FavoriteIpoResponse>> addFavorite(
            @PathVariable Long ipoId,
            @UserHeader Long userId) {
        return ResponseEntity.ok(ApiResponse.success(
                ipoExplorationService.addFavorite(userId, ipoId)));
    }

    /** IPO-004: 찜 삭제 */
    @DeleteMapping("/{ipoId}/favorites")
    public ResponseEntity<ApiResponse<Void>> removeFavorite(
            @PathVariable Long ipoId,
            @UserHeader Long userId) {
        ipoExplorationService.removeFavorite(userId, ipoId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
