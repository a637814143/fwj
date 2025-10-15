package com.example.demo.wallet;

import com.example.demo.auth.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserWalletRepository extends JpaRepository<UserWallet, Long> {

    Optional<UserWallet> findByUserAccount(UserAccount userAccount);

    boolean existsByVirtualPort(String virtualPort);
}
