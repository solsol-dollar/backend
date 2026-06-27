package com.shinhan.eclipse.ledger.subscription;

import com.shinhan.eclipse.LedgerApplication;
import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.domain.account.FinancialAccount;
import com.shinhan.eclipse.domain.ipo.Ipo;
import com.shinhan.eclipse.domain.subscription.IpoSubscription;
import com.shinhan.eclipse.ledger.accountlink.AccountLinkService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        classes = LedgerApplication.class,
        properties = {
                // CI 환경변수(SPRING_DATASOURCE_*)가 application.yml보다 우선순위가 높아 H2 설정을 덮어쓰는 것을 방지
                "spring.datasource.url=jdbc:h2:mem:eclipse-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        }
)
class SubscriptionFacadeImplTest {

    @TestConfiguration
    static class TestClockConfig {
        @Bean
        Clock clock() {
            // 청약 가능 시간(09:00~17:00 KST) 안에 고정하여 CI 시간대 무관하게 테스트 통과
            return Clock.fixed(
                    LocalDate.now(ZoneId.of("Asia/Seoul"))
                            .atTime(LocalTime.of(10, 0))
                            .atZone(ZoneId.of("Asia/Seoul"))
                            .toInstant(),
                    ZoneId.of("Asia/Seoul")
            );
        }
    }

    @Autowired
    private SubscriptionFacade subscriptionFacade;

    @Autowired
    private AccountLinkService accountLinkService;

    @Autowired
    private EntityManager em;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate txTemplate;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @BeforeEach
    void setUp() {
        txTemplate = new TransactionTemplate(transactionManager);
    }

    @Test
    void 청약신청에_성공하면_REQUESTED_상태로_저장된다() {
        Ipo ipo = persistIpo(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
        FinancialAccount account = persistAccount(USER_ID, new BigDecimal("10000"));

        IpoSubscription saved = subscriptionFacade.requestSubscription(USER_ID, ipo.getId(), account.getId(), new BigDecimal("1000"), new BigDecimal("100"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSubscriptionStatus()).isEqualTo("REQUESTED");
        assertThat(saved.getSubscriptionAmount()).isEqualByComparingTo("1000");
    }

    @Test
    void 청약신청에_성공하면_실제잔액은_그대로고_사용가능잔액만_줄어든다() {
        Ipo ipo = persistIpo(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
        FinancialAccount account = persistAccount(USER_ID, new BigDecimal("10000"));

        subscriptionFacade.requestSubscription(USER_ID, ipo.getId(), account.getId(), new BigDecimal("1000"), new BigDecimal("100"));

        FinancialAccount updated = accountLinkService.getLinkedAccount(USER_ID, account.getId());
        assertThat(updated.getBalance()).isEqualByComparingTo("10000");
        assertThat(updated.availableBalance()).isEqualByComparingTo("9000");
    }

    @Test
    void 잔액이_부족하면_L001_예외가_발생한다() {
        Ipo ipo = persistIpo(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
        FinancialAccount account = persistAccount(USER_ID, new BigDecimal("500"));

        assertThatThrownBy(() -> subscriptionFacade.requestSubscription(USER_ID, ipo.getId(), account.getId(), new BigDecimal("1000"), new BigDecimal("100")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);
    }

    @Test
    void 청약기간이_아니면_L003_예외가_발생한다() {
        Ipo ipo = persistIpo(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5));
        FinancialAccount account = persistAccount(USER_ID, new BigDecimal("10000"));

        assertThatThrownBy(() -> subscriptionFacade.requestSubscription(USER_ID, ipo.getId(), account.getId(), new BigDecimal("1000"), new BigDecimal("100")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SUBSCRIPTION_PERIOD_INVALID);
    }

    @Test
    void 연동되지_않은_계좌면_L005_예외가_발생한다() {
        Ipo ipo = persistIpo(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
        FinancialAccount account = persistAccount(OTHER_USER_ID, new BigDecimal("10000"));

        assertThatThrownBy(() -> subscriptionFacade.requestSubscription(USER_ID, ipo.getId(), account.getId(), new BigDecimal("1000"), new BigDecimal("100")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_NOT_LINKED);
    }

    @Test
    void 청약확정에_성공하면_CONFIRMED가_되지만_홀딩된_금액은_그대로_잠겨있다() {
        Ipo ipo = persistIpo(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
        FinancialAccount account = persistAccount(USER_ID, new BigDecimal("10000"));
        IpoSubscription saved = subscriptionFacade.requestSubscription(USER_ID, ipo.getId(), account.getId(), new BigDecimal("1000"), new BigDecimal("100"));

        IpoSubscription confirmed = subscriptionFacade.confirmSubscription(saved.getId(), USER_ID);

        assertThat(confirmed.getSubscriptionStatus()).isEqualTo("CONFIRMED");
        assertThat(confirmed.getConfirmedAt()).isNotNull();

        // 확정 단계는 실제 차감(settle)을 하지 않는다 - 배정 결과가 나올 때까지 계속 홀딩 상태.
        FinancialAccount updated = accountLinkService.getLinkedAccount(USER_ID, account.getId());
        assertThat(updated.getBalance()).isEqualByComparingTo("10000");
        assertThat(updated.availableBalance()).isEqualByComparingTo("9000");
    }

    @Test
    void 이미_확정된_청약을_다시_확정하면_L002_예외가_발생한다() {
        Ipo ipo = persistIpo(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
        FinancialAccount account = persistAccount(USER_ID, new BigDecimal("10000"));
        IpoSubscription saved = subscriptionFacade.requestSubscription(USER_ID, ipo.getId(), account.getId(), new BigDecimal("1000"), new BigDecimal("100"));
        subscriptionFacade.confirmSubscription(saved.getId(), USER_ID);

        assertThatThrownBy(() -> subscriptionFacade.confirmSubscription(saved.getId(), USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SUBSCRIPTION_CONFLICT);
    }

    @Test
    void 청약취소에_성공하면_CANCELLED가_되고_홀딩이_해제된다() {
        Ipo ipo = persistIpo(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
        FinancialAccount account = persistAccount(USER_ID, new BigDecimal("10000"));
        IpoSubscription saved = subscriptionFacade.requestSubscription(USER_ID, ipo.getId(), account.getId(), new BigDecimal("1000"), new BigDecimal("100"));

        subscriptionFacade.cancelSubscription(saved.getId(), USER_ID);

        List<IpoSubscription> subscriptions = subscriptionFacade.getSubscriptions(USER_ID, ipo.getId(), null, null, null);
        assertThat(subscriptions).extracting(IpoSubscription::getSubscriptionStatus).containsExactly("CANCELLED");

        FinancialAccount updated = accountLinkService.getLinkedAccount(USER_ID, account.getId());
        assertThat(updated.getBalance()).isEqualByComparingTo("10000");
        assertThat(updated.availableBalance()).isEqualByComparingTo("10000");
    }

    @Test
    void 이미_확정된_청약을_취소하면_예외가_발생한다() {
        Ipo ipo = persistIpo(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
        FinancialAccount account = persistAccount(USER_ID, new BigDecimal("10000"));
        IpoSubscription saved = subscriptionFacade.requestSubscription(USER_ID, ipo.getId(), account.getId(), new BigDecimal("1000"), new BigDecimal("100"));
        subscriptionFacade.confirmSubscription(saved.getId(), USER_ID);

        assertThatThrownBy(() -> subscriptionFacade.cancelSubscription(saved.getId(), USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SUBSCRIPTION_PERIOD_INVALID);
    }

    /**
     * REQ-05-93-01: 동일 청약을 두 스레드가 동시에 확정 요청해도, 청약 row 락(findByIdAndUserIdForUpdate) 덕분에
     * 한쪽만 성공(CONFIRMED)하고 나머지 한쪽은 L002(SUBSCRIPTION_CONFLICT)로 막혀야 한다.
     */
    @Test
    void 동일_청약을_동시에_확정하면_한쪽만_성공하고_나머지는_충돌예외가_발생한다() throws InterruptedException {
        Ipo ipo = persistIpo(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
        FinancialAccount account = persistAccount(USER_ID, new BigDecimal("10000"));
        IpoSubscription saved = subscriptionFacade.requestSubscription(USER_ID, ipo.getId(), account.getId(), new BigDecimal("1000"), new BigDecimal("100"));

        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicReference<Object> result1 = new AtomicReference<>();
        AtomicReference<Object> result2 = new AtomicReference<>();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        executor.submit(() -> runConfirm(saved.getId(), readyLatch, startLatch, result1));
        executor.submit(() -> runConfirm(saved.getId(), readyLatch, startLatch, result2));

        assertThat(readyLatch.await(5, TimeUnit.SECONDS)).as("두 스레드가 준비되지 못함").isTrue();
        startLatch.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).as("스레드가 제한 시간 내에 종료되지 않음").isTrue();

        List<Object> results = List.of(result1.get(), result2.get());
        long successCount = results.stream().filter(r -> r instanceof IpoSubscription).count();
        long conflictCount = results.stream()
                .filter(r -> r instanceof BusinessException)
                .filter(r -> ((BusinessException) r).getErrorCode() == ErrorCode.SUBSCRIPTION_CONFLICT)
                .count();

        assertThat(successCount).isEqualTo(1);
        assertThat(conflictCount).isEqualTo(1);
    }

    private void runConfirm(Long subscriptionId, CountDownLatch readyLatch, CountDownLatch startLatch,
                            AtomicReference<Object> result) {
        try {
            readyLatch.countDown();
            startLatch.await();
            result.set(subscriptionFacade.confirmSubscription(subscriptionId, USER_ID));
        } catch (BusinessException e) {
            result.set(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Ipo persistIpo(LocalDate startDate, LocalDate endDate) {
        return txTemplate.execute(status -> {
            Ipo ipo = new Ipo();
            ReflectionTestUtils.setField(ipo, "ticker", "CRWV");
            ReflectionTestUtils.setField(ipo, "companyName", "CoreWeave");
            ReflectionTestUtils.setField(ipo, "subscriptionStartDate", startDate);
            ReflectionTestUtils.setField(ipo, "subscriptionEndDate", endDate);
            em.persist(ipo);
            em.flush();
            return ipo;
        });
    }

    private FinancialAccount persistAccount(Long userId, BigDecimal balance) {
        return txTemplate.execute(status -> {
            FinancialAccount account = new FinancialAccount();
            ReflectionTestUtils.setField(account, "userId", userId);
            ReflectionTestUtils.setField(account, "accountType", "SECURITIES");
            ReflectionTestUtils.setField(account, "institutionType", "SECURITIES_FIRM");
            ReflectionTestUtils.setField(account, "institutionName", "신한투자증권");
            ReflectionTestUtils.setField(account, "balance", balance);
            ReflectionTestUtils.setField(account, "linked", true);
            em.persist(account);
            em.flush();
            return account;
        });
    }
}
