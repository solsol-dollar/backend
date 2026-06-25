package com.shinhan.eclipse.ledger.returnplan.internal;

import com.shinhan.eclipse.domain.returnplan.ReturnPlanPreset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface ReturnPlanPresetRepository extends JpaRepository<ReturnPlanPreset, Long> {
    List<ReturnPlanPreset> findAllByOrderByDisplayOrderAsc();

    Optional<ReturnPlanPreset> findByPresetCode(String presetCode);
}
