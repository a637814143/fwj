package com.example.demo.house;

public class SecondHandHouseNotFoundException extends RuntimeException {
    public SecondHandHouseNotFoundException(Long id) {
        super("Second-hand house with id " + id + " not found");
    }
}
