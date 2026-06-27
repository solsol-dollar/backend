package com.shinhan.eclipse.ledger.returnplan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 리턴 플랜 통합 테스트 (RP-001 ~ RP-008).
 *
 * <p>실제 HTTP 레이어까지 MockMvc로 태우되, DB는 H2 인메모리를 사용해
 * 외부 인프라 없이 CI에서 동작한다.
 *
 * <p>각 @Test 는 @BeforeEach 에서 DB를 초기화하므로 순서·반복 실행에 무관하다.
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
                "eclipse.internal.api-key=test-internal-api-key",
        }
)
@AutoConfigureMockMvc
class ReturnPlanApiTest {

    private static final Long    USER_ID      = 1L;
    private static final String  INTERNAL_KEY = "test-internal-api-key";

    /** 청약 환불금: 모든 계산의 기준이 되는 값 */
    private static final BigDecimal REFUND_AMOUNT = new BigDecimal("210.50");

    @Autowired MockMvc                    mockMvc;
    @Autowired ObjectMapper               objectMapper;
    @Autowired EntityManager              em;
    @Autowired PlatformTransactionManager transactionManager;

    private TransactionTemplate txTemplate;

    @BeforeEach
    void cleanUpAndInit() {
        txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.execute(status -> {
            em.createQuery("DELETE FROM ReturnPlanAllocation").executeUpdate();
            em.createQuery("DELETE FROM ReturnPlan").executeUpdate();
            em.createQuery("DELETE FROM BalanceHold").executeUpdate();
            em.createQuery("DELETE FROM IpoSubscription").executeUpdate();
            em.createQuery("DELETE FROM FinancialAccount").executeUpdate();
            em.createQuery("DELETE FROM Ipo").executeUpdate();
            return null;
        });
    }

    // ── ALLOC-001: 배정 결과 목록 조회 ─────────────────────────────────────────

    @Test
    void ALLOC001_배정결과_목록조회_성공() throws Exception {
        // given
        Ipo ipo = persistIpo(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5), LocalDate.now().plusDays(3));
        IpoSubscription sub = persistAllocatedSubscription(ipo.getId());

        // when & then
        mockMvc.perform(get("/api/v1/subscription-results")
                        .with(user("tester"))
                        .header("X-User-Id", USER_ID)
                        .param("subscriptionId", sub.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.results[0].ticker").value("CRWV"))
                .andExpect(jsonPath("$.data.results[0].refundAmount").value(REFUND_AMOUNT.doubleValue()));
    }

    // ── ALLOC-002: 배정 결과 상세 ───────────────────────────────────────────────

    @Test
    void ALLOC002_리턴플랜_없는_청약_상세조회시_hasReturnPlan이_false이다() throws Exception {
        // given
        Ipo ipo = persistIpo(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5), LocalDate.now().plusDays(3));
        IpoSubscription sub = persistAllocatedSubscription(ipo.getId());

        // when & then
        mockMvc.perform(get("/api/v1/subscription-results/{id}", sub.getId())
                        .with(user("tester"))
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasReturnPlan").value(false));
    }

    @Test
    void ALLOC002_리턴플랜_생성_후_상세조회시_hasReturnPlan이_true이다() throws Exception {
        // given
        Ipo ipo = persistIpo(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5), LocalDate.now().plusDays(3));
        IpoSubscription sub = persistAllocatedSubscription(ipo.getId());
        persistLinkedAccount("SAVINGS");
        persistLinkedAccount("DEPOSIT");
        createReturnPlan(sub.getId());

        // when & then
        mockMvc.perform(get("/api/v1/subscription-results/{id}", sub.getId())
                        .with(user("tester"))
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasReturnPlan").value(true));
    }

    // ── RP-001: 리턴 플랜 생성 ─────────────────────────────────────────────────

    @Test
    void RP001_리턴플랜_생성_성공시_201과_DRAFT_상태로_반환된다() throws Exception {
        // given
        Ipo ipo = persistIpo(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5), LocalDate.now().plusDays(3));
        IpoSubscription sub = persistAllocatedSubscription(ipo.getId());
        persistLinkedAccount("SAVINGS");
        persistLinkedAccount("DEPOSIT");

        // when & then
        mockMvc.perform(post("/api/v1/return-plans")
                        .with(user("tester"))
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subscriptionId\":" + sub.getId() + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalRefundAmount").value(REFUND_AMOUNT.doubleValue()))
                .andExpect(jsonPath("$.data.sourceTicker").value("CRWV"))
                .andExpect(jsonPath("$.data.planStatus").value("DRAFT"))
                .andExpect(jsonPath("$.data.allocations[0].ratio").value(0));
    }

    @Test
    void RP001_같은_청약으로_중복_생성하면_L008_409를_반환한다() throws Exception {
        // given
        Ipo ipo = persistIpo(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5), LocalDate.now().plusDays(3));
        IpoSubscription sub = persistAllocatedSubscription(ipo.getId());
        persistLinkedAccount("SAVINGS");
        persistLinkedAccount("DEPOSIT");
        createReturnPlan(sub.getId());

        // when & then
        mockMvc.perform(post("/api/v1/return-plans")
                        .with(user("tester"))
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subscriptionId\":" + sub.getId() + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("L008"));
    }

    @Test
    void RP001_배정결과_없는_청약으로_생성하면_L007_404를_반환한다() throws Exception {
        // given: refundAmount = null 인 청약
        Ipo ipo = persistIpo(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5), LocalDate.now().plusDays(3));
        IpoSubscription sub = persistUnallocatedSubscription(ipo.getId());

        // when & then
        mockMvc.perform(post("/api/v1/return-plans")
                        .with(user("tester"))
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subscriptionId\":" + sub.getId() + "}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("L007"));
    }

    // ── RP-002: 비율 수정 ──────────────────────────────────────────────────────

    @Test
    void RP002_비율합이_100이면_수정_성공한다() throws Exception {
        // given
        Ipo ipo = persistIpo(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5), LocalDate.now().plusDays(3));
        IpoSubscription sub = persistAllocatedSubscription(ipo.getId());
        persistLinkedAccount("SAVINGS");
        persistLinkedAccount("DEPOSIT");
        Long planId = createReturnPlan(sub.getId());

        // when & then — SAVINGS 40% of 210.50 = 84.20
        mockMvc.perform(put("/api/v1/return-plans/{id}", planId)
                        .with(user("tester"))
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"allocations":[
                                  {"destinationType":"SECURITIES","ratio":30},
                                  {"destinationType":"SAVINGS","ratio":40},
                                  {"destinationType":"DEPOSIT","ratio":30}
                                ]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allocations[?(@.destinationType=='SAVINGS')].ratio").value(40))
                .andExpect(jsonPath("$.data.allocations[?(@.destinationType=='SAVINGS')].amount")
                        .value(expectedAmount(40).doubleValue()));
    }

    @Test
    void RP002_비율합이_100이_아니면_L010_422를_반환한다() throws Exception {
        // given
        Ipo ipo = persistIpo(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5), LocalDate.now().plusDays(3));
        IpoSubscription sub = persistAllocatedSubscription(ipo.getId());
        persistLinkedAccount("SAVINGS");
        persistLinkedAccount("DEPOSIT");
        Long planId = createReturnPlan(sub.getId());

        // when & then
        mockMvc.perform(put("/api/v1/return-plans/{id}", planId)
                        .with(user("tester"))
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"allocations":[
                                  {"destinationType":"SECURITIES","ratio":50},
                                  {"destinationType":"SAVINGS","ratio":20},
                                  {"destinationType":"DEPOSIT","ratio":20}
                                ]}"""))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("L010"));
    }

    @Test
    void RP002_실행된_플랜_비율수정은_L011_409를_반환한다() throws Exception {
        // given
        Ipo ipo = persistIpo(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5), LocalDate.now().plusDays(3));
        IpoSubscription sub = persistAllocatedSubscription(ipo.getId());
        persistLinkedAccount("SAVINGS");
        persistLinkedAccount("DEPOSIT");
        persistLinkedAccount("SECURITIES");
        Long planId = createReturnPlan(sub.getId());
        updateRatios(planId, 30, 40, 30);
        executeReturnPlan(planId);

        // when & then
        mockMvc.perform(put("/api/v1/return-plans/{id}", planId)
                        .with(user("tester"))
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"allocations":[{"destinationType":"SECURITIES","ratio":100}]}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("L011"));
    }

    // ── 단건 조회 ──────────────────────────────────────────────────────────────

    @Test
    void 단건조회_비율수정_후_저장된_비율이_반영된다() throws Exception {
        // given
        Ipo ipo = persistIpo(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5), LocalDate.now().plusDays(3));
        IpoSubscription sub = persistAllocatedSubscription(ipo.getId());
        persistLinkedAccount("SAVINGS");
        persistLinkedAccount("DEPOSIT");
        Long planId = createReturnPlan(sub.getId());
        updateRatios(planId, 20, 50, 30);

        // when & then
        mockMvc.perform(get("/api/v1/return-plans/{id}", planId)
                        .with(user("tester"))
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allocations[?(@.destinationType=='SAVINGS')].ratio").value(50))
                .andExpect(jsonPath("$.data.allocations[?(@.destinationType=='DEPOSIT')].ratio").value(30));
    }

    // ── RP-008: 내부 실행 API ──────────────────────────────────────────────────

    @Test
    void RP008_내부키_정상이면_실행_성공하고_EXECUTED_상태가_된다() throws Exception {
        // given
        Ipo ipo = persistIpo(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5), LocalDate.now().plusDays(3));
        IpoSubscription sub = persistAllocatedSubscription(ipo.getId());
        FinancialAccount savings    = persistLinkedAccount("SAVINGS");
        FinancialAccount deposit    = persistLinkedAccount("DEPOSIT");
        FinancialAccount securities = persistLinkedAccount("SECURITIES");
        Long planId = createReturnPlan(sub.getId());
        updateRatios(planId, 30, 40, 30);

        // when & then
        mockMvc.perform(put("/internal/return-plans/{id}/execute", planId)
                        .header("X-Internal-Api-Key", INTERNAL_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planStatus").value("EXECUTED"))
                .andExpect(jsonPath("$.data.executedAt").exists());

        // 30/40/30 비율로 REFUND_AMOUNT 가 각 계좌에 적립됐는지 확인
        assertAccountBalance(savings.getId(),    expectedAmount(40));
        assertAccountBalance(deposit.getId(),    expectedAmount(30));
        assertAccountBalance(securities.getId(), expectedAmount(30));
    }

    @Test
    void RP008_내부키_없으면_401을_반환한다() throws Exception {
        // given
        Ipo ipo = persistIpo(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5), LocalDate.now().plusDays(3));
        IpoSubscription sub = persistAllocatedSubscription(ipo.getId());
        persistLinkedAccount("SAVINGS");
        persistLinkedAccount("DEPOSIT");
        Long planId = createReturnPlan(sub.getId());

        // when & then
        mockMvc.perform(put("/internal/return-plans/{id}/execute", planId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void RP008_내부키_틀리면_401을_반환한다() throws Exception {
        // given
        Ipo ipo = persistIpo(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5), LocalDate.now().plusDays(3));
        IpoSubscription sub = persistAllocatedSubscription(ipo.getId());
        persistLinkedAccount("SAVINGS");
        persistLinkedAccount("DEPOSIT");
        Long planId = createReturnPlan(sub.getId());

        // when & then
        mockMvc.perform(put("/internal/return-plans/{id}/execute", planId)
                        .header("X-Internal-Api-Key", "wrong-key"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void RP008_이미_실행된_플랜_재실행시_L011_409를_반환한다() throws Exception {
        // given
        Ipo ipo = persistIpo(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5), LocalDate.now().plusDays(3));
        IpoSubscription sub = persistAllocatedSubscription(ipo.getId());
        persistLinkedAccount("SAVINGS");
        persistLinkedAccount("DEPOSIT");
        persistLinkedAccount("SECURITIES");
        Long planId = createReturnPlan(sub.getId());
        updateRatios(planId, 30, 40, 30);
        executeReturnPlan(planId);

        // when & then
        mockMvc.perform(put("/internal/return-plans/{id}/execute", planId)
                        .header("X-Internal-Api-Key", INTERNAL_KEY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("L011"));
    }

    // ── RP-004: 목록 조회 ─────────────────────────────────────────────────────

    @Test
    void RP004_목록조회_성공시_실행된_플랜이_포함된다() throws Exception {
        // given
        Ipo ipo = persistIpo(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5), LocalDate.now().plusDays(3));
        IpoSubscription sub = persistAllocatedSubscription(ipo.getId());
        persistLinkedAccount("SAVINGS");
        persistLinkedAccount("DEPOSIT");
        persistLinkedAccount("SECURITIES");
        Long planId = createReturnPlan(sub.getId());
        updateRatios(planId, 30, 40, 30);
        executeReturnPlan(planId);

        // when & then
        mockMvc.perform(get("/api/v1/return-plans")
                        .with(user("tester"))
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.returnPlans[0].planStatus").value("EXECUTED"))
                .andExpect(jsonPath("$.data.returnPlans[0].sourceCompanyName").value("CoreWeave"));
    }

    @Test
    void RP004_from이_to보다_늦으면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/return-plans")
                        .with(user("tester"))
                        .header("X-User-Id", USER_ID)
                        .param("from", "2025-12-31")
                        .param("to",   "2025-01-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void RP004_size가_0이면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/return-plans")
                        .with(user("tester"))
                        .header("X-User-Id", USER_ID)
                        .param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void RP004_size가_100초과이면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/return-plans")
                        .with(user("tester"))
                        .header("X-User-Id", USER_ID)
                        .param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    // ── 즉시 분배 ──────────────────────────────────────────────────────────────

    @Test
    void 즉시분배_성공시_가용잔액이_비율대로_각_계좌에_이동된다() throws Exception {
        // given: CMA 계좌에 1000달러 보유
        FinancialAccount savings = persistLinkedAccount("SAVINGS");
        FinancialAccount deposit = persistLinkedAccount("DEPOSIT");
        persistLinkedAccount("SECURITIES", new BigDecimal("1000.00"));

        // when
        mockMvc.perform(post("/api/v1/return-plans/immediate")
                        .with(user("tester"))
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"allocations":[
                                  {"destinationType":"SECURITIES","ratio":50},
                                  {"destinationType":"SAVINGS","ratio":30},
                                  {"destinationType":"DEPOSIT","ratio":20}
                                ]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(1000.00));

        // then: SAVINGS +300, DEPOSIT +200 적립
        assertAccountBalance(savings.getId(), new BigDecimal("300.0000"));
        assertAccountBalance(deposit.getId(), new BigDecimal("200.0000"));
    }

    @Test
    void 즉시분배_비율합이_100이_아니면_L010_422를_반환한다() throws Exception {
        // given
        persistLinkedAccount("SECURITIES", new BigDecimal("1000.00"));

        // when & then
        mockMvc.perform(post("/api/v1/return-plans/immediate")
                        .with(user("tester"))
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"allocations":[
                                  {"destinationType":"SECURITIES","ratio":60},
                                  {"destinationType":"SAVINGS","ratio":30}
                                ]}"""))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("L010"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private BigDecimal expectedAmount(int ratioPercent) {
        return REFUND_AMOUNT.multiply(BigDecimal.valueOf(ratioPercent))
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
    }

    private void assertAccountBalance(Long accountId, BigDecimal expected) {
        BigDecimal balance = txTemplate.execute(status -> {
            em.clear(); // 1차 캐시를 비우고 DB 값 직접 조회
            return em.find(FinancialAccount.class, accountId).getBalance();
        });
        assertThat(balance).isEqualByComparingTo(expected);
    }

    private Long createReturnPlan(Long subscriptionId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/return-plans")
                        .with(user("tester"))
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subscriptionId\":" + subscriptionId + "}"))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.path("data").path("returnPlanId").asLong();
    }

    private void updateRatios(Long planId, int sec, int sav, int dep) throws Exception {
        mockMvc.perform(put("/api/v1/return-plans/{id}", planId)
                        .with(user("tester"))
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"allocations":[
                                  {"destinationType":"SECURITIES","ratio":%d},
                                  {"destinationType":"SAVINGS","ratio":%d},
                                  {"destinationType":"DEPOSIT","ratio":%d}
                                ]}""".formatted(sec, sav, dep)))
                .andExpect(status().isOk());
    }

    private void executeReturnPlan(Long planId) throws Exception {
        mockMvc.perform(put("/internal/return-plans/{id}/execute", planId)
                        .header("X-Internal-Api-Key", INTERNAL_KEY))
                .andExpect(status().isOk());
    }

    // ── DB persist helpers ─────────────────────────────────────────────────────

    private Ipo persistIpo(LocalDate startDate, LocalDate endDate, LocalDate refundDate) {
        return txTemplate.execute(status -> {
            Ipo ipo = new Ipo();
            ReflectionTestUtils.setField(ipo, "ticker",                "CRWV");
            ReflectionTestUtils.setField(ipo, "companyName",           "CoreWeave");
            ReflectionTestUtils.setField(ipo, "subscriptionStartDate", startDate);
            ReflectionTestUtils.setField(ipo, "subscriptionEndDate",   endDate);
            ReflectionTestUtils.setField(ipo, "refundDate",            refundDate);
            ReflectionTestUtils.setField(ipo, "ipoStatus",             "COMPLETED");
            em.persist(ipo);
            em.flush();
            return ipo;
        });
    }

    /** 배정 완료된 청약: refundAmount 있음 */
    private IpoSubscription persistAllocatedSubscription(Long ipoId) {
        return txTemplate.execute(status -> {
            IpoSubscription sub = IpoSubscription.request(
                    USER_ID, ipoId, 0L, 10, new BigDecimal("100"), new BigDecimal("1000"));
            em.persist(sub);
            ReflectionTestUtils.setField(sub, "subscriptionStatus", "CONFIRMED");
            ReflectionTestUtils.setField(sub, "confirmedAt",        LocalDateTime.now());
            ReflectionTestUtils.setField(sub, "allocatedShares",    7);
            ReflectionTestUtils.setField(sub, "allocatedAmount",    new BigDecimal("700.00"));
            ReflectionTestUtils.setField(sub, "refundAmount",       REFUND_AMOUNT);
            ReflectionTestUtils.setField(sub, "allocationRate",     new BigDecimal("70.0000"));
            ReflectionTestUtils.setField(sub, "resultStatus",       "COMPLETED");
            em.flush();
            return sub;
        });
    }

    /** 배정 미완료 청약: refundAmount = null */
    private IpoSubscription persistUnallocatedSubscription(Long ipoId) {
        return txTemplate.execute(status -> {
            IpoSubscription sub = IpoSubscription.request(
                    USER_ID, ipoId, 0L, 10, new BigDecimal("100"), new BigDecimal("1000"));
            em.persist(sub);
            em.flush();
            return sub;
        });
    }

    private FinancialAccount persistLinkedAccount(String accountType) {
        return persistLinkedAccount(accountType, BigDecimal.ZERO);
    }

    private FinancialAccount persistLinkedAccount(String accountType, BigDecimal balance) {
        return txTemplate.execute(status -> {
            FinancialAccount acc = new FinancialAccount();
            ReflectionTestUtils.setField(acc, "userId",          USER_ID);
            ReflectionTestUtils.setField(acc, "accountType",     accountType);
            ReflectionTestUtils.setField(acc, "institutionType", "SECURITIES".equals(accountType) ? "SECURITIES_FIRM" : "BANK");
            ReflectionTestUtils.setField(acc, "institutionName", "신한");
            ReflectionTestUtils.setField(acc, "currency",        "USD");
            ReflectionTestUtils.setField(acc, "balance",         balance);
            ReflectionTestUtils.setField(acc, "reservedBalance", BigDecimal.ZERO);
            ReflectionTestUtils.setField(acc, "linked",          true);
            em.persist(acc);
            em.flush();
            return acc;
        });
    }
}
