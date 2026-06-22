package com.shinhan.eclipse.worker.allocation;

import com.shinhan.eclipse.worker.allocation.dto.AllocationApplicant;
import com.shinhan.eclipse.worker.allocation.dto.AllocationResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * 해외 주관사로부터 실제로 받은 totalAllocatedQuantity를 청약자들에게 분배하는 순수 배정 엔진.
 * 부작용(IO/영속화) 없음 — 같은 입력 + 같은 Random 시퀀스면 항상 같은 결과를 반환한다.
 *
 * <p>정책: 균등배정(50%) → 비례배정(나머지) → 잔여주 1주씩 추가배정(소수점 큰 순, 동률은 추첨).
 *
 * <p>스펙 보정: 비례배정 원안(remainingPool * remainingRequest / totalRemainingRequest)은
 * remainingPool이 totalRemainingRequest보다 큰 경우(공급이 잔여 수요를 초과하는 경우) 신청수량을
 * 초과해 배정할 수 있다. "최종 배정수량은 신청수량을 초과할 수 없다"는 제약을 지키기 위해
 * proportionalAllocated는 remainingRequest로 상한을 두고, 그 차이는 잔여주 풀에 다시 합산해
 * 다른 청약자에게 재분배한다.
 *
 * <p>시간복잡도: O(n log n) — 잔여주 우선순위 정렬이 지배적. 잔여주 재분배 루프는 일반적으로
 * 반올림 손실 수준(n 미만)의 소량이라 실질적으로 O(n)에 가깝다.
 */
public final class IpoAllocationEngine {

    private IpoAllocationEngine() {}

    public static List<AllocationResult> allocate(int totalAllocatedQuantity, List<AllocationApplicant> applicants) {
        return allocate(totalAllocatedQuantity, applicants, new Random());
    }

    public static List<AllocationResult> allocate(
            int totalAllocatedQuantity, List<AllocationApplicant> applicants, Random random) {
        int n = applicants.size();
        if (n == 0 || totalAllocatedQuantity <= 0) {
            return applicants.stream().map(IpoAllocationEngine::zeroResult).toList();
        }

        // 1. 균등배정
        int equalPool = totalAllocatedQuantity / 2;
        int equalShare = equalPool / n;

        int[] equalAllocated = new int[n];
        int[] remainingRequest = new int[n];
        long totalRemainingRequest = 0;
        long equalAllocatedSum = 0;

        for (int i = 0; i < n; i++) {
            int requested = applicants.get(i).requestedQuantity();
            equalAllocated[i] = Math.min(equalShare, requested);
            remainingRequest[i] = requested - equalAllocated[i];
            totalRemainingRequest += remainingRequest[i];
            equalAllocatedSum += equalAllocated[i];
        }

        // 2~3. 비례배정 (remainingRequest 초과 방지 캡 포함)
        long remainingPool = totalAllocatedQuantity - equalAllocatedSum;
        int[] proportionalAllocated = new int[n];
        BigDecimal[] fractionalParts = new BigDecimal[n];

        if (totalRemainingRequest > 0) {
            BigDecimal remainingPoolBd = BigDecimal.valueOf(remainingPool);
            BigDecimal totalRemainingBd = BigDecimal.valueOf(totalRemainingRequest);
            for (int i = 0; i < n; i++) {
                if (remainingRequest[i] == 0) {
                    fractionalParts[i] = BigDecimal.ZERO;
                    continue;
                }
                BigDecimal raw = remainingPoolBd
                        .multiply(BigDecimal.valueOf(remainingRequest[i]))
                        .divide(totalRemainingBd, 10, RoundingMode.HALF_UP);
                int floorValue = raw.setScale(0, RoundingMode.FLOOR).intValue();
                int capped = Math.min(floorValue, remainingRequest[i]);
                proportionalAllocated[i] = capped;
                // 캡으로 잘려나간 경우 소수부 의미가 없으므로 0으로 둔다 (이미 신청수량만큼 받음 = room 없음).
                fractionalParts[i] = capped < floorValue ? BigDecimal.ZERO : raw.subtract(BigDecimal.valueOf(floorValue));
            }
        } else {
            for (int i = 0; i < n; i++) fractionalParts[i] = BigDecimal.ZERO;
        }

        // 4. 잔여주 처리 (소수부 큰 순, 동률은 추첨 / room 없는 신청자는 제외)
        long allocatedSoFar = equalAllocatedSum;
        for (int v : proportionalAllocated) allocatedSoFar += v;
        long leftoverShares = totalAllocatedQuantity - allocatedSoFar;

        int[] additionalAllocated = new int[n];
        if (leftoverShares > 0) {
            List<Integer> ranked = rankByFractionalPartDesc(n, fractionalParts, random);

            boolean progress = true;
            while (leftoverShares > 0 && progress) {
                progress = false;
                for (int idx : ranked) {
                    if (leftoverShares <= 0) break;
                    int requested = applicants.get(idx).requestedQuantity();
                    int allocatedToIdx = equalAllocated[idx] + proportionalAllocated[idx] + additionalAllocated[idx];
                    if (allocatedToIdx >= requested) continue;
                    additionalAllocated[idx] += 1;
                    leftoverShares -= 1;
                    progress = true;
                }
            }
        }

        // 5. 최종 배정
        List<AllocationResult> results = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            AllocationApplicant applicant = applicants.get(i);
            int finalAllocated = equalAllocated[i] + proportionalAllocated[i] + additionalAllocated[i];
            BigDecimal allocationRate = applicant.requestedQuantity() == 0
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(finalAllocated)
                            .divide(BigDecimal.valueOf(applicant.requestedQuantity()), 6, RoundingMode.HALF_UP);
            results.add(new AllocationResult(
                    applicant.customerId(),
                    applicant.requestedQuantity(),
                    equalAllocated[i],
                    proportionalAllocated[i],
                    additionalAllocated[i],
                    finalAllocated,
                    allocationRate
            ));
        }
        return results;
    }

    private static List<Integer> rankByFractionalPartDesc(int n, BigDecimal[] fractionalParts, Random random) {
        List<Integer> ranked = new ArrayList<>(n);
        double[] tieBreak = new double[n];
        for (int i = 0; i < n; i++) {
            ranked.add(i);
            tieBreak[i] = random.nextDouble();
        }
        ranked.sort(
                Comparator.<Integer, BigDecimal>comparing(i -> fractionalParts[i]).reversed()
                        .thenComparing(i -> tieBreak[i], Comparator.reverseOrder()));
        return ranked;
    }

    private static AllocationResult zeroResult(AllocationApplicant a) {
        return new AllocationResult(a.customerId(), a.requestedQuantity(), 0, 0, 0, 0, BigDecimal.ZERO);
    }
}
