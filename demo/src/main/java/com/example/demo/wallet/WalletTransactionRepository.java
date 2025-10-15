package com.example.demo.wallet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    List<WalletTransaction> findTop10ByWalletOrderByCreatedAtDesc(UserWallet wallet);

    void deleteByWallet(UserWallet wallet);
}
