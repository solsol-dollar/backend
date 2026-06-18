package com.shinhan.eclipse.worker.candle.ls;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * g3204 응답 DTO (worker-app 전용).
 * g3203 분봉은 배치에서 사용하지 않으므로 g3204만 정의.
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class G3204Response {

    @JsonProperty("g3204OutBlock1")
    private List<DailyCandle> candles;

    private String trCont;

    void setTrCont(String trCont) {
        this.trCont = trCont;
    }

    public boolean hasMore() {
        return "Y".equalsIgnoreCase(trCont);
    }

    public List<DailyCandle> getCandles() {
        return candles != null ? candles : Collections.emptyList();
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DailyCandle {
        @JsonProperty("date")   private String date;
        @JsonProperty("open")   private String open;
        @JsonProperty("high")   private String high;
        @JsonProperty("low")    private String low;
        @JsonProperty("close")  private String close;
        @JsonProperty("volume") private String volume;
        @JsonProperty("amount") private String amount;
        /** 등락구분: 2=상승, 3=보합, 5=하락 */
        @JsonProperty("sign")   private String sign;
    }
}
