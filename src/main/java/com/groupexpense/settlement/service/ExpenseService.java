package com.groupexpense.settlement.service;

import com.groupexpense.settlement.dto.CreateExpenseRequest;
import com.groupexpense.settlement.dto.ExpenseResponse;
import com.groupexpense.settlement.dto.ExpenseShareRequest;
import com.groupexpense.settlement.dto.ExpenseShareResponse;
import com.groupexpense.settlement.entity.Expense;
import com.groupexpense.settlement.entity.ExpenseGroup;
import com.groupexpense.settlement.entity.ExpenseShare;
import com.groupexpense.settlement.entity.Member;
import com.groupexpense.settlement.entity.SplitMethod;
import com.groupexpense.settlement.exception.ResourceNotFoundException;
import com.groupexpense.settlement.repository.ExpenseGroupRepository;
import com.groupexpense.settlement.repository.ExpenseRepository;
import com.groupexpense.settlement.repository.ExpenseShareRepository;
import com.groupexpense.settlement.repository.GroupMemberRepository;
import com.groupexpense.settlement.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999.99");

    private final ExpenseGroupRepository expenseGroupRepository;
    private final MemberRepository memberRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseShareRepository expenseShareRepository;

    public ExpenseService(ExpenseGroupRepository expenseGroupRepository,
                          MemberRepository memberRepository,
                          GroupMemberRepository groupMemberRepository,
                          ExpenseRepository expenseRepository,
                          ExpenseShareRepository expenseShareRepository) {
        this.expenseGroupRepository = expenseGroupRepository;
        this.memberRepository = memberRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.expenseRepository = expenseRepository;
        this.expenseShareRepository = expenseShareRepository;
    }

    @Transactional
    public ExpenseResponse recordExpense(Long groupId, CreateExpenseRequest request) {
        // 1. Validate Group Existence
        ExpenseGroup group = expenseGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + groupId));

        // 2. Validate Description
        if (request.getDescription() == null || request.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Expense description cannot be blank.");
        }
        if (request.getDescription().trim().length() > 255) {
            throw new IllegalArgumentException("Expense description cannot exceed 255 characters.");
        }
        String description = request.getDescription().trim();

        // 3. Validate Total Amount and scale
        if (request.getTotalAmount() == null || request.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Total amount must be greater than zero.");
        }
        if (request.getTotalAmount().scale() > 2 && request.getTotalAmount().stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Total amount cannot have more than 2 decimal places.");
        }
        if (request.getTotalAmount().compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException("Total amount exceeds the maximum allowable limit of 9,999,999,999.99.");
        }
        BigDecimal totalAmount = request.getTotalAmount().setScale(2, RoundingMode.HALF_UP);

        // 4. Validate Payer
        if (request.getPayerMemberId() == null) {
            throw new IllegalArgumentException("Payer member ID is required.");
        }
        if (!groupMemberRepository.existsByGroupIdAndMemberId(groupId, request.getPayerMemberId())) {
            throw new IllegalArgumentException(
                    "Payer with member ID " + request.getPayerMemberId() + " does not belong to group with ID " + groupId + "."
            );
        }
        Member payer = memberRepository.findById(request.getPayerMemberId())
                .orElseThrow(() -> new ResourceNotFoundException("Payer member not found with id: " + request.getPayerMemberId()));

        // 5. Validate Expense Date and Split Method
        if (request.getExpenseDate() == null) {
            throw new IllegalArgumentException("Expense date is required.");
        }
        if (request.getSplitMethod() == null) {
            throw new IllegalArgumentException("Split method is required.");
        }

        // 6. Calculate and Validate Shares
        Map<Long, BigDecimal> memberShares = calculateAndValidateShares(groupId, totalAmount, request);

        // 7. Persist Expense
        Expense expense = new Expense(
                group,
                payer,
                description,
                totalAmount,
                request.getExpenseDate(),
                request.getSplitMethod()
        );
        Expense savedExpense = expenseRepository.save(expense);

        // 8. Persist Expense Shares (inside same transaction)
        List<ExpenseShareResponse> shareResponses = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : memberShares.entrySet()) {
            Long memberId = entry.getKey();
            BigDecimal assignedAmount = entry.getValue();

            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new ResourceNotFoundException("Member not found with id: " + memberId));

            ExpenseShare share = new ExpenseShare(savedExpense, member, assignedAmount);
            ExpenseShare savedShare = expenseShareRepository.save(share);

            shareResponses.add(new ExpenseShareResponse(
                    savedShare.getId(),
                    member.getId(),
                    member.getName(),
                    savedShare.getAssignedAmount()
            ));
        }

        return new ExpenseResponse(
                savedExpense.getId(),
                group.getId(),
                payer.getId(),
                payer.getName(),
                savedExpense.getDescription(),
                savedExpense.getTotalAmount(),
                savedExpense.getExpenseDate(),
                savedExpense.getSplitMethod(),
                savedExpense.getCreatedAt(),
                shareResponses
        );
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpenseHistory(Long groupId) {
        if (!expenseGroupRepository.existsById(groupId)) {
            throw new ResourceNotFoundException("Group not found with id: " + groupId);
        }

        List<Expense> expenses = expenseRepository.findByGroupIdWithPayerOrderByExpenseDateDescCreatedAtDesc(groupId);
        if (expenses.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> expenseIds = expenses.stream().map(Expense::getId).toList();
        List<ExpenseShare> allShares = expenseShareRepository.findByExpenseIdInWithMember(expenseIds);

        Map<Long, List<ExpenseShareResponse>> sharesByExpenseId = allShares.stream()
                .collect(Collectors.groupingBy(
                        es -> es.getExpense().getId(),
                        Collectors.mapping(
                                es -> new ExpenseShareResponse(
                                        es.getId(),
                                        es.getMember().getId(),
                                        es.getMember().getName(),
                                        es.getAssignedAmount()
                                ),
                                Collectors.toList()
                        )
                ));

        return expenses.stream()
                .map(exp -> new ExpenseResponse(
                        exp.getId(),
                        groupId,
                        exp.getPayer().getId(),
                        exp.getPayer().getName(),
                        exp.getDescription(),
                        exp.getTotalAmount(),
                        exp.getExpenseDate(),
                        exp.getSplitMethod(),
                        exp.getCreatedAt(),
                        sharesByExpenseId.getOrDefault(exp.getId(), Collections.emptyList())
                ))
                .collect(Collectors.toList());
    }

    private Map<Long, BigDecimal> calculateAndValidateShares(Long groupId, BigDecimal totalAmount, CreateExpenseRequest request) {
        if (request.getSplitMethod() == SplitMethod.EQUAL) {
            List<Long> participantIds = extractParticipantIds(request);
            validateParticipants(groupId, participantIds);
            return calculateEqualShares(totalAmount, participantIds);
        } else if (request.getSplitMethod() == SplitMethod.CUSTOM) {
            List<ExpenseShareRequest> customShares = request.getCustomShares();
            if (customShares == null || customShares.isEmpty()) {
                throw new IllegalArgumentException("Custom split requires a non-empty customShares list.");
            }

            List<Long> participantIds = new ArrayList<>();
            for (ExpenseShareRequest shareReq : customShares) {
                if (shareReq.getMemberId() == null) {
                    throw new IllegalArgumentException("Each custom share entry must specify a memberId.");
                }
                participantIds.add(shareReq.getMemberId());
            }
            validateParticipants(groupId, participantIds);

            Map<Long, BigDecimal> result = new LinkedHashMap<>();
            BigDecimal runningSum = BigDecimal.ZERO;

            for (ExpenseShareRequest shareReq : customShares) {
                BigDecimal shareAmt = shareReq.getAmount();
                if (shareAmt == null || shareAmt.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("Custom share amount cannot be null or negative for member ID " + shareReq.getMemberId() + ".");
                }
                if (shareAmt.scale() > 2 && shareAmt.stripTrailingZeros().scale() > 2) {
                    throw new IllegalArgumentException("Custom share amount cannot have more than 2 decimal places for member ID " + shareReq.getMemberId() + ".");
                }
                if (shareAmt.compareTo(MAX_AMOUNT) > 0) {
                    throw new IllegalArgumentException("Custom share amount exceeds maximum allowable limit for member ID " + shareReq.getMemberId() + ".");
                }
                BigDecimal scaledAmt = shareAmt.setScale(2, RoundingMode.HALF_UP);
                runningSum = runningSum.add(scaledAmt);
                result.put(shareReq.getMemberId(), scaledAmt);
            }

            if (runningSum.compareTo(totalAmount) != 0) {
                throw new IllegalArgumentException(
                        "Sum of custom shares (" + runningSum + ") must exactly equal total amount (" + totalAmount + ")."
                );
            }

            return result;
        } else {
            throw new IllegalArgumentException("Unsupported split method: " + request.getSplitMethod());
        }
    }

    private List<Long> extractParticipantIds(CreateExpenseRequest request) {
        if (request.getParticipantMemberIds() != null && !request.getParticipantMemberIds().isEmpty()) {
            return request.getParticipantMemberIds();
        }
        if (request.getCustomShares() != null && !request.getCustomShares().isEmpty()) {
            return request.getCustomShares().stream()
                    .map(ExpenseShareRequest::getMemberId)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private void validateParticipants(Long groupId, List<Long> participantIds) {
        if (participantIds == null || participantIds.isEmpty()) {
            throw new IllegalArgumentException("Participant list cannot be empty.");
        }

        Set<Long> uniqueIds = new HashSet<>();
        for (Long memberId : participantIds) {
            if (memberId == null) {
                throw new IllegalArgumentException("Participant member ID cannot be null.");
            }
            if (!uniqueIds.add(memberId)) {
                throw new IllegalArgumentException("Duplicate participant ID found in split: " + memberId);
            }
            if (!groupMemberRepository.existsByGroupIdAndMemberId(groupId, memberId)) {
                throw new IllegalArgumentException(
                        "Participant member with ID " + memberId + " does not belong to group with ID " + groupId + "."
                );
            }
        }
    }

    private Map<Long, BigDecimal> calculateEqualShares(BigDecimal totalAmount, List<Long> participantIds) {
        // Sort member IDs deterministically ascending
        List<Long> sortedIds = participantIds.stream().sorted().toList();
        int n = sortedIds.size();

        long totalPaise = totalAmount.multiply(new BigDecimal("100")).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
        long basePaise = totalPaise / n;
        long remainderPaise = totalPaise % n;

        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            Long memberId = sortedIds.get(i);
            long memberPaise = basePaise + (i < remainderPaise ? 1 : 0);
            BigDecimal shareAmount = BigDecimal.valueOf(memberPaise).divide(new BigDecimal("100"), 2, RoundingMode.UNNECESSARY);
            result.put(memberId, shareAmount);
        }

        return result;
    }
}
