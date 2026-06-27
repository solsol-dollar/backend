package com.shinhan.eclipse.ledger.returnplan.internal;

import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.domain.account.FinancialAccount;
import com.shinhan.eclipse.domain.ipo.Ipo;
import com.shinhan.eclipse.domain.returnplan.ReturnPlan;
import com.shinhan.eclipse.domain.returnplan.ReturnPlanAllocation;
import com.shinhan.eclipse.domain.returnplan.ReturnPlanPreset;
import com.shinhan.eclipse.domain.subscription.IpoSubscription;
import com.shinhan.eclipse.ledger.accountlink.AccountLinkService;
import com.shinhan.eclipse.ledger.returnplan.dto.AllocationItem;
import com.shinhan.eclipse.ledger.subscription.SubscriptionFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * ReturnPlanFacadeImpl 단위 테스트.
 * DB·외부 서비스는 모두 Mock으로 대체하여 비즈니스 로직 분기만 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class ReturnPlanFacadeImplTest {

    @Mock ReturnPlanRepository           returnPlanRepository;
    @Mock ReturnPlanAllocationRepository allocationRepository;
    @Mock ReturnPlanPresetRepository     presetRepository;
    @Mock SubscriptionFacade             subscriptionFacade;
    @Mock AccountLinkService             accountLinkService;
    @Mock ApplicationEventPublisher      eventPublisher;

    @InjectMocks ReturnPlanFacadeImpl facade;

    private static final Long USER_ID = 1L;
    private static final Long PLAN_ID = 10L;
    private static final Long SUB_ID  = 20L;
    private static final Long IPO_ID  = 30L;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private IpoSubscription subscriptionFixture(BigDecimal refundAmount) {
        IpoSubscription sub = new IpoSubscription();
        ReflectionTestUtils.setField(sub, "id",                 SUB_ID);
        ReflectionTestUtils.setField(sub, "userId",             USER_ID);
        ReflectionTestUtils.setField(sub, "ipoId",              IPO_ID);
        ReflectionTestUtils.setField(sub, "refundAmount",       refundAmount);
        ReflectionTestUtils.setField(sub, "subscriptionAmount", new BigDecimal("1000.00"));
        ReflectionTestUtils.setField(sub, "subscriptionStatus", "CONFIRMED");
        return sub;
    }

    private FinancialAccount accountFixture(String type, BigDecimal balance) {
        FinancialAccount acc = new FinancialAccount();
        ReflectionTestUtils.setField(acc, "id",              (long) type.hashCode());
        ReflectionTestUtils.setField(acc, "userId",          USER_ID);
        ReflectionTestUtils.setField(acc, "accountType",     type);
        ReflectionTestUtils.setField(acc, "balance",         balance);
        ReflectionTestUtils.setField(acc, "reservedBalance", BigDecimal.ZERO);
        ReflectionTestUtils.setField(acc, "linked",          true);
        return acc;
    }

    private ReturnPlan planFixture(String status) {
        ReturnPlan plan = ReturnPlan.create(USER_ID, SUB_ID, new BigDecimal("210.50"), null, null, null);
        ReflectionTestUtils.setField(plan, "id",         PLAN_ID);
        ReflectionTestUtils.setField(plan, "planStatus", status);
        return plan;
    }

    private ReturnPlanAllocation allocationFixture(String type, int ratio) {
        ReturnPlanAllocation alloc = ReturnPlanAllocation.initZero(PLAN_ID, type);
        alloc.updateRatio(ratio, new BigDecimal("210.50"));
        return alloc;
    }

    private Ipo ipoFixture(LocalDate refundDate) {
        Ipo ipo = new Ipo();
        ReflectionTestUtils.setField(ipo, "id",                    IPO_ID);
        ReflectionTestUtils.setField(ipo, "refundDate",            refundDate);
        ReflectionTestUtils.setField(ipo, "subscriptionStartDate", LocalDate.now().minusDays(5));
        ReflectionTestUtils.setField(ipo, "subscriptionEndDate",   LocalDate.now().minusDays(1));
        return ipo;
    }

    // ── RP-001: createReturnPlan ───────────────────────────────────────────────

    @Test
    void 리턴플랜_생성_성공시_DRAFT_상태로_반환된다() {
        // given
        given(subscriptionFacade.getSubscriptionResult(SUB_ID, USER_ID))
                .willReturn(subscriptionFixture(new BigDecimal("210.50")));
        given(returnPlanRepository.existsBySubscriptionId(SUB_ID)).willReturn(false);
        given(accountLinkService.findAccountByType(USER_ID, "SAVINGS"))
                .willReturn(Optional.of(accountFixture("SAVINGS", BigDecimal.ZERO)));
        given(accountLinkService.findAccountByType(USER_ID, "DEPOSIT"))
                .willReturn(Optional.of(accountFixture("DEPOSIT", BigDecimal.ZERO)));
        given(subscriptionFacade.findNextUpcomingIpo()).willReturn(Optional.empty());
        given(returnPlanRepository.saveAndFlush(any())).willAnswer(inv -> {
            ReturnPlan p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", PLAN_ID);
            return p;
        });

        // when
        ReturnPlan result = facade.createReturnPlan(USER_ID, SUB_ID);

        // then
        assertThat(result.isDraft()).isTrue();
        assertThat(result.getTotalRefundAmount()).isEqualByComparingTo("210.50");
        then(allocationRepository).should(org.mockito.Mockito.times(3)).save(any());
    }

    @Test
    void 배정결과가_없으면_L007_예외가_발생한다() {
        // given
        given(subscriptionFacade.getSubscriptionResult(SUB_ID, USER_ID))
                .willReturn(subscriptionFixture(null)); // refundAmount == null

        // when & then
        assertThatThrownBy(() -> facade.createReturnPlan(USER_ID, SUB_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALLOCATION_NOT_FOUND);
    }

    @Test
    void 같은_청약으로_이미_리턴플랜이_있으면_L008_예외가_발생한다() {
        // given
        given(subscriptionFacade.getSubscriptionResult(SUB_ID, USER_ID))
                .willReturn(subscriptionFixture(new BigDecimal("210.50")));
        given(returnPlanRepository.existsBySubscriptionId(SUB_ID)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> facade.createReturnPlan(USER_ID, SUB_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RETURN_PLAN_ALREADY_EXISTS);
    }

    @Test
    void SAVINGS_계좌가_미연동이면_L005_예외가_발생한다() {
        // given
        given(subscriptionFacade.getSubscriptionResult(SUB_ID, USER_ID))
                .willReturn(subscriptionFixture(new BigDecimal("210.50")));
        given(returnPlanRepository.existsBySubscriptionId(SUB_ID)).willReturn(false);
        given(accountLinkService.findAccountByType(USER_ID, "SAVINGS")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> facade.createReturnPlan(USER_ID, SUB_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_NOT_LINKED);
    }

    @Test
    void DEPOSIT_계좌가_미연동이면_L005_예외가_발생한다() {
        // given
        given(subscriptionFacade.getSubscriptionResult(SUB_ID, USER_ID))
                .willReturn(subscriptionFixture(new BigDecimal("210.50")));
        given(returnPlanRepository.existsBySubscriptionId(SUB_ID)).willReturn(false);
        given(accountLinkService.findAccountByType(USER_ID, "SAVINGS"))
                .willReturn(Optional.of(accountFixture("SAVINGS", BigDecimal.ZERO)));
        given(accountLinkService.findAccountByType(USER_ID, "DEPOSIT")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> facade.createReturnPlan(USER_ID, SUB_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_NOT_LINKED);
    }

    // ── RP-002: updateRatios ──────────────────────────────────────────────────

    @Test
    void 비율_수정_성공시_전체_합이_100이면_정상처리된다() {
        // given
        ReturnPlan plan = planFixture("DRAFT");
        given(returnPlanRepository.findByIdAndUserIdForUpdate(PLAN_ID, USER_ID)).willReturn(Optional.of(plan));
        given(subscriptionFacade.getSubscriptionResult(SUB_ID, USER_ID))
                .willReturn(subscriptionFixture(new BigDecimal("210.50")));
        given(subscriptionFacade.getIpo(IPO_ID)).willReturn(ipoFixture(LocalDate.now().plusDays(5)));
        stubAllocations(0, 0, 0);
        stubAllAllocations(30, 40, 30);

        // when
        ReturnPlan result = facade.updateRatios(PLAN_ID, USER_ID, List.of(
                new AllocationItem("SECURITIES", 30),
                new AllocationItem("SAVINGS",    40),
                new AllocationItem("DEPOSIT",    30)));

        // then
        assertThat(result.isDraft()).isTrue();
    }

    @Test
    void 비율_합이_100이_아니면_L010_예외가_발생한다() {
        // given
        ReturnPlan plan = planFixture("DRAFT");
        given(returnPlanRepository.findByIdAndUserIdForUpdate(PLAN_ID, USER_ID)).willReturn(Optional.of(plan));
        given(subscriptionFacade.getSubscriptionResult(SUB_ID, USER_ID))
                .willReturn(subscriptionFixture(new BigDecimal("210.50")));
        given(subscriptionFacade.getIpo(IPO_ID)).willReturn(ipoFixture(LocalDate.now().plusDays(5)));
        stubAllocations(0, 0, 0);
        stubAllAllocations(50, 20, 20); // 전체 재검증 시 합 = 90

        // when & then
        assertThatThrownBy(() -> facade.updateRatios(PLAN_ID, USER_ID, List.of(
                new AllocationItem("SECURITIES", 50),
                new AllocationItem("SAVINGS",    20),
                new AllocationItem("DEPOSIT",    20))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RATIO_SUM_INVALID);
    }

    @Test
    void 이미_실행된_플랜을_수정하면_L011_예외가_발생한다() {
        // given
        ReturnPlan plan = planFixture("EXECUTED");
        given(returnPlanRepository.findByIdAndUserIdForUpdate(PLAN_ID, USER_ID)).willReturn(Optional.of(plan));

        // when & then
        assertThatThrownBy(() -> facade.updateRatios(PLAN_ID, USER_ID, List.of(new AllocationItem("SECURITIES", 100))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RETURN_PLAN_CONFLICT);
    }

    @Test
    void 환불일이_이미_지났으면_L013_예외가_발생한다() {
        // given: refundDate = 어제 → 20:00 KST 마감이 이미 지남
        ReturnPlan plan = planFixture("DRAFT");
        given(returnPlanRepository.findByIdAndUserIdForUpdate(PLAN_ID, USER_ID)).willReturn(Optional.of(plan));
        given(subscriptionFacade.getSubscriptionResult(SUB_ID, USER_ID))
                .willReturn(subscriptionFixture(new BigDecimal("210.50")));
        given(subscriptionFacade.getIpo(IPO_ID)).willReturn(ipoFixture(LocalDate.now().minusDays(1)));

        // when & then
        assertThatThrownBy(() -> facade.updateRatios(PLAN_ID, USER_ID, List.of(new AllocationItem("SECURITIES", 100))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RETURN_PLAN_EDIT_WINDOW_CLOSED);
    }

    @Test
    void refundDate가_null이면_수정_기한_검사를_건너뛴다() {
        // given
        ReturnPlan plan = planFixture("DRAFT");
        given(returnPlanRepository.findByIdAndUserIdForUpdate(PLAN_ID, USER_ID)).willReturn(Optional.of(plan));
        given(subscriptionFacade.getSubscriptionResult(SUB_ID, USER_ID))
                .willReturn(subscriptionFixture(new BigDecimal("210.50")));
        given(subscriptionFacade.getIpo(IPO_ID)).willReturn(ipoFixture(null)); // refundDate = null
        stubAllocations(0, 0, 0);
        stubAllAllocations(100, 0, 0);

        // when — 예외 없이 정상 처리돼야 한다
        ReturnPlan result = facade.updateRatios(PLAN_ID, USER_ID, List.of(new AllocationItem("SECURITIES", 100)));

        assertThat(result.isDraft()).isTrue();
    }

    // ── RP-008: executeReturnPlan ─────────────────────────────────────────────

    @Test
    void 실행_성공시_EXECUTED_상태로_전환된다() {
        // given
        ReturnPlan plan = planFixture("DRAFT");
        given(returnPlanRepository.findByIdForUpdate(PLAN_ID)).willReturn(Optional.of(plan));
        given(accountLinkService.findAccountByType(USER_ID, "SAVINGS"))
                .willReturn(Optional.of(accountFixture("SAVINGS", BigDecimal.ZERO)));
        given(accountLinkService.findAccountByType(USER_ID, "DEPOSIT"))
                .willReturn(Optional.of(accountFixture("DEPOSIT", BigDecimal.ZERO)));
        given(accountLinkService.findAccountByType(USER_ID, "SECURITIES"))
                .willReturn(Optional.of(accountFixture("SECURITIES", BigDecimal.ZERO)));
        given(allocationRepository.findByReturnPlanIdOrderByIdAsc(PLAN_ID)).willReturn(List.of(
                allocationFixture("SECURITIES", 30),
                allocationFixture("SAVINGS",    40),
                allocationFixture("DEPOSIT",    30)));

        // when
        ReturnPlan result = facade.executeReturnPlan(PLAN_ID);

        // then
        assertThat(result.isExecuted()).isTrue();
        assertThat(result.getExecutedAt()).isNotNull();
    }

    @Test
    void 이미_실행된_플랜을_재실행하면_L011_예외가_발생한다() {
        // given
        ReturnPlan plan = planFixture("EXECUTED");
        given(returnPlanRepository.findByIdForUpdate(PLAN_ID)).willReturn(Optional.of(plan));

        // when & then
        assertThatThrownBy(() -> facade.executeReturnPlan(PLAN_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RETURN_PLAN_CONFLICT);
    }

    @Test
    void 비율_미설정_플랜_실행시_SECURITIES_100프로가_기본값으로_적용된다() {
        // given: 모든 allocation이 0% → 합계 0 → 기본값(SECURITIES 100%) 적용
        ReturnPlan plan = planFixture("DRAFT");
        given(returnPlanRepository.findByIdForUpdate(PLAN_ID)).willReturn(Optional.of(plan));
        given(accountLinkService.findAccountByType(USER_ID, "SAVINGS"))
                .willReturn(Optional.of(accountFixture("SAVINGS", BigDecimal.ZERO)));
        given(accountLinkService.findAccountByType(USER_ID, "DEPOSIT"))
                .willReturn(Optional.of(accountFixture("DEPOSIT", BigDecimal.ZERO)));
        given(accountLinkService.findAccountByType(USER_ID, "SECURITIES"))
                .willReturn(Optional.of(accountFixture("SECURITIES", BigDecimal.ZERO)));
        given(allocationRepository.findByReturnPlanIdOrderByIdAsc(PLAN_ID)).willReturn(List.of(
                allocationFixture("SECURITIES", 0),
                allocationFixture("SAVINGS",    0),
                allocationFixture("DEPOSIT",    0)));

        // when
        facade.executeReturnPlan(PLAN_ID);

        // then: SECURITIES 에만 credit 이 호출된다 (SAVINGS/DEPOSIT은 amount = 0 이라 skip)
        then(accountLinkService).should(org.mockito.Mockito.times(1)).credit(eq(USER_ID), anyLong(), any());
    }

    @Test
    void 실행시_SAVINGS_계좌가_미연동이면_L005_예외가_발생한다() {
        // given
        ReturnPlan plan = planFixture("DRAFT");
        given(returnPlanRepository.findByIdForUpdate(PLAN_ID)).willReturn(Optional.of(plan));
        given(accountLinkService.findAccountByType(USER_ID, "SAVINGS")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> facade.executeReturnPlan(PLAN_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_NOT_LINKED);
    }

    // ── applyPreset ───────────────────────────────────────────────────────────

    @Test
    void 존재하지_않는_프리셋_코드면_L012_예외가_발생한다() {
        // given
        given(presetRepository.findByPresetCode("INVALID_CODE")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> facade.applyPreset(PLAN_ID, USER_ID, "INVALID_CODE"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RETURN_PLAN_PRESET_NOT_FOUND);
    }

    @Test
    void 유효한_프리셋_적용시_해당_비율로_업데이트된다() {
        // given
        ReturnPlanPreset preset = new ReturnPlanPreset();
        ReflectionTestUtils.setField(preset, "presetCode",       "INVEST_FOCUS");
        ReflectionTestUtils.setField(preset, "securitiesRatio",  new BigDecimal("70"));
        ReflectionTestUtils.setField(preset, "savingsRatio",     new BigDecimal("20"));
        ReflectionTestUtils.setField(preset, "accountRatio",     new BigDecimal("10"));

        ReturnPlan plan = planFixture("DRAFT");
        given(presetRepository.findByPresetCode("INVEST_FOCUS")).willReturn(Optional.of(preset));
        given(returnPlanRepository.findByIdAndUserIdForUpdate(PLAN_ID, USER_ID)).willReturn(Optional.of(plan));
        given(subscriptionFacade.getSubscriptionResult(SUB_ID, USER_ID))
                .willReturn(subscriptionFixture(new BigDecimal("210.50")));
        given(subscriptionFacade.getIpo(IPO_ID)).willReturn(ipoFixture(LocalDate.now().plusDays(5)));
        stubAllocations(0, 0, 0);
        stubAllAllocations(70, 20, 10);

        // when
        ReturnPlan result = facade.applyPreset(PLAN_ID, USER_ID, "INVEST_FOCUS");

        // then
        assertThat(result.isDraft()).isTrue();
    }

    // ── executeImmediateAllocation ─────────────────────────────────────────────

    @Test
    void 즉시분배_성공시_가용잔액_기준으로_분배금액이_계산된다() {
        // given: CMA 가용 잔액 $1000
        FinancialAccount cma = accountFixture("SECURITIES", new BigDecimal("1000.00"));
        given(accountLinkService.findAccountByType(USER_ID, "SECURITIES")).willReturn(Optional.of(cma));
        given(accountLinkService.findAccountByType(USER_ID, "SAVINGS"))
                .willReturn(Optional.of(accountFixture("SAVINGS", BigDecimal.ZERO)));
        given(accountLinkService.findAccountByType(USER_ID, "DEPOSIT"))
                .willReturn(Optional.of(accountFixture("DEPOSIT", BigDecimal.ZERO)));

        // when
        var result = facade.executeImmediateAllocation(USER_ID, List.of(
                new AllocationItem("SECURITIES", 50),
                new AllocationItem("SAVINGS",    30),
                new AllocationItem("DEPOSIT",    20)));

        // then
        assertThat(result.totalAmount()).isEqualByComparingTo("1000.00");
        assertThat(result.allocations()).hasSize(3);
        assertThat(result.allocations().stream()
                .filter(a -> "SAVINGS".equals(a.destinationType()))
                .findFirst().orElseThrow().amount())
                .isEqualByComparingTo("300.0000");
    }

    @Test
    void 즉시분배_비율합이_100이_아니면_L010_예외가_발생한다() {
        // given: 합계 = 90 (경계값: 1 부족)
        // when & then
        assertThatThrownBy(() -> facade.executeImmediateAllocation(USER_ID, List.of(
                new AllocationItem("SECURITIES", 60),
                new AllocationItem("SAVINGS",    30))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RATIO_SUM_INVALID);
    }

    @Test
    void 즉시분배_가용잔액이_0이면_L001_예외가_발생한다() {
        // given: balance = 0 → availableBalance = 0
        FinancialAccount cma = accountFixture("SECURITIES", BigDecimal.ZERO);
        given(accountLinkService.findAccountByType(USER_ID, "SECURITIES")).willReturn(Optional.of(cma));

        // when & then
        assertThatThrownBy(() -> facade.executeImmediateAllocation(USER_ID, List.of(
                new AllocationItem("SECURITIES", 100))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);
    }

    @Test
    void 즉시분배_CMA계좌_미연동이면_L005_예외가_발생한다() {
        // given
        given(accountLinkService.findAccountByType(USER_ID, "SECURITIES")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> facade.executeImmediateAllocation(USER_ID, List.of(
                new AllocationItem("SECURITIES", 100))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_NOT_LINKED);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** updateRatios 내부의 findByReturnPlanIdAndDestinationType stub */
    private void stubAllocations(int secRatio, int savRatio, int depRatio) {
        given(allocationRepository.findByReturnPlanIdAndDestinationType(PLAN_ID, "SECURITIES"))
                .willReturn(Optional.of(allocationFixture("SECURITIES", secRatio)));
        given(allocationRepository.findByReturnPlanIdAndDestinationType(PLAN_ID, "SAVINGS"))
                .willReturn(Optional.of(allocationFixture("SAVINGS", savRatio)));
        given(allocationRepository.findByReturnPlanIdAndDestinationType(PLAN_ID, "DEPOSIT"))
                .willReturn(Optional.of(allocationFixture("DEPOSIT", depRatio)));
    }

    /** validateRatioSum 에서 전체 목록 재조회 stub */
    private void stubAllAllocations(int secRatio, int savRatio, int depRatio) {
        given(allocationRepository.findByReturnPlanIdOrderByIdAsc(PLAN_ID))
                .willReturn(List.of(
                        allocationFixture("SECURITIES", secRatio),
                        allocationFixture("SAVINGS",    savRatio),
                        allocationFixture("DEPOSIT",    depRatio)));
    }
}
