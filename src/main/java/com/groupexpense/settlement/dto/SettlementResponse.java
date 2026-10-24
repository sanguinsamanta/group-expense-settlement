package com.groupexpense.settlement.dto;

import java.math.BigDecimal;

public class SettlementResponse {

    private Long fromMemberId;
    private String fromMemberName;
    private Long toMemberId;
    private String toMemberName;
    private BigDecimal amount;

    public SettlementResponse() {
    }

    public SettlementResponse(Long fromMemberId, String fromMemberName,
                              Long toMemberId, String toMemberName, BigDecimal amount) {
        this.fromMemberId = fromMemberId;
        this.fromMemberName = fromMemberName;
        this.toMemberId = toMemberId;
        this.toMemberName = toMemberName;
        this.amount = amount;
    }

    public Long getFromMemberId() {
        return fromMemberId;
    }

    public void setFromMemberId(Long fromMemberId) {
        this.fromMemberId = fromMemberId;
    }

    public String getFromMemberName() {
        return fromMemberName;
    }

    public void setFromMemberName(String fromMemberName) {
        this.fromMemberName = fromMemberName;
    }

    public Long getToMemberId() {
        return toMemberId;
    }

    public void setToMemberId(Long toMemberId) {
        this.toMemberId = toMemberId;
    }

    public String getToMemberName() {
        return toMemberName;
    }

    public void setToMemberName(String toMemberName) {
        this.toMemberName = toMemberName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
