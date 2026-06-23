package com.shinhan.eclipse.worker.ipo.repository;

import com.shinhan.eclipse.domain.ipo.Ipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkerIpoRepository extends JpaRepository<Ipo, Long> {

    List<Ipo> findByStatus(String status);
}
