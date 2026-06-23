package com.shinhan.eclipse.ledger.returnplan.internal;

import com.shinhan.eclipse.domain.returnplan.ReturnPlan;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

interface ReturnPlanRepository extends JpaRepository<ReturnPlan, Long> {
    /** 대시보드 요약처럼 전체 목록이 필요할 때 (페이지네이션으로 우회하지 않도록). */
    List<ReturnPlan> findByUserId(Long userId);

    Optional<ReturnPlan> findByIdAndUserId(Long id, Long userId);

    boolean existsBySubscriptionId(Long subscriptionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ReturnPlan p where p.id = :id and p.userId = :userId")
    Optional<ReturnPlan> findByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);

    /** 환불일 배치(시스템 트리거)용 — 사용자 스코프 없이 락 조회. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ReturnPlan p where p.id = :id")
    Optional<ReturnPlan> findByIdForUpdate(@Param("id") Long id);

    /** 조회 조건(기간/상태) 필터를 DB 단에서 적용한 뒤 페이지네이션한다 — 필터를 페이지네이션 이후에 적용하면 결과 수가 줄어드는 버그를 피한다. */
    @Query("""
            select p from ReturnPlan p
            where p.userId = :userId
              and (:status is null or upper(p.planStatus) = upper(:status))
              and (:fromDateTime is null or coalesce(p.confirmedAt, p.createdAt) >= :fromDateTime)
              and (:toDateTime is null or coalesce(p.confirmedAt, p.createdAt) <= :toDateTime)
            """)
    Page<ReturnPlan> search(@Param("userId") Long userId,
                            @Param("status") String status,
                            @Param("fromDateTime") LocalDateTime fromDateTime,
                            @Param("toDateTime") LocalDateTime toDateTime,
                            Pageable pageable);
}
