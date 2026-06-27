package com.shinhan.eclipse.worker.allocation.repository;

import com.shinhan.eclipse.domain.holding.HoldingLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkerHoldingLotRepository extends JpaRepository<HoldingLot, Long> {

    boolean existsBySourceTypeAndSourceId(String sourceType, Long sourceId);
}
