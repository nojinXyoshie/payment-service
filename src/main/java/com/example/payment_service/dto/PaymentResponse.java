package com.example.payment_service.dto;

import com.example.payment_service.domain.PaymentStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class PaymentResponse {

    private final String paymentId;
    private final String merchantId;
    private final String customerId;
    private final BigDecimal amount;
    private final String currency;
    private final String description;
    private final PaymentStatus status;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public PaymentResponse(String paymentId, String merchantId, String customerId, BigDecimal amount, String currency, String description, PaymentStatus status, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.paymentId = paymentId;
        this.merchantId = merchantId;
        this.customerId = customerId;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getDescription() {
        return description;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
