package com.groupexpense.settlement.repository;

import com.groupexpense.settlement.entity.ExpenseShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseShareRepository extends JpaRepository<ExpenseShare, Long> {

    List<ExpenseShare> findByExpenseId(Long expenseId);

    List<ExpenseShare> findByExpenseIdIn(List<Long> expenseIds);

    List<ExpenseShare> findByMemberId(Long memberId);

    @Query("SELECT es FROM ExpenseShare es JOIN FETCH es.member WHERE es.expense.id = :expenseId ORDER BY es.member.id ASC")
    List<ExpenseShare> findByExpenseIdWithMember(@Param("expenseId") Long expenseId);

    @Query("SELECT es FROM ExpenseShare es JOIN FETCH es.member WHERE es.expense.id IN :expenseIds ORDER BY es.expense.id DESC, es.member.id ASC")
    List<ExpenseShare> findByExpenseIdInWithMember(@Param("expenseIds") List<Long> expenseIds);
}
