package com.shinhan.eclipse.service.securities.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
class LsOrderBookDto {

    @JsonProperty("rsp_cd")  String rspCd;
    @JsonProperty("rsp_msg") String rspMsg;
    @JsonProperty("g3106OutBlock") OutBlock outBlock;

    boolean isSuccess() {
        return "00000".equals(rspCd) && outBlock != null;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OutBlock {
        @JsonProperty("symbol")    String symbol;
        @JsonProperty("korname")   String korname;
        @JsonProperty("price")     BigDecimal price;
        @JsonProperty("sign")      String sign;
        @JsonProperty("diff")      BigDecimal diff;
        @JsonProperty("rate")      BigDecimal rate;
        @JsonProperty("volume")    Long volume;
        @JsonProperty("jnilclose") BigDecimal jnilclose;
        @JsonProperty("open")      BigDecimal open;
        @JsonProperty("high")      BigDecimal high;
        @JsonProperty("low")       BigDecimal low;
        @JsonProperty("hotime")    String hotime;

        // 매도 호가 1~10
        @JsonProperty("offerho1")  BigDecimal offerho1;
        @JsonProperty("offerho2")  BigDecimal offerho2;
        @JsonProperty("offerho3")  BigDecimal offerho3;
        @JsonProperty("offerho4")  BigDecimal offerho4;
        @JsonProperty("offerho5")  BigDecimal offerho5;
        @JsonProperty("offerho6")  BigDecimal offerho6;
        @JsonProperty("offerho7")  BigDecimal offerho7;
        @JsonProperty("offerho8")  BigDecimal offerho8;
        @JsonProperty("offerho9")  BigDecimal offerho9;
        @JsonProperty("offerho10") BigDecimal offerho10;

        // 매수 호가 1~10
        @JsonProperty("bidho1")  BigDecimal bidho1;
        @JsonProperty("bidho2")  BigDecimal bidho2;
        @JsonProperty("bidho3")  BigDecimal bidho3;
        @JsonProperty("bidho4")  BigDecimal bidho4;
        @JsonProperty("bidho5")  BigDecimal bidho5;
        @JsonProperty("bidho6")  BigDecimal bidho6;
        @JsonProperty("bidho7")  BigDecimal bidho7;
        @JsonProperty("bidho8")  BigDecimal bidho8;
        @JsonProperty("bidho9")  BigDecimal bidho9;
        @JsonProperty("bidho10") BigDecimal bidho10;

        // 매도 잔량 1~10
        @JsonProperty("offerrem1")  Long offerrem1;
        @JsonProperty("offerrem2")  Long offerrem2;
        @JsonProperty("offerrem3")  Long offerrem3;
        @JsonProperty("offerrem4")  Long offerrem4;
        @JsonProperty("offerrem5")  Long offerrem5;
        @JsonProperty("offerrem6")  Long offerrem6;
        @JsonProperty("offerrem7")  Long offerrem7;
        @JsonProperty("offerrem8")  Long offerrem8;
        @JsonProperty("offerrem9")  Long offerrem9;
        @JsonProperty("offerrem10") Long offerrem10;

        // 매수 잔량 1~10
        @JsonProperty("bidrem1")  Long bidrem1;
        @JsonProperty("bidrem2")  Long bidrem2;
        @JsonProperty("bidrem3")  Long bidrem3;
        @JsonProperty("bidrem4")  Long bidrem4;
        @JsonProperty("bidrem5")  Long bidrem5;
        @JsonProperty("bidrem6")  Long bidrem6;
        @JsonProperty("bidrem7")  Long bidrem7;
        @JsonProperty("bidrem8")  Long bidrem8;
        @JsonProperty("bidrem9")  Long bidrem9;
        @JsonProperty("bidrem10") Long bidrem10;

        @JsonProperty("offer") Long offerTotal;
        @JsonProperty("bid")   Long bidTotal;

        List<BigDecimal> askPrices() {
            return List.of(offerho1, offerho2, offerho3, offerho4, offerho5,
                           offerho6, offerho7, offerho8, offerho9, offerho10);
        }

        List<BigDecimal> bidPrices() {
            return List.of(bidho1, bidho2, bidho3, bidho4, bidho5,
                           bidho6, bidho7, bidho8, bidho9, bidho10);
        }

        List<Long> askVolumes() {
            return List.of(offerrem1, offerrem2, offerrem3, offerrem4, offerrem5,
                           offerrem6, offerrem7, offerrem8, offerrem9, offerrem10);
        }

        List<Long> bidVolumes() {
            return List.of(bidrem1, bidrem2, bidrem3, bidrem4, bidrem5,
                           bidrem6, bidrem7, bidrem8, bidrem9, bidrem10);
        }
    }
}
