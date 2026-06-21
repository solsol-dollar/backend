package com.shinhan.eclipse.service.securities.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
class LsCurrentPriceDto {

    @JsonProperty("rsp_cd")  String rspCd;
    @JsonProperty("rsp_msg") String rspMsg;
    @JsonProperty("g3101OutBlock") OutBlock outBlock;

    boolean isSuccess() {
        return "00000".equals(rspCd) && outBlock != null;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OutBlock {
        @JsonProperty("symbol")    String symbol;
        @JsonProperty("korname")   String korname;
        @JsonProperty("induname")  String induname;
        @JsonProperty("currency")  String currency;
        @JsonProperty("price")     BigDecimal price;
        @JsonProperty("sign")      String sign;
        @JsonProperty("diff")      BigDecimal diff;
        @JsonProperty("rate")      BigDecimal rate;
        @JsonProperty("volume")    Long volume;
        @JsonProperty("open")      BigDecimal open;
        @JsonProperty("high")      BigDecimal high;
        @JsonProperty("low")       BigDecimal low;
        @JsonProperty("high52p")   BigDecimal high52p;
        @JsonProperty("low52p")    BigDecimal low52p;
        @JsonProperty("perv")      BigDecimal perv;
        @JsonProperty("epsv")      BigDecimal epsv;
        @JsonProperty("suspend")   String suspend;
        @JsonProperty("sellonly")  String sellonly;
    }
}
