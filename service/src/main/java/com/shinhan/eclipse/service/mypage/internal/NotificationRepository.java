package com.shinhan.eclipse.service.mypage.internal;

import com.shinhan.eclipse.domain.notification.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdAndStatusAndSentAtIsNotNull(Long userId, String status, Pageable pageable);

    Page<Notification> findByUserIdAndIsReadAndStatusAndSentAtIsNotNull(Long userId, Boolean isRead, String status, Pageable pageable);

    List<Notification> findBySentAtIsNullAndStatus(String status);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.id = :id AND n.userId = :userId")
    int markAsRead(@Param("id") Long id, @Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.sentAt = :sentAt WHERE n.id = :id")
    void updateSentAt(@Param("id") Long id, @Param("sentAt") LocalDateTime sentAt);
}
