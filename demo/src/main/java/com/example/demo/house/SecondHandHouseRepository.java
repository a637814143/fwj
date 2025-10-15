package com.example.demo.house;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SecondHandHouseRepository extends JpaRepository<SecondHandHouse, Long> {

    List<SecondHandHouse> findByListingDateBefore(LocalDate listingDate);
}
