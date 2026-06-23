package com.shinhan.eclipse.worker.settlement.repository;

import com.shinhan.eclipse.domain.returnplan.ReturnPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkerReturnPlanRepository extends JpaRepository<ReturnPlan, Long> {

    List<ReturnPlan> findByPlanStatus(String planStatus);
}
