package com.example.demo.house;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SecondHandHouseRepository extends JpaRepository<SecondHandHouse, Long> {
}
