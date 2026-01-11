package com.example.payment_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String paymentId;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false)
    private String channel;

    @Column(nullable = false, length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    public Notification() {
    }

    public Notification(String paymentId, String customerId, String channel, String message, NotificationStatus status) {
        this.paymentId = paymentId;
        this.customerId = customerId;
        this.channel = channel;
        this.message = message;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getChannel() {
        return channel;
    }

    public String getMessage() {
        return message;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void markSent() {
        this.status = NotificationStatus.SENT;
    }

    public void markFailed() {
        this.status = NotificationStatus.FAILED;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }
}
