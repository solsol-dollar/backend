package com.shinhan.eclipse.service.securities;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * SEC-006 차트 조회 응답 DTO.
 *
 * <p>candleType:
 * <ul>
 *   <li>MINUTE — 5분봉 (period=1D)</li>
 *   <li>DAY    — 일봉 (period=1W, 1M)</li>
 *   <li>WEEK   — 주봉 (period=3M, 6M)</li>
 *   <li>MONTH  — 월봉 (period=1Y, 5Y)</li>
 * </ul>
 */
@Getter
@AllArgsConstructor
public class ChartResponse {

    private final String ticker;
    private final String period;
    private final String candleType;
    private final List<CandleItem> candles;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CandleItem {
        /** 날짜 (YYYYMMDD) */
        private String date;

        /** 시각 (HHMMSS) — 분봉(1D)에만 존재, 나머지 null */
        private String time;

        private BigDecimal open;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal close;
        private Long volume;

        /** 등락구분: 2=상승, 3=보합, 5=하락 (null 가능) */
        private String sign;
    }
}
