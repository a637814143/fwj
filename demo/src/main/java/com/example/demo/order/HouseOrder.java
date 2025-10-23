package com.example.demo.order;

import com.example.demo.auth.UserAccount;
import com.example.demo.house.SecondHandHouse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "house_orders")
public class HouseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "house_id", nullable = false)
    private SecondHandHouse house;

    @ManyToOne(optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    private UserAccount buyer;

    @ManyToOne(optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private UserAccount seller;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod = PaymentMethod.FULL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "progress_stage", nullable = false, length = 30)
    private OrderProgressStage progressStage = OrderProgressStage.DEPOSIT_PAID;

    @Column(name = "return_reason", length = 255)
    private String returnReason;

    @Column(name = "viewing_time")
    private OffsetDateTime viewingTime;

    @Column(name = "viewing_message", length = 255)
    private String viewingMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public SecondHandHouse getHouse() {
        return house;
    }

    public void setHouse(SecondHandHouse house) {
        this.house = house;
    }

    public UserAccount getBuyer() {
        return buyer;
    }

    public void setBuyer(UserAccount buyer) {
        this.buyer = buyer;
    }

    public UserAccount getSeller() {
        return seller;
    }

    public void setSeller(UserAccount seller) {
        this.seller = seller;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod == null ? PaymentMethod.FULL : paymentMethod;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public OrderProgressStage getProgressStage() {
        return progressStage;
    }

    public void setProgressStage(OrderProgressStage progressStage) {
        if (progressStage == null) {
            return;
        }
        this.progressStage = progressStage;
        this.updatedAt = OffsetDateTime.now();
    }

    public String getReturnReason() {
        return returnReason;
    }

    public void setReturnReason(String returnReason) {
        this.returnReason = returnReason;
    }

    public OffsetDateTime getViewingTime() {
        return viewingTime;
    }

    public void setViewingTime(OffsetDateTime viewingTime) {
        this.viewingTime = viewingTime;
    }

    public String getViewingMessage() {
        return viewingMessage;
    }

    public void setViewingMessage(String viewingMessage) {
        this.viewingMessage = viewingMessage;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void markPaid() {
        this.status = OrderStatus.PAID;
        this.updatedAt = OffsetDateTime.now();
    }

    public void markReserved() {
        this.status = OrderStatus.RESERVED;
        this.updatedAt = OffsetDateTime.now();
    }

    public void markReturned(String reason) {
        this.status = OrderStatus.RETURNED;
        this.returnReason = reason;
        this.updatedAt = OffsetDateTime.now();
    }

    public void markCancelled(String reason) {
        this.status = OrderStatus.CANCELLED;
        this.returnReason = reason;
        this.updatedAt = OffsetDateTime.now();
    }

    @PrePersist
    public void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
