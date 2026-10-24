package com.groupexpense.settlement.service;

import com.groupexpense.settlement.dto.MemberBalanceResponse;
import com.groupexpense.settlement.dto.SettlementResponse;
import com.groupexpense.settlement.entity.Expense;
import com.groupexpense.settlement.entity.ExpenseShare;
import com.groupexpense.settlement.entity.GroupMember;
import com.groupexpense.settlement.entity.Member;
import com.groupexpense.settlement.exception.ResourceNotFoundException;
import com.groupexpense.settlement.repository.ExpenseGroupRepository;
import com.groupexpense.settlement.repository.ExpenseRepository;
import com.groupexpense.settlement.repository.ExpenseShareRepository;
import com.groupexpense.settlement.repository.GroupMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for calculating group net balances and generating simplified settlement transfers
 * using a deterministic greedy algorithm.
 */
@Service
public class SettlementService {

    private final ExpenseGroupRepository expenseGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseShareRepository expenseShareRepository;

    public SettlementService(ExpenseGroupRepository expenseGroupRepository,
                             GroupMemberRepository groupMemberRepository,
                             ExpenseRepository expenseRepository,
                             ExpenseShareRepository expenseShareRepository) {
        this.expenseGroupRepository = expenseGroupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.expenseRepository = expenseRepository;
        this.expenseShareRepository = expenseShareRepository;
    }

    /**
     * Calculates net balance for every member in the specified group.
     * netBalance = totalPaidByMember - totalAssignedSharesForMember.
     * Positive = Creditor (group owes member).
     * Negative = Debtor (member owes money).
     * Zero = Settled.
     */
    @Transactional(readOnly = true)
    public List<MemberBalanceResponse> getGroupBalances(Long groupId) {
        if (!expenseGroupRepository.existsById(groupId)) {
            throw new ResourceNotFoundException("Group not found with id: " + groupId);
        }

        // 1. Fetch all members of this group in a single query, ordered by joinedAt/ID
        List<GroupMember> groupMembers = groupMemberRepository.findByGroupIdWithMembers(groupId);
        List<Member> members = groupMembers.stream()
                .map(GroupMember::getMember)
                .sorted(Comparator.comparing(Member::getId))
                .toList();

        // Initialize tracking maps with 0.00 for every group member (including those with 0 expenses)
        Map<Long, BigDecimal> totalPaidMap = new LinkedHashMap<>();
        Map<Long, BigDecimal> totalOwedMap = new LinkedHashMap<>();
        for (Member m : members) {
            totalPaidMap.put(m.getId(), BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            totalOwedMap.put(m.getId(), BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }

        // 2. Fetch all expenses in this group
        List<Expense> expenses = expenseRepository.findByGroupId(groupId);

        if (!expenses.isEmpty()) {
            for (Expense exp : expenses) {
                Long payerId = exp.getPayer().getId();
                if (totalPaidMap.containsKey(payerId)) {
                    totalPaidMap.put(payerId, totalPaidMap.get(payerId).add(exp.getTotalAmount()));
                }
            }

            // 3. Fetch all shares for these expenses in a single query
            List<Long> expenseIds = expenses.stream().map(Expense::getId).toList();
            List<ExpenseShare> shares = expenseShareRepository.findByExpenseIdIn(expenseIds);

            for (ExpenseShare share : shares) {
                Long memberId = share.getMember().getId();
                if (totalOwedMap.containsKey(memberId)) {
                    totalOwedMap.put(memberId, totalOwedMap.get(memberId).add(share.getAssignedAmount()));
                }
            }
        }

        // 4. Compute net balances and construct deterministic response
        List<MemberBalanceResponse> balances = new ArrayList<>();
        BigDecimal sumOfNetBalances = BigDecimal.ZERO;

        for (Member m : members) {
            BigDecimal totalPaid = totalPaidMap.get(m.getId()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalOwed = totalOwedMap.get(m.getId()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal netBalance = totalPaid.subtract(totalOwed).setScale(2, RoundingMode.HALF_UP);

            balances.add(new MemberBalanceResponse(
                    m.getId(),
                    m.getName(),
                    totalPaid,
                    totalOwed,
                    netBalance
            ));

            sumOfNetBalances = sumOfNetBalances.add(netBalance);
        }

        // 5. Zero-sum verification
        if (sumOfNetBalances.compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException(
                    "Inconsistent balance state: sum of net balances is " + sumOfNetBalances + " instead of 0.00."
            );
        }

        return balances;
    }

    /**
     * Generates a deterministic settlement plan using a greedy algorithm.
     * Note: While the greedy matching produces a valid settlement plan that clears all debts
     * with zero remaining balances, it does not guarantee the globally minimum number of transactions
     * (the minimum cash flow problem is NP-complete, reducible from the subset sum problem).
     */
    @Transactional(readOnly = true)
    public List<SettlementResponse> getSuggestedSettlements(Long groupId) {
        List<MemberBalanceResponse> balances = getGroupBalances(groupId);

        List<MutableBalance> debtors = new ArrayList<>();
        List<MutableBalance> creditors = new ArrayList<>();

        for (MemberBalanceResponse b : balances) {
            int cmp = b.getNetBalance().compareTo(BigDecimal.ZERO);
            if (cmp < 0) {
                debtors.add(new MutableBalance(b.getMemberId(), b.getMemberName(), b.getNetBalance().abs()));
            } else if (cmp > 0) {
                creditors.add(new MutableBalance(b.getMemberId(), b.getMemberName(), b.getNetBalance()));
            }
            // netBalance == 0.00 is already settled, ignore
        }

        // Sort deterministically: descending by amount, tie-breaker ascending by member ID
        Comparator<MutableBalance> comparator = Comparator
                .comparing(MutableBalance::getAmount, Comparator.reverseOrder())
                .thenComparing(MutableBalance::getMemberId);

        debtors.sort(comparator);
        creditors.sort(comparator);

        List<SettlementResponse> settlements = new ArrayList<>();
        int dIdx = 0;
        int cIdx = 0;

        while (dIdx < debtors.size() && cIdx < creditors.size()) {
            MutableBalance debtor = debtors.get(dIdx);
            MutableBalance creditor = creditors.get(cIdx);

            BigDecimal transferAmount = debtor.amount.min(creditor.amount).setScale(2, RoundingMode.HALF_UP);

            if (transferAmount.compareTo(BigDecimal.ZERO) > 0) {
                settlements.add(new SettlementResponse(
                        debtor.memberId,
                        debtor.memberName,
                        creditor.memberId,
                        creditor.memberName,
                        transferAmount
                ));

                debtor.amount = debtor.amount.subtract(transferAmount);
                creditor.amount = creditor.amount.subtract(transferAmount);
            }

            if (debtor.amount.compareTo(BigDecimal.ZERO) == 0) {
                dIdx++;
            }
            if (creditor.amount.compareTo(BigDecimal.ZERO) == 0) {
                cIdx++;
            }
        }

        return settlements;
    }

    private static class MutableBalance {
        private final Long memberId;
        private final String memberName;
        private BigDecimal amount;

        public MutableBalance(Long memberId, String memberName, BigDecimal amount) {
            this.memberId = memberId;
            this.memberName = memberName;
            this.amount = amount;
        }

        public Long getMemberId() {
            return memberId;
        }

        public BigDecimal getAmount() {
            return amount;
        }
    }
}
