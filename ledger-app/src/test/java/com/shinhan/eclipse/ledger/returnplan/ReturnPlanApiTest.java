package com.shinhan.eclipse.ledger.returnplan;

import com.shinhan.eclipse.LedgerApplication;
import com.shinhan.eclipse.domain.account.FinancialAccount;
import com.shinhan.eclipse.domain.ipo.Ipo;
import com.shinhan.eclipse.domain.subscription.IpoSubscription;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * RP-001~004, ALLOC-001/002를 MockMvc로 실제 HTTP 레이어까지 태워 검증하는 수동 API 테스트.
 * docker(MySQL)가 없는 환경에서도 동작하도록 H2로 구동한다.
 */
@SpringBootTest(
        classes = LedgerApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:returnplan-api-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        }
)
@AutoConfigureMockMvc
class ReturnPlanApiTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager em;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate txTemplate;

    @BeforeEach
    void setUp() {
        txTemplate = new TransactionTemplate(transactionManager);
    }

    @Test
    void 배정결과_조회부터_리턴플랜_생성_수정_확정_목록까지_전체_흐름이_동작한다() throws Exception {
        Ipo ipo = persistIpo();
        IpoSubscription subscription = persistAllocatedSubscription(ipo.getId());
        persistLinkedAccount("SAVINGS");
        persistLinkedAccount("DEPOSIT");
        persistLinkedAccount("SECURITIES");

        // ALLOC-001: 목록 조회
        mockMvc.perform(get("/api/v1/subscription-results")
                        .with(user("tester"))
                        .header("X-User-Id", USER_ID)
                        .param("subscriptionId", subscription.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.results[0].refundAmount").value(210.50))
                .andExpect(jsonPath("$.data.results[0].ticker").value("CRWV"));

        // ALLOC-002: 상세 조회 (아직 리턴 플랜 없음 -> hasReturnPlan=false)
        mockMvc.perform(get("/api/v1/subscription-results/{id}", subscription.getId())
                        .with(user("tester"))
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasReturnPlan").value(false));

        // RP-001: 생성
        String createRes = mockMvc.perform(post("/api/v1/return-plans")
                        .with(user("tester"))
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subscriptionId\":" + subscription.getId() + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalRefundAmount").value(210.50))
                .andExpect(jsonPath("$.data.sourceTicker").value("CRWV"))
                .andExpect(jsonPath("$.data.allocations[0].ratio").value(0))
                .andReturn().getResponse().getContentAsString();

        Long returnPlanId = extractReturnPlanId(createRes);

        // RP-002: 비율 합 100이 아니면 422
        mockMvc.perform(put("/api/v1/return-plans/{id}", returnPlanId)
                        .with(user("tester"))
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"allocations":[
                                  {"destinationType":"SECURITIES","ratio":50},
                                  {"destinationType":"SAVINGS","ratio":20},
                                  {"destinationType":"DEPOSIT","ratio":20}
                                ]}"""))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").value("L010"));

        // RP-002: 정상 비율(20/50/30)
        mockMvc.perform(put("/api/v1/return-plans/{id}", returnPlanId)
                        .with(user("tester"))
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"allocations":[
                                  {"destinationType":"SECURITIES","ratio":20},
                                  {"destinationType":"SAVINGS","ratio":50},
                                  {"destinationType":"DEPOSIT","ratio":30}
                                ]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allocations[1].ratio").value(50))
                .andExpect(jsonPath("$.data.allocations[1].amount").value(105.25));

        // 단건 조회로 저장된 비율 재확인
        mockMvc.perform(get("/api/v1/return-plans/{id}", returnPlanId)
                        .with(user("tester"))
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allocations[2].ratio").value(30));

        // RP-003: 확정 — 상태 전이는 없고 confirmedAt만 기록 (이후에도 비율 수정 가능)
        mockMvc.perform(put("/api/v1/return-plans/{id}/confirm", returnPlanId)
                        .with(user("tester"))
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planStatus").value("DRAFT"))
                .andExpect(jsonPath("$.data.confirmedAt").exists());

        // 확정 후에도 비율 수정 가능
        mockMvc.perform(put("/api/v1/return-plans/{id}", returnPlanId)
                        .with(user("tester"))
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"allocations":[
                                  {"destinationType":"SECURITIES","ratio":30},
                                  {"destinationType":"SAVINGS","ratio":40},
                                  {"destinationType":"DEPOSIT","ratio":30}
                                ]}"""))
                .andExpect(status().isOk());

        // 환불일 당일/이후에는 비율 수정 불가(L013) — 테스트 IPO의 refundDate는 오늘+3일이라 아직 가능,
        // 같은 검증 로직이 internal 실행 API에는 적용되지 않는다는 점만 RP-008에서 별도 확인한다.

        // RP-008(명세 외 추가): 환불일 정산 배치가 호출하는 내부 실행 API
        mockMvc.perform(put("/internal/return-plans/{id}/execute", returnPlanId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planStatus").value("EXECUTED"));

        // 이미 실행된 플랜을 다시 실행하면 충돌(L011)
        mockMvc.perform(put("/internal/return-plans/{id}/execute", returnPlanId))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").value("L011"));

        // 실행된 플랜은 더 이상 비율 수정 불가
        mockMvc.perform(put("/api/v1/return-plans/{id}", returnPlanId)
                        .with(user("tester"))
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"allocations":[
                                  {"destinationType":"SECURITIES","ratio":100}
                                ]}"""))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").value("L011"));

        // RP-004: 목록 조회
        mockMvc.perform(get("/api/v1/return-plans")
                        .with(user("tester"))
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.returnPlans[0].planStatus").value("EXECUTED"))
                .andExpect(jsonPath("$.data.returnPlans[0].sourceCompanyName").value("CoreWeave"));

        // 같은 청약으로 다시 생성하면 중복(L008)
        mockMvc.perform(post("/api/v1/return-plans")
                        .with(user("tester"))
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subscriptionId\":" + subscription.getId() + "}"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").value("L008"));
    }

    private Long extractReturnPlanId(String json) {
        String marker = "\"returnPlanId\":";
        int start = json.indexOf(marker) + marker.length();
        int end = json.indexOf(',', start);
        return Long.parseLong(json.substring(start, end).trim());
    }

    private Ipo persistIpo() {
        return txTemplate.execute(status -> {
            Ipo ipo = new Ipo();
            ReflectionTestUtils.setField(ipo, "ticker", "CRWV");
            ReflectionTestUtils.setField(ipo, "companyName", "CoreWeave");
            ReflectionTestUtils.setField(ipo, "subscriptionStartDate", LocalDate.now().minusDays(10));
            ReflectionTestUtils.setField(ipo, "subscriptionEndDate", LocalDate.now().minusDays(5));
            ReflectionTestUtils.setField(ipo, "refundDate", LocalDate.now().plusDays(3));
            ReflectionTestUtils.setField(ipo, "ipoStatus", "COMPLETED");
            em.persist(ipo);
            em.flush();
            return ipo;
        });
    }

    private FinancialAccount persistLinkedAccount(String accountType) {
        return txTemplate.execute(status -> {
            FinancialAccount account = new FinancialAccount();
            ReflectionTestUtils.setField(account, "userId", USER_ID);
            ReflectionTestUtils.setField(account, "accountType", accountType);
            ReflectionTestUtils.setField(account, "institutionType", "BANK");
            ReflectionTestUtils.setField(account, "institutionName", "신한은행");
            ReflectionTestUtils.setField(account, "currency", "USD");
            ReflectionTestUtils.setField(account, "balance", BigDecimal.ZERO);
            ReflectionTestUtils.setField(account, "linked", true);
            em.persist(account);
            em.flush();
            return account;
        });
    }

    /** 이미 배정이 완료되어 환불금이 발생한 청약을 시뮬레이션. */
    private IpoSubscription persistAllocatedSubscription(Long ipoId) {
        return txTemplate.execute(status -> {
            IpoSubscription subscription = IpoSubscription.request(
                    USER_ID, ipoId, 1L, 10, new BigDecimal("100"));
            em.persist(subscription);
            ReflectionTestUtils.setField(subscription, "subscriptionStatus", "CONFIRMED");
            ReflectionTestUtils.setField(subscription, "confirmedAt", LocalDateTime.now());
            ReflectionTestUtils.setField(subscription, "allocatedShares", 7);
            ReflectionTestUtils.setField(subscription, "allocatedAmount", new BigDecimal("700.00"));
            ReflectionTestUtils.setField(subscription, "refundAmount", new BigDecimal("210.50"));
            ReflectionTestUtils.setField(subscription, "allocationRate", new BigDecimal("70.0000"));
            ReflectionTestUtils.setField(subscription, "resultStatus", "COMPLETED");
            em.flush();
            return subscription;
        });
    }
}
