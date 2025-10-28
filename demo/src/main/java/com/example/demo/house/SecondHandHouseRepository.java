package com.example.demo.house;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface SecondHandHouseRepository extends JpaRepository<SecondHandHouse, Long> {

    List<SecondHandHouse> findByListingDateBefore(LocalDate listingDate);

    List<SecondHandHouse> findBySellerUsername(String sellerUsername);

    List<SecondHandHouse> findBySellerUsernameIgnoreCase(String sellerUsername);

    List<SecondHandHouse> findByStatusAndLatitudeIsNotNullAndLongitudeIsNotNull(ListingStatus status);

    List<SecondHandHouse> findByStatusAndUpdatedAtBefore(ListingStatus status, OffsetDateTime updatedAt);
}
