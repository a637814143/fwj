package com.example.demo.house;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class SecondHandHouseService {

    private final SecondHandHouseRepository repository;

    public SecondHandHouseService(SecondHandHouseRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<SecondHandHouse> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public SecondHandHouse findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new SecondHandHouseNotFoundException(id));
    }

    public SecondHandHouse create(SecondHandHouse house) {
        house.setId(null);
        return repository.save(house);
    }

    public SecondHandHouse update(Long id, SecondHandHouse updatedHouse) {
        SecondHandHouse existing = findById(id);
        existing.setTitle(updatedHouse.getTitle());
        existing.setAddress(updatedHouse.getAddress());
        existing.setPrice(updatedHouse.getPrice());
        existing.setArea(updatedHouse.getArea());
        existing.setDescription(updatedHouse.getDescription());
        existing.setSellerUsername(updatedHouse.getSellerUsername());
        existing.setSellerName(updatedHouse.getSellerName());
        existing.setContactNumber(updatedHouse.getContactNumber());
        existing.setListingDate(updatedHouse.getListingDate());
        existing.setImageUrls(new ArrayList<>(updatedHouse.getImageUrls()));
        return repository.save(existing);
    }

    public void delete(Long id) {
        SecondHandHouse house = findById(id);
        repository.delete(house);
    }

    @Transactional(readOnly = true)
    public List<SecondHandHouse> findByFilters(
            BigDecimal minPrice,
            BigDecimal maxPrice,
            BigDecimal minArea,
            BigDecimal maxArea,
            LocalDate listedFrom,
            LocalDate listedTo,
            String sellerUsername,
            String keyword
    ) {
        boolean noFilters = minPrice == null && maxPrice == null && minArea == null && maxArea == null
                && listedFrom == null && listedTo == null && (sellerUsername == null || sellerUsername.isBlank())
                && (keyword == null || keyword.isBlank());

        if (noFilters) {
            return repository.findAll();
        }

        Specification<SecondHandHouse> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            if (minArea != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("area"), minArea));
            }

            if (maxArea != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("area"), maxArea));
            }

            if (listedFrom != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("listingDate"), listedFrom));
            }

            if (listedTo != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("listingDate"), listedTo));
            }

            if (sellerUsername != null && !sellerUsername.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("sellerUsername"), sellerUsername.trim()));
            }

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(root.get("title"), pattern),
                        criteriaBuilder.like(root.get("address"), pattern),
                        criteriaBuilder.like(root.get("description"), pattern)
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return repository.findAll(specification);
    }
}
