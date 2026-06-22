package com.shinhan.eclipse.service.securities.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 한국투자증권(KIS) 현재가·호가 응답 DTO.
 *
 * <ul>
 *   <li>{@link PriceDetailResponse} — HHDFS76200200 (현재가상세)</li>
 *   <li>{@link AskingPriceResponse} — HHDFS76200100 (현재가 호��)</li>
 * </ul>
 */
class KisQuoteDto {

    // ── HHDFS76200200 현재가상세 ────────────────────────────────────────────────

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class PriceDetailResponse {

        @JsonProperty("rt_cd")  private String rtCd;
        @JsonProperty("msg_cd") private String msgCd;
        @JsonProperty("msg1")   private String msg1;

        @JsonProperty("output")
        private Output output;

        boolean isSuccess() {
            return "0".equals(rtCd) && output != null;
        }

        @Getter
        @NoArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class Output {
            /** 현재가 */
            @JsonProperty("last")  private BigDecimal last;
            /** 전일종가 */
            @JsonProperty("base")  private BigDecimal base;
            /** 거래량 */
            @JsonProperty("tvol")  private String tvol;
            @JsonProperty("open")  private BigDecimal open;
            @JsonProperty("high")  private BigDecimal high;
            @JsonProperty("low")   private BigDecimal low;

            /** 전일대비 = last - base */
            BigDecimal diff() {
                if (last == null || base == null || base.compareTo(BigDecimal.ZERO) == 0)
                    return BigDecimal.ZERO;
                return last.subtract(base);
            }

            /** 등락률 = (last - base) / base * 100 */
            BigDecimal rate() {
                if (last == null || base == null || base.compareTo(BigDecimal.ZERO) == 0)
                    return BigDecimal.ZERO;
                return last.subtract(base)
                        .divide(base, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }

            /** 등락구분: 2=상승, 3=보합, 5=하락 */
            String sign() {
                BigDecimal d = diff();
                int cmp = d.compareTo(BigDecimal.ZERO);
                if (cmp > 0) return "2";
                if (cmp < 0) return "5";
                return "3";
            }

            Long volume() {
                if (tvol == null || tvol.isBlank()) return 0L;
                try { return Long.parseLong(tvol.trim()); }
                catch (NumberFormatException e) { return 0L; }
            }
        }
    }

    // ── HHDFS76200100 현재가 호가 ───────────────────────────────────────────────

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class AskingPriceResponse {

        @JsonProperty("rt_cd")  private String rtCd;
        @JsonProperty("msg_cd") private String msgCd;
        @JsonProperty("msg1")   private String msg1;

        @JsonProperty("output1")
        private Output1 output1;

        @JsonProperty("output2")
        private Output2 output2;

        boolean isSuccess() {
            return "0".equals(rtCd) && output1 != null;
        }

        @Getter
        @NoArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class Output1 {
            @JsonProperty("last") private BigDecimal last;
        }

        @Getter
        @NoArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class Output2 {
            // 매도 호가 1~10
            @JsonProperty("pask1")  private BigDecimal pask1;
            @JsonProperty("pask2")  private BigDecimal pask2;
            @JsonProperty("pask3")  private BigDecimal pask3;
            @JsonProperty("pask4")  private BigDecimal pask4;
            @JsonProperty("pask5")  private BigDecimal pask5;
            @JsonProperty("pask6")  private BigDecimal pask6;
            @JsonProperty("pask7")  private BigDecimal pask7;
            @JsonProperty("pask8")  private BigDecimal pask8;
            @JsonProperty("pask9")  private BigDecimal pask9;
            @JsonProperty("pask10") private BigDecimal pask10;
            // 매수 호가 1~10
            @JsonProperty("pbid1")  private BigDecimal pbid1;
            @JsonProperty("pbid2")  private BigDecimal pbid2;
            @JsonProperty("pbid3")  private BigDecimal pbid3;
            @JsonProperty("pbid4")  private BigDecimal pbid4;
            @JsonProperty("pbid5")  private BigDecimal pbid5;
            @JsonProperty("pbid6")  private BigDecimal pbid6;
            @JsonProperty("pbid7")  private BigDecimal pbid7;
            @JsonProperty("pbid8")  private BigDecimal pbid8;
            @JsonProperty("pbid9")  private BigDecimal pbid9;
            @JsonProperty("pbid10") private BigDecimal pbid10;
            // 매도 잔량 1~10
            @JsonProperty("vask1")  private Long vask1;
            @JsonProperty("vask2")  private Long vask2;
            @JsonProperty("vask3")  private Long vask3;
            @JsonProperty("vask4")  private Long vask4;
            @JsonProperty("vask5")  private Long vask5;
            @JsonProperty("vask6")  private Long vask6;
            @JsonProperty("vask7")  private Long vask7;
            @JsonProperty("vask8")  private Long vask8;
            @JsonProperty("vask9")  private Long vask9;
            @JsonProperty("vask10") private Long vask10;
            // 매수 잔량 1~10
            @JsonProperty("vbid1")  private Long vbid1;
            @JsonProperty("vbid2")  private Long vbid2;
            @JsonProperty("vbid3")  private Long vbid3;
            @JsonProperty("vbid4")  private Long vbid4;
            @JsonProperty("vbid5")  private Long vbid5;
            @JsonProperty("vbid6")  private Long vbid6;
            @JsonProperty("vbid7")  private Long vbid7;
            @JsonProperty("vbid8")  private Long vbid8;
            @JsonProperty("vbid9")  private Long vbid9;
            @JsonProperty("vbid10") private Long vbid10;

            List<BigDecimal> askPrices() {
                return List.of(pask1, pask2, pask3, pask4, pask5,
                               pask6, pask7, pask8, pask9, pask10);
            }

            List<BigDecimal> bidPrices() {
                return List.of(pbid1, pbid2, pbid3, pbid4, pbid5,
                               pbid6, pbid7, pbid8, pbid9, pbid10);
            }

            List<Long> askVolumes() {
                List<Long> v = new ArrayList<>();
                v.add(vask1);  v.add(vask2);  v.add(vask3);  v.add(vask4);  v.add(vask5);
                v.add(vask6);  v.add(vask7);  v.add(vask8);  v.add(vask9);  v.add(vask10);
                return v;
            }

            List<Long> bidVolumes() {
                List<Long> v = new ArrayList<>();
                v.add(vbid1);  v.add(vbid2);  v.add(vbid3);  v.add(vbid4);  v.add(vbid5);
                v.add(vbid6);  v.add(vbid7);  v.add(vbid8);  v.add(vbid9);  v.add(vbid10);
                return v;
            }
        }
    }
}
