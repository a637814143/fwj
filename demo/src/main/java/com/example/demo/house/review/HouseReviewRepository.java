package com.example.demo.house.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HouseReviewRepository extends JpaRepository<HouseReview, Long> {

    List<HouseReview> findAllByHouse_IdOrderByCreatedAtDesc(Long houseId);
}
