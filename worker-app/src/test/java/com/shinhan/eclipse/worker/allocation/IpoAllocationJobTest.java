package com.shinhan.eclipse.worker.allocation;

import com.shinhan.eclipse.WorkerApplication;
import com.shinhan.eclipse.domain.account.BalanceHold;
import com.shinhan.eclipse.domain.account.FinancialAccount;
import com.shinhan.eclipse.domain.ipo.Ipo;
import com.shinhan.eclipse.domain.subscription.IpoSubscription;
import com.shinhan.eclipse.worker.allocation.repository.WorkerBalanceHoldRepository;
import com.shinhan.eclipse.worker.allocation.repository.WorkerFinancialAccountRepository;
import com.shinhan.eclipse.worker.allocation.repository.WorkerIpoSubscriptionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 배정 결과에 따라 신청 시점에 잠가둔(reserve) 금액이 정확히 정리되는지(SETTLE/RELEASE) 검증한다.
 * 배정률 자체는 IpoAllocationEngine 내부에서 무작위로 결정되므로, 정확한 배정 수량을 단정하지 않고
 * "reservedBalance가 0으로 정리되고, balance는 배정분만큼만 줄어든다"는 불변식으로 검증한다.
 */
@SpringBootTest(
        classes = WorkerApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:worker-eclipse-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
                "spring.ai.openai.api-key=test-key",
        }
)
class IpoAllocationJobTest {

    @Autowired
    private IpoAllocationJob ipoAllocationJob;

    @Autowired
    private WorkerIpoSubscriptionRepository subscriptionRepository;

    @Autowired
    private WorkerFinancialAccountRepository financialAccountRepository;

    @Autowired
    private WorkerBalanceHoldRepository balanceHoldRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void 배정결과에따라_홀딩이_정산되어_reservedBalance가_0이되고_실제잔액은_배정분만큼만_줄어든다() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        BigDecimal initialBalance = new BigDecimal("100000");
        BigDecimal subscriptionAmount = new BigDecimal("1000");
        // 실제로 잠겨있는 금액은 신청금액이 아니라 청약대행증거금(신청금액 × 1.01)이다.
        BigDecimal agencyDeposit = new BigDecimal("1010.00");

        Long ipoId = tx.execute(status -> {
            Ipo ipo = new Ipo();
            ReflectionTestUtils.setField(ipo, "ticker", "CRWV");
            ReflectionTestUtils.setField(ipo, "companyName", "CoreWeave");
            ReflectionTestUtils.setField(ipo, "listingDate", LocalDate.now());
            em.persist(ipo);
            em.flush();
            return ipo.getId();
        });

        Long accountId = tx.execute(status -> {
            FinancialAccount account = new FinancialAccount();
            ReflectionTestUtils.setField(account, "userId", 1L);
            ReflectionTestUtils.setField(account, "accountType", "SECURITIES");
            ReflectionTestUtils.setField(account, "institutionType", "SECURITIES_FIRM");
            ReflectionTestUtils.setField(account, "institutionName", "신한투자증권");
            ReflectionTestUtils.setField(account, "balance", initialBalance);
            ReflectionTestUtils.setField(account, "reservedBalance", agencyDeposit);
            ReflectionTestUtils.setField(account, "linked", true);
            em.persist(account);
            em.flush();
            return account.getId();
        });

        Long subscriptionId = tx.execute(status -> {
            IpoSubscription subscription = IpoSubscription.request(1L, ipoId, accountId, 10, new BigDecimal("100"), subscriptionAmount);
            ReflectionTestUtils.setField(subscription, "subscriptionStatus", "CONFIRMED");
            em.persist(subscription);

            BalanceHold hold = BalanceHold.lock(accountId, subscription.getId(), agencyDeposit);
            em.persist(hold);
            em.flush();
            return subscription.getId();
        });

        Ipo ipo = tx.execute(status -> em.find(Ipo.class, ipoId));

        tx.executeWithoutResult(status -> ipoAllocationJob.allocateForIpo(ipo));

        IpoSubscription updatedSubscription = subscriptionRepository.findById(subscriptionId).orElseThrow();
        FinancialAccount updatedAccount = financialAccountRepository.findById(accountId).orElseThrow();
        BalanceHold updatedHold = balanceHoldRepository.findBySubscriptionId(subscriptionId).orElseThrow();

        assertThat(updatedSubscription.isAllocated()).isTrue();

        BigDecimal allocatedAmount = updatedSubscription.getAllocatedAmount();
        BigDecimal refundAmount = updatedSubscription.getRefundAmount();
        assertThat(allocatedAmount.add(refundAmount)).isEqualByComparingTo(agencyDeposit);

        // 핵심 불변식: 홀딩이 정리되어 reservedBalance는 0, 실제 차감은 배정분만큼만.
        assertThat(updatedAccount.getReservedBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo(initialBalance.subtract(allocatedAmount));

        assertThat(updatedHold.isLocked()).isFalse();
        assertThat(updatedHold.getHoldStatus()).isIn("SETTLED", "RELEASED");
    }
}
