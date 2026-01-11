package com.example.payment_service.repository;

import com.example.payment_service.domain.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, String> {

    Optional<Payment> findByCustomerId(String customerId);
}
