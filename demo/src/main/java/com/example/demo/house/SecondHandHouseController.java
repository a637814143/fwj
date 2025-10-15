package com.example.demo.house;

import jakarta.validation.Valid;
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

import java.util.List;

@RestController
@RequestMapping("/api/houses")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class SecondHandHouseController {

    private final SecondHandHouseService service;

    public SecondHandHouseController(SecondHandHouseService service) {
        this.service = service;
    }

    @GetMapping
    public List<SecondHandHouse> list(@RequestParam(value = "keyword", required = false) String keyword,
                                      @RequestParam(value = "minPrice", required = false) java.math.BigDecimal minPrice,
                                      @RequestParam(value = "maxPrice", required = false) java.math.BigDecimal maxPrice,
                                      @RequestParam(value = "minArea", required = false) java.math.BigDecimal minArea,
                                      @RequestParam(value = "maxArea", required = false) java.math.BigDecimal maxArea) {
        return service.search(keyword, minPrice, maxPrice, minArea, maxArea);
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
    public void delete(@PathVariable Long id, @RequestParam("requester") String requesterUsername) {
        service.delete(id, requesterUsername);
    }
}
