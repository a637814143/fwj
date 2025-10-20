package com.example.demo.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsername(String username);

    boolean existsByUsername(String username);

    List<UserAccount> findByRoleOrderByReputationScoreDesc(UserRole role);

    List<UserAccount> findByRoleInOrderByReputationScoreDesc(Collection<UserRole> roles);

    List<UserAccount> findTop5ByRoleAndBlacklistedFalseOrderByReputationScoreDesc(UserRole role);

    List<UserAccount> findTop5ByRoleInAndBlacklistedFalseOrderByReputationScoreDesc(Collection<UserRole> roles);

    List<UserAccount> findByRole(UserRole role);

    long countByBlacklistedTrue();
}
