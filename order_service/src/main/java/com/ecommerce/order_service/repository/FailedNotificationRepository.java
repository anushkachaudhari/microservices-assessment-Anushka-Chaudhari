package com.ecommerce.order_service.repository;

import com.ecommerce.order_service.model.FailedNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FailedNotificationRepository extends JpaRepository<FailedNotification, UUID> {
    List<FailedNotification> findByTenantId(String tenantId);
}
