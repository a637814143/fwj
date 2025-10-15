package com.example.demo.house;

import com.example.demo.house.review.HouseReviewRequest;
import com.example.demo.house.review.HouseReviewResponse;
import com.example.demo.house.review.HouseReviewService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/houses")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class SecondHandHouseController {

    private final SecondHandHouseService service;
    private final HouseReviewService reviewService;

    public SecondHandHouseController(SecondHandHouseService service, HouseReviewService reviewService) {
        this.service = service;
        this.reviewService = reviewService;
    }

    @GetMapping
    public List<SecondHandHouse> list(
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) BigDecimal minArea,
            @RequestParam(required = false) BigDecimal maxArea,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate listedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate listedTo,
            @RequestParam(required = false) String sellerUsername,
            @RequestParam(required = false) String keyword
    ) {
        return service.findByFilters(minPrice, maxPrice, minArea, maxArea, listedFrom, listedTo, sellerUsername, keyword);
    }

    @GetMapping("/{id}")
    public SecondHandHouse get(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SecondHandHouse create(@Valid @RequestBody SecondHandHouseRequest request) {
        return service.create(request.toEntity());
    }

    @PutMapping("/{id}")
    public SecondHandHouse update(@PathVariable Long id, @Valid @RequestBody SecondHandHouseRequest request) {
        return service.update(id, request.toEntity());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/{id}/reviews")
    public List<HouseReviewResponse> listReviews(@PathVariable Long id) {
        return reviewService.listByHouseId(id);
    }

    @PostMapping("/{id}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public HouseReviewResponse createReview(@PathVariable Long id, @Valid @RequestBody HouseReviewRequest request) {
        return reviewService.createReview(id, request);
    }
}
