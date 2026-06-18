package com.shinhan.eclipse.service.securities.internal;

import com.shinhan.eclipse.domain.holding.HoldingLot;
import org.springframework.data.jpa.repository.JpaRepository;

interface HoldingLotRepository extends JpaRepository<HoldingLot, Long> {
}
