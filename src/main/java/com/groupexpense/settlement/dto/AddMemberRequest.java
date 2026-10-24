package com.groupexpense.settlement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AddMemberRequest {

    @NotBlank(message = "Member name is required")
    @Size(max = 100, message = "Member name cannot exceed 100 characters")
    private String name;

    private Long memberId;

    public AddMemberRequest() {
    }

    public AddMemberRequest(String name) {
        this.name = name;
    }

    public AddMemberRequest(String name, Long memberId) {
        this.name = name;
        this.memberId = memberId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }
}
