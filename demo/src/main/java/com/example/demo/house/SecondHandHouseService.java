package com.example.demo.house;

import com.example.demo.auth.UserAccount;
import com.example.demo.auth.UserAccountRepository;
import com.example.demo.auth.UserRole;
import com.example.demo.order.HouseOrderRepository;
import com.example.demo.order.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class SecondHandHouseService {

    private static final Logger log = LoggerFactory.getLogger(SecondHandHouseService.class);

    private final SecondHandHouseRepository repository;
    private final UserAccountRepository userAccountRepository;
    private final HouseOrderRepository houseOrderRepository;

    public SecondHandHouseService(SecondHandHouseRepository repository,
                                  UserAccountRepository userAccountRepository,
                                  HouseOrderRepository houseOrderRepository) {
        this.repository = repository;
        this.userAccountRepository = userAccountRepository;
        this.houseOrderRepository = houseOrderRepository;
    }

    @Transactional(readOnly = true)
    public List<SecondHandHouse> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<SecondHandHouse> search(String keyword,
                                        BigDecimal minPrice,
                                        BigDecimal maxPrice,
                                        BigDecimal minArea,
                                        BigDecimal maxArea) {
        String normalized = keyword == null ? null : keyword.trim().toLowerCase(Locale.ROOT);
        return repository.findAll().stream()
                .filter(house -> filterByKeyword(house, normalized))
                .filter(house -> filterByRange(house.getPrice(), minPrice, maxPrice))
                .filter(house -> filterByRange(house.getArea(), minArea, maxArea))
                .toList();
    }

    @Transactional(readOnly = true)
    public SecondHandHouse findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new SecondHandHouseNotFoundException(id));
    }

    public SecondHandHouse create(SecondHandHouse house) {
        validateSellerAccount(house.getSellerUsername());
        house.setId(null);
        return repository.save(house);
    }

    public SecondHandHouse update(Long id, SecondHandHouse updatedHouse) {
        SecondHandHouse existing = findById(id);
        validateSellerAccount(updatedHouse.getSellerUsername());
        existing.setTitle(updatedHouse.getTitle());
        existing.setAddress(updatedHouse.getAddress());
        existing.setPrice(updatedHouse.getPrice());
        existing.setArea(updatedHouse.getArea());
        existing.setDescription(updatedHouse.getDescription());
        existing.setSellerUsername(updatedHouse.getSellerUsername());
        existing.setSellerName(updatedHouse.getSellerName());
        existing.setContactNumber(updatedHouse.getContactNumber());
        existing.setListingDate(updatedHouse.getListingDate());
        existing.getImageUrls().clear();
        existing.getImageUrls().addAll(updatedHouse.getImageUrls());
        return repository.save(existing);
    }

    public void delete(Long id, String requesterUsername) {
        if (requesterUsername == null || requesterUsername.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "删除房源时必须提供操作用户。");
        }
        SecondHandHouse house = findById(id);
        UserAccount requester = userAccountRepository.findByUsername(requesterUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "操作用户不存在"));

        if (requester.getRole() == UserRole.ADMIN) {
            repository.delete(house);
            return;
        }

        if (requester.getRole() == UserRole.SELLER && requester.getUsername().equals(house.getSellerUsername())) {
            repository.delete(house);
            return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "您无权删除该房源");
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void removeStaleListings() {
        int removed = cleanupStaleListings();
        if (removed > 0) {
            log.info("自动清理 {} 套超过一个月无人购买的房源", removed);
        }
    }

    public int cleanupStaleListings() {
        LocalDate cutoffDate = LocalDate.now().minusMonths(1);
        List<SecondHandHouse> candidates = repository.findByListingDateBefore(cutoffDate);
        List<SecondHandHouse> toRemove = candidates.stream()
                .filter(house -> !houseOrderRepository.existsByHouse_IdAndStatus(house.getId(), OrderStatus.PAID))
                .toList();
        if (!toRemove.isEmpty()) {
            repository.deleteAll(toRemove);
        }
        return toRemove.size();
    }

    private boolean filterByKeyword(SecondHandHouse house, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String haystack = (house.getTitle() + " " + house.getAddress() + " " + (house.getDescription() == null ? "" : house.getDescription()))
                .toLowerCase(Locale.ROOT);
        return haystack.contains(keyword);
    }

    private boolean filterByRange(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value == null) {
            return false;
        }
        boolean greaterThanMin = min == null || value.compareTo(min) >= 0;
        boolean lessThanMax = max == null || value.compareTo(max) <= 0;
        return greaterThanMin && lessThanMax;
    }

    private void validateSellerAccount(String sellerUsername) {
        if (sellerUsername == null || sellerUsername.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "卖家账号不能为空");
        }
        UserAccount seller = userAccountRepository.findByUsername(sellerUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "卖家账号不存在"));
        if (seller.getRole() != UserRole.SELLER && seller.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "指定账号不是合法的卖家角色");
        }
        if (seller.isBlacklisted()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "该卖家已被加入黑名单，无法发布房源");
        }
    }
}
