package com.groupexpense.settlement.dto;

import java.math.BigDecimal;

public class ExpenseShareRequest {

    private Long memberId;
    private BigDecimal amount;

    public ExpenseShareRequest() {
    }

    public ExpenseShareRequest(Long memberId, BigDecimal amount) {
        this.memberId = memberId;
        this.amount = amount;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
