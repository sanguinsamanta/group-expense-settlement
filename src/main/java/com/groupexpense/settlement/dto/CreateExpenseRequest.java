package com.groupexpense.settlement.dto;

import com.groupexpense.settlement.entity.SplitMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class CreateExpenseRequest {

    @NotBlank(message = "Description is required")
    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    @NotNull(message = "Total amount is required")
    private BigDecimal totalAmount;

    @NotNull(message = "Payer member ID is required")
    private Long payerMemberId;

    @NotNull(message = "Expense date is required")
    private LocalDate expenseDate;

    @NotNull(message = "Split method is required")
    private SplitMethod splitMethod;

    // For EQUAL splits: participating member IDs
    private List<Long> participantMemberIds;

    // For CUSTOM splits: member IDs with individual amounts
    private List<ExpenseShareRequest> customShares;

    public CreateExpenseRequest() {
    }

    public CreateExpenseRequest(String description, BigDecimal totalAmount, Long payerMemberId,
                                LocalDate expenseDate, SplitMethod splitMethod,
                                List<Long> participantMemberIds, List<ExpenseShareRequest> customShares) {
        this.description = description;
        this.totalAmount = totalAmount;
        this.payerMemberId = payerMemberId;
        this.expenseDate = expenseDate;
        this.splitMethod = splitMethod;
        this.participantMemberIds = participantMemberIds;
        this.customShares = customShares;
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

    public Long getPayerMemberId() {
        return payerMemberId;
    }

    public void setPayerMemberId(Long payerMemberId) {
        this.payerMemberId = payerMemberId;
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

    public List<Long> getParticipantMemberIds() {
        return participantMemberIds;
    }

    public void setParticipantMemberIds(List<Long> participantMemberIds) {
        this.participantMemberIds = participantMemberIds;
    }

    public List<ExpenseShareRequest> getCustomShares() {
        return customShares;
    }

    public void setCustomShares(List<ExpenseShareRequest> customShares) {
        this.customShares = customShares;
    }
}
