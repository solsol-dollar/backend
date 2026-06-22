package com.shinhan.eclipse.worker.candle.kis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * KIS HHDFS76240000 — 해외주식 기간별시세(일/주/월봉) 응답 DTO.
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KisDailyPriceResponse {

    @JsonProperty("rt_cd")  private String rtCd;
    @JsonProperty("msg_cd") private String msgCd;
    @JsonProperty("msg1")   private String msg1;

    @JsonProperty("output2")
    private List<DailyCandle> candles;

    public boolean isSuccess() { return "0".equals(rtCd); }

    public List<DailyCandle> getCandles() {
        return candles != null ? candles : Collections.emptyList();
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DailyCandle {
        /** 날짜 (YYYYMMDD) */
        @JsonProperty("xymd") private String xymd;
        /** 종가 */
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
