package com.ecommerce.notification_service.repository;

import com.ecommerce.notification_service.model.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationLog, UUID> {
    List<NotificationLog> findByTenantId(String tenantId);
}