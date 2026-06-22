package com.shinhan.eclipse.worker.allocation;

import com.shinhan.eclipse.worker.allocation.dto.AllocationApplicant;
import com.shinhan.eclipse.worker.allocation.dto.AllocationResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class IpoAllocationEngineTest {

    @Test
    void 수요가_공급을_초과하면_균등_비례_잔여주_순으로_정확히_배정된다() {
        // total=10, 신청 A=7,B=5,C=4 (총수요 16 > 공급 10)
        List<AllocationApplicant> applicants = List.of(
                new AllocationApplicant(1L, 7),
                new AllocationApplicant(2L, 5),
                new AllocationApplicant(3L, 4)
        );

        List<AllocationResult> results = IpoAllocationEngine.allocate(10, applicants, new Random(1));

        AllocationResult a = byCustomerId(results, 1L);
        AllocationResult b = byCustomerId(results, 2L);
        AllocationResult c = byCustomerId(results, 3L);

        assertThat(a.equalAllocated()).isEqualTo(1);
        assertThat(b.equalAllocated()).isEqualTo(1);
        assertThat(c.equalAllocated()).isEqualTo(1);

        assertThat(a.proportionalAllocated()).isEqualTo(3);
        assertThat(b.proportionalAllocated()).isEqualTo(2);
        assertThat(c.proportionalAllocated()).isEqualTo(1);

        // 잔여주 1주는 소수부가 가장 큰 C(0.6153...)에게 간다.
        assertThat(c.additionalAllocated()).isEqualTo(1);
        assertThat(a.additionalAllocated()).isEqualTo(0);
        assertThat(b.additionalAllocated()).isEqualTo(0);

        assertThat(a.finalAllocated()).isEqualTo(4);
        assertThat(b.finalAllocated()).isEqualTo(3);
        assertThat(c.finalAllocated()).isEqualTo(3);

        assertThat(a.allocationRate()).isEqualByComparingTo(BigDecimal.valueOf(4).divide(BigDecimal.valueOf(7), 6, java.math.RoundingMode.HALF_UP));

        int totalAllocated = a.finalAllocated() + b.finalAllocated() + c.finalAllocated();
        assertThat(totalAllocated).isEqualTo(10);
    }

    @Test
    void 공급이_수요를_초과하면_전원_신청수량만큼_전부_배정되고_잔여주는_버려진다() {
        List<AllocationApplicant> applicants = List.of(
                new AllocationApplicant(1L, 50),
                new AllocationApplicant(2L, 30),
                new AllocationApplicant(3L, 5)
        );

        List<AllocationResult> results = IpoAllocationEngine.allocate(100, applicants, new Random(1));

        AllocationResult a = byCustomerId(results, 1L);
        AllocationResult b = byCustomerId(results, 2L);
        AllocationResult c = byCustomerId(results, 3L);

        assertThat(a.finalAllocated()).isEqualTo(50);
        assertThat(b.finalAllocated()).isEqualTo(30);
        assertThat(c.finalAllocated()).isEqualTo(5);

        assertThat(a.allocationRate()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(b.allocationRate()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(c.allocationRate()).isEqualByComparingTo(BigDecimal.ONE);

        int totalAllocated = a.finalAllocated() + b.finalAllocated() + c.finalAllocated();
        assertThat(totalAllocated).isLessThanOrEqualTo(100);
    }

    @Test
    void 동일_시드면_결과가_재현된다() {
        List<AllocationApplicant> applicants = List.of(
                new AllocationApplicant(1L, 10),
                new AllocationApplicant(2L, 10),
                new AllocationApplicant(3L, 10)
        );

        List<AllocationResult> first = IpoAllocationEngine.allocate(15, applicants, new Random(42));
        List<AllocationResult> second = IpoAllocationEngine.allocate(15, applicants, new Random(42));

        assertThat(first).isEqualTo(second);
    }

    @Test
    void 어떤_경우에도_신청수량을_초과해_배정되지_않고_총배정량도_공급량을_넘지_않는다() {
        List<AllocationApplicant> applicants = List.of(
                new AllocationApplicant(1L, 3),
                new AllocationApplicant(2L, 1),
                new AllocationApplicant(3L, 17),
                new AllocationApplicant(4L, 0),
                new AllocationApplicant(5L, 9)
        );

        for (int seed = 0; seed < 20; seed++) {
            List<AllocationResult> results = IpoAllocationEngine.allocate(13, applicants, new Random(seed));

            int sum = 0;
            for (AllocationResult r : results) {
                assertThat(r.finalAllocated()).isLessThanOrEqualTo(r.requestedQuantity());
                assertThat(r.finalAllocated())
                        .isEqualTo(r.equalAllocated() + r.proportionalAllocated() + r.additionalAllocated());
                sum += r.finalAllocated();
            }
            assertThat(sum).isLessThanOrEqualTo(13);
        }
    }

    @Test
    void 청약자가_없으면_빈_리스트를_반환한다() {
        assertThat(IpoAllocationEngine.allocate(100, List.of())).isEmpty();
    }

    @Test
    void 공급량이_0이면_전원_0주가_배정된다() {
        List<AllocationApplicant> applicants = List.of(new AllocationApplicant(1L, 10));

        List<AllocationResult> results = IpoAllocationEngine.allocate(0, applicants);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).finalAllocated()).isZero();
        assertThat(results.get(0).allocationRate()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void 신청수량이_0인_청약자는_배정률이_0이고_예외가_발생하지_않는다() {
        List<AllocationApplicant> applicants = List.of(
                new AllocationApplicant(1L, 0),
                new AllocationApplicant(2L, 5)
        );

        List<AllocationResult> results = IpoAllocationEngine.allocate(10, applicants, new Random(1));

        AllocationResult zero = byCustomerId(results, 1L);
        assertThat(zero.finalAllocated()).isZero();
        assertThat(zero.allocationRate()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private static AllocationResult byCustomerId(List<AllocationResult> results, Long customerId) {
        return results.stream()
                .filter(r -> r.customerId().equals(customerId))
                .findFirst()
                .orElseThrow();
    }
}
