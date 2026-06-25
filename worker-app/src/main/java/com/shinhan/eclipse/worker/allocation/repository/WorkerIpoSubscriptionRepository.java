package com.shinhan.eclipse.worker.allocation.repository;

import com.shinhan.eclipse.domain.subscription.IpoSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkerIpoSubscriptionRepository extends JpaRepository<IpoSubscription, Long> {

    /** 상장일에 배정 대상이 되는, 확정됐지만 아직 배정 결과가 없는 청약. */
    List<IpoSubscription> findByIpoIdAndSubscriptionStatusAndResultStatusIsNull(Long ipoId, String subscriptionStatus);
}
