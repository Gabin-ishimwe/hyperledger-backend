package com.openledger.api.controller;

import com.openledger.institutions.bk.service.BankService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * REST controller for Bank of Kigali operations.
 * Exposes endpoints for querying blockchain data related to BK.
 */
@RestController
@RequestMapping("/api/v1/bk")
@Tag(name = "Bank of Kigali", description = "Bank of Kigali blockchain operations")
@Validated
public class BankController {

    private final BankService bankService;

    public BankController(BankService bankService) {
        this.bankService = bankService;
    }

    /**
     * Get transactions for Bank of Kigali.
     */
    @GetMapping("/transactions")
    @Operation(summary = "Get BK transactions", description = "Retrieve transactions from Bank of Kigali on the blockchain")
    public ResponseEntity<Map<String, Object>> getTransactions(
            @Parameter(description = "Start date for transaction query")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

            @Parameter(description = "End date for transaction query")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,

            @Parameter(description = "Maximum number of transactions")
            @RequestParam(defaultValue = "100") Integer limit,

            @Parameter(description = "Offset for pagination")
            @RequestParam(defaultValue = "0") Integer offset) {

        Map<String, Object> transactions = bankService.getTransactions(fromDate, toDate, limit, offset);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Get account balances for Bank of Kigali.
     */
    @GetMapping("/balances")
    @Operation(summary = "Get BK account balances", description = "Retrieve account balances from Bank of Kigali on the blockchain")
    public ResponseEntity<Map<String, Object>> getBalances(
            @Parameter(description = "Specific account ID")
            @RequestParam(required = false) String accountId) {

        Map<String, Object> balances = bankService.getBalances(accountId);
        return ResponseEntity.ok(balances);
    }

    /**
     * Get specific transaction by ID.
     */
    @GetMapping("/transactions/{transactionId}")
    @Operation(summary = "Get BK transaction by ID", description = "Retrieve a specific transaction from Bank of Kigali")
    public ResponseEntity<Map<String, Object>> getTransaction(
            @Parameter(description = "Transaction ID")
            @PathVariable @NotBlank String transactionId) {

        Map<String, Object> transaction = bankService.getTransactionById(transactionId);
        return ResponseEntity.ok(transaction);
    }

    /**
     * Get customer information.
     */
    @GetMapping("/customers/{customerId}")
    @Operation(summary = "Get BK customer", description = "Retrieve customer information from Bank of Kigali")
    public ResponseEntity<Map<String, Object>> getCustomer(
            @Parameter(description = "Customer ID")
            @PathVariable @NotBlank String customerId) {

        Map<String, Object> customer = bankService.getCustomer(customerId);
        return ResponseEntity.ok(customer);
    }

    /**
     * Create a new transaction.
     */
    @PostMapping("/transactions")
    @Operation(summary = "Create BK transaction", description = "Submit a new transaction to Bank of Kigali blockchain")
    public ResponseEntity<Map<String, Object>> createTransaction(
            @RequestBody Map<String, Object> transactionData) {

        Map<String, Object> result = bankService.createTransaction(transactionData);
        return ResponseEntity.ok(result);
    }

    /**
     * Get blockchain network status for BK.
     */
    @GetMapping("/status")
    @Operation(summary = "Get BK network status", description = "Check the status of Bank of Kigali blockchain connection")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = bankService.getNetworkStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * Get transaction history for a specific account.
     */
    @GetMapping("/accounts/{accountId}/transactions")
    @Operation(summary = "Get account transaction history", description = "Retrieve transaction history for a specific BK account")
    public ResponseEntity<Map<String, Object>> getAccountTransactions(
            @Parameter(description = "Account ID")
            @PathVariable @NotBlank String accountId,

            @Parameter(description = "Start date for query")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

            @Parameter(description = "End date for query")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,

            @Parameter(description = "Maximum number of transactions")
            @RequestParam(defaultValue = "50") Integer limit) {

        Map<String, Object> transactions = bankService.getAccountTransactions(accountId, fromDate, toDate, limit);
        return ResponseEntity.ok(transactions);
    }
}
