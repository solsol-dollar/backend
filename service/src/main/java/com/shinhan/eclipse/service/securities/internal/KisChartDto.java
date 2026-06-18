package com.shinhan.eclipse.service.securities.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 한국투자증권(KIS) 차트 API 응답 DTO.
 *
 * <ul>
 *   <li>{@link MinuteChartResponse}  — HHDFS76950200 (분봉)</li>
 *   <li>{@link DailyChartResponse}   — HHDFS76240000 (일/주/월봉)</li>
 * </ul>
 */
class KisChartDto {

    // ── HHDFS76950200 분봉 ──────────────────────────────────────────────────────

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class MinuteChartResponse {

        @JsonProperty("rt_cd")  private String rtCd;
        @JsonProperty("msg_cd") private String msgCd;
        @JsonProperty("msg1")   private String msg1;

        @JsonProperty("output2")
        private List<MinuteCandle> candles;

        boolean isSuccess() { return "0".equals(rtCd); }

        List<MinuteCandle> getCandles() {
            return candles != null ? candles : Collections.emptyList();
        }
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class MinuteCandle {
        /** 현지 기준 날짜 (YYYYMMDD) */
        @JsonProperty("xymd") private String xymd;
        /** 현지 기준 시간 (HHMMSS) */
        @JsonProperty("xhms") private String xhms;

        @JsonProperty("open") private String open;
        @JsonProperty("high") private String high;
        @JsonProperty("low")  private String low;
        /** 종가 (KIS는 last 필드 사용) */
        @JsonProperty("last") private String last;
        /** 체결량 */
        @JsonProperty("evol") private String evol;
    }

    // ── HHDFS76240000 일/주/월봉 ────────────────────────────────────────────────

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class DailyChartResponse {

        @JsonProperty("rt_cd")  private String rtCd;
        @JsonProperty("msg_cd") private String msgCd;
        @JsonProperty("msg1")   private String msg1;

        @JsonProperty("output2")
        private List<DailyCandle> candles;

        boolean isSuccess() { return "0".equals(rtCd); }

        List<DailyCandle> getCandles() {
            return candles != null ? candles : Collections.emptyList();
        }
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class DailyCandle {
        /** 날짜 (YYYYMMDD) */
        @JsonProperty("xymd") private String xymd;
        /** 종가 (KIS는 clos 필드 사용) */
        @JsonProperty("clos") private String clos;
        /** 등락구분: 1=상한, 2=상승, 3=보합, 4=하한, 5=하락 */
        @JsonProperty("sign") private String sign;
        @JsonProperty("open") private String open;
        @JsonProperty("high") private String high;
        @JsonProperty("low")  private String low;
        /** 거래량 */
        @JsonProperty("tvol") private String tvol;
        /** 거래대금 */
        @JsonProperty("tamt") private String tamt;
    }
}
