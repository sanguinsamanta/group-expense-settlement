package com.groupexpense.settlement.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupexpense.settlement.dto.AddMemberRequest;
import com.groupexpense.settlement.dto.CreateExpenseRequest;
import com.groupexpense.settlement.dto.CreateGroupRequest;
import com.groupexpense.settlement.dto.MemberBalanceResponse;
import com.groupexpense.settlement.dto.SettlementResponse;
import com.groupexpense.settlement.entity.SplitMethod;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SettlementControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long createGroup(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateGroupRequest(name))))
                .andExpect(status().isCreated())
                .andReturn();
        Map<?, ?> map = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        return ((Number) map.get("id")).longValue();
    }

    private Long addMember(Long groupId, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/groups/{groupId}/members", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddMemberRequest(name))))
                .andExpect(status().isCreated())
                .andReturn();
        Map<?, ?> map = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        return ((Number) map.get("id")).longValue();
    }

    private void createEqualExpense(Long groupId, Long payerId, BigDecimal amount, List<Long> participants) throws Exception {
        CreateExpenseRequest request = new CreateExpenseRequest(
                "Expense",
                amount,
                payerId,
                LocalDate.now(),
                SplitMethod.EQUAL,
                participants,
                null
        );
        mockMvc.perform(post("/api/groups/{groupId}/expenses", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private List<MemberBalanceResponse> getBalances(Long groupId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/groups/{groupId}/balances", groupId))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<List<MemberBalanceResponse>>() {});
    }

    private List<SettlementResponse> getSettlements(Long groupId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/groups/{groupId}/settlements", groupId))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<List<SettlementResponse>>() {});
    }

    @Test
    @DisplayName("Should return zero balances and no settlements when a group has no expenses")
    void testNoExpenses_AllBalancesZero_NoSettlements() throws Exception {
        Long groupId = createGroup("Empty Group");
        addMember(groupId, "User A");
        addMember(groupId, "User B");

        List<MemberBalanceResponse> balances = getBalances(groupId);
        assertEquals(2, balances.size());
        for (MemberBalanceResponse b : balances) {
            assertEquals(0, BigDecimal.ZERO.compareTo(b.getTotalPaid()));
            assertEquals(0, BigDecimal.ZERO.compareTo(b.getTotalOwed()));
            assertEquals(0, BigDecimal.ZERO.compareTo(b.getNetBalance()));
        }

        List<SettlementResponse> settlements = getSettlements(groupId);
        assertTrue(settlements.isEmpty(), "No settlements should be emitted when balances are zero.");
    }

    @Test
    @DisplayName("Should calculate correct balances and settlement for a single expense")
    void testSingleExpense_CalculatesBalancesAndSettlement() throws Exception {
        Long groupId = createGroup("Weekend Trip");
        Long m1 = addMember(groupId, "Aman");
        Long m2 = addMember(groupId, "Bina");
        Long m3 = addMember(groupId, "Chetan");

        // Aman pays ₹90 split equally among all 3 (₹30 each)
        createEqualExpense(groupId, m1, new BigDecimal("90.00"), Arrays.asList(m1, m2, m3));

        List<MemberBalanceResponse> balances = getBalances(groupId);
        assertEquals(3, balances.size());

        MemberBalanceResponse b1 = balances.stream().filter(b -> b.getMemberId().equals(m1)).findFirst().orElseThrow();
        MemberBalanceResponse b2 = balances.stream().filter(b -> b.getMemberId().equals(m2)).findFirst().orElseThrow();
        MemberBalanceResponse b3 = balances.stream().filter(b -> b.getMemberId().equals(m3)).findFirst().orElseThrow();

        assertEquals(new BigDecimal("90.00"), b1.getTotalPaid());
        assertEquals(new BigDecimal("30.00"), b1.getTotalOwed());
        assertEquals(new BigDecimal("60.00"), b1.getNetBalance()); // Aman is owed 60

        assertEquals(new BigDecimal("-30.00"), b2.getNetBalance()); // Bina owes 30
        assertEquals(new BigDecimal("-30.00"), b3.getNetBalance()); // Chetan owes 30

        List<SettlementResponse> settlements = getSettlements(groupId);
        assertEquals(2, settlements.size());

        // Verify simulated transfers settle every member's balance to 0.00
        simulateAndVerifyZeroSettlement(balances, settlements);
    }

    @Test
    @DisplayName("Should handle payer who is not participating in the expense split")
    void testPayerNotParticipatingInSplit() throws Exception {
        Long groupId = createGroup("Sponsor Lunch");
        Long sponsor = addMember(groupId, "Sponsor");
        Long eater1 = addMember(groupId, "Eater 1");
        Long eater2 = addMember(groupId, "Eater 2");

        // Sponsor pays ₹100, but only Eater 1 and Eater 2 split it (₹50 each)
        createEqualExpense(groupId, sponsor, new BigDecimal("100.00"), Arrays.asList(eater1, eater2));

        List<MemberBalanceResponse> balances = getBalances(groupId);
        MemberBalanceResponse sponsorBal = balances.stream().filter(b -> b.getMemberId().equals(sponsor)).findFirst().orElseThrow();
        assertEquals(new BigDecimal("100.00"), sponsorBal.getNetBalance());

        List<SettlementResponse> settlements = getSettlements(groupId);
        assertEquals(2, settlements.size());
        simulateAndVerifyZeroSettlement(balances, settlements);
    }

    @Test
    @DisplayName("Should correctly compute balances when multiple members pay and a member both pays and owes")
    void testMultiplePayersAndMemberWhoBothPaysAndOwes() throws Exception {
        Long groupId = createGroup("Flatmates");
        Long m1 = addMember(groupId, "Dev");
        Long m2 = addMember(groupId, "Ena");
        Long m3 = addMember(groupId, "Fox");

        // Dev pays ₹120 for Dev, Ena, Fox (40 each)
        createEqualExpense(groupId, m1, new BigDecimal("120.00"), Arrays.asList(m1, m2, m3));

        // Ena pays ₹60 for Dev, Ena, Fox (20 each)
        createEqualExpense(groupId, m2, new BigDecimal("60.00"), Arrays.asList(m1, m2, m3));

        // Net balances:
        // Dev: +120 - (40 + 20) = +60.00
        // Ena: +60 - (40 + 20) = 0.00 (settled naturally!)
        // Fox: 0 - (40 + 20) = -60.00
        List<MemberBalanceResponse> balances = getBalances(groupId);
        MemberBalanceResponse devBal = balances.stream().filter(b -> b.getMemberId().equals(m1)).findFirst().orElseThrow();
        MemberBalanceResponse enaBal = balances.stream().filter(b -> b.getMemberId().equals(m2)).findFirst().orElseThrow();
        MemberBalanceResponse foxBal = balances.stream().filter(b -> b.getMemberId().equals(m3)).findFirst().orElseThrow();

        assertEquals(new BigDecimal("60.00"), devBal.getNetBalance());
        assertEquals(new BigDecimal("0.00"), enaBal.getNetBalance());
        assertEquals(new BigDecimal("-60.00"), foxBal.getNetBalance());

        // Settlements should only involve Fox paying Dev ₹60.00, Ena is untouched
        List<SettlementResponse> settlements = getSettlements(groupId);
        assertEquals(1, settlements.size());
        assertEquals(m3, settlements.get(0).getFromMemberId());
        assertEquals(m1, settlements.get(0).getToMemberId());
        assertEquals(new BigDecimal("60.00"), settlements.get(0).getAmount());

        simulateAndVerifyZeroSettlement(balances, settlements);
    }

    @Test
    @DisplayName("Should handle multiple debtors and creditors and verify simulation reaches zero")
    void testMultipleDebtorsAndCreditors_SimulateAllSettlementsReachZero() throws Exception {
        Long groupId = createGroup("Complex Settlement");
        Long a = addMember(groupId, "A");
        Long b = addMember(groupId, "B");
        Long c = addMember(groupId, "C");
        Long d = addMember(groupId, "D");

        // A pays ₹300 for A, B, C (₹100 each)
        createEqualExpense(groupId, a, new BigDecimal("300.00"), Arrays.asList(a, b, c));

        // B pays ₹100 for B, D (₹50 each)
        createEqualExpense(groupId, b, new BigDecimal("100.00"), Arrays.asList(b, d));

        // Expected Net:
        // A: +300 - 100 = +200.00
        // B: +100 - (100 + 50) = -50.00
        // C: 0 - 100 = -100.00
        // D: 0 - 50 = -50.00
        List<MemberBalanceResponse> balances = getBalances(groupId);
        List<SettlementResponse> settlements = getSettlements(groupId);

        assertFalse(settlements.isEmpty());
        // Verify no negative or zero transfers
        for (SettlementResponse s : settlements) {
            assertTrue(s.getAmount().compareTo(BigDecimal.ZERO) > 0);
        }

        simulateAndVerifyZeroSettlement(balances, settlements);
    }

    @Test
    @DisplayName("Should include group members with zero balance or no expenses in balance list")
    void testZeroBalanceMembersAndMemberWithNoExpenses() throws Exception {
        Long groupId = createGroup("Inclusive Group");
        Long active1 = addMember(groupId, "Active 1");
        Long active2 = addMember(groupId, "Active 2");
        Long passive = addMember(groupId, "Passive Member");

        createEqualExpense(groupId, active1, new BigDecimal("50.00"), Arrays.asList(active1, active2));

        List<MemberBalanceResponse> balances = getBalances(groupId);
        assertEquals(3, balances.size());

        MemberBalanceResponse passiveBal = balances.stream()
                .filter(b -> b.getMemberId().equals(passive))
                .findFirst().orElseThrow();

        assertEquals(new BigDecimal("0.00"), passiveBal.getTotalPaid());
        assertEquals(new BigDecimal("0.00"), passiveBal.getTotalOwed());
        assertEquals(new BigDecimal("0.00"), passiveBal.getNetBalance());

        List<SettlementResponse> settlements = getSettlements(groupId);
        // Passive member must not be part of any settlement transfers
        for (SettlementResponse s : settlements) {
            assertNotEquals(passive, s.getFromMemberId());
            assertNotEquals(passive, s.getToMemberId());
        }
    }

    @Test
    @DisplayName("Should handle leftover paise splitting without any cash loss or drift")
    void testLeftoverPaise_CalculatesExactPaiseSettlements() throws Exception {
        Long groupId = createGroup("Paise Test");
        Long m1 = addMember(groupId, "M1");
        Long m2 = addMember(groupId, "M2");
        Long m3 = addMember(groupId, "M3");

        // ₹100 split among 3: 33.34, 33.33, 33.33
        createEqualExpense(groupId, m1, new BigDecimal("100.00"), Arrays.asList(m1, m2, m3));

        List<MemberBalanceResponse> balances = getBalances(groupId);
        MemberBalanceResponse b1 = balances.stream().filter(b -> b.getMemberId().equals(m1)).findFirst().orElseThrow();
        assertEquals(new BigDecimal("66.66"), b1.getNetBalance()); // 100 - 33.34 = 66.66

        List<SettlementResponse> settlements = getSettlements(groupId);
        assertEquals(2, settlements.size());

        simulateAndVerifyZeroSettlement(balances, settlements);
    }

    @Test
    @DisplayName("Should isolate balances and settlements between distinct groups")
    void testGroupIsolation() throws Exception {
        Long groupA = createGroup("Group A");
        Long groupB = createGroup("Group B");

        Long a1 = addMember(groupA, "Alice");
        Long a2 = addMember(groupA, "Arthur");
        Long b1 = addMember(groupB, "Bob");

        createEqualExpense(groupA, a1, new BigDecimal("100.00"), Arrays.asList(a1, a2));

        // Group B has no expenses
        List<MemberBalanceResponse> balancesB = getBalances(groupB);
        assertEquals(1, balancesB.size());
        assertEquals(new BigDecimal("0.00"), balancesB.get(0).getNetBalance());

        List<SettlementResponse> settlementsB = getSettlements(groupB);
        assertTrue(settlementsB.isEmpty());
    }

    @Test
    @DisplayName("Should return 404 for nonexistent group on both balances and settlements endpoints")
    void testNonexistentGroup_Returns404() throws Exception {
        mockMvc.perform(get("/api/groups/999999/balances"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));

        mockMvc.perform(get("/api/groups/999999/settlements"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    @DisplayName("Should produce deterministic settlement transfers across repeated calls")
    void testDeterministicOutput() throws Exception {
        Long groupId = createGroup("Deterministic Group");
        Long m1 = addMember(groupId, "P1");
        Long m2 = addMember(groupId, "P2");
        Long m3 = addMember(groupId, "P3");

        createEqualExpense(groupId, m1, new BigDecimal("150.00"), Arrays.asList(m1, m2, m3));

        List<SettlementResponse> run1 = getSettlements(groupId);
        List<SettlementResponse> run2 = getSettlements(groupId);

        assertEquals(run1.size(), run2.size());
        for (int i = 0; i < run1.size(); i++) {
            assertEquals(run1.get(i).getFromMemberId(), run2.get(i).getFromMemberId());
            assertEquals(run1.get(i).getToMemberId(), run2.get(i).getToMemberId());
            assertEquals(run1.get(i).getAmount(), run2.get(i).getAmount());
        }
    }

    /**
     * Independent verification simulation:
     * Takes the computed net balances and applies each suggested transfer (debtor pays creditor).
     * Asserts that after executing all suggested transfers, EVERY member's final balance is exactly 0.00.
     */
    private void simulateAndVerifyZeroSettlement(List<MemberBalanceResponse> balances, List<SettlementResponse> settlements) {
        Map<Long, BigDecimal> simulatedBalances = new HashMap<>();
        for (MemberBalanceResponse b : balances) {
            simulatedBalances.put(b.getMemberId(), b.getNetBalance());
        }

        // Apply every transfer
        for (SettlementResponse s : settlements) {
            assertTrue(s.getAmount().compareTo(BigDecimal.ZERO) > 0,
                    "Transfer amount must be strictly greater than zero");

            Long from = s.getFromMemberId();
            Long to = s.getToMemberId();

            // Debtor pays amount (reduces negative debt towards zero)
            simulatedBalances.put(from, simulatedBalances.get(from).add(s.getAmount()));
            // Creditor receives amount (reduces positive credit towards zero)
            simulatedBalances.put(to, simulatedBalances.get(to).subtract(s.getAmount()));
        }

        // After all transfers, every simulated balance must be 0.00
        for (Map.Entry<Long, BigDecimal> entry : simulatedBalances.entrySet()) {
            assertEquals(0, BigDecimal.ZERO.compareTo(entry.getValue()),
                    "Member ID " + entry.getKey() + " has remaining balance of " + entry.getValue() + " instead of 0.00");
        }
    }
}
