package com.shinhan.eclipse.worker.settlement.repository;

import com.shinhan.eclipse.domain.returnplan.ReturnPlan;
import com.shinhan.eclipse.domain.subscription.IpoSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WorkerReturnPlanRepository extends JpaRepository<ReturnPlan, Long> {

    /**
     * 환불일이 오늘이거나 지난(배치 실패 등으로 놓친 건 포함) DRAFT 플랜을 한 번의 쿼리로 찾는다.
     * ReturnPlan ↔ IpoSubscription ↔ Ipo가 매핑된 연관관계가 아니라 서브쿼리로 연결한다.
     */
    @Query("""
            select p from ReturnPlan p
            where p.planStatus = 'DRAFT'
              and p.subscriptionId in (
                  select s.id from IpoSubscription s
                  where s.ipoId in (
                      select i.id from Ipo i where i.refundDate <= :today
                  )
              )
            """)
    List<ReturnPlan> findDraftPlansDueForSettlement(@Param("today") LocalDate today);

    @Query("""
            select s.id, i.companyName
            from IpoSubscription s join Ipo i on s.ipoId = i.id
            where s.id in :subscriptionIds
            """)
    List<Object[]> findCompanyNamesBySubscriptionIds(@Param("subscriptionIds") List<Long> subscriptionIds);

    /**
     * 환불일이 오늘이거나 지난 COMPLETED 청약 중 리턴 플랜이 없는 것을 찾는다.
     * 기한 내에 리턴 플랜을 생성하지 않은 사용자를 위한 자동 생성 대상.
     */
    @Query("""
            select s from IpoSubscription s
            where s.resultStatus = 'COMPLETED'
              and s.refundAmount > 0
              and s.ipoId in (select i.id from Ipo i where i.refundDate <= :today)
              and s.id not in (select p.subscriptionId from ReturnPlan p)
            """)
    List<IpoSubscription> findSubscriptionsNeedingAutoReturnPlan(@Param("today") LocalDate today);
}
