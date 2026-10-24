package com.groupexpense.settlement.dto;

import com.groupexpense.settlement.entity.SplitMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ExpenseResponse {

    private Long id;
    private Long groupId;
    private Long payerId;
    private String payerName;
    private String description;
    private BigDecimal totalAmount;
    private LocalDate expenseDate;
    private SplitMethod splitMethod;
    private Instant createdAt;
    private List<ExpenseShareResponse> shares;

    public ExpenseResponse() {
        this.shares = new ArrayList<>();
    }

    public ExpenseResponse(Long id, Long groupId, Long payerId, String payerName, String description,
                           BigDecimal totalAmount, LocalDate expenseDate, SplitMethod splitMethod,
                           Instant createdAt, List<ExpenseShareResponse> shares) {
        this.id = id;
        this.groupId = groupId;
        this.payerId = payerId;
        this.payerName = payerName;
        this.description = description;
        this.totalAmount = totalAmount;
        this.expenseDate = expenseDate;
        this.splitMethod = splitMethod;
        this.createdAt = createdAt;
        this.shares = shares != null ? shares : new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getPayerId() {
        return payerId;
    }

    public void setPayerId(Long payerId) {
        this.payerId = payerId;
    }

    public String getPayerName() {
        return payerName;
    }

    public void setPayerName(String payerName) {
        this.payerName = payerName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    public SplitMethod getSplitMethod() {
        return splitMethod;
    }

    public void setSplitMethod(SplitMethod splitMethod) {
        this.splitMethod = splitMethod;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<ExpenseShareResponse> getShares() {
        return shares;
    }

    public void setShares(List<ExpenseShareResponse> shares) {
        this.shares = shares;
    }
}
