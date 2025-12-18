package com.openledger.institutions.bk.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openledger.blockchain.client.FabricGatewayService;
import org.hyperledger.fabric.client.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for Bank of Kigali operations.
 * Obtains specific Contract from GatewayRegistry to query the ledger.
 */
@Service
public class BankService {

    private static final Logger logger = LoggerFactory.getLogger(BankService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final FabricGatewayService fabricGatewayService;
    private final ObjectMapper objectMapper;

    @Value("${institutions.bk.org:BankOfKigaliMSP}")
    private String bkOrgId;

    @Value("${institutions.bk.user:admin}")
    private String bkUserId;

    @Value("${institutions.bk.peer:peer0}")
    private String bkPeerId;

    @Value("${institutions.bk.channel:openledger-channel}")
    private String bkChannel;

    @Value("${institutions.bk.chaincode:openledger}")
    private String bkChaincode;

    public BankService(FabricGatewayService fabricGatewayService) {
        this.fabricGatewayService = fabricGatewayService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Get transactions from Bank of Kigali on the blockchain.
     */
    public Map<String, Object> getTransactions(LocalDate fromDate, LocalDate toDate, Integer limit, Integer offset) {
        try {
            Contract contract = getContract();

            String fromDateStr = fromDate != null ? fromDate.format(DATE_FORMATTER) : "";
            String toDateStr = toDate != null ? toDate.format(DATE_FORMATTER) : "";

            byte[] result = contract.evaluateTransaction(
                "queryTransactions",
                bkOrgId,
                fromDateStr,
                toDateStr,
                limit.toString(),
                offset.toString()
            );

            return parseResponse(result);

        } catch (Exception e) {
            logger.error("Error fetching BK transactions", e);
            throw new RuntimeException("Failed to fetch transactions", e);
        }
    }

    /**
     * Get account balances from Bank of Kigali on the blockchain.
     */
    public Map<String, Object> getBalances(String accountId) {
        try {
            Contract contract = getContract();

            byte[] result = contract.evaluateTransaction(
                "queryBalances",
                bkOrgId,
                accountId != null ? accountId : ""
            );

            return parseResponse(result);

        } catch (Exception e) {
            logger.error("Error fetching BK balances", e);
            throw new RuntimeException("Failed to fetch balances", e);
        }
    }

    /**
     * Get specific transaction by ID.
     */
    public Map<String, Object> getTransactionById(String transactionId) {
        try {
            Contract contract = getContract();

            byte[] result = contract.evaluateTransaction(
                "queryTransactionById",
                bkOrgId,
                transactionId
            );

            return parseResponse(result);

        } catch (Exception e) {
            logger.error("Error fetching BK transaction {}", transactionId, e);
            throw new RuntimeException("Failed to fetch transaction: " + transactionId, e);
        }
    }

    /**
     * Get customer information from the blockchain.
     */
    public Map<String, Object> getCustomer(String customerId) {
        try {
            Contract contract = getContract();

            byte[] result = contract.evaluateTransaction(
                "queryCustomer",
                bkOrgId,
                customerId
            );

            return parseResponse(result);

        } catch (Exception e) {
            logger.error("Error fetching BK customer {}", customerId, e);
            throw new RuntimeException("Failed to fetch customer: " + customerId, e);
        }
    }

    /**
     * Create a new transaction on the blockchain.
     */
    public Map<String, Object> createTransaction(Map<String, Object> transactionData) {
        try {
            Contract contract = getContract();
            String transactionJson = objectMapper.writeValueAsString(transactionData);

            byte[] result = contract.submitTransaction(
                "createTransaction",
                bkOrgId,
                transactionJson,
                String.valueOf(System.currentTimeMillis())
            );

            Map<String, Object> response = parseResponse(result);
            logger.info("Transaction created successfully: {}", response);

            return response;

        } catch (Exception e) {
            logger.error("Error creating BK transaction", e);
            throw new RuntimeException("Failed to create transaction", e);
        }
    }

    /**
     * Get account transaction history.
     */
    public Map<String, Object> getAccountTransactions(String accountId, LocalDate fromDate, LocalDate toDate, Integer limit) {
        try {
            Contract contract = getContract();

            String fromDateStr = fromDate != null ? fromDate.format(DATE_FORMATTER) : "";
            String toDateStr = toDate != null ? toDate.format(DATE_FORMATTER) : "";

            byte[] result = contract.evaluateTransaction(
                "queryAccountTransactions",
                bkOrgId,
                accountId,
                fromDateStr,
                toDateStr,
                limit.toString()
            );

            return parseResponse(result);

        } catch (Exception e) {
            logger.error("Error fetching transactions for account {}", accountId, e);
            throw new RuntimeException("Failed to fetch account transactions: " + accountId, e);
        }
    }

    /**
     * Get network status and connectivity information.
     */
    public Map<String, Object> getNetworkStatus() {
        Map<String, Object> status = new HashMap<>();

        try {
            Contract contract = getContract();

            // Test blockchain connectivity
            byte[] result = contract.evaluateTransaction("ping");
            boolean isConnected = result != null;

            status.put("blockchain", Map.of(
                "connected", isConnected,
                "org", bkOrgId,
                "peer", bkPeerId,
                "channel", bkChannel,
                "chaincode", bkChaincode
            ));

            // Get blockchain info
            if (isConnected) {
                byte[] infoResult = contract.evaluateTransaction("getNetworkInfo", bkOrgId);
                Map<String, Object> networkInfo = parseResponse(infoResult);
                status.put("networkInfo", networkInfo);
            }

        } catch (Exception e) {
            logger.error("Error checking BK network status", e);
            status.put("blockchain", Map.of(
                "connected", false,
                "error", e.getMessage()
            ));
        }

        status.put("timestamp", System.currentTimeMillis());
        return status;
    }

    /**
     * Get the Fabric contract for Bank of Kigali operations.
     */
    private Contract getContract() {
        return fabricGatewayService.getContract(bkOrgId, bkUserId, bkPeerId, bkChannel, bkChaincode);
    }

    /**
     * Parse blockchain response bytes to Map.
     */
    private Map<String, Object> parseResponse(byte[] response) {
        try {
            if (response == null || response.length == 0) {
                return Map.of("data", "No data returned");
            }

            String responseStr = new String(response);
            return objectMapper.readValue(responseStr, new TypeReference<Map<String, Object>>() {});

        } catch (Exception e) {
            logger.warn("Failed to parse response as JSON, returning as string", e);
            return Map.of("data", new String(response));
        }
    }
}
