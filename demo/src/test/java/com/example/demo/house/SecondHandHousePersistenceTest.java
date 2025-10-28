package com.example.demo.house;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class SecondHandHousePersistenceTest {

    @Autowired
    private SecondHandHouseRepository repository;

    @Test
    void savesCertificateUrlWhenPersistingHouse() {
        SecondHandHouse house = new SecondHandHouse();
        house.setTitle("测试房源");
        house.setAddress("测试城市大道1号");
        house.setPrice(new BigDecimal("1000000"));
        house.setDownPayment(new BigDecimal("200000"));
        house.setInstallmentMonthlyPayment(new BigDecimal("12000"));
        house.setInstallmentMonths(60);
        house.setArea(new BigDecimal("120"));
        house.setSellerUsername("seller01");
        house.setSellerName("张三");
        house.setContactNumber("13800138000");
        house.setListingDate(LocalDate.now());
        house.setPropertyCertificateUrl("/api/houses/images/certificates/test-cert.jpg");

        SecondHandHouse saved = repository.saveAndFlush(house);
        assertThat(saved.getPropertyCertificateUrl()).isEqualTo("/api/houses/images/certificates/test-cert.jpg");

        SecondHandHouse reloaded = repository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getPropertyCertificateUrl()).isEqualTo("/api/houses/images/certificates/test-cert.jpg");
    }
}
