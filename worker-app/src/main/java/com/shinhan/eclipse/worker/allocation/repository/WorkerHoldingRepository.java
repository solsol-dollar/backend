package com.shinhan.eclipse.worker.allocation.repository;

import com.shinhan.eclipse.domain.holding.Holding;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkerHoldingRepository extends JpaRepository<Holding, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM Holding h WHERE h.userId = :userId AND h.productId = :productId")
    Optional<Holding> findByUserIdAndProductIdForUpdate(Long userId, Long productId);
}
