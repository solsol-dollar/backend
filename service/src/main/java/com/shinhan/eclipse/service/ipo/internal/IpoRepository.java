package com.shinhan.eclipse.service.ipo.internal;

import com.shinhan.eclipse.domain.ipo.Ipo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface IpoRepository extends JpaRepository<Ipo, Long> {
    List<Ipo> findByIpoStatusOrderBySubscriptionStartDateAsc(String ipoStatus);
}
