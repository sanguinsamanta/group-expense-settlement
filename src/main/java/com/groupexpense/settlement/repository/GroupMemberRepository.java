package com.groupexpense.settlement.repository;

import com.groupexpense.settlement.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    boolean existsByGroupIdAndMemberId(Long groupId, Long memberId);

    Optional<GroupMember> findByGroupIdAndMemberId(Long groupId, Long memberId);

    List<GroupMember> findByGroupId(Long groupId);

    List<GroupMember> findByMemberId(Long memberId);

    @Query("SELECT gm FROM GroupMember gm JOIN FETCH gm.member WHERE gm.group.id = :groupId ORDER BY gm.joinedAt ASC")
    List<GroupMember> findByGroupIdWithMembers(@Param("groupId") Long groupId);
}
