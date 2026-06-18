package com.shinhan.eclipse.service.app.api;

import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.common.resolver.UserHeader;
import com.shinhan.eclipse.common.response.ApiResponse;
import com.shinhan.eclipse.service.securities.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/securities")
@RequiredArgsConstructor
public class SecuritiesController {

    private static final Set<String> VALID_PERIODS = Set.of("1D", "1W", "1M", "3M", "6M", "1Y", "5Y");

    private final SecuritiesService securitiesService;
    private final ChartService chartService;

    /** SEC-001: 종목 목록 */
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<ProductListItem>>> listProducts(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.success(securitiesService.listProducts(type, keyword)));
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

    /** SEC-004: 보유 종목 + 손익 */
    @GetMapping("/holdings")
    public ResponseEntity<ApiResponse<List<HoldingItem>>> getHoldings(@UserHeader Long userId) {
        return ResponseEntity.ok(ApiResponse.success(securitiesService.getHoldings(userId)));
    }

    /** SEC-005: AI 추천 */
    @GetMapping("/recommended")
    public ResponseEntity<ApiResponse<List<RecommendedProduct>>> getRecommended(@UserHeader Long userId) {
        return ResponseEntity.ok(ApiResponse.success(securitiesService.getRecommended(userId)));
    }

    /** SEC-006: 차트 조회 */
    @GetMapping("/products/{id}/chart")
    public ResponseEntity<ApiResponse<ChartResponse>> getChart(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1M") String period) {
        if (!VALID_PERIODS.contains(period)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "유효하지 않은 period: " + period + ". 허용 값: 1D, 1W, 1M, 3M, 6M, 1Y, 5Y");
        }
        return ResponseEntity.ok(ApiResponse.success(chartService.getChart(id, period)));
    }
}
