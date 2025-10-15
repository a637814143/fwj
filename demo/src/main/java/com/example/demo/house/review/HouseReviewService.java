package com.example.demo.house.review;

import com.example.demo.house.SecondHandHouse;
import com.example.demo.house.SecondHandHouseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class HouseReviewService {

    private final HouseReviewRepository repository;
    private final SecondHandHouseService houseService;

    public HouseReviewService(HouseReviewRepository repository, SecondHandHouseService houseService) {
        this.repository = repository;
        this.houseService = houseService;
    }

    @Transactional(readOnly = true)
    public List<HouseReviewResponse> listByHouseId(Long houseId) {
        houseService.findById(houseId);
        return repository.findAllByHouse_IdOrderByCreatedAtDesc(houseId)
                .stream()
                .map(HouseReviewResponse::fromEntity)
                .toList();
    }

    public HouseReviewResponse createReview(Long houseId, HouseReviewRequest request) {
        SecondHandHouse house = houseService.findById(houseId);

        HouseReview review = new HouseReview();
        review.setHouse(house);
        review.setBuyerUsername(request.buyerUsername().trim());
        review.setBuyerDisplayName(request.buyerDisplayName().trim());
        review.setRating(request.rating());
        review.setComment(request.comment() == null ? null : request.comment().trim());

        HouseReview saved = repository.save(review);
        return HouseReviewResponse.fromEntity(saved);
    }
}
