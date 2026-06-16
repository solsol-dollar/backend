package com.shinhan.eclipse.service.securities.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LsRestClientTest {

    MockWebServer server;
    LsRestClient  client;

    @Mock LsTokenManager tokenManager;
    @Mock LsProperties   props;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        given(props.getBaseUrl()).willReturn(server.url("/").toString());
        given(tokenManager.getAccessToken()).willReturn("test-token");
        client = new LsRestClient(props, tokenManager);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void getCurrentPrice_정상응답이면_DTO를_반환한다() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "rsp_cd": "00000",
                          "rsp_msg": "정상처리되었습니다",
                          "g3101OutBlock": {
                            "symbol": "TSLA",
                            "korname": "테슬라",
                            "price": "250.50",
                            "sign": "2",
                            "diff": "3.20",
                            "rate": "1.29",
                            "volume": 98765432,
                            "currency": "USD"
                          }
                        }
                        """)
        );

        Optional<LsCurrentPriceDto> result = client.getCurrentPrice("TSLA", "NASDAQ");

        assertThat(result).isPresent();
        assertThat(result.get().getOutBlock().getSymbol()).isEqualTo("TSLA");
        assertThat(result.get().getOutBlock().getPrice()).isNotNull();

        RecordedRequest req = server.takeRequest();
        assertThat(req.getHeader("tr_cd")).isEqualTo("g3101");
        assertThat(req.getHeader("Authorization")).isEqualTo("Bearer test-token");
    }

    @Test
    void getCurrentPrice_실패응답이면_빈_Optional을_반환한다() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "rsp_cd": "99999",
                          "rsp_msg": "오류"
                        }
                        """)
        );

        Optional<LsCurrentPriceDto> result = client.getCurrentPrice("TSLA", "NASDAQ");

        assertThat(result).isEmpty();
    }

    @Test
    void getStockInfo_정상응답이면_DTO를_반환한다() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "rsp_cd": "00000",
                          "rsp_msg": "정상처리되었습니다",
                          "g3104OutBlock": {
                            "symbol": "AAPL",
                            "korname": "애플",
                            "engname": "APPLE INC",
                            "exchange_name": "NASDAQ",
                            "currency": "USD",
                            "induname": "Technology"
                          }
                        }
                        """)
        );

        Optional<LsStockInfoDto> result = client.getStockInfo("AAPL", "NASDAQ");

        assertThat(result).isPresent();
        assertThat(result.get().getOutBlock().getSymbol()).isEqualTo("AAPL");

        RecordedRequest req = server.takeRequest();
        assertThat(req.getHeader("tr_cd")).isEqualTo("g3104");
    }

    @Test
    void getOrderBook_정상응답이면_DTO를_반환한다() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "rsp_cd": "00000",
                          "rsp_msg": "정상처리되었습니다",
                          "g3106OutBlock": {
                            "symbol": "NVDA",
                            "price": "880.00",
                            "offerho1": "880.50", "offerho2": "881.00",
                            "offerho3": "0", "offerho4": "0", "offerho5": "0",
                            "offerho6": "0", "offerho7": "0", "offerho8": "0",
                            "offerho9": "0", "offerho10": "0",
                            "bidho1": "879.50", "bidho2": "879.00",
                            "bidho3": "0", "bidho4": "0", "bidho5": "0",
                            "bidho6": "0", "bidho7": "0", "bidho8": "0",
                            "bidho9": "0", "bidho10": "0",
                            "offerrem1": 100, "offerrem2": 200,
                            "offerrem3": 0, "offerrem4": 0, "offerrem5": 0,
                            "offerrem6": 0, "offerrem7": 0, "offerrem8": 0,
                            "offerrem9": 0, "offerrem10": 0,
                            "bidrem1": 150, "bidrem2": 300,
                            "bidrem3": 0, "bidrem4": 0, "bidrem5": 0,
                            "bidrem6": 0, "bidrem7": 0, "bidrem8": 0,
                            "bidrem9": 0, "bidrem10": 0
                          }
                        }
                        """)
        );

        Optional<LsOrderBookDto> result = client.getOrderBook("NVDA", "NASDAQ");

        assertThat(result).isPresent();
        LsOrderBookDto.OutBlock block = result.get().getOutBlock();
        assertThat(block.askPrices()).hasSize(10);
        assertThat(block.bidPrices()).hasSize(10);

        RecordedRequest req = server.takeRequest();
        assertThat(req.getHeader("tr_cd")).isEqualTo("g3106");
    }

    @Test
    void NYSE_종목은_exchcd_81을_사용한다() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "rsp_cd": "00000",
                          "rsp_msg": "정상처리되었습니다",
                          "g3101OutBlock": {
                            "symbol": "BRK",
                            "price": "400.00",
                            "currency": "USD"
                          }
                        }
                        """)
        );

        client.getCurrentPrice("BRK", "NYSE");

        RecordedRequest req = server.takeRequest();
        String body = req.getBody().readUtf8();
        assertThat(body).contains("\"exchcd\":\"81\"");
    }

    @Test
    void HTTP_401이면_토큰을_무효화한다() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(401).setBody("Unauthorized"));

        client.getCurrentPrice("AAPL", "NASDAQ");

        org.mockito.Mockito.verify(tokenManager).invalidate();
    }
}
