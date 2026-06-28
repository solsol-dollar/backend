package com.shinhan.eclipse.worker.ipo.repository;

import com.shinhan.eclipse.domain.ipo.IpoFinancial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IpoFinancialRepository extends JpaRepository<IpoFinancial, Long> {
    boolean existsByIpoIdAndFiscalYear(Long ipoId, Integer fiscalYear);
}
