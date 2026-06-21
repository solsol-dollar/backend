package com.shinhan.eclipse.service.securities.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
class LsMasterDto {

    @JsonProperty("rsp_cd")  String rspCd;
    @JsonProperty("rsp_msg") String rspMsg;
    @JsonProperty("g3190OutBlock")  Header header;
    @JsonProperty("g3190OutBlock1") List<Item> items;

    boolean isSuccess() {
        return "00000".equals(rspCd);
    }

    boolean hasMore() {
        return header != null
            && header.ctsValue != null
            && !header.ctsValue.isBlank()
            && header.recCount > 0;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Header {
        @JsonProperty("natcode")   String natcode;
        @JsonProperty("exgubun")   String exgubun;
        @JsonProperty("cts_value") String ctsValue;
        @JsonProperty("rec_count") int recCount;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Item {
        @JsonProperty("keysymbol")   String keysymbol;
        @JsonProperty("exchcd")      String exchcd;
        @JsonProperty("symbol")      String symbol;
        @JsonProperty("korname")     String korname;
        @JsonProperty("engname")     String engname;
        @JsonProperty("currency")    String currency;
        @JsonProperty("isin")        String isin;
        @JsonProperty("indusury")    String indusury;
        @JsonProperty("share")       Long share;
        @JsonProperty("clos")        BigDecimal clos;
        @JsonProperty("listed_date") String listedDate;
        @JsonProperty("suspend")     String suspend;
        @JsonProperty("pcls")        BigDecimal pcls;
    }
}
