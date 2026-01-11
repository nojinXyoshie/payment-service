package com.example.payment_service.service;

import com.example.payment_service.domain.Notification;
import com.example.payment_service.domain.NotificationStatus;
import com.example.payment_service.domain.Payment;
import com.example.payment_service.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void notifyPaymentSuccess(Payment payment) {
        if (notificationRepository.existsByPaymentIdAndStatus(payment.getPaymentId(), NotificationStatus.SENT)) {
            log.info("Notification already sent for payment {}", payment.getPaymentId());
            return;
        }

        Notification notification = new Notification(payment.getPaymentId(), payment.getCustomerId(), "EMAIL",
                "Pembayaran untuk payment %s berhasil".formatted(payment.getPaymentId()), NotificationStatus.PENDING);
        notification.markSent();
        notificationRepository.save(notification);
        log.info("Notification {} created for payment {} with status {}", notification.getId(), payment.getPaymentId(), notification.getStatus());
    }
}
