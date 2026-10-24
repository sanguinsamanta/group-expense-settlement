package com.groupexpense.settlement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents an expense recorded within an expense group, paid by a specific member.
 */
@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "group_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_expenses_group")
    )
    private ExpenseGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "payer_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_expenses_payer")
    )
    private Member payer;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "split_method", nullable = false, length = 20)
    private SplitMethod splitMethod;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Expense() {
    }

    public Expense(ExpenseGroup group, Member payer, String description, BigDecimal totalAmount,
                   LocalDate expenseDate, SplitMethod splitMethod) {
        this.group = group;
        this.payer = payer;
        this.description = description;
        this.totalAmount = totalAmount;
        this.expenseDate = expenseDate;
        this.splitMethod = splitMethod;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ExpenseGroup getGroup() {
        return group;
    }

    public void setGroup(ExpenseGroup group) {
        this.group = group;
    }

    public Member getPayer() {
        return payer;
    }

    public void setPayer(Member payer) {
        this.payer = payer;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Expense expense = (Expense) o;
        return id != null && Objects.equals(id, expense.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Expense{" +
                "id=" + id +
                ", groupId=" + (group != null ? group.getId() : null) +
                ", payerId=" + (payer != null ? payer.getId() : null) +
                ", description='" + description + '\'' +
                ", totalAmount=" + totalAmount +
                ", expenseDate=" + expenseDate +
                ", splitMethod=" + splitMethod +
                ", createdAt=" + createdAt +
                '}';
    }
}
