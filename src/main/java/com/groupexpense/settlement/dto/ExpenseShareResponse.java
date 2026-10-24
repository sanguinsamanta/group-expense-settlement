package com.groupexpense.settlement.dto;

import java.math.BigDecimal;

public class ExpenseShareResponse {

    private Long id;
    private Long memberId;
    private String memberName;
    private BigDecimal assignedAmount;

    public ExpenseShareResponse() {
    }

    public ExpenseShareResponse(Long id, Long memberId, String memberName, BigDecimal assignedAmount) {
        this.id = id;
        this.memberId = memberId;
        this.memberName = memberName;
        this.assignedAmount = assignedAmount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public BigDecimal getAssignedAmount() {
        return assignedAmount;
    }

    public void setAssignedAmount(BigDecimal assignedAmount) {
        this.assignedAmount = assignedAmount;
    }
}
