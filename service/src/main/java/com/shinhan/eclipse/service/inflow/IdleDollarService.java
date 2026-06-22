package com.shinhan.eclipse.service.inflow;

import com.shinhan.eclipse.domain.inflow.IdleDollarTrigger;
import java.util.List;

public interface IdleDollarService {
    void detectAll();
    void detectAndNotify(Long userId);
    List<IdleDollarTrigger> getTriggerHistory(Long userId);
}
