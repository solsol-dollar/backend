package com.shinhan.eclipse.service.securities.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
class KisDailyPriceDto {

    @JsonProperty("rt_cd") private String rtCd;
    @JsonProperty("msg_cd") private String msgCd;
    @JsonProperty("msg1")   private String msg1;
    @JsonProperty("output2") private List<DailyCandle> output2;

    boolean isSuccess() {
        return "0".equals(rtCd);
    }

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class DailyCandle {
        @JsonProperty("xymd") private String xymd;  // 날짜 yyyyMMdd
        @JsonProperty("clos") private String clos;  // 종가
        @JsonProperty("sign") private String sign;  // 등락구분 (2=상승, 3=보합, 5=하락)
        @JsonProperty("diff") private String diff;  // 전일대비
        @JsonProperty("rate") private String rate;  // 등락율
        @JsonProperty("open") private String open;  // 시가
        @JsonProperty("high") private String high;  // 고가
        @JsonProperty("low")  private String low;   // 저가
        @JsonProperty("tvol") private String tvol;  // 거래량
        @JsonProperty("tamt") private String tamt;  // 거래대금

        BigDecimal closeBD() { return parseBD(clos); }
        BigDecimal amtBD()   { return parseBD(tamt); }
        long       volLong() { return parseLong(tvol); }

        private static BigDecimal parseBD(String s) {
            if (s == null || s.isBlank()) return null;
            try { return new BigDecimal(s.trim()); } catch (NumberFormatException e) { return null; }
        }
        private static long parseLong(String s) {
            if (s == null || s.isBlank()) return 0L;
            try { return Long.parseLong(s.trim()); } catch (NumberFormatException e) { return 0L; }
        }
    }
}
