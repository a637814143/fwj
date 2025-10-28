package com.example.demo.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HouseOrderRepository extends JpaRepository<HouseOrder, Long> {

    List<HouseOrder> findByBuyer_UsernameOrSeller_UsernameOrderByCreatedAtDesc(String buyerUsername, String sellerUsername);

    boolean existsByHouse_IdAndStatus(Long houseId, OrderStatus status);

    Optional<HouseOrder> findFirstByHouse_IdAndStatusOrderByCreatedAtDesc(Long houseId, OrderStatus status);

    List<HouseOrder> findByHouse_IdAndStatus(Long houseId, OrderStatus status);

    List<HouseOrder> findByStatusAndAdminReviewedFalseOrderByCreatedAtAsc(OrderStatus status);

    List<HouseOrder> findByStatusInAndAdminReviewedFalseOrderByCreatedAtAsc(List<OrderStatus> statuses);
}
