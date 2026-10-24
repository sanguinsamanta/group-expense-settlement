package com.groupexpense.settlement.repository;

import com.groupexpense.settlement.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByGroupId(Long groupId);

    List<Expense> findByGroupIdOrderByExpenseDateDescCreatedAtDesc(Long groupId);

    @Query("SELECT e FROM Expense e JOIN FETCH e.payer WHERE e.group.id = :groupId ORDER BY e.expenseDate DESC, e.createdAt DESC")
    List<Expense> findByGroupIdWithPayerOrderByExpenseDateDescCreatedAtDesc(@Param("groupId") Long groupId);
}
