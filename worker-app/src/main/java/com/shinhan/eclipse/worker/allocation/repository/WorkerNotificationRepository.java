package com.shinhan.eclipse.worker.allocation.repository;

import com.shinhan.eclipse.domain.notification.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkerNotificationRepository extends JpaRepository<Notification, Long> {}
