package com.shinhan.eclipse.service.app.api;

import com.shinhan.eclipse.common.resolver.UserHeader;
import com.shinhan.eclipse.common.response.ApiResponse;
import com.shinhan.eclipse.service.securities.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/securities")
@RequiredArgsConstructor
public class SecuritiesController {

    private final SecuritiesService securitiesService;

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
}
