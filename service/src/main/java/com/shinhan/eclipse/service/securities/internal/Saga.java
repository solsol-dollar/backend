package com.shinhan.eclipse.service.securities.internal;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

/**
 * Orchestration-based Saga.
 *
 * 각 step()은 액션 실행 후 보상 액션을 스택에 등록한다.
 * 이후 단계에서 예외 발생 시 compensate()가 역순으로 보상을 실행한다.
 *
 * 사용 패턴:
 * <pre>
 *   Saga saga = new Saga("BuyTrade");
 *   try {
 *       saga.step("잔고 차감",
 *           () -> ledgerClient.deductBalance(...),
 *           () -> ledgerClient.addBalance(...)   // 보상
 *       );
 *       saga.step("주문 저장",
 *           () -> repo.save(order),
 *           () -> {}  // @Transactional 롤백이 처리
 *       );
 *       ...
 *   } catch (Exception e) {
 *       saga.compensate(e);
 *       throw e;
 *   }
 * </pre>
 */
@Slf4j
class Saga {

    private final String name;
    private final Deque<CompensationEntry> stack = new ArrayDeque<>();

    Saga(String name) {
        this.name = name;
    }

    void step(String stepName, Runnable action, Runnable compensation) {
        log.debug("[Saga:{}] step '{}' 시작", name, stepName);
        action.run();
        stack.push(new CompensationEntry(stepName, compensation));
        log.debug("[Saga:{}] step '{}' 완료", name, stepName);
    }

    <T> T step(String stepName, Supplier<T> action, Runnable compensation) {
        log.debug("[Saga:{}] step '{}' 시작", name, stepName);
        T result = action.get();
        stack.push(new CompensationEntry(stepName, compensation));
        log.debug("[Saga:{}] step '{}' 완료", name, stepName);
        return result;
    }

    void compensate(Exception cause) {
        if (stack.isEmpty()) return;
        log.warn("[Saga:{}] 보상 시작 — 실패 원인: {}", name, cause.getMessage());
        while (!stack.isEmpty()) {
            CompensationEntry entry = stack.pop();
            try {
                log.info("[Saga:{}] 보상 실행: '{}'", name, entry.stepName);
                entry.compensation.run();
            } catch (Exception ce) {
                log.error("[Saga:{}] 보상 실패 '{}' — 수동 처리 필요: {}",
                        name, entry.stepName, ce.getMessage(), ce);
            }
        }
        log.warn("[Saga:{}] 보상 완료", name);
    }

    private record CompensationEntry(String stepName, Runnable compensation) {}
}
