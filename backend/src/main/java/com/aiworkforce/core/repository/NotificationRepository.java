package com.aiworkforce.core.repository;

import com.aiworkforce.core.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId);
    List<Notification> findByRecipientIdAndIsReadOrderByCreatedAtDesc(UUID recipientId, Boolean isRead);
    long countByRecipientIdAndIsRead(UUID recipientId, Boolean isRead);
}
