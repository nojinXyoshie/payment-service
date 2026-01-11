package com.example.payment_service.service;

import com.example.payment_service.domain.Payment;
import com.example.payment_service.domain.PaymentStatus;
import com.example.payment_service.dto.CreatePaymentRequest;
import com.example.payment_service.dto.CreatePaymentResponse;
import com.example.payment_service.dto.PaymentCallbackRequest;
import com.example.payment_service.dto.PaymentResponse;
import com.example.payment_service.exception.ResourceNotFoundException;
import com.example.payment_service.repository.PaymentRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;

    public PaymentService(PaymentRepository paymentRepository,
                         NotificationService notificationService) {
        this.paymentRepository = paymentRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public CreatePaymentResponse createPayment(CreatePaymentRequest request) {
        String paymentId = UUID.randomUUID().toString();
        Payment payment = new Payment(paymentId, request.getMerchantId(), request.getCustomerId(),
                request.getAmount(), request.getCurrency(), request.getDescription());
        payment = paymentRepository.save(payment);

        log.info("Payment {} created for merchant {} and customer {}. Waiting for payment callback.", 
                paymentId, request.getMerchantId(), request.getCustomerId());

        return new CreatePaymentResponse(payment.getPaymentId(), payment.getMerchantId(), 
                payment.getCustomerId(), payment.getAmount(), payment.getCurrency(), 
                payment.getDescription(), payment.getStatus());
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment %s not found".formatted(paymentId)));
        return mapToPaymentResponse(payment);
    }

    @Transactional
    public void handlePaymentCallback(PaymentCallbackRequest request) {
        processPaymentUpdate(request.getPaymentId(), request.getAmount(), request.getStatus());
    }

    private PaymentResponse mapToPaymentResponse(Payment payment) {
        return new PaymentResponse(payment.getPaymentId(), payment.getMerchantId(), 
                payment.getCustomerId(), payment.getAmount(), payment.getCurrency(), 
                payment.getDescription(), payment.getStatus(), payment.getCreatedAt(), payment.getUpdatedAt());
    }

    private void processPaymentUpdate(String paymentId, java.math.BigDecimal amount, PaymentStatus status) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment %s not found".formatted(paymentId)));

        if (payment.getAmount().compareTo(amount) != 0) {
            throw new IllegalArgumentException("Payment amount mismatch");
        }

        PaymentStatus currentStatus = payment.getStatus();

        if (status == currentStatus) {
            log.info("Idempotent update ignored for payment {} with status {}", payment.getPaymentId(), status);
            return;
        }

        if (currentStatus == PaymentStatus.SUCCESS) {
            log.warn("Ignoring conflicting update for already successful payment {} with status {}", payment.getPaymentId(), status);
            return;
        }

        switch (status) {
            case SUCCESS -> {
                payment.markSuccess();
                notificationService.notifyPaymentSuccess(payment);
            }
            case FAILED -> payment.markFailed();
            default -> payment.markUnknown();
        }

        log.info("Payment {} transitioned from {} to {}", payment.getPaymentId(), currentStatus, status);
    }
}
