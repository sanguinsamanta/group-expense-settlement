package com.groupexpense.settlement.controller;

import com.groupexpense.settlement.dto.MemberBalanceResponse;
import com.groupexpense.settlement.dto.SettlementResponse;
import com.groupexpense.settlement.service.SettlementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/groups/{groupId}")
@CrossOrigin(origins = "*")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @GetMapping("/balances")
    public ResponseEntity<List<MemberBalanceResponse>> getGroupBalances(@PathVariable("groupId") Long groupId) {
        List<MemberBalanceResponse> balances = settlementService.getGroupBalances(groupId);
        return ResponseEntity.ok(balances);
    }

    @GetMapping("/settlements")
    public ResponseEntity<List<SettlementResponse>> getSuggestedSettlements(@PathVariable("groupId") Long groupId) {
        List<SettlementResponse> settlements = settlementService.getSuggestedSettlements(groupId);
        return ResponseEntity.ok(settlements);
    }
}
