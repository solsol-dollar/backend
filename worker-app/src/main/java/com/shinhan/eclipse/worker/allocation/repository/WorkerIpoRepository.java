package com.shinhan.eclipse.worker.allocation.repository;

import com.shinhan.eclipse.domain.ipo.Ipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WorkerIpoRepository extends JpaRepository<Ipo, Long> {

    List<Ipo> findByListingDate(LocalDate listingDate);

    List<Ipo> findByRefundDateLessThanAndIpoStatusNot(LocalDate refundDate, String ipoStatus);
}
