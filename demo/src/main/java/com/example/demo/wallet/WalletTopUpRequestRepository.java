package com.example.demo.wallet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WalletTopUpRequestRepository extends JpaRepository<WalletTopUpRequest, Long> {

    List<WalletTopUpRequest> findByStatusOrderByCreatedAtAsc(TopUpStatus status);
}
