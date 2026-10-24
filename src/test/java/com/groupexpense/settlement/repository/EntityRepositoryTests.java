package com.groupexpense.settlement.repository;

import com.groupexpense.settlement.entity.Expense;
import com.groupexpense.settlement.entity.ExpenseGroup;
import com.groupexpense.settlement.entity.ExpenseShare;
import com.groupexpense.settlement.entity.GroupMember;
import com.groupexpense.settlement.entity.Member;
import com.groupexpense.settlement.entity.SplitMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class EntityRepositoryTests {

    @Autowired
    private ExpenseGroupRepository expenseGroupRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private ExpenseShareRepository expenseShareRepository;

    @Test
    @DisplayName("Should persist and retrieve ExpenseGroup and Member with timestamps")
    void testPersistAndRetrieveGroupAndMember() {
        ExpenseGroup group = expenseGroupRepository.saveAndFlush(new ExpenseGroup("Weekend Trip"));
        Member member = memberRepository.saveAndFlush(new Member("Alice"));

        assertNotNull(group.getId());
        assertNotNull(group.getCreatedAt());
        assertEquals("Weekend Trip", group.getName());

        assertNotNull(member.getId());
        assertNotNull(member.getCreatedAt());
        assertEquals("Alice", member.getName());

        Optional<ExpenseGroup> retrievedGroup = expenseGroupRepository.findById(group.getId());
        assertTrue(retrievedGroup.isPresent());
        assertEquals("Weekend Trip", retrievedGroup.get().getName());

        Optional<Member> retrievedMember = memberRepository.findById(member.getId());
        assertTrue(retrievedMember.isPresent());
        assertEquals("Alice", retrievedMember.get().getName());
    }

    @Test
    @DisplayName("Should enforce unique membership constraint for same group and member")
    void testGroupMembershipUniquenessConstraint() {
        ExpenseGroup group = expenseGroupRepository.saveAndFlush(new ExpenseGroup("Flat 101"));
        Member member = memberRepository.saveAndFlush(new Member("Bob"));

        groupMemberRepository.saveAndFlush(new GroupMember(group, member));

        GroupMember duplicate = new GroupMember(group, member);
        assertThrows(DataIntegrityViolationException.class, () -> {
            groupMemberRepository.saveAndFlush(duplicate);
        });
    }

    @Test
    @DisplayName("Should support a member belonging to multiple distinct groups")
    void testMultipleGroupsPerMember() {
        Member member = memberRepository.saveAndFlush(new Member("Charlie"));
        ExpenseGroup group1 = expenseGroupRepository.saveAndFlush(new ExpenseGroup("Office Lunch"));
        ExpenseGroup group2 = expenseGroupRepository.saveAndFlush(new ExpenseGroup("Badminton Club"));

        GroupMember gm1 = groupMemberRepository.saveAndFlush(new GroupMember(group1, member));
        GroupMember gm2 = groupMemberRepository.saveAndFlush(new GroupMember(group2, member));

        assertNotNull(gm1.getId());
        assertNotNull(gm2.getId());

        List<GroupMember> memberGroups = groupMemberRepository.findByMemberId(member.getId());
        assertEquals(2, memberGroups.size());
    }

    @Test
    @DisplayName("Should enforce unique member share per expense")
    void testExpenseShareUniquenessConstraint() {
        ExpenseGroup group = expenseGroupRepository.saveAndFlush(new ExpenseGroup("Dinner Club"));
        Member payer = memberRepository.saveAndFlush(new Member("Dave"));
        Member participant = memberRepository.saveAndFlush(new Member("Eve"));

        Expense expense = expenseRepository.saveAndFlush(new Expense(
                group, payer, "Team Dinner",
                new BigDecimal("100.00"),
                LocalDate.now(),
                SplitMethod.EQUAL
        ));

        expenseShareRepository.saveAndFlush(new ExpenseShare(expense, participant, new BigDecimal("50.00")));

        ExpenseShare duplicateShare = new ExpenseShare(expense, participant, new BigDecimal("50.00"));
        assertThrows(DataIntegrityViolationException.class, () -> {
            expenseShareRepository.saveAndFlush(duplicateShare);
        });
    }

    @Test
    @DisplayName("Should accurately store and retrieve BigDecimal amounts with exact precision")
    void testMonetaryPrecisionBigDecimal() {
        ExpenseGroup group = expenseGroupRepository.saveAndFlush(new ExpenseGroup("Goa Vacation"));
        Member payer = memberRepository.saveAndFlush(new Member("Frank"));
        Member debtor = memberRepository.saveAndFlush(new Member("Grace"));

        BigDecimal exactTotal = new BigDecimal("12345.67");
        BigDecimal exactShare = new BigDecimal("6172.84");

        Expense expense = expenseRepository.saveAndFlush(new Expense(
                group, payer, "Resort Booking",
                exactTotal,
                LocalDate.of(2026, 10, 24),
                SplitMethod.CUSTOM
        ));

        ExpenseShare share = expenseShareRepository.saveAndFlush(new ExpenseShare(expense, debtor, exactShare));

        Expense reloadedExpense = expenseRepository.findById(expense.getId()).orElseThrow();
        assertEquals(0, exactTotal.compareTo(reloadedExpense.getTotalAmount()),
                "Expense total amount must preserve exact financial precision");
        assertEquals(2, reloadedExpense.getTotalAmount().scale());

        ExpenseShare reloadedShare = expenseShareRepository.findById(share.getId()).orElseThrow();
        assertEquals(0, exactShare.compareTo(reloadedShare.getAssignedAmount()),
                "Expense share assigned amount must preserve exact financial precision");
        assertEquals(2, reloadedShare.getAssignedAmount().scale());
    }

    @Test
    @DisplayName("Should query membership existence and retrieve expenses by group ordered by date")
    void testRepositoryMembershipAndExpenseQueries() {
        ExpenseGroup group = expenseGroupRepository.saveAndFlush(new ExpenseGroup("Roommates"));
        Member member1 = memberRepository.saveAndFlush(new Member("Harry"));
        Member member2 = memberRepository.saveAndFlush(new Member("Ian"));
        Member outsider = memberRepository.saveAndFlush(new Member("Outsider"));

        groupMemberRepository.saveAndFlush(new GroupMember(group, member1));
        groupMemberRepository.saveAndFlush(new GroupMember(group, member2));

        assertTrue(groupMemberRepository.existsByGroupIdAndMemberId(group.getId(), member1.getId()));
        assertTrue(groupMemberRepository.existsByGroupIdAndMemberId(group.getId(), member2.getId()));
        assertFalse(groupMemberRepository.existsByGroupIdAndMemberId(group.getId(), outsider.getId()));

        Expense exp1 = new Expense(group, member1, "Groceries", new BigDecimal("45.50"),
                LocalDate.of(2026, 10, 20), SplitMethod.EQUAL);
        Expense exp2 = new Expense(group, member2, "Internet Bill", new BigDecimal("60.00"),
                LocalDate.of(2026, 10, 22), SplitMethod.EQUAL);

        expenseRepository.saveAndFlush(exp1);
        expenseRepository.saveAndFlush(exp2);

        List<Expense> expenses = expenseRepository.findByGroupId(group.getId());
        assertEquals(2, expenses.size());

        List<Expense> orderedExpenses = expenseRepository.findByGroupIdOrderByExpenseDateDescCreatedAtDesc(group.getId());
        assertEquals(2, orderedExpenses.size());
        assertEquals("Internet Bill", orderedExpenses.get(0).getDescription(),
                "Expenses must be ordered by expenseDate descending");
        assertEquals("Groceries", orderedExpenses.get(1).getDescription());

        // Check shares retrieval for exp1
        expenseShareRepository.saveAndFlush(new ExpenseShare(exp1, member1, new BigDecimal("22.75")));
        expenseShareRepository.saveAndFlush(new ExpenseShare(exp1, member2, new BigDecimal("22.75")));

        List<ExpenseShare> shares = expenseShareRepository.findByExpenseId(exp1.getId());
        assertEquals(2, shares.size());
    }
}
