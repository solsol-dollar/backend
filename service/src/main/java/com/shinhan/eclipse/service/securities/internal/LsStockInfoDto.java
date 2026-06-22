package com.shinhan.eclipse.service.securities.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
class LsStockInfoDto {

    @JsonProperty("rsp_cd")  String rspCd;
    @JsonProperty("rsp_msg") String rspMsg;
    @JsonProperty("g3104OutBlock") OutBlock outBlock;

    boolean isSuccess() {
        return "00000".equals(rspCd) && outBlock != null;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OutBlock {
        @JsonProperty("symbol")        String symbol;
        @JsonProperty("korname")       String korname;
        @JsonProperty("engname")       String engname;
        @JsonProperty("exchange_name") String exchangeName;
        @JsonProperty("nation_name")   String nationName;
        @JsonProperty("induname")      String induname;
        @JsonProperty("instname")      String instname;
        @JsonProperty("currency")      String currency;
        @JsonProperty("floatpoint")    String floatpoint;
        @JsonProperty("share")         Long share;
        @JsonProperty("volume")        Long volume;
        @JsonProperty("pcls")          BigDecimal pcls;
        @JsonProperty("clos")          BigDecimal clos;
        @JsonProperty("open")          BigDecimal open;
        @JsonProperty("high")          BigDecimal high;
        @JsonProperty("low")           BigDecimal low;
        @JsonProperty("high52p")       BigDecimal high52p;
        @JsonProperty("low52p")        BigDecimal low52p;
        @JsonProperty("shareprc")      BigDecimal shareprc;
        @JsonProperty("perv")          BigDecimal perv;
        @JsonProperty("epsv")          BigDecimal epsv;
        @JsonProperty("exrate")        BigDecimal exrate;
        @JsonProperty("suspend")       String suspend;
        @JsonProperty("sellonly")      String sellonly;
    }
}
