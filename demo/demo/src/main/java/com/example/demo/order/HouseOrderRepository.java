package com.example.demo.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HouseOrderRepository extends JpaRepository<HouseOrder, Long> {

    List<HouseOrder> findByBuyer_UsernameOrSeller_UsernameOrderByCreatedAtDesc(String buyerUsername, String sellerUsername);
}
