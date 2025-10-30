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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class SecondHandHouseService {

    private static final Logger log = LoggerFactory.getLogger(SecondHandHouseService.class);

    private static final double CHINA_LAT_MIN = 17.0d;
    private static final double CHINA_LAT_MAX = 54.5d;
    private static final double CHINA_LNG_MIN = 73.0d;
    private static final double CHINA_LNG_MAX = 136.5d;

    private static final Pattern MUNICIPALITY_PATTERN = Pattern.compile("(北京|上海|天津|重庆)");
    private static final Pattern PROVINCE_PATTERN = Pattern.compile("([\\p{IsHan}]{2,}(?:省|自治区|特别行政区))");
    private static final Pattern CITY_PATTERN = Pattern.compile("([\\p{IsHan}]{2,6}市)");
    private static final Pattern COUNTY_PATTERN = Pattern.compile("([\\p{IsHan}]{2,6}(?:自治县|县|区|旗|自治旗|林区))");

    private final SecondHandHouseRepository repository;
    private final UserAccountRepository userAccountRepository;
    private final HouseOrderRepository houseOrderRepository;
    private final GaodeGeocodingClient geocodingClient;

    public SecondHandHouseService(SecondHandHouseRepository repository,
                                  UserAccountRepository userAccountRepository,
                                  HouseOrderRepository houseOrderRepository,
                                  GaodeGeocodingClient geocodingClient) {
        this.repository = repository;
        this.userAccountRepository = userAccountRepository;
        this.houseOrderRepository = houseOrderRepository;
        this.geocodingClient = geocodingClient;
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
        house.setId(null);
        house.setStatus(targetStatus);
        house.setReviewedAt(null);
        house.setReviewedBy(null);
        house.setReviewMessage(null);
        ensureCoordinates(house, !hasCoordinates(house));
        return repository.save(house);
    }

    public SecondHandHouse update(Long id, SecondHandHouse updatedHouse) {
        SecondHandHouse existing = findById(id);
        validateSellerAccount(updatedHouse.getSellerUsername());
        ListingStatus targetStatus = resolveTargetStatus(updatedHouse.getStatus());
        if (targetStatus != ListingStatus.DRAFT) {
            ensureNotDuplicate(updatedHouse, id);
        }
        String previousAddress = existing.getAddress();
        String nextAddress = updatedHouse.getAddress();
        boolean addressChanged = !Objects.equals(normalize(previousAddress), normalize(nextAddress));
        boolean newCoordinatesProvided = hasCoordinates(updatedHouse);
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
        boolean shouldRefreshCoordinates = addressChanged ? !newCoordinatesProvided : !hasCoordinates(existing);
        ensureCoordinates(existing, shouldRefreshCoordinates);
        existing.setStatus(targetStatus);
        existing.setReviewedAt(null);
        existing.setReviewedBy(null);
        existing.setReviewMessage(null);
        return repository.save(existing);
    }

    @Transactional(readOnly = true)
    public List<HouseLocationView> listLocations(Double centerLat,
                                                 Double centerLng,
                                                 Double radiusKm,
                                                 String requesterUsername) {
        UserAccount requester = resolveRequester(requesterUsername);
        Double sanitizedLat = sanitizeCoordinate(centerLat, CHINA_LAT_MIN, CHINA_LAT_MAX);
        Double sanitizedLng = sanitizeCoordinate(centerLng, CHINA_LNG_MIN, CHINA_LNG_MAX);
        Double sanitizedRadius = radiusKm != null && Double.isFinite(radiusKm) && radiusKm > 0
                ? radiusKm
                : null;
        List<HouseLocationView> locations = repository.findAll().stream()
                .filter(house -> hasCoordinates(house) || hasMappableAddress(house))
                .filter(house -> isVisibleToRequester(house, requester))
                .map(this::buildLocationView)
                .filter(Objects::nonNull)
                .toList();
        if (sanitizedLat != null && sanitizedLng != null) {
            return locations.stream()
                    .filter(view -> isWithinRadius(view, sanitizedLat, sanitizedLng, sanitizedRadius))
                    .sorted(Comparator.comparingDouble(view -> distanceToView(view, sanitizedLat, sanitizedLng)))
                    .toList();
        }
        return locations;
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

    private boolean hasCoordinates(SecondHandHouse house) {
        if (house == null) {
            return false;
        }
        Double latitude = house.getLatitude();
        Double longitude = house.getLongitude();
        return latitude != null
                && longitude != null
                && Double.isFinite(latitude)
                && Double.isFinite(longitude)
                && isCoordinateWithinChina(latitude, longitude);
    }

    private boolean hasMappableAddress(SecondHandHouse house) {
        if (house == null) {
            return false;
        }
        String address = house.getAddress();
        return address != null && !address.trim().isEmpty();
    }

    private void ensureCoordinates(SecondHandHouse house, boolean requireFreshLookup) {
        if (house == null) {
            return;
        }
        if (!requireFreshLookup && hasCoordinates(house)) {
            return;
        }
        if (!hasMappableAddress(house)) {
            if (requireFreshLookup) {
                house.setLatitude(null);
                house.setLongitude(null);
            }
            return;
        }
        if (!geocodingClient.isEnabled()) {
            if (requireFreshLookup) {
                house.setLatitude(null);
                house.setLongitude(null);
            }
            return;
        }
        String address = house.getAddress();
        String cityHint = resolveCityHint(address);
        Optional<GaodeGeocodingClient.Coordinate> coordinate = geocodingClient.geocode(address, cityHint);
        if (coordinate.isEmpty()) {
            coordinate = geocodingClient.searchPlace(address, cityHint)
                    .map(place -> new GaodeGeocodingClient.Coordinate(place.latitude(), place.longitude()));
        }
        if (coordinate.isEmpty()) {
            if (requireFreshLookup) {
                house.setLatitude(null);
                house.setLongitude(null);
            }
            return;
        }
        Double latitude = sanitizeCoordinate(coordinate.get().latitude(), CHINA_LAT_MIN, CHINA_LAT_MAX);
        Double longitude = sanitizeCoordinate(coordinate.get().longitude(), CHINA_LNG_MIN, CHINA_LNG_MAX);
        if (latitude == null || longitude == null || !isCoordinateWithinChina(latitude, longitude)) {
            if (requireFreshLookup) {
                house.setLatitude(null);
                house.setLongitude(null);
            }
            return;
        }
        house.setLatitude(latitude);
        house.setLongitude(longitude);
    }

    private HouseLocationView buildLocationView(SecondHandHouse house) {
        if (house == null) {
            return null;
        }
        Double latitude = sanitizeCoordinate(house.getLatitude(), CHINA_LAT_MIN, CHINA_LAT_MAX);
        Double longitude = sanitizeCoordinate(house.getLongitude(), CHINA_LNG_MIN, CHINA_LNG_MAX);
        if ((latitude == null || longitude == null) && hasMappableAddress(house) && geocodingClient.isEnabled()) {
            Optional<GaodeGeocodingClient.Coordinate> coordinate =
                    geocodingClient.geocode(house.getAddress(), resolveCityHint(house.getAddress()));
            if (coordinate.isPresent()) {
                Double lookedUpLatitude = sanitizeCoordinate(coordinate.get().latitude(), CHINA_LAT_MIN, CHINA_LAT_MAX);
                Double lookedUpLongitude = sanitizeCoordinate(coordinate.get().longitude(), CHINA_LNG_MIN, CHINA_LNG_MAX);
                if (lookedUpLatitude != null && lookedUpLongitude != null
                        && isCoordinateWithinChina(lookedUpLatitude, lookedUpLongitude)) {
                    latitude = lookedUpLatitude;
                    longitude = lookedUpLongitude;
                }
            }
        }
        if (latitude == null || longitude == null || !isCoordinateWithinChina(latitude, longitude)) {
            latitude = null;
            longitude = null;
        }
        return new HouseLocationView(
                house.getId(),
                house.getTitle(),
                house.getAddress(),
                house.getPrice(),
                house.getStatus(),
                latitude,
                longitude,
                house.getUpdatedAt()
        );
    }

    public MapSearchResult searchMapLocation(String query, String cityHint, Long houseId) {
        if (query == null || query.isBlank()) {
            return MapSearchResult.empty();
        }
        SecondHandHouse targetHouse = houseId == null
                ? null
                : repository.findById(houseId).orElse(null);
        LinkedHashMap<String, GaodeGeocodingClient.PlaceSuggestion> suggestionMap = new LinkedHashMap<>();
        String normalizedQuery = normalize(query);
        String normalizedQueryNoSpace = normalizeForMatch(query);
        GaodeGeocodingClient.PlaceSuggestion bestMatch = null;

        if (targetHouse != null) {
            bestMatch = buildSuggestionFromHouse(targetHouse).orElse(null);
            addSuggestion(suggestionMap, bestMatch);
        }

        AddressComponents queryComponents = parseAddressComponents(query);
        List<HouseCandidate> localCandidates = collectHouseCandidates();
        if (targetHouse != null && localCandidates.stream()
                .noneMatch(candidate -> Objects.equals(candidate.house().getId(), targetHouse.getId()))) {
            localCandidates.add(new HouseCandidate(targetHouse, parseAddressComponents(targetHouse.getAddress())));
        }

        List<HouseCandidate> prioritizedCandidates = prioritizeCandidates(localCandidates, queryComponents, normalizedQueryNoSpace);
        for (HouseCandidate candidate : prioritizedCandidates) {
            if (suggestionMap.size() >= 8) {
                break;
            }
            if (targetHouse != null && Objects.equals(candidate.house().getId(), targetHouse.getId())) {
                continue;
            }
            Optional<GaodeGeocodingClient.PlaceSuggestion> suggestion = buildSuggestionFromHouse(candidate.house());
            if (suggestion.isEmpty()) {
                suggestion = buildSuggestionFromAddress(candidate.house(), candidate.components());
            }
            if (suggestion.isEmpty()) {
                continue;
            }
            addSuggestion(suggestionMap, suggestion.get());
            if (bestMatch == null) {
                bestMatch = suggestion.get();
            }
        }

        if (bestMatch == null && !normalizedQuery.isEmpty()) {
            Optional<GaodeGeocodingClient.PlaceSuggestion> houseMatch = repository.findAll().stream()
                    .filter(house -> addressMatchesQuery(house, normalizedQuery))
                    .map(this::buildSuggestionFromHouse)
                    .flatMap(Optional::stream)
                    .findFirst();
            if (houseMatch.isPresent()) {
                bestMatch = houseMatch.get();
                addSuggestion(suggestionMap, bestMatch);
            }
        }

        List<String> queryCandidates = buildQueryCandidates(query, targetHouse);
        for (String candidateQuery : queryCandidates) {
            if (suggestionMap.size() >= 8) {
                break;
            }
            String effectiveCityCandidate = firstNonBlank(
                    cityHint,
                    resolveCityHint(candidateQuery),
                    targetHouse == null ? null : resolveCityHint(targetHouse.getAddress())
            );
            String effectiveCity = effectiveCityCandidate == null
                    ? null
                    : sanitizeAdministrativeName(effectiveCityCandidate);

            Optional<GaodeGeocodingClient.PlaceSuggestion> placeSuggestion = geocodingClient.searchPlace(candidateQuery, effectiveCity)
                    .filter(place -> isCoordinateWithinChina(place.latitude(), place.longitude()));
            if (placeSuggestion.isPresent()) {
                GaodeGeocodingClient.PlaceSuggestion suggestion = placeSuggestion.get();
                addSuggestion(suggestionMap, suggestion);
                if (bestMatch == null) {
                    bestMatch = suggestion;
                }
            }

            if (suggestionMap.size() >= 8) {
                continue;
            }

            List<GaodeGeocodingClient.PlaceSuggestion> tipSuggestions =
                    geocodingClient.suggestPlaces(candidateQuery, effectiveCity);
            for (GaodeGeocodingClient.PlaceSuggestion suggestion : tipSuggestions) {
                if (!isCoordinateWithinChina(suggestion.latitude(), suggestion.longitude())) {
                    continue;
                }
                addSuggestion(suggestionMap, suggestion);
                if (bestMatch == null) {
                    bestMatch = suggestion;
                }
                if (suggestionMap.size() >= 8) {
                    break;
                }
            }

            if (suggestionMap.size() >= 8) {
                continue;
            }

            Optional<GaodeGeocodingClient.Coordinate> geocoded = geocodingClient.geocode(candidateQuery, effectiveCity);
            if (geocoded.isPresent()) {
                double lat = geocoded.get().latitude();
                double lng = geocoded.get().longitude();
                if (isCoordinateWithinChina(lat, lng)) {
                    GaodeGeocodingClient.PlaceSuggestion suggestion = new GaodeGeocodingClient.PlaceSuggestion(
                            candidateQuery,
                            candidateQuery,
                            lat,
                            lng
                    );
                    addSuggestion(suggestionMap, suggestion);
                    if (bestMatch == null) {
                        bestMatch = suggestion;
                    }
                }
            }
        }

        if (bestMatch == null && !suggestionMap.isEmpty()) {
            bestMatch = suggestionMap.values().iterator().next();
        }

        List<GaodeGeocodingClient.PlaceSuggestion> orderedSuggestions = new ArrayList<>();
        if (bestMatch != null) {
            orderedSuggestions.add(bestMatch);
        }
        for (GaodeGeocodingClient.PlaceSuggestion suggestion : suggestionMap.values()) {
            if (bestMatch != null && suggestionsEqual(bestMatch, suggestion)) {
                continue;
            }
            orderedSuggestions.add(suggestion);
        }

        List<GaodeGeocodingClient.PlaceSuggestion> limitedSuggestions = orderedSuggestions.stream()
                .limit(8)
                .toList();

        return new MapSearchResult(Optional.ofNullable(bestMatch), limitedSuggestions);
    }

    private List<HouseCandidate> collectHouseCandidates() {
        List<HouseCandidate> candidates = new ArrayList<>();
        for (SecondHandHouse house : repository.findAll()) {
            if (!hasMappableAddress(house)) {
                continue;
            }
            candidates.add(new HouseCandidate(house, parseAddressComponents(house.getAddress())));
        }
        return candidates;
    }

    private List<HouseCandidate> prioritizeCandidates(List<HouseCandidate> candidates,
                                                      AddressComponents queryComponents,
                                                      String normalizedQuery) {
        if (candidates.isEmpty()) {
            return List.of();
        }
        List<HouseCandidate> filtered = new ArrayList<>(candidates);
        filtered = filterByComponent(filtered, queryComponents.province(), AddressComponents::province);
        if (filtered.isEmpty()) {
            return List.of();
        }
        filtered = filterByComponent(filtered, queryComponents.city(), AddressComponents::city);
        if (filtered.isEmpty()) {
            return List.of();
        }
        filtered = filterByComponent(filtered, queryComponents.district(), AddressComponents::district);
        if (filtered.isEmpty()) {
            return List.of();
        }
        filtered = filterByStreet(filtered, queryComponents.street(), normalizedQuery);
        if (filtered.isEmpty()) {
            return List.of();
        }
        filtered.sort(Comparator
                .comparingInt((HouseCandidate candidate) -> computeMatchScore(candidate, queryComponents, normalizedQuery))
                .reversed()
                .thenComparing(candidate -> candidate.house().getUpdatedAt(), Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(candidate -> candidate.house().getId(), Comparator.nullsLast(Long::compareTo)));
        return filtered;
    }

    private List<HouseCandidate> filterByComponent(List<HouseCandidate> candidates,
                                                   String queryValue,
                                                   java.util.function.Function<AddressComponents, String> extractor) {
        if (queryValue == null || queryValue.isBlank()) {
            return candidates;
        }
        List<HouseCandidate> matches = new ArrayList<>();
        for (HouseCandidate candidate : candidates) {
            String component = extractor.apply(candidate.components());
            if (componentMatches(component, queryValue)) {
                matches.add(candidate);
            }
        }
        return matches;
    }

    private List<HouseCandidate> filterByStreet(List<HouseCandidate> candidates,
                                                String streetFragment,
                                                String normalizedQuery) {
        if ((streetFragment == null || streetFragment.isBlank())
                && (normalizedQuery == null || normalizedQuery.isBlank())) {
            return candidates;
        }
        List<HouseCandidate> matches = new ArrayList<>();
        for (HouseCandidate candidate : candidates) {
            boolean matched = false;
            if (streetFragment != null && !streetFragment.isBlank()) {
                matched = componentMatches(candidate.components().street(), streetFragment);
            }
            if (!matched && normalizedQuery != null && !normalizedQuery.isBlank()) {
                String normalizedAddress = candidate.components().normalizedFull();
                matched = normalizedAddress.contains(normalizedQuery);
            }
            if (matched) {
                matches.add(candidate);
            }
        }
        return matches;
    }

    private int computeMatchScore(HouseCandidate candidate,
                                  AddressComponents queryComponents,
                                  String normalizedQuery) {
        int score = 0;
        AddressComponents components = candidate.components();
        if (componentMatches(components.province(), queryComponents.province())) {
            score += 1000;
        }
        if (componentMatches(components.city(), queryComponents.city())) {
            score += 200;
        }
        if (componentMatches(components.district(), queryComponents.district())) {
            score += 50;
        }
        if (componentMatches(components.street(), queryComponents.street())) {
            score += 20;
        }
        if (normalizedQuery != null && !normalizedQuery.isBlank()) {
            String address = components.normalizedFull();
            if (!address.isBlank()) {
                if (address.contains(normalizedQuery)) {
                    score += 15;
                } else if (normalizedQuery.contains(address)) {
                    score += 10;
                }
            }
        }
        if (hasCoordinates(candidate.house())) {
            score += 5;
        }
        return score;
    }

    private Optional<GaodeGeocodingClient.PlaceSuggestion> buildSuggestionFromAddress(SecondHandHouse house,
                                                                                      AddressComponents components) {
        if (house == null || !hasMappableAddress(house) || !geocodingClient.isEnabled()) {
            return Optional.empty();
        }
        String address = house.getAddress();
        String cityHint = firstNonBlank(
                resolveCityHint(address),
                components == null ? null : components.city(),
                components == null ? null : components.province()
        );
        Optional<GaodeGeocodingClient.Coordinate> coordinate = geocodingClient.geocode(address, cityHint);
        if (coordinate.isEmpty()) {
            return Optional.empty();
        }
        Double latitude = sanitizeCoordinate(coordinate.get().latitude(), CHINA_LAT_MIN, CHINA_LAT_MAX);
        Double longitude = sanitizeCoordinate(coordinate.get().longitude(), CHINA_LNG_MIN, CHINA_LNG_MAX);
        if (latitude == null || longitude == null || !isCoordinateWithinChina(latitude, longitude)) {
            return Optional.empty();
        }
        String name = house.getTitle();
        if (name == null || name.isBlank()) {
            name = address == null || address.isBlank() ? "房源" : address.trim();
        }
        String displayAddress = address == null || address.isBlank() ? name : address.trim();
        return Optional.of(new GaodeGeocodingClient.PlaceSuggestion(name, displayAddress, latitude, longitude));
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) {
                return trimmed;
            }
        }
        return null;
    }

    private void addSuggestion(LinkedHashMap<String, GaodeGeocodingClient.PlaceSuggestion> suggestions,
                               GaodeGeocodingClient.PlaceSuggestion suggestion) {
        if (suggestion == null) {
            return;
        }
        String key = buildSuggestionKey(suggestion);
        suggestions.putIfAbsent(key, suggestion);
    }

    private boolean suggestionsEqual(GaodeGeocodingClient.PlaceSuggestion left,
                                     GaodeGeocodingClient.PlaceSuggestion right) {
        if (left == null || right == null) {
            return false;
        }
        return buildSuggestionKey(left).equals(buildSuggestionKey(right));
    }

    private String buildSuggestionKey(GaodeGeocodingClient.PlaceSuggestion suggestion) {
        long latKey = Math.round(suggestion.latitude() * 1_000_000d);
        long lngKey = Math.round(suggestion.longitude() * 1_000_000d);
        String normalizedAddress = normalize(suggestion.address());
        return latKey + ":" + lngKey + "::" + normalizedAddress;
    }

    @Transactional(readOnly = true)
    public GaodeApiUsageTracker.UsageSnapshot gaodeUsageSnapshot() {
        return geocodingClient.usageSnapshot();
    }


    public record MapSearchResult(Optional<GaodeGeocodingClient.PlaceSuggestion> match,
                                  List<GaodeGeocodingClient.PlaceSuggestion> suggestions) {
        public MapSearchResult {
            Objects.requireNonNull(match, "match must not be null");
            Objects.requireNonNull(suggestions, "suggestions must not be null");
        }

        public static MapSearchResult empty() {
            return new MapSearchResult(Optional.empty(), List.of());
        }
    }

    private boolean hasCoordinates(HouseLocationView view) {
        if (view == null) {
            return false;
        }
        Double latitude = view.latitude();
        Double longitude = view.longitude();
        return latitude != null
                && longitude != null
                && Double.isFinite(latitude)
                && Double.isFinite(longitude)
                && isCoordinateWithinChina(latitude, longitude);
    }

    private boolean isWithinRadius(HouseLocationView view,
                                   Double centerLat,
                                   Double centerLng,
                                   Double radiusKm) {
        if (radiusKm == null || centerLat == null || centerLng == null) {
            return true;
        }
        if (!hasCoordinates(view)) {
            return false;
        }
        double distance = haversineDistanceKm(centerLat, centerLng, view.latitude(), view.longitude());
        return distance <= radiusKm;
    }

    private List<String> buildQueryCandidates(String query, SecondHandHouse targetHouse) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        if (query != null) {
            String trimmed = query.trim();
            addCandidate(candidates, trimmed);
            addCandidate(candidates, trimmed.replaceAll("\\s+", ""));
            String withoutParentheses = trimmed.replaceAll("（.*?）", "").replaceAll("\\(.*?\\)", "");
            addCandidate(candidates, withoutParentheses);
            for (String delimiter : new String[]{"，", ",", "。", " "}) {
                int index = trimmed.indexOf(delimiter);
                if (index > 3) {
                    addCandidate(candidates, trimmed.substring(0, index));
                }
            }
            int haoIndex = trimmed.indexOf('号');
            if (haoIndex > 3) {
                addCandidate(candidates, trimmed.substring(0, haoIndex + 1));
            }
            int hashIndex = trimmed.indexOf('#');
            if (hashIndex > 3) {
                addCandidate(candidates, trimmed.substring(0, hashIndex));
            }
        }
        if (targetHouse != null) {
            addCandidate(candidates, targetHouse.getAddress());
            addCandidate(candidates, targetHouse.getTitle());
            String address = targetHouse.getAddress();
            if (address != null) {
                addCandidate(candidates, address.replaceAll("\\s+", ""));
            }
        }
        return candidates.stream()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }

    private Optional<GaodeGeocodingClient.PlaceSuggestion> buildSuggestionFromHouse(SecondHandHouse house) {
        if (house == null) {
            return Optional.empty();
        }
        Double latitude = sanitizeCoordinate(house.getLatitude(), CHINA_LAT_MIN, CHINA_LAT_MAX);
        Double longitude = sanitizeCoordinate(house.getLongitude(), CHINA_LNG_MIN, CHINA_LNG_MAX);
        if (latitude == null || longitude == null || !isCoordinateWithinChina(latitude, longitude)) {
            return Optional.empty();
        }
        String address = house.getAddress();
        String name = house.getTitle();
        if (name == null || name.isBlank()) {
            name = address == null || address.isBlank() ? "房源" : address;
        }
        if (address == null || address.isBlank()) {
            address = name;
        }
        return Optional.of(new GaodeGeocodingClient.PlaceSuggestion(name, address, latitude, longitude));
    }

    private boolean addressMatchesQuery(SecondHandHouse house, String normalizedQuery) {
        if (house == null || normalizedQuery == null || normalizedQuery.isEmpty()) {
            return false;
        }
        String normalizedAddress = normalize(house.getAddress());
        if (normalizedAddress.isEmpty()) {
            return false;
        }
        return normalizedAddress.equals(normalizedQuery)
                || normalizedAddress.contains(normalizedQuery)
                || normalizedQuery.contains(normalizedAddress);
    }

    private void addCandidate(LinkedHashSet<String> candidates, String value) {
        if (value == null) {
            return;
        }
        String candidate = value.trim();
        if (candidate.isEmpty()) {
            return;
        }
        candidates.add(candidate);
    }

    private AddressComponents parseAddressComponents(String value) {
        if (value == null || value.isBlank()) {
            return AddressComponents.EMPTY;
        }
        String trimmed = value.replaceAll("\\s+", "").trim();
        if (trimmed.isEmpty()) {
            return AddressComponents.EMPTY;
        }
        String normalizedFull = normalizeForMatch(trimmed);
        String municipality = findLastMatch(MUNICIPALITY_PATTERN, trimmed);
        String province = findLastMatch(PROVINCE_PATTERN, trimmed);
        if (province == null && municipality != null) {
            province = municipality + "市";
        }
        String city = findLastMatch(CITY_PATTERN, trimmed);
        if (city == null && municipality != null) {
            city = municipality + "市";
        }
        String district = findLastMatch(COUNTY_PATTERN, trimmed);
        String street = trimmed;
        for (String segment : new String[]{province, city, district}) {
            if (segment != null && !segment.isBlank()) {
                street = street.replace(segment, "");
            }
        }
        street = street.replace("中华人民共和国", "").replace("中国", "");
        street = street.trim();
        if (street.isEmpty()) {
            street = null;
        }
        return new AddressComponents(province, city, district, street, normalizedFull);
    }

    private boolean componentMatches(String component, String queryComponent) {
        if (component == null || component.isBlank() || queryComponent == null || queryComponent.isBlank()) {
            return false;
        }
        String normalizedComponent = normalizeForMatch(component);
        String normalizedQuery = normalizeForMatch(queryComponent);
        if (!normalizedComponent.isBlank() && !normalizedQuery.isBlank()
                && (normalizedComponent.contains(normalizedQuery) || normalizedQuery.contains(normalizedComponent))) {
            return true;
        }
        String sanitizedComponent = normalizeAdministrativeSegment(component);
        String sanitizedQuery = normalizeAdministrativeSegment(queryComponent);
        return !sanitizedComponent.isBlank()
                && !sanitizedQuery.isBlank()
                && (sanitizedComponent.contains(sanitizedQuery) || sanitizedQuery.contains(sanitizedComponent));
    }

    private String normalizeAdministrativeSegment(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.replaceAll("\\s+", "").trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty()) {
            return "";
        }
        return trimmed.replaceAll("(特别行政区|自治区|自治州|自治县|自治旗|地区|盟|省|市|区|县|旗)$", "");
    }

    private String normalizeForMatch(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", "").trim().toLowerCase(Locale.ROOT);
    }

    private String resolveCityHint(String address) {
        if (address == null || address.isBlank()) {
            return null;
        }
        String trimmed = address.replaceAll("\\s+", "").trim();
        String municipality = findLastMatch(MUNICIPALITY_PATTERN, trimmed);
        if (municipality != null) {
            return municipality + "市";
        }
        String city = findLastMatch(CITY_PATTERN, trimmed);
        if (city != null) {
            return sanitizeAdministrativeName(city);
        }
        String county = findLastMatch(COUNTY_PATTERN, trimmed);
        if (county != null) {
            return sanitizeAdministrativeName(county);
        }
        return null;
    }

    private String findLastMatch(Pattern pattern, String input) {
        Matcher matcher = pattern.matcher(input);
        String candidate = null;
        while (matcher.find()) {
            candidate = matcher.group(1);
        }
        return candidate;
    }

    private String sanitizeAdministrativeName(String value) {
        if (value == null) {
            return null;
        }
        String sanitized = value.replaceAll("(自治州|地区|盟)", "").trim();
        return sanitized.isEmpty() ? null : sanitized;
    }

    private record HouseCandidate(SecondHandHouse house, AddressComponents components) {
    }

    private record AddressComponents(String province,
                                     String city,
                                     String district,
                                     String street,
                                     String normalizedFull) {
        private static final AddressComponents EMPTY = new AddressComponents(null, null, null, null, "");

        @Override
        public String normalizedFull() {
            return normalizedFull == null ? "" : normalizedFull;
        }
    }

    private double haversineDistanceKm(double lat1, double lng1, double lat2, double lng2) {
        final double earthRadiusKm = 6371.0088d;
        double latRad1 = Math.toRadians(lat1);
        double latRad2 = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(latRad1) * Math.cos(latRad2) * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }

    private double distanceToView(HouseLocationView view, double centerLat, double centerLng) {
        if (!hasCoordinates(view)) {
            return Double.POSITIVE_INFINITY;
        }
        return haversineDistanceKm(centerLat, centerLng, view.latitude(), view.longitude());
    }

    private boolean isCoordinateWithinChina(double latitude, double longitude) {
        return latitude >= CHINA_LAT_MIN
                && latitude <= CHINA_LAT_MAX
                && longitude >= CHINA_LNG_MIN
                && longitude <= CHINA_LNG_MAX;
    }

    private Double sanitizeCoordinate(Double value, double min, double max) {
        if (value == null) {
            return null;
        }
        double number = value;
        if (!Double.isFinite(number)) {
            return null;
        }
        if (number < min || number > max) {
            return null;
        }
        return number;
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
