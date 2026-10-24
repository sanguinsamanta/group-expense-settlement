package com.groupexpense.settlement.service;

import com.groupexpense.settlement.dto.AddMemberRequest;
import com.groupexpense.settlement.dto.CreateGroupRequest;
import com.groupexpense.settlement.dto.GroupResponse;
import com.groupexpense.settlement.dto.MemberResponse;
import com.groupexpense.settlement.entity.ExpenseGroup;
import com.groupexpense.settlement.entity.GroupMember;
import com.groupexpense.settlement.entity.Member;
import com.groupexpense.settlement.exception.DuplicateResourceException;
import com.groupexpense.settlement.exception.ResourceNotFoundException;
import com.groupexpense.settlement.repository.ExpenseGroupRepository;
import com.groupexpense.settlement.repository.GroupMemberRepository;
import com.groupexpense.settlement.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GroupService {

    private final ExpenseGroupRepository expenseGroupRepository;
    private final MemberRepository memberRepository;
    private final GroupMemberRepository groupMemberRepository;

    public GroupService(ExpenseGroupRepository expenseGroupRepository,
                        MemberRepository memberRepository,
                        GroupMemberRepository groupMemberRepository) {
        this.expenseGroupRepository = expenseGroupRepository;
        this.memberRepository = memberRepository;
        this.groupMemberRepository = groupMemberRepository;
    }

    @Transactional
    public GroupResponse createGroup(CreateGroupRequest request) {
        String trimmedName = request.getName().trim();
        ExpenseGroup group = new ExpenseGroup(trimmedName);
        ExpenseGroup savedGroup = expenseGroupRepository.save(group);
        return new GroupResponse(savedGroup.getId(), savedGroup.getName(), savedGroup.getCreatedAt(), new ArrayList<>());
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> getAllGroups() {
        return expenseGroupRepository.findAll().stream()
                .map(group -> {
                    List<MemberResponse> members = fetchMembersForGroup(group.getId());
                    return new GroupResponse(group.getId(), group.getName(), group.getCreatedAt(), members);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroupById(Long groupId) {
        ExpenseGroup group = expenseGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + groupId));

        List<MemberResponse> members = fetchMembersForGroup(groupId);
        return new GroupResponse(group.getId(), group.getName(), group.getCreatedAt(), members);
    }

    @Transactional
    public MemberResponse addMemberToGroup(Long groupId, AddMemberRequest request) {
        ExpenseGroup group = expenseGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + groupId));

        String memberName = request.getName().trim();
        Member member;

        if (request.getMemberId() != null) {
            member = memberRepository.findById(request.getMemberId())
                    .orElseThrow(() -> new ResourceNotFoundException("Member not found with id: " + request.getMemberId()));
        } else {
            // Reuse existing member with matching name or create new member
            member = memberRepository.findByNameIgnoreCase(memberName)
                    .orElseGet(() -> memberRepository.save(new Member(memberName)));
        }

        // Application-level uniqueness check before saving
        if (groupMemberRepository.existsByGroupIdAndMemberId(groupId, member.getId())) {
            throw new DuplicateResourceException(
                    "Member '" + member.getName() + "' is already a member of group '" + group.getName() + "'."
            );
        }

        GroupMember groupMember = new GroupMember(group, member);
        GroupMember savedGroupMember = groupMemberRepository.save(groupMember);

        return new MemberResponse(member.getId(), member.getName(), savedGroupMember.getJoinedAt());
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> getGroupMembers(Long groupId) {
        if (!expenseGroupRepository.existsById(groupId)) {
            throw new ResourceNotFoundException("Group not found with id: " + groupId);
        }
        return fetchMembersForGroup(groupId);
    }

    private List<MemberResponse> fetchMembersForGroup(Long groupId) {
        return groupMemberRepository.findByGroupIdWithMembers(groupId).stream()
                .map(gm -> new MemberResponse(
                        gm.getMember().getId(),
                        gm.getMember().getName(),
                        gm.getJoinedAt()
                ))
                .collect(Collectors.toList());
    }
}
