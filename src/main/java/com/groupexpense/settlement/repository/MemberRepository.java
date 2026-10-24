package com.groupexpense.settlement.repository;

import com.groupexpense.settlement.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByNameIgnoreCase(String name);
}
