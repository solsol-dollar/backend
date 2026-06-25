package com.shinhan.eclipse.service.app.api;

import com.shinhan.eclipse.auth.AuthUser;
import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.common.response.ApiResponse;
import com.shinhan.eclipse.service.securities.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/securities")
@RequiredArgsConstructor
public class SecuritiesController {

    private static final Set<String> VALID_PERIODS = Set.of("5MIN", "1D", "1W", "1M");

    private final SecuritiesService securitiesService;
    private final ChartService chartService;

    /** SEC-001: 종목 목록 */
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<ProductListItem>>> listProducts(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.success(securitiesService.listProducts(type, keyword, sort)));
    }

    /** SEC-008: 종목 랭킹 */
    @GetMapping("/products/ranking")
    public ResponseEntity<ApiResponse<List<RankingItem>>> getRanking(
            @RequestParam(defaultValue = "gainer") String type,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.success(securitiesService.getRanking(type, Math.min(limit, 50))));
    }

    /** SEC-002: 종목 상세 */
    @GetMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductDetail>> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(securitiesService.getProduct(id)));
    }

    /** SEC-003: 호가 */
    @GetMapping("/products/{id}/quotes")
    public ResponseEntity<ApiResponse<OrderBookResponse>> getOrderBook(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(securitiesService.getOrderBook(id)));
    }

    /** SEC-004: 보유 종목 + 손익 (래퍼 포함) */
    @GetMapping("/holdings")
    public ResponseEntity<ApiResponse<HoldingsSummary>> getHoldings(@AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity.ok(ApiResponse.success(securitiesService.getHoldings(authUser.userId())));
    }

    /** SEC-005: AI 추천 */
    @GetMapping("/recommended")
    public ResponseEntity<ApiResponse<List<RecommendedProduct>>> getRecommended(@AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity.ok(ApiResponse.success(securitiesService.getRecommended(authUser.userId())));
    }

    /** SEC-007: 종목 통계 (52주 고저, 기간별 수익률) */
    @GetMapping("/products/{id}/stats")
    public ResponseEntity<ApiResponse<ProductStats>> getProductStats(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(securitiesService.getProductStats(id)));
    }

    /** SEC-006: 차트 조회 */
    @GetMapping("/products/{id}/chart")
    public ResponseEntity<ApiResponse<ChartResponse>> getChart(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1M") String period) {
        if (!VALID_PERIODS.contains(period)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "유효하지 않은 period: " + period + ". 허용 값: 5MIN, 1D, 1W, 1M");
        }
        return ResponseEntity.ok(ApiResponse.success(chartService.getChart(id, period)));
    }

    /** B-01: 시장 지수 */
    @GetMapping("/market/indices")
    public ResponseEntity<ApiResponse<List<MarketIndex>>> getMarketIndices() {
        return ResponseEntity.ok(ApiResponse.success(securitiesService.getMarketIndices()));
    }
}
