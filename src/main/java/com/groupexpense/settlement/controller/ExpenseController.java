package com.groupexpense.settlement.controller;

import com.groupexpense.settlement.dto.CreateExpenseRequest;
import com.groupexpense.settlement.dto.ExpenseResponse;
import com.groupexpense.settlement.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/groups/{groupId}/expenses")
@CrossOrigin(origins = "*")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> recordExpense(
            @PathVariable("groupId") Long groupId,
            @Valid @RequestBody CreateExpenseRequest request) {
        ExpenseResponse response = expenseService.recordExpense(groupId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ExpenseResponse>> getExpenseHistory(@PathVariable("groupId") Long groupId) {
        List<ExpenseResponse> response = expenseService.getExpenseHistory(groupId);
        return ResponseEntity.ok(response);
    }
}
