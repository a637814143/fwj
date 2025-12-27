package com.example.demo.house;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Entity
@Table(name = "second_hand_houses")
public class SecondHandHouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String address;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "down_payment", nullable = false, precision = 15, scale = 2)
    private BigDecimal downPayment;

    @Column(name = "installment_monthly_payment", precision = 15, scale = 2)
    private BigDecimal installmentMonthlyPayment;

    @Column(name = "installment_months")
    private Integer installmentMonths;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal area;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "seller_username", nullable = false, length = 50)
    private String sellerUsername;

    @Column(name = "seller_name", nullable = false)
    private String sellerName;

    @Column(name = "contact_number", nullable = false)
    private String contactNumber;

    @Column(name = "listing_date", nullable = false)
    private LocalDate listingDate;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "second_hand_house_images", joinColumns = @JoinColumn(name = "house_id"))
    @Column(name = "image_url", length = 500)
    private List<String> imageUrls = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "second_hand_house_keywords", joinColumns = @JoinColumn(name = "house_id"))
    @Column(name = "keyword", length = 50)
    private List<String> keywords = new ArrayList<>();

    @Column(name = "floor")
    private Integer floor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ListingStatus status = ListingStatus.PENDING_REVIEW;

    @Column(name = "reviewed_by", length = 50)
    private String reviewedBy;

    @Column(name = "review_message", length = 255)
    private String reviewMessage;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        final OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = ListingStatus.PENDING_REVIEW;
        }
        ensureInstallmentDefaults();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = OffsetDateTime.now();
        ensureInstallmentDefaults();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getInstallmentMonthlyPayment() {
        return installmentMonthlyPayment;
    }

    public void setInstallmentMonthlyPayment(BigDecimal installmentMonthlyPayment) {
        this.installmentMonthlyPayment = installmentMonthlyPayment;
    }

    public Integer getInstallmentMonths() {
        return installmentMonths;
    }

    public void setInstallmentMonths(Integer installmentMonths) {
        this.installmentMonths = installmentMonths;
    }

    private void ensureInstallmentDefaults() {
        if (installmentMonthlyPayment == null) {
            installmentMonthlyPayment = BigDecimal.ZERO;
        }
        if (installmentMonths == null) {
            installmentMonths = 0;
        }
    }

    public BigDecimal getDownPayment() {
        return downPayment;
    }

    public void setDownPayment(BigDecimal downPayment) {
        this.downPayment = downPayment;
    }

    public BigDecimal getArea() {
        return area;
    }

    public void setArea(BigDecimal area) {
        this.area = area;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSellerName() {
        return sellerName;
    }

    public String getSellerUsername() {
        return sellerUsername;
    }

    public void setSellerUsername(String sellerUsername) {
        this.sellerUsername = sellerUsername;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public LocalDate getListingDate() {
        return listingDate;
    }

    public void setListingDate(LocalDate listingDate) {
        this.listingDate = listingDate;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls == null ? new ArrayList<>() : new ArrayList<>(imageUrls);
    }

    public List<String> getKeywords() {
        return Collections.unmodifiableList(keywords);
    }

    public void setKeywords(List<String> keywords) {
        if (keywords == null) {
            this.keywords = new ArrayList<>();
            return;
        }
        this.keywords = keywords.stream()
                .filter(item -> item != null && !item.isBlank())
                .map(item -> item.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .map(item -> item.length() > 50 ? item.substring(0, 50) : item)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public Integer getFloor() {
        return floor;
    }

    public void setFloor(Integer floor) {
        this.floor = floor;
    }

    public ListingStatus getStatus() {
        return status;
    }

    public void setStatus(ListingStatus status) {
        this.status = status == null ? ListingStatus.PENDING_REVIEW : status;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public String getReviewMessage() {
        return reviewMessage;
    }

    public void setReviewMessage(String reviewMessage) {
        this.reviewMessage = reviewMessage;
    }

    public OffsetDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(OffsetDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
