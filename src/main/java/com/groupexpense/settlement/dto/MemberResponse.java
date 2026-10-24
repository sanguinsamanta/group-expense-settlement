package com.groupexpense.settlement.dto;

import java.time.Instant;

public class MemberResponse {

    private Long id;
    private String name;
    private Instant joinedAt;

    public MemberResponse() {
    }

    public MemberResponse(Long id, String name, Instant joinedAt) {
        this.id = id;
        this.name = name;
        this.joinedAt = joinedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }
}
