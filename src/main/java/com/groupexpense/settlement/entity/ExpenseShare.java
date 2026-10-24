package com.groupexpense.settlement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Represents a member's individual financial share/liability for a given Expense.
 * Uniqueness constraint ensures a member has at most one share entry per expense.
 */
@Entity
@Table(
    name = "expense_shares",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_expense_shares_expense_member",
            columnNames = {"expense_id", "member_id"}
        )
    }
)
public class ExpenseShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "expense_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_expense_shares_expense")
    )
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "member_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_expense_shares_member")
    )
    private Member member;

    @Column(name = "assigned_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal assignedAmount;

    public ExpenseShare() {
    }

    public ExpenseShare(Expense expense, Member member, BigDecimal assignedAmount) {
        this.expense = expense;
        this.member = member;
        this.assignedAmount = assignedAmount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Expense getExpense() {
        return expense;
    }

    public void setExpense(Expense expense) {
        this.expense = expense;
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    public BigDecimal getAssignedAmount() {
        return assignedAmount;
    }

    public void setAssignedAmount(BigDecimal assignedAmount) {
        this.assignedAmount = assignedAmount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExpenseShare that = (ExpenseShare) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ExpenseShare{" +
                "id=" + id +
                ", expenseId=" + (expense != null ? expense.getId() : null) +
                ", memberId=" + (member != null ? member.getId() : null) +
                ", assignedAmount=" + assignedAmount +
                '}';
    }
}
