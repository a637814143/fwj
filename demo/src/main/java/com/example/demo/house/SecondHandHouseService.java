package com.example.demo.house;

import com.example.demo.auth.UserAccount;
import com.example.demo.auth.UserAccountRepository;
import com.example.demo.auth.UserRole;
import com.example.demo.order.HouseOrder;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

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
    public List<SecondHandHouseView> search(String keyword,
                                            BigDecimal minPrice,
                                            BigDecimal maxPrice,
                                            BigDecimal minArea,
                                            BigDecimal maxArea,
                                            String requesterUsername) {
        UserAccount requester = resolveRequester(requesterUsername);
        String normalized = keyword == null ? null : keyword.trim().toLowerCase(Locale.ROOT);
        return repository.findAll().stream()
                .filter(house -> isVisibleToRequester(house, requester))
                .filter(house -> filterByKeyword(house, normalized))
                .filter(house -> filterByRange(house.getPrice(), minPrice, maxPrice))
                .filter(house -> filterByRange(house.getArea(), minArea, maxArea))
                .map(house -> buildViewForRequester(house, requester))
                .toList();
    }

    @Transactional(readOnly = true)
    public SecondHandHouseView viewById(Long id, String requesterUsername) {
        SecondHandHouse house = findById(id);
        UserAccount requester = resolveRequester(requesterUsername);
        if (!isVisibleToRequester(house, requester)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "房源不存在或尚未通过审核");
        }
        return buildViewForRequester(house, requester);
    }

    public SecondHandHouse findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new SecondHandHouseNotFoundException(id));
    }

    public SecondHandHouse create(SecondHandHouse house) {
        validateSellerAccount(house.getSellerUsername());
        ListingStatus targetStatus = resolveTargetStatus(house.getStatus());
        if (targetStatus != ListingStatus.DRAFT) {
            ensureNotDuplicate(house, null);
        }
        ensureInstallmentPlan(house);
        house.setId(null);
        house.setStatus(targetStatus);
        house.setReviewedAt(null);
        house.setReviewedBy(null);
        house.setReviewMessage(null);
        return repository.save(house);
    }

    public SecondHandHouse update(Long id, SecondHandHouse updatedHouse) {
        SecondHandHouse existing = findById(id);
        validateSellerAccount(updatedHouse.getSellerUsername());
        ListingStatus targetStatus = resolveTargetStatus(updatedHouse.getStatus());
        if (targetStatus != ListingStatus.DRAFT) {
            ensureNotDuplicate(updatedHouse, id);
        }
        ensureInstallmentPlan(updatedHouse);
        existing.setTitle(updatedHouse.getTitle());
        existing.setAddress(updatedHouse.getAddress());
        existing.setPrice(updatedHouse.getPrice());
        existing.setDownPayment(updatedHouse.getDownPayment());
        existing.setInstallmentMonthlyPayment(updatedHouse.getInstallmentMonthlyPayment());
        existing.setInstallmentMonths(updatedHouse.getInstallmentMonths());
        existing.setArea(updatedHouse.getArea());
        existing.setDescription(updatedHouse.getDescription());
        existing.setLatitude(updatedHouse.getLatitude());
        existing.setLongitude(updatedHouse.getLongitude());
        existing.setSellerUsername(updatedHouse.getSellerUsername());
        existing.setSellerName(updatedHouse.getSellerName());
        existing.setContactNumber(updatedHouse.getContactNumber());
        existing.setListingDate(updatedHouse.getListingDate());
        existing.setFloor(updatedHouse.getFloor());
        existing.setKeywords(updatedHouse.getKeywords());
        existing.setImageUrls(updatedHouse.getImageUrls());
        existing.setStatus(targetStatus);
        existing.setReviewedAt(null);
        existing.setReviewedBy(null);
        existing.setReviewMessage(null);
        return repository.save(existing);
    }

    public void delete(Long id, String requesterUsername) {
        if (requesterUsername == null || requesterUsername.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "删除房源时必须提供操作用户。");
        }
        SecondHandHouse house = findById(id);
        UserAccount requester = userAccountRepository.findByUsernameIgnoreCase(requesterUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "操作用户不存在"));

        List<HouseOrder> relatedOrders = house.getId() == null
                ? List.of()
                : houseOrderRepository.findByHouse_Id(house.getId());

        if (!relatedOrders.isEmpty() && requester.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "房源存在正在处理的订单，请联系管理员删除或完成相关流程。");
        }

        if (requester.getRole() == UserRole.ADMIN) {
            if (!relatedOrders.isEmpty()) {
                houseOrderRepository.deleteAll(relatedOrders);
                log.info("管理员 {} 删除房源 {} 时移除了 {} 条关联订单", requester.getUsername(), house.getId(), relatedOrders.size());
            }
            repository.delete(house);
            return;
        }

        if (requester.getRole() != null && requester.getRole().isSellerRole()
                && requester.getUsername().equalsIgnoreCase(house.getSellerUsername())) {
            if (!relatedOrders.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "房源存在正在处理的订单，无法删除。");
            }
            repository.delete(house);
            return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "您无权删除该房源");
    }

    public SecondHandHouseView review(Long id, ListingStatus status, String reviewMessage, String reviewerUsername) {
        if (status == null || status == ListingStatus.PENDING_REVIEW) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "审核状态无效");
        }
        UserAccount reviewer = requireAdmin(reviewerUsername);
        SecondHandHouse house = findById(id);
        if (status == ListingStatus.REJECTED) {
            if (reviewMessage == null || reviewMessage.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写驳回原因");
            }
            house.setReviewMessage(reviewMessage.trim());
        } else {
            house.setReviewMessage(reviewMessage == null || reviewMessage.isBlank() ? "审核通过" : reviewMessage.trim());
        }
        house.setStatus(status);
        house.setReviewedBy(reviewer.getUsername());
        house.setReviewedAt(OffsetDateTime.now());
        SecondHandHouse saved = repository.save(house);
        return SecondHandHouseView.fromEntity(saved, false);
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void removeStaleListings() {
        int staleRemoved = cleanupStaleListings();
        int delistedRemoved = cleanupDelistedListings();
        if (staleRemoved > 0 || delistedRemoved > 0) {
            log.info("自动清理 {} 套超过一个月无人购买的房源，移除 {} 套已下架房源", staleRemoved, delistedRemoved);
        }
    }

    public int cleanupStaleListings() {
        LocalDate cutoffDate = LocalDate.now().minusMonths(1);
        List<SecondHandHouse> candidates = repository.findByListingDateBefore(cutoffDate);
        List<SecondHandHouse> toRemove = candidates.stream()
                .filter(house -> house.getStatus() == ListingStatus.APPROVED)
                .filter(house -> !houseOrderRepository.existsByHouse_IdAndStatus(house.getId(), OrderStatus.PAID))
                .toList();
        if (!toRemove.isEmpty()) {
            repository.deleteAll(toRemove);
        }
        return toRemove.size();
    }

    public int cleanupDelistedListings() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(7);
        List<SecondHandHouse> soldListings = repository.findByStatusAndUpdatedAtBefore(ListingStatus.SOLD, cutoff);
        if (!soldListings.isEmpty()) {
            repository.deleteAll(soldListings);
        }
        return soldListings.size();
    }

    private SecondHandHouseView buildViewForRequester(SecondHandHouse house, UserAccount requester) {
        boolean maskSensitive = shouldMaskSensitive(house, requester);
        boolean reservationActive = false;
        boolean reservationOwnedByRequester = false;
        if (house.getId() != null) {
            HouseOrder reservation = houseOrderRepository
                    .findFirstByHouse_IdAndStatusOrderByCreatedAtDesc(house.getId(), OrderStatus.RESERVED)
                    .orElse(null);
            if (reservation != null) {
                reservationActive = true;
                if (requester != null && reservation.getBuyer() != null
                        && reservation.getBuyer().getUsername() != null) {
                    reservationOwnedByRequester = reservation.getBuyer().getUsername()
                            .equalsIgnoreCase(requester.getUsername());
                }
            }
        }
        return SecondHandHouseView.fromEntity(
                house,
                maskSensitive,
                reservationActive,
                reservationOwnedByRequester
        );
    }

    private boolean filterByKeyword(SecondHandHouse house, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String haystack = (house.getTitle() + " " + house.getAddress() + " " + (house.getDescription() == null ? "" : house.getDescription()))
                .toLowerCase(Locale.ROOT);
        if (haystack.contains(keyword)) {
            return true;
        }
        return house.getKeywords().stream().anyMatch(item -> item.contains(keyword));
    }

    private UserAccount resolveRequester(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        return userAccountRepository.findByUsernameIgnoreCase(username).orElse(null);
    }

    private boolean shouldMaskSensitive(SecondHandHouse house, UserAccount requester) {
        if (requester == null) {
            return true;
        }
        if (requester.getRole() == UserRole.ADMIN) {
            return false;
        }
        if (house.getSellerUsername() != null && house.getSellerUsername().equalsIgnoreCase(requester.getUsername())) {
            return false;
        }
        return !requester.isRealNameVerified();
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
        UserAccount seller = userAccountRepository.findByUsernameIgnoreCase(sellerUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "卖家账号不存在"));
        if (!seller.getRole().isSellerRole() && seller.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "指定账号不是合法的卖家角色");
        }
        if (seller.isBlacklisted()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "该卖家已被加入黑名单，无法发布房源");
        }
    }

    private void ensureNotDuplicate(SecondHandHouse house, Long ignoreId) {
        if (house.getSellerUsername() == null || house.getSellerUsername().isBlank()) {
            return;
        }
        if (house.getStatus() == ListingStatus.DRAFT) {
            return;
        }
        String normalizedTitle = normalize(house.getTitle());
        String normalizedAddress = normalize(house.getAddress());
        if (normalizedTitle.isEmpty() || normalizedAddress.isEmpty()) {
            return;
        }
        List<SecondHandHouse> existingList = repository.findBySellerUsernameIgnoreCase(house.getSellerUsername());
        boolean duplicate = existingList.stream()
                .filter(existing -> ignoreId == null || !Objects.equals(existing.getId(), ignoreId))
                .filter(existing -> existing.getStatus() != ListingStatus.REJECTED)
                .anyMatch(existing -> normalize(existing.getTitle()).equals(normalizedTitle)
                        && normalize(existing.getAddress()).equals(normalizedAddress));
        if (duplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "检测到重复房源信息，请勿重复上架。");
        }
    }

    private ListingStatus resolveTargetStatus(ListingStatus requestedStatus) {
        if (requestedStatus == ListingStatus.DRAFT) {
            return ListingStatus.DRAFT;
        }
        return ListingStatus.PENDING_REVIEW;
    }

    private void ensureInstallmentPlan(SecondHandHouse house) {
        if (house == null) {
            return;
        }
        BigDecimal price = house.getPrice() == null ? BigDecimal.ZERO : house.getPrice();
        BigDecimal downPayment = house.getDownPayment() == null ? BigDecimal.ZERO : house.getDownPayment();
        int months = house.getInstallmentMonths() == null || house.getInstallmentMonths() <= 0 ? 12 : house.getInstallmentMonths();
        if (house.getInstallmentMonths() == null || house.getInstallmentMonths() <= 0) {
            house.setInstallmentMonths(months);
        }
        BigDecimal monthly = house.getInstallmentMonthlyPayment();
        if (monthly == null || monthly.compareTo(BigDecimal.ZERO) <= 0) {
            BigDecimal remaining = price.subtract(downPayment);
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                remaining = price.multiply(BigDecimal.valueOf(0.1));
            }
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                remaining = BigDecimal.valueOf(1L);
            }
            monthly = remaining.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
            house.setInstallmentMonthlyPayment(monthly);
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }

    private boolean isVisibleToRequester(SecondHandHouse house, UserAccount requester) {
        if (house.getStatus() == ListingStatus.APPROVED) {
            return true;
        }
        if (requester == null) {
            return false;
        }
        if (requester.getRole() == UserRole.ADMIN) {
            return true;
        }
        if (house.getSellerUsername() != null
                && house.getSellerUsername().equalsIgnoreCase(requester.getUsername())) {
            return true;
        }
        return false;
    }

    private UserAccount requireAdmin(String requesterUsername) {
        if (requesterUsername == null || requesterUsername.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求人不能为空");
        }
        UserAccount requester = userAccountRepository.findByUsername(requesterUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "请求人账号不存在"));
        if (requester.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅管理员可以执行该操作");
        }
        if (requester.isBlacklisted()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "请求人已被加入黑名单");
        }
        return requester;
    }
}
