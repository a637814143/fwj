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
import org.springframework.web.bind.annotation.PatchMapping;

import java.util.List;

@RestController
@RequestMapping("/api/houses")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class SecondHandHouseController {

    private final SecondHandHouseService service;
 
    public SecondHandHouseController(SecondHandHouseService service) {
        this.service = service;
    }

    // 搜索房源列表，支持关键词、价格、面积等筛选
    @GetMapping
    public List<SecondHandHouseView> list(@RequestParam(value = "keyword", required = false) String keyword,
                                          @RequestParam(value = "minPrice", required = false) java.math.BigDecimal minPrice,
                                          @RequestParam(value = "maxPrice", required = false) java.math.BigDecimal maxPrice,
                                          @RequestParam(value = "minArea", required = false) java.math.BigDecimal minArea,
                                          @RequestParam(value = "maxArea", required = false) java.math.BigDecimal maxArea,
                                          @RequestParam(value = "requester", required = false) String requesterUsername) {
        return service.search(keyword, minPrice, maxPrice, minArea, maxArea, requesterUsername);
    }

    // 按 ID 查看房源详情，可附带请求人信息控制敏感字段
    @GetMapping("/{id}")
    public SecondHandHouseView get(@PathVariable Long id,
                                   @RequestParam(value = "requester", required = false) String requesterUsername) {
        return service.viewById(id, requesterUsername);
    }

    // 创建新的房源记录
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SecondHandHouseView create(@Valid @RequestBody SecondHandHouseRequest request) {
        SecondHandHouse house = service.create(request.toEntity());
        return SecondHandHouseView.fromEntity(house, false);
    }

    // 更新指定房源的信息
    @PutMapping("/{id}")
    public SecondHandHouseView update(@PathVariable Long id, @Valid @RequestBody SecondHandHouseRequest request) {
        SecondHandHouse house = service.update(id, request.toEntity());
        return SecondHandHouseView.fromEntity(house, false);
    }

    // 删除房源，需校验请求人身份
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @RequestParam("requester") String requesterUsername) {
        service.delete(id, requesterUsername);
    }

    // 审核房源，上架或驳回时保存审核意见
    @PatchMapping("/{id}/review")
    public SecondHandHouseView review(@PathVariable Long id, @Valid @RequestBody SecondHandHouseReviewRequest request) {
        return service.review(id, request.status(), request.message(), request.reviewerUsername());
    }
}
