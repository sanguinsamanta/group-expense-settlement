package com.groupexpense.settlement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupexpense.settlement.dto.AddMemberRequest;
import com.groupexpense.settlement.dto.CreateExpenseRequest;
import com.groupexpense.settlement.dto.CreateGroupRequest;
import com.groupexpense.settlement.dto.ExpenseShareRequest;
import com.groupexpense.settlement.entity.SplitMethod;
import com.groupexpense.settlement.repository.ExpenseRepository;
import com.groupexpense.settlement.service.ExpenseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExpenseControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private ExpenseService expenseService;

    // Helper method to create a group
    private Long createGroup(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateGroupRequest(name))))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> map = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        return ((Number) map.get("id")).longValue();
    }

    // Helper method to add a member to a group
    private Long addMember(Long groupId, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/groups/{groupId}/members", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddMemberRequest(name))))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> map = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        return ((Number) map.get("id")).longValue();
    }

    @Test
    @DisplayName("Should split ₹100 equally among 3 members with exact paise remainder distribution (33.34, 33.33, 33.33)")
    void testEqualSplit_100AmongThreeMembers_CorrectPaiseRemainder() throws Exception {
        Long groupId = createGroup("Goa Trip");
        Long m1 = addMember(groupId, "Aarav");
        Long m2 = addMember(groupId, "Bhavna");
        Long m3 = addMember(groupId, "Chirag");

        CreateExpenseRequest request = new CreateExpenseRequest(
                "Hotel Room",
                new BigDecimal("100.00"),
                m1,
                LocalDate.of(2026, 10, 24),
                SplitMethod.EQUAL,
                Arrays.asList(m1, m2, m3),
                null
        );

        mockMvc.perform(post("/api/groups/{groupId}/expenses", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.totalAmount", is(100.00)))
                .andExpect(jsonPath("$.splitMethod", is("EQUAL")))
                .andExpect(jsonPath("$.shares", hasSize(3)))
                // Ascending ID order receives remainder paise first
                .andExpect(jsonPath("$.shares[0].assignedAmount", is(33.34)))
                .andExpect(jsonPath("$.shares[1].assignedAmount", is(33.33)))
                .andExpect(jsonPath("$.shares[2].assignedAmount", is(33.33)));
    }

    @Test
    @DisplayName("Should record valid custom split where shares sum exactly to total")
    void testValidCustomSplit() throws Exception {
        Long groupId = createGroup("Dinner Party");
        Long m1 = addMember(groupId, "Deepak");
        Long m2 = addMember(groupId, "Esha");

        List<ExpenseShareRequest> customShares = Arrays.asList(
                new ExpenseShareRequest(m1, new BigDecimal("40.00")),
                new ExpenseShareRequest(m2, new BigDecimal("60.00"))
        );

        CreateExpenseRequest request = new CreateExpenseRequest(
                "Barbeque Feast",
                new BigDecimal("100.00"),
                m1,
                LocalDate.of(2026, 10, 24),
                SplitMethod.CUSTOM,
                null,
                customShares
        );

        mockMvc.perform(post("/api/groups/{groupId}/expenses", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount", is(100.00)))
                .andExpect(jsonPath("$.shares", hasSize(2)))
                .andExpect(jsonPath("$.shares[0].assignedAmount", is(40.00)))
                .andExpect(jsonPath("$.shares[1].assignedAmount", is(60.00)));
    }

    @Test
    @DisplayName("Should return 400 when custom shares sum is less than total amount")
    void testCustomSharesTooSmall_ReturnsBadRequest() throws Exception {
        Long groupId = createGroup("Movie Night");
        Long m1 = addMember(groupId, "Farhan");
        Long m2 = addMember(groupId, "Gita");

        List<ExpenseShareRequest> customShares = Arrays.asList(
                new ExpenseShareRequest(m1, new BigDecimal("40.00")),
                new ExpenseShareRequest(m2, new BigDecimal("50.00")) // Sum = 90.00 != 100.00
        );

        CreateExpenseRequest request = new CreateExpenseRequest(
                "Cinema Tickets",
                new BigDecimal("100.00"),
                m1,
                LocalDate.of(2026, 10, 24),
                SplitMethod.CUSTOM,
                null,
                customShares
        );

        mockMvc.perform(post("/api/groups/{groupId}/expenses", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("must exactly equal total amount")));
    }

    @Test
    @DisplayName("Should return 400 when custom shares sum is greater than total amount")
    void testCustomSharesTooLarge_ReturnsBadRequest() throws Exception {
        Long groupId = createGroup("Camping");
        Long m1 = addMember(groupId, "Harsh");
        Long m2 = addMember(groupId, "Isha");

        List<ExpenseShareRequest> customShares = Arrays.asList(
                new ExpenseShareRequest(m1, new BigDecimal("60.00")),
                new ExpenseShareRequest(m2, new BigDecimal("55.00")) // Sum = 115.00 != 100.00
        );

        CreateExpenseRequest request = new CreateExpenseRequest(
                "Tent Rental",
                new BigDecimal("100.00"),
                m1,
                LocalDate.of(2026, 10, 24),
                SplitMethod.CUSTOM,
                null,
                customShares
        );

        mockMvc.perform(post("/api/groups/{groupId}/expenses", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("must exactly equal total amount")));
    }

    @Test
    @DisplayName("Should return 400 when total amount is zero or negative")
    void testZeroOrNegativeTotal_ReturnsBadRequest() throws Exception {
        Long groupId = createGroup("Test Group");
        Long m1 = addMember(groupId, "Jay");

        CreateExpenseRequest zeroRequest = new CreateExpenseRequest(
                "Zero expense",
                BigDecimal.ZERO,
                m1,
                LocalDate.now(),
                SplitMethod.EQUAL,
                Collections.singletonList(m1),
                null
        );

        mockMvc.perform(post("/api/groups/{groupId}/expenses", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroRequest)))
                .andExpect(status().isBadRequest());

        CreateExpenseRequest negRequest = new CreateExpenseRequest(
                "Negative expense",
                new BigDecimal("-25.00"),
                m1,
                LocalDate.now(),
                SplitMethod.EQUAL,
                Collections.singletonList(m1),
                null
        );

        mockMvc.perform(post("/api/groups/{groupId}/expenses", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when amount has more than two fractional decimal places")
    void testMoreThanTwoDecimalPlaces_ReturnsBadRequest() throws Exception {
        Long groupId = createGroup("Precision Group");
        Long m1 = addMember(groupId, "Karan");

        CreateExpenseRequest request = new CreateExpenseRequest(
                "Three decimals",
                new BigDecimal("10.555"),
                m1,
                LocalDate.now(),
                SplitMethod.EQUAL,
                Collections.singletonList(m1),
                null
        );

        mockMvc.perform(post("/api/groups/{groupId}/expenses", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cannot have more than 2 decimal places")));
    }

    @Test
    @DisplayName("Should return 404 when group is missing")
    void testUnknownGroup_ReturnsNotFound() throws Exception {
        CreateExpenseRequest request = new CreateExpenseRequest(
                "Ghost expense",
                new BigDecimal("50.00"),
                1L,
                LocalDate.now(),
                SplitMethod.EQUAL,
                Collections.singletonList(1L),
                null
        );

        mockMvc.perform(post("/api/groups/999999/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Group not found with id: 999999")));
    }

    @Test
    @DisplayName("Should return 400 when payer is an outsider to the group")
    void testOutsiderPayer_ReturnsBadRequest() throws Exception {
        Long groupA = createGroup("Group A");
        Long groupB = createGroup("Group B");

        Long payerInB = addMember(groupB, "Outsider");
        Long memberInA = addMember(groupA, "Member A");

        CreateExpenseRequest request = new CreateExpenseRequest(
                "Lunch",
                new BigDecimal("50.00"),
                payerInB, // belongs to group B, not group A
                LocalDate.now(),
                SplitMethod.EQUAL,
                Collections.singletonList(memberInA),
                null
        );

        mockMvc.perform(post("/api/groups/{groupId}/expenses", groupA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("does not belong to group")));
    }

    @Test
    @DisplayName("Should return 400 when participant is an outsider to the group")
    void testOutsiderParticipant_ReturnsBadRequest() throws Exception {
        Long groupA = createGroup("Group A");
        Long groupB = createGroup("Group B");

        Long payerInA = addMember(groupA, "Member A");
        Long participantInB = addMember(groupB, "Outsider");

        CreateExpenseRequest request = new CreateExpenseRequest(
                "Snacks",
                new BigDecimal("50.00"),
                payerInA,
                LocalDate.now(),
                SplitMethod.EQUAL,
                Arrays.asList(payerInA, participantInB), // participantInB is not in group A
                null
        );

        mockMvc.perform(post("/api/groups/{groupId}/expenses", groupA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("does not belong to group")));
    }

    @Test
    @DisplayName("Should return 400 when duplicate participant IDs are present in split")
    void testDuplicateParticipantIds_ReturnsBadRequest() throws Exception {
        Long groupId = createGroup("Duplicate Group");
        Long m1 = addMember(groupId, "Meera");

        CreateExpenseRequest request = new CreateExpenseRequest(
                "Coffee",
                new BigDecimal("20.00"),
                m1,
                LocalDate.now(),
                SplitMethod.EQUAL,
                Arrays.asList(m1, m1), // duplicate participant ID
                null
        );

        mockMvc.perform(post("/api/groups/{groupId}/expenses", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Duplicate participant ID found")));
    }

    @Test
    @DisplayName("Should return 400 when participant list is empty")
    void testEmptyParticipantList_ReturnsBadRequest() throws Exception {
        Long groupId = createGroup("Empty Group");
        Long m1 = addMember(groupId, "Nikhil");

        CreateExpenseRequest request = new CreateExpenseRequest(
                "Snacks",
                new BigDecimal("20.00"),
                m1,
                LocalDate.now(),
                SplitMethod.EQUAL,
                Collections.emptyList(),
                null
        );

        mockMvc.perform(post("/api/groups/{groupId}/expenses", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Participant list cannot be empty")));
    }

    @Test
    @DisplayName("Should return 400 when custom share amount is negative")
    void testNegativeCustomShares_ReturnsBadRequest() throws Exception {
        Long groupId = createGroup("Negative Share Group");
        Long m1 = addMember(groupId, "Omkar");
        Long m2 = addMember(groupId, "Pooja");

        List<ExpenseShareRequest> shares = Arrays.asList(
                new ExpenseShareRequest(m1, new BigDecimal("110.00")),
                new ExpenseShareRequest(m2, new BigDecimal("-10.00"))
        );

        CreateExpenseRequest request = new CreateExpenseRequest(
                "Bad Split",
                new BigDecimal("100.00"),
                m1,
                LocalDate.now(),
                SplitMethod.CUSTOM,
                null,
                shares
        );

        mockMvc.perform(post("/api/groups/{groupId}/expenses", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cannot be null or negative")));
    }

    @Test
    @DisplayName("Should succeed when payer is in group but does not participate in the split")
    void testPayerNotParticipatingInSplit_Success() throws Exception {
        Long groupId = createGroup("Gift Contribution");
        Long payer = addMember(groupId, "Quinn");
        Long m1 = addMember(groupId, "Rohan");
        Long m2 = addMember(groupId, "Sana");

        // Quinn pays ₹100, but only Rohan and Sana split it ₹50 each
        CreateExpenseRequest request = new CreateExpenseRequest(
                "Birthday Cake",
                new BigDecimal("100.00"),
                payer,
                LocalDate.of(2026, 10, 24),
                SplitMethod.EQUAL,
                Arrays.asList(m1, m2),
                null
        );

        mockMvc.perform(post("/api/groups/{groupId}/expenses", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payerName", is("Quinn")))
                .andExpect(jsonPath("$.shares", hasSize(2)))
                .andExpect(jsonPath("$.shares[0].memberName", is("Rohan")))
                .andExpect(jsonPath("$.shares[0].assignedAmount", is(50.00)))
                .andExpect(jsonPath("$.shares[1].memberName", is("Sana")))
                .andExpect(jsonPath("$.shares[1].assignedAmount", is(50.00)));
    }

    @Test
    @DisplayName("Should retrieve expense history filtered by group and ordered by expenseDate DESC, createdAt DESC")
    void testExpenseHistory_GroupFilteringAndOrdering() throws Exception {
        Long groupA = createGroup("Flat 1");
        Long groupB = createGroup("Flat 2");

        Long a1 = addMember(groupA, "Tanvi");
        Long b1 = addMember(groupB, "Uday");

        // Create older expense in group A
        mockMvc.perform(post("/api/groups/{groupId}/expenses", groupA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateExpenseRequest(
                        "Groceries Week 1",
                        new BigDecimal("200.00"),
                        a1,
                        LocalDate.of(2026, 10, 10),
                        SplitMethod.EQUAL,
                        Collections.singletonList(a1),
                        null
                )))).andExpect(status().isCreated());

        // Create newer expense in group A
        mockMvc.perform(post("/api/groups/{groupId}/expenses", groupA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateExpenseRequest(
                        "Groceries Week 2",
                        new BigDecimal("350.00"),
                        a1,
                        LocalDate.of(2026, 10, 20),
                        SplitMethod.EQUAL,
                        Collections.singletonList(a1),
                        null
                )))).andExpect(status().isCreated());

        // Create expense in group B (must not appear in group A history)
        mockMvc.perform(post("/api/groups/{groupId}/expenses", groupB)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateExpenseRequest(
                        "Electricity Bill",
                        new BigDecimal("500.00"),
                        b1,
                        LocalDate.of(2026, 10, 15),
                        SplitMethod.EQUAL,
                        Collections.singletonList(b1),
                        null
                )))).andExpect(status().isCreated());

        // Query Group A history
        mockMvc.perform(get("/api/groups/{groupId}/expenses", groupA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].description", is("Groceries Week 2"))) // Newer date first
                .andExpect(jsonPath("$[1].description", is("Groceries Week 1")));

        // Query Group B history
        mockMvc.perform(get("/api/groups/{groupId}/expenses", groupB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].description", is("Electricity Bill")));
    }

    @Test
    @DisplayName("Should rollback expense creation when saving shares fails")
    void testRollbackWhenSavingShareFails() throws Exception {
        Long groupId = createGroup("Rollback Test Group");
        Long m1 = addMember(groupId, "Vikram");

        long initialExpenseCount = expenseRepository.count();

        // Custom split with invalid participant ID causes exception during processing
        CreateExpenseRequest badRequest = new CreateExpenseRequest(
                "Failed Expense",
                new BigDecimal("100.00"),
                m1,
                LocalDate.now(),
                SplitMethod.CUSTOM,
                null,
                Arrays.asList(
                        new ExpenseShareRequest(m1, new BigDecimal("50.00")),
                        new ExpenseShareRequest(999999L, new BigDecimal("50.00")) // non-existent member
                )
        );

        assertThrows(Exception.class, () -> {
            expenseService.recordExpense(groupId, badRequest);
        });

        // Expense count must remain unchanged
        assertEquals(initialExpenseCount, expenseRepository.count(),
                "Expense record must be rolled back when share validation/saving fails.");
    }
}
