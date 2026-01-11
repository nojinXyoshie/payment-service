package com.example.payment_service.dto;

import com.example.payment_service.domain.PaymentStatus;
import java.math.BigDecimal;

public class CreatePaymentResponse {

    private final String paymentId;
    private final String merchantId;
    private final String customerId;
    private final BigDecimal amount;
    private final String currency;
    private final String description;
    private final PaymentStatus status;

    public CreatePaymentResponse(String paymentId, String merchantId, String customerId, BigDecimal amount, String currency, String description, PaymentStatus status) {
        this.paymentId = paymentId;
        this.merchantId = merchantId;
        this.customerId = customerId;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.status = status;
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
}
