package com.shinhan.eclipse.service.securities;

/**
 * SEC-006 차트 조회 서비스 인터페이스 (Spring Modulith public API).
 */
public interface ChartService {

    /**
     * 종목 차트 데이터를 조회한다.
     *
     * @param productId 투자 상품 ID
     * @param period    차트 기간 (1D | 1W | 1M | 3M | 6M | 1Y | 5Y)
     * @return 차트 응답 (캔들 배열 포함)
     */
    ChartResponse getChart(Long productId, String period);
}
