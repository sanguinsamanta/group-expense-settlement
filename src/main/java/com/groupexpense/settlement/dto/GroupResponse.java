package com.groupexpense.settlement.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class GroupResponse {

    private Long id;
    private String name;
    private Instant createdAt;
    private List<MemberResponse> members;

    public GroupResponse() {
        this.members = new ArrayList<>();
    }

    public GroupResponse(Long id, String name, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.members = new ArrayList<>();
    }

    public GroupResponse(Long id, String name, Instant createdAt, List<MemberResponse> members) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.members = members != null ? members : new ArrayList<>();
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<MemberResponse> getMembers() {
        return members;
    }

    public void setMembers(List<MemberResponse> members) {
        this.members = members;
    }
}
