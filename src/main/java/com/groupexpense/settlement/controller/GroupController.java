package com.groupexpense.settlement.controller;

import com.groupexpense.settlement.dto.AddMemberRequest;
import com.groupexpense.settlement.dto.CreateGroupRequest;
import com.groupexpense.settlement.dto.GroupResponse;
import com.groupexpense.settlement.dto.MemberResponse;
import com.groupexpense.settlement.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@CrossOrigin(origins = "*")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(@Valid @RequestBody CreateGroupRequest request) {
        GroupResponse response = groupService.createGroup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<GroupResponse>> getAllGroups() {
        List<GroupResponse> response = groupService.getAllGroups();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupResponse> getGroupById(@PathVariable("groupId") Long groupId) {
        GroupResponse response = groupService.getGroupById(groupId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<MemberResponse> addMemberToGroup(
            @PathVariable("groupId") Long groupId,
            @Valid @RequestBody AddMemberRequest request) {
        MemberResponse response = groupService.addMemberToGroup(groupId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<MemberResponse>> getGroupMembers(@PathVariable("groupId") Long groupId) {
        List<MemberResponse> response = groupService.getGroupMembers(groupId);
        return ResponseEntity.ok(response);
    }
}
