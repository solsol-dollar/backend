package com.shinhan.eclipse.worker.allocation;

import com.shinhan.eclipse.worker.allocation.dto.AllocationApplicant;
import com.shinhan.eclipse.worker.allocation.dto.AllocationResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 청약자 각각을 서로 독립적으로 무작위 배정하는 순수 배정 엔진. "중개사가 받아온 총 물량을
 * 나눠 갖는" 풀(pool) 개념이 없다 — 미국 현지 IPO 중개사의 실제 배정 로직이 비공개·불투명하다는
 * 점을 반영해, 신청자별로 독립적인 배정률을 무작위로 부여한다.
 *
 * <p>배정률 분포: 80% 확률로 0~15%(저배정, 흔한 경우), 20% 확률로 15~40%(고배정, 가끔 나오는 경우)
 * 구간에서 균등 난수로 뽑는다. 순수 0~40% 균등분포보다 저배정 쪽으로 치우쳐 "물량이 적어 대부분
 * 못 받는다"는 현실감을 주면서도, 가끔 후한 배정이 나와야 데모/화면이 죽어 보이지 않는다.
 *
 * <p>부작용(IO/영속화) 없음 — 같은 입력 + 같은 Random 시퀀스면 항상 같은 결과를 반환한다.
 * 시간복잡도: O(n).
 */
public final class IpoAllocationEngine {

    private static final double LOW_TIER_PROBABILITY = 0.8;
    private static final double LOW_TIER_MAX_RATE = 0.15;
    private static final double HIGH_TIER_MIN_RATE = 0.15;
    private static final double HIGH_TIER_MAX_RATE = 0.40;

    private IpoAllocationEngine() {}

    public static List<AllocationResult> allocate(List<AllocationApplicant> applicants) {
        return allocate(applicants, new Random());
    }

    public static List<AllocationResult> allocate(List<AllocationApplicant> applicants, Random random) {
        List<AllocationResult> results = new ArrayList<>(applicants.size());
        for (AllocationApplicant applicant : applicants) {
            results.add(allocateOne(applicant, random));
        }
        return results;
    }

    private static AllocationResult allocateOne(AllocationApplicant applicant, Random random) {
        int requested = applicant.requestedQuantity();
        if (requested <= 0) {
            return new AllocationResult(applicant.customerId(), requested, 0, BigDecimal.ZERO);
        }

        double rate = rollAllocationRate(random);
        int finalAllocated = (int) Math.round(requested * rate);
        finalAllocated = Math.max(0, Math.min(requested, finalAllocated));

        BigDecimal allocationRate = BigDecimal.valueOf(finalAllocated)
                .divide(BigDecimal.valueOf(requested), 6, RoundingMode.HALF_UP);

        return new AllocationResult(applicant.customerId(), requested, finalAllocated, allocationRate);
    }

    private static double rollAllocationRate(Random random) {
        if (random.nextDouble() < LOW_TIER_PROBABILITY) {
            return random.nextDouble() * LOW_TIER_MAX_RATE;
        }
        return HIGH_TIER_MIN_RATE + random.nextDouble() * (HIGH_TIER_MAX_RATE - HIGH_TIER_MIN_RATE);
    }
}
