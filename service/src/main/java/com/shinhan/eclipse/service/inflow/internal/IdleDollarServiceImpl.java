package com.shinhan.eclipse.service.inflow.internal;

import com.shinhan.eclipse.domain.inflow.IdleDollarTrigger;
import com.shinhan.eclipse.service.inflow.IdleDollarService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
class IdleDollarServiceImpl implements IdleDollarService {

    private final IdleDollarTriggerRepository idleDollarTriggerRepository;

    @Override
    public void detectAndNotify(Long userId) {
        // TODO: 유휴 달러 감지 → 억제 조건 확인 → 알림 발송
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public List<IdleDollarTrigger> getTriggerHistory(Long userId) {
        // TODO: 구현
        throw new UnsupportedOperationException("TODO");
    }
}
