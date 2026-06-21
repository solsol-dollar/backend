package com.shinhan.eclipse.service.securities.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * LS증권 차트 API 응답 DTO.
 *
 * <ul>
 *   <li>{@link G3203Response} — g3203 (분봉)</li>
 *   <li>{@link G3204Response} — g3204 (일/주/월/년봉, 연속조회 지원)</li>
 * </ul>
 */
class LsChartDto {

    // ── g3203 (분봉) ──────────────────────────────────────────────────────────

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class G3203Response {

        @JsonProperty("g3203OutBlock1")
        private List<MinuteCandle> candles;

        List<MinuteCandle> getCandles() {
            return candles != null ? candles : Collections.emptyList();
        }
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class MinuteCandle {
        /** 날짜 (YYYYMMDD) */
        @JsonProperty("date")
        private String date;

        /** 현지시간 (HHMMSS) */
        @JsonProperty("loctime")
        private String loctime;

        @JsonProperty("open")
        private String open;

        @JsonProperty("high")
        private String high;

        @JsonProperty("low")
        private String low;

        @JsonProperty("close")
        private String close;

        /** 체결량 */
        @JsonProperty("exevol")
        private String exevol;

        @JsonProperty("amount")
        private String amount;
    }

    // ── g3204 (일/주/월/년봉) ─────────────────────────────────────────────────

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class G3204Response {

        @JsonProperty("g3204OutBlock1")
        private List<DailyCandle> candles;

        /**
         * 연속조회 여부 헤더.
         * WebClient 응답 헤더의 tr_cont 값을 별도로 추출해 주입한다.
         */
        private String trCont;

        void setTrCont(String trCont) {
            this.trCont = trCont;
        }

        boolean hasMore() {
            return "Y".equalsIgnoreCase(trCont);
        }

        List<DailyCandle> getCandles() {
            return candles != null ? candles : Collections.emptyList();
        }
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class DailyCandle {
        /** 날짜 (YYYYMMDD) */
        @JsonProperty("date")
        private String date;

        @JsonProperty("open")
        private String open;

        @JsonProperty("high")
        private String high;

        @JsonProperty("low")
        private String low;

        @JsonProperty("close")
        private String close;

        @JsonProperty("volume")
        private String volume;

        @JsonProperty("amount")
        private String amount;

        /** 등락구분: 2=상승, 3=보합, 5=하락 */
        @JsonProperty("sign")
        private String sign;
    }
}
