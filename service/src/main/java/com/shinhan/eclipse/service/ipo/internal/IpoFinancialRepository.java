package com.shinhan.eclipse.service.ipo.internal;

import com.shinhan.eclipse.domain.ipo.IpoFinancial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface IpoFinancialRepository extends JpaRepository<IpoFinancial, Long> {
    List<IpoFinancial> findByIpoIdOrderByFiscalYearDesc(Long ipoId);
}
