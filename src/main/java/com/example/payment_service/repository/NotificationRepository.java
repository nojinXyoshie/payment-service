package com.example.payment_service.repository;

import com.example.payment_service.domain.Notification;
import com.example.payment_service.domain.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, String> {

    boolean existsByPaymentIdAndStatus(String paymentId, NotificationStatus status);
}
