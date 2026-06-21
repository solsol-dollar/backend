package com.shinhan.eclipse.service.exchange.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shinhan.eclipse.common.exchange.ExchangeRateInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@NoArgsConstructor
class ExchangeRateApiDto {

    /** 1=정상, 2=DATA코드 오류, 3=인증코드 오류, 4=일일 제한 초과 */
    @JsonProperty("result")
    private int result;

    @JsonProperty("cur_unit")
    private String currencyCode;

    @JsonProperty("cur_nm")
    private String currencyName;

    /** 전신환 살때 (매수 기준율) — 소수점 포함, 천단위 쉼표 */
    @JsonProperty("ttb")
    private String ttb;

    /** 전신환 팔때 (매도 기준율) */
    @JsonProperty("tts")
    private String tts;

    /** 기준환율 */
    @JsonProperty("deal_bas_r")
    private String dealBasR;

    boolean isSuccess() {
        return result == 1 && currencyCode != null;
    }

    ExchangeRateInfo toInfo() {
        return new ExchangeRateInfo(
                currencyCode,
                currencyName,
                parse(dealBasR),
                parse(ttb),
                parse(tts),
                Instant.now()
        );
    }

    private static BigDecimal parse(String raw) {
        if (raw == null || raw.isBlank()) return BigDecimal.ZERO;
        return new BigDecimal(raw.replace(",", "").trim());
    }
}