package com.ecommerce.order_service.repository;

import com.ecommerce.order_service.model.Order;
import com.ecommerce.order_service.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByTenantId(String tenantId);

    Optional<Order> findByIdAndTenantId(UUID id, String tenantId);

    List<Order> findByTenantIdAndStatus(String tenantId, OrderStatus status);
}