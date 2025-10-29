package com.example.demo.house;

import com.example.demo.house.SecondHandHouse;
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
import org.springframework.web.bind.annotation.PatchMapping;

import java.util.List;

@RestController
@RequestMapping("/api/houses")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class SecondHandHouseController {

    private final SecondHandHouseService service;
    private final GaodeMapSettings mapSettings;

    public SecondHandHouseController(SecondHandHouseService service, GaodeMapSettings mapSettings) {
        this.service = service;
        this.mapSettings = mapSettings;
    }

    @GetMapping
    public List<SecondHandHouseView> list(@RequestParam(value = "keyword", required = false) String keyword,
                                          @RequestParam(value = "minPrice", required = false) java.math.BigDecimal minPrice,
                                          @RequestParam(value = "maxPrice", required = false) java.math.BigDecimal maxPrice,
                                          @RequestParam(value = "minArea", required = false) java.math.BigDecimal minArea,
                                          @RequestParam(value = "maxArea", required = false) java.math.BigDecimal maxArea,
                                          @RequestParam(value = "requester", required = false) String requesterUsername) {
        return service.search(keyword, minPrice, maxPrice, minArea, maxArea, requesterUsername);
    }

    @GetMapping("/locations")
    public List<HouseLocationView> locations(@RequestParam(value = "centerLat", required = false) Double centerLat,
                                             @RequestParam(value = "centerLng", required = false) Double centerLng,
                                             @RequestParam(value = "radiusKm", required = false) Double radiusKm,
                                             @RequestParam(value = "requester", required = false) String requesterUsername) {
        return service.listLocations(centerLat, centerLng, radiusKm, requesterUsername);
    }

    @GetMapping("/map-config")
    public MapConfigResponse mapConfig() {
        return new MapConfigResponse(mapSettings.apiKey(), mapSettings.jsSecurityCode().orElse(null));
    }

    @GetMapping("/{id}")
    public SecondHandHouseView get(@PathVariable Long id,
                                   @RequestParam(value = "requester", required = false) String requesterUsername) {
        return service.viewById(id, requesterUsername);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SecondHandHouseView create(@Valid @RequestBody SecondHandHouseRequest request) {
        SecondHandHouse house = service.create(request.toEntity());
        return SecondHandHouseView.fromEntity(house, false);
    }

    @PutMapping("/{id}")
    public SecondHandHouseView update(@PathVariable Long id, @Valid @RequestBody SecondHandHouseRequest request) {
        SecondHandHouse house = service.update(id, request.toEntity());
        return SecondHandHouseView.fromEntity(house, false);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @RequestParam("requester") String requesterUsername) {
        service.delete(id, requesterUsername);
    }

    @PatchMapping("/{id}/review")
    public SecondHandHouseView review(@PathVariable Long id, @Valid @RequestBody SecondHandHouseReviewRequest request) {
        return service.review(id, request.status(), request.message(), request.reviewerUsername());
    }
}

record MapConfigResponse(String apiKey, String jsSecurityCode) {
}
