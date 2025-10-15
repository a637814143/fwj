package com.example.demo.house;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        return repository.save(existing);
    }

    public void delete(Long id) {
        SecondHandHouse house = findById(id);
        repository.delete(house);
    }
}
