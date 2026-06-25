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
    void 신청자별로_독립적으로_배정되고_신청수량을_초과하지_않는다() {
        List<AllocationApplicant> applicants = List.of(
                new AllocationApplicant(1L, 100),
                new AllocationApplicant(2L, 50),
                new AllocationApplicant(3L, 7)
        );

        for (int seed = 0; seed < 50; seed++) {
            List<AllocationResult> results = IpoAllocationEngine.allocate(applicants, new Random(seed));

            assertThat(results).hasSize(3);
            for (AllocationResult r : results) {
                assertThat(r.finalAllocated()).isGreaterThanOrEqualTo(0);
                assertThat(r.finalAllocated()).isLessThanOrEqualTo(r.requestedQuantity());
                assertThat(r.allocationRate())
                        .isEqualByComparingTo(
                                BigDecimal.valueOf(r.finalAllocated())
                                        .divide(BigDecimal.valueOf(r.requestedQuantity()), 6, java.math.RoundingMode.HALF_UP));
            }
        }
    }

    @Test
    void 배정률은_저배정_구간에_훨씬_자주_몰린다() {
        int lowTierCount = 0;
        int totalTrials = 1000;
        Random random = new Random(7);

        for (int i = 0; i < totalTrials; i++) {
            List<AllocationResult> results = IpoAllocationEngine.allocate(
                    List.of(new AllocationApplicant(1L, 10_000)), random);
            BigDecimal rate = results.get(0).allocationRate();
            if (rate.compareTo(BigDecimal.valueOf(0.15)) <= 0) {
                lowTierCount++;
            }
        }

        // 80% 확률로 저배정(0~15%) 구간에서 뽑히므로, 1000회 중 대다수가 그 구간에 속해야 한다.
        assertThat(lowTierCount).isGreaterThan(totalTrials * 6 / 10);
    }

    @Test
    void 동일_시드면_결과가_재현된다() {
        List<AllocationApplicant> applicants = List.of(
                new AllocationApplicant(1L, 10),
                new AllocationApplicant(2L, 10),
                new AllocationApplicant(3L, 10)
        );

        List<AllocationResult> first = IpoAllocationEngine.allocate(applicants, new Random(42));
        List<AllocationResult> second = IpoAllocationEngine.allocate(applicants, new Random(42));

        assertThat(first).isEqualTo(second);
    }

    @Test
    void 청약자가_없으면_빈_리스트를_반환한다() {
        assertThat(IpoAllocationEngine.allocate(List.of())).isEmpty();
    }

    @Test
    void 신청수량이_0이면_배정률도_0이고_예외가_발생하지_않는다() {
        List<AllocationResult> results = IpoAllocationEngine.allocate(
                List.of(new AllocationApplicant(1L, 0)), new Random(1));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).finalAllocated()).isZero();
        assertThat(results.get(0).allocationRate()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
