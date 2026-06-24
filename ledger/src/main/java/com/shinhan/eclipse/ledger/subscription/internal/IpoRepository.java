package com.shinhan.eclipse.ledger.subscription.internal;

import com.shinhan.eclipse.domain.ipo.Ipo;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

interface IpoRepository extends JpaRepository<Ipo, Long> {
    /**
     * 청약 시작일이 오늘 이후(오늘 포함)인 IPO 중 가장 빠른 것. 저장된 ipo_status 컬럼은 생성 후
     * 갱신되지 않아 신뢰할 수 없으므로, service 모듈의 findWithFilters와 동일하게 날짜로 직접 계산한다.
     */
    @Query("select i from Ipo i where i.subscriptionStartDate >= :today order by i.subscriptionStartDate asc")
    List<Ipo> findUpcomingOrderByStartDate(@Param("today") LocalDate today, Pageable pageable);
}
