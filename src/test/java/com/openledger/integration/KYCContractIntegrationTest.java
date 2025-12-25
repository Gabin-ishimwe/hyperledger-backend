package com.openledger.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openledger.core.config.FabricGatewayProperties;
import com.openledger.blockchain.client.FabricGatewayService;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.GatewayException;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for KYC Smart Contract.
 *
 * These tests verify the KYC chaincode is working properly on the blockchain network.
 * Run with: mvn test -Dtest=KYCContractIntegrationTest
 *
 * Prerequisites:
 * - Running Fabric network with KYC chaincode deployed
 * - Crypto service running
 * - Proper configuration in application-test.yml
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KYCContractIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(KYCContractIntegrationTest.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private FabricGatewayService fabricGatewayService;

    @Autowired
    private FabricGatewayProperties properties;

    private static final String TEST_ORG_MSP_ID = "Org1MSP";
    private static final String TEST_USER_ID = "User1";
    private static final String TEST_PEER_ID = "peer0";
    private static final String KYC_CHAINCODE = "chaincode-java";
    private static final String KYC_CONTRACT_NAME = "kyc";

    // Test data - using UUID to ensure unique IDs per test run
    private static String testKycId;
    private static final String TEST_FULL_NAME = "John Doe";
    private static final String TEST_DOB = "1990-01-15";
    private static final String TEST_NATIONALITY = "US";
    private static final String TEST_DOCUMENT_TYPE = "Passport";
    private static final String TEST_DOCUMENT_HASH = "QmTestHash123456789abcdef";

    private Contract contract;

    @BeforeAll
    static void initTestData() {
        // Generate unique ID for this test run to avoid conflicts
        testKycId = "KYC_" + UUID.randomUUID().toString().substring(0, 8);
        logger.info("Test KYC ID for this run: {}", testKycId);
    }

    @BeforeEach
    void setUp() {
        logger.info("=== Setting up KYC Contract Integration Test ===");
        logger.info("Test configuration:");
        logger.info("  Org MSP ID: {}", TEST_ORG_MSP_ID);
        logger.info("  User ID: {}", TEST_USER_ID);
        logger.info("  Chaincode: {}", KYC_CHAINCODE);
        logger.info("  Contract: {}", KYC_CONTRACT_NAME);

        // Get the KYC contract - use the named contract within the chaincode
        // The KYC contract is defined with @Contract(name = "kyc") in the chaincode
        contract = fabricGatewayService.getContract(
                TEST_ORG_MSP_ID, TEST_USER_ID, TEST_PEER_ID, null, KYC_CHAINCODE, KYC_CONTRACT_NAME);
        assertNotNull(contract, "Contract should not be null");
    }

    @AfterEach
    void tearDown() {
        logger.info("=== Test completed ===");
    }

    @AfterAll
    static void cleanup() {
        logger.info("=== All KYC Contract tests completed ===");
    }

    // ==================== Network Connectivity Test ====================

    @Test
    @Order(0)
    @DisplayName("Should verify Fabric network is up and running")
    void testNetworkConnectivity() {
        logger.info("Testing Fabric network connectivity...");

        // Test 1: Verify gateway connection is healthy
        boolean isHealthy = fabricGatewayService.isHealthy(TEST_ORG_MSP_ID, TEST_USER_ID, TEST_PEER_ID);
        assertTrue(isHealthy, "Fabric Gateway should be healthy and connected to the network");
        logger.info("  Gateway health check: PASSED");

        // Test 2: Verify we can get a gateway instance
        var gateway = fabricGatewayService.getGateway(TEST_ORG_MSP_ID, TEST_USER_ID, TEST_PEER_ID);
        assertNotNull(gateway, "Gateway instance should not be null");
        logger.info("  Gateway instance: AVAILABLE");

        // Test 3: Verify we can get the network/channel
        var network = fabricGatewayService.getNetwork(TEST_ORG_MSP_ID, TEST_USER_ID, TEST_PEER_ID, null);
        assertNotNull(network, "Network/Channel should be accessible");
        logger.info("  Network/Channel: CONNECTED ({})", properties.getDefault().getChannel());

        // Test 4: Verify we can get the KYC contract
        assertNotNull(contract, "KYC Contract should be accessible");
        logger.info("  KYC Contract: AVAILABLE ({})", KYC_CHAINCODE);

        logger.info("Network connectivity test PASSED - Fabric network is up and running!");
    }

    // ==================== CreateKYC Tests ====================

    @Test
    @Order(1)
    @DisplayName("Should create a new KYC record successfully")
    void testCreateKYC() throws Exception {
        logger.info("Testing CreateKYC with ID: {}", testKycId);

        byte[] result = contract.submitTransaction(
                "CreateKYC",
                testKycId,
                TEST_FULL_NAME,
                TEST_DOB,
                TEST_NATIONALITY,
                TEST_DOCUMENT_TYPE,
                TEST_DOCUMENT_HASH
        );

        assertNotNull(result, "CreateKYC should return a result");
        String resultJson = new String(result, StandardCharsets.UTF_8);
        logger.info("CreateKYC result: {}", resultJson);

        // Parse and validate the response
        JsonNode kycRecord = objectMapper.readTree(resultJson);
        assertEquals(testKycId, kycRecord.get("id").asText(), "ID should match");
        assertEquals(TEST_FULL_NAME, kycRecord.get("fullName").asText(), "Full name should match");
        assertEquals(TEST_DOB, kycRecord.get("dob").asText(), "DOB should match");
        assertEquals(TEST_NATIONALITY, kycRecord.get("nationality").asText(), "Nationality should match");
        assertEquals(TEST_DOCUMENT_TYPE, kycRecord.get("documentType").asText(), "Document type should match");
        assertEquals(TEST_DOCUMENT_HASH, kycRecord.get("documentHash").asText(), "Document hash should match");
        assertEquals("PENDING", kycRecord.get("status").asText(), "Initial status should be PENDING");
        assertNotNull(kycRecord.get("issuerMSP"), "Issuer MSP should be set");
        assertTrue(kycRecord.get("updatedAt").asLong() > 0, "Updated timestamp should be set");

        logger.info("CreateKYC test passed - KYC record created with PENDING status");
    }

    @Test
    @Order(2)
    @DisplayName("Should fail to create duplicate KYC record")
    void testCreateKYC_DuplicateShouldFail() {
        logger.info("Testing CreateKYC with duplicate ID: {}", testKycId);

        GatewayException exception = assertThrows(GatewayException.class, () -> {
            contract.submitTransaction(
                    "CreateKYC",
                    testKycId,
                    "Another Person",
                    "1995-05-20",
                    "UK",
                    "National ID",
                    "QmAnotherHash"
            );
        });

        logger.info("Expected exception thrown: {}", exception.getMessage());
        assertTrue(exception.getMessage().contains("already exists") ||
                        exception.getMessage().contains("KYC_ALREADY_EXISTS"),
                "Exception should indicate duplicate record");

        logger.info("CreateKYC duplicate test passed - correctly rejected duplicate");
    }

    // ==================== KYCExists Tests ====================

    @Test
    @Order(3)
    @DisplayName("Should return true for existing KYC record")
    void testKYCExists_True() throws Exception {
        logger.info("Testing KYCExists for existing ID: {}", testKycId);

        byte[] result = contract.evaluateTransaction("KYCExists", testKycId);

        String resultStr = new String(result, StandardCharsets.UTF_8);
        logger.info("KYCExists result: {}", resultStr);

        assertEquals("true", resultStr.toLowerCase(), "KYCExists should return true for existing record");
        logger.info("KYCExists test passed - record exists");
    }

    @Test
    @Order(4)
    @DisplayName("Should return false for non-existing KYC record")
    void testKYCExists_False() throws Exception {
        String nonExistentId = "KYC_NONEXISTENT_" + UUID.randomUUID().toString().substring(0, 8);
        logger.info("Testing KYCExists for non-existing ID: {}", nonExistentId);

        byte[] result = contract.evaluateTransaction("KYCExists", nonExistentId);

        String resultStr = new String(result, StandardCharsets.UTF_8);
        logger.info("KYCExists result: {}", resultStr);

        assertEquals("false", resultStr.toLowerCase(), "KYCExists should return false for non-existing record");
        logger.info("KYCExists test passed - record does not exist");
    }

    // ==================== GetKYC Tests ====================

    @Test
    @Order(5)
    @DisplayName("Should retrieve existing KYC record")
    void testGetKYC() throws Exception {
        logger.info("Testing GetKYC for ID: {}", testKycId);

        byte[] result = contract.evaluateTransaction("GetKYC", testKycId);

        assertNotNull(result, "GetKYC should return a result");
        String resultJson = new String(result, StandardCharsets.UTF_8);
        logger.info("GetKYC result: {}", resultJson);

        JsonNode kycRecord = objectMapper.readTree(resultJson);
        assertEquals(testKycId, kycRecord.get("id").asText(), "ID should match");
        assertEquals(TEST_FULL_NAME, kycRecord.get("fullName").asText(), "Full name should match");
        assertEquals("PENDING", kycRecord.get("status").asText(), "Status should still be PENDING");

        logger.info("GetKYC test passed - record retrieved successfully");
    }

    @Test
    @Order(6)
    @DisplayName("Should fail to get non-existing KYC record")
    void testGetKYC_NotFound() {
        String nonExistentId = "KYC_NOTFOUND_" + UUID.randomUUID().toString().substring(0, 8);
        logger.info("Testing GetKYC for non-existing ID: {}", nonExistentId);

        GatewayException exception = assertThrows(GatewayException.class, () -> {
            contract.evaluateTransaction("GetKYC", nonExistentId);
        });

        logger.info("Expected exception thrown: {}", exception.getMessage());
        assertTrue(exception.getMessage().contains("does not exist") ||
                        exception.getMessage().contains("KYC_NOT_FOUND"),
                "Exception should indicate record not found");

        logger.info("GetKYC not found test passed - correctly threw exception");
    }

    // ==================== VerifyKYC Tests ====================

    @Test
    @Order(7)
    @DisplayName("Should approve KYC record successfully")
    void testVerifyKYC_Approve() throws Exception {
        // Create a new KYC record for approval test
        String approveTestId = "KYC_APPROVE_" + UUID.randomUUID().toString().substring(0, 8);
        logger.info("Creating KYC record for approval test: {}", approveTestId);

        // First create the record
        contract.submitTransaction(
                "CreateKYC",
                approveTestId,
                "Jane Smith",
                "1985-03-20",
                "CA",
                "Driver's License",
                "QmApproveTestHash"
        );

        // Now approve it
        logger.info("Testing VerifyKYC APPROVE for ID: {}", approveTestId);
        byte[] result = contract.submitTransaction("VerifyKYC", approveTestId, "APPROVE");

        String resultJson = new String(result, StandardCharsets.UTF_8);
        logger.info("VerifyKYC APPROVE result: {}", resultJson);

        JsonNode kycRecord = objectMapper.readTree(resultJson);
        assertEquals(approveTestId, kycRecord.get("id").asText(), "ID should match");
        assertEquals("VERIFIED", kycRecord.get("status").asText(), "Status should be VERIFIED after approval");

        logger.info("VerifyKYC APPROVE test passed - status changed to VERIFIED");
    }

    @Test
    @Order(8)
    @DisplayName("Should reject KYC record successfully")
    void testVerifyKYC_Reject() throws Exception {
        // Create a new KYC record for rejection test
        String rejectTestId = "KYC_REJECT_" + UUID.randomUUID().toString().substring(0, 8);
        logger.info("Creating KYC record for rejection test: {}", rejectTestId);

        // First create the record
        contract.submitTransaction(
                "CreateKYC",
                rejectTestId,
                "Bob Johnson",
                "1992-07-10",
                "UK",
                "National ID",
                "QmRejectTestHash"
        );

        // Now reject it
        logger.info("Testing VerifyKYC REJECT for ID: {}", rejectTestId);
        byte[] result = contract.submitTransaction("VerifyKYC", rejectTestId, "REJECT");

        String resultJson = new String(result, StandardCharsets.UTF_8);
        logger.info("VerifyKYC REJECT result: {}", resultJson);

        JsonNode kycRecord = objectMapper.readTree(resultJson);
        assertEquals(rejectTestId, kycRecord.get("id").asText(), "ID should match");
        assertEquals("REJECTED", kycRecord.get("status").asText(), "Status should be REJECTED after rejection");

        logger.info("VerifyKYC REJECT test passed - status changed to REJECTED");
    }

    @Test
    @Order(9)
    @DisplayName("Should fail with invalid decision")
    void testVerifyKYC_InvalidDecision() throws Exception {
        logger.info("Testing VerifyKYC with invalid decision for ID: {}", testKycId);

        GatewayException exception = assertThrows(GatewayException.class, () -> {
            contract.submitTransaction("VerifyKYC", testKycId, "INVALID_DECISION");
        });

        logger.info("Expected exception thrown: {}", exception.getMessage());
        assertTrue(exception.getMessage().contains("Invalid decision") ||
                        exception.getMessage().contains("INVALID_DECISION") ||
                        exception.getMessage().contains("APPROVE or REJECT"),
                "Exception should indicate invalid decision");

        logger.info("VerifyKYC invalid decision test passed - correctly rejected invalid decision");
    }

    @Test
    @Order(10)
    @DisplayName("Should fail to verify non-existing KYC record")
    void testVerifyKYC_NotFound() {
        String nonExistentId = "KYC_VERIFY_NOTFOUND_" + UUID.randomUUID().toString().substring(0, 8);
        logger.info("Testing VerifyKYC for non-existing ID: {}", nonExistentId);

        GatewayException exception = assertThrows(GatewayException.class, () -> {
            contract.submitTransaction("VerifyKYC", nonExistentId, "APPROVE");
        });

        logger.info("Expected exception thrown: {}", exception.getMessage());
        assertTrue(exception.getMessage().contains("does not exist") ||
                        exception.getMessage().contains("KYC_NOT_FOUND"),
                "Exception should indicate record not found");

        logger.info("VerifyKYC not found test passed - correctly threw exception");
    }

    // ==================== ExpireKYC Tests ====================

    @Test
    @Order(11)
    @DisplayName("Should expire KYC record successfully")
    void testExpireKYC() throws Exception {
        // Create a new KYC record for expiration test
        String expireTestId = "KYC_EXPIRE_" + UUID.randomUUID().toString().substring(0, 8);
        logger.info("Creating KYC record for expiration test: {}", expireTestId);

        // First create and verify the record
        contract.submitTransaction(
                "CreateKYC",
                expireTestId,
                "Alice Brown",
                "1988-11-25",
                "AU",
                "Passport",
                "QmExpireTestHash"
        );

        // Approve it first
        contract.submitTransaction("VerifyKYC", expireTestId, "APPROVE");

        // Now expire it
        logger.info("Testing ExpireKYC for ID: {}", expireTestId);
        byte[] result = contract.submitTransaction("ExpireKYC", expireTestId);

        String resultJson = new String(result, StandardCharsets.UTF_8);
        logger.info("ExpireKYC result: {}", resultJson);

        JsonNode kycRecord = objectMapper.readTree(resultJson);
        assertEquals(expireTestId, kycRecord.get("id").asText(), "ID should match");
        assertEquals("EXPIRED", kycRecord.get("status").asText(), "Status should be EXPIRED");

        logger.info("ExpireKYC test passed - status changed to EXPIRED");
    }

    @Test
    @Order(12)
    @DisplayName("Should fail to expire non-existing KYC record")
    void testExpireKYC_NotFound() {
        String nonExistentId = "KYC_EXPIRE_NOTFOUND_" + UUID.randomUUID().toString().substring(0, 8);
        logger.info("Testing ExpireKYC for non-existing ID: {}", nonExistentId);

        GatewayException exception = assertThrows(GatewayException.class, () -> {
            contract.submitTransaction("ExpireKYC", nonExistentId);
        });

        logger.info("Expected exception thrown: {}", exception.getMessage());
        assertTrue(exception.getMessage().contains("does not exist") ||
                        exception.getMessage().contains("KYC_NOT_FOUND"),
                "Exception should indicate record not found");

        logger.info("ExpireKYC not found test passed - correctly threw exception");
    }

    // ==================== GetKYCHistory Tests ====================

    @Test
    @Order(13)
    @DisplayName("Should retrieve KYC history with multiple entries")
    void testGetKYCHistory() throws Exception {
        // Create a new KYC record with full lifecycle for history test
        String historyTestId = "KYC_HISTORY_" + UUID.randomUUID().toString().substring(0, 8);
        logger.info("Creating KYC record for history test: {}", historyTestId);

        // Step 1: Create the record (PENDING)
        contract.submitTransaction(
                "CreateKYC",
                historyTestId,
                "Charlie Wilson",
                "1995-02-14",
                "NZ",
                "Passport",
                "QmHistoryTestHash"
        );

        // Step 2: Approve the record (VERIFIED)
        contract.submitTransaction("VerifyKYC", historyTestId, "APPROVE");

        // Step 3: Expire the record (EXPIRED)
        contract.submitTransaction("ExpireKYC", historyTestId);

        // Now get the history
        logger.info("Testing GetKYCHistory for ID: {}", historyTestId);
        byte[] result = contract.evaluateTransaction("GetKYCHistory", historyTestId);

        assertNotNull(result, "GetKYCHistory should return a result");
        String resultJson = new String(result, StandardCharsets.UTF_8);
        logger.info("GetKYCHistory result: {}", resultJson);

        JsonNode historyArray = objectMapper.readTree(resultJson);
        assertTrue(historyArray.isArray(), "Result should be an array");
        assertTrue(historyArray.size() >= 3, "History should have at least 3 entries (create, verify, expire)");

        // Verify history entries contain expected data
        for (JsonNode entry : historyArray) {
            assertNotNull(entry.get("txId"), "Each history entry should have txId");
            assertTrue(entry.get("timestamp").asLong() > 0, "Each entry should have timestamp");
            assertFalse(entry.get("isDeleted").asBoolean(), "Entries should not be deleted");
            assertNotNull(entry.get("record"), "Each entry should have record data");
        }

        logger.info("GetKYCHistory test passed - {} history entries found", historyArray.size());
    }

    @Test
    @Order(14)
    @DisplayName("Should fail to get history for non-existing KYC record")
    void testGetKYCHistory_NotFound() {
        String nonExistentId = "KYC_HISTORY_NOTFOUND_" + UUID.randomUUID().toString().substring(0, 8);
        logger.info("Testing GetKYCHistory for non-existing ID: {}", nonExistentId);

        GatewayException exception = assertThrows(GatewayException.class, () -> {
            contract.evaluateTransaction("GetKYCHistory", nonExistentId);
        });

        logger.info("Expected exception thrown: {}", exception.getMessage());
        assertTrue(exception.getMessage().contains("does not exist") ||
                        exception.getMessage().contains("KYC_NOT_FOUND"),
                "Exception should indicate record not found");

        logger.info("GetKYCHistory not found test passed - correctly threw exception");
    }

    // ==================== Full Lifecycle Test ====================

    @Test
    @Order(15)
    @DisplayName("Should handle complete KYC lifecycle: Create -> Verify -> Expire")
    void testCompleteKYCLifecycle() throws Exception {
        String lifecycleId = "KYC_LIFECYCLE_" + UUID.randomUUID().toString().substring(0, 8);
        logger.info("Testing complete KYC lifecycle with ID: {}", lifecycleId);

        // Step 1: Create KYC record
        logger.info("Step 1: Creating KYC record");
        byte[] createResult = contract.submitTransaction(
                "CreateKYC",
                lifecycleId,
                "Lifecycle Test User",
                "2000-06-15",
                "DE",
                "National ID",
                "QmLifecycleHash"
        );
        JsonNode createdRecord = objectMapper.readTree(new String(createResult, StandardCharsets.UTF_8));
        assertEquals("PENDING", createdRecord.get("status").asText(), "Initial status should be PENDING");
        logger.info("  Created with status: PENDING");

        // Step 2: Verify the record exists
        logger.info("Step 2: Verifying record exists");
        byte[] existsResult = contract.evaluateTransaction("KYCExists", lifecycleId);
        assertEquals("true", new String(existsResult, StandardCharsets.UTF_8).toLowerCase());
        logger.info("  Record exists: true");

        // Step 3: Get the record
        logger.info("Step 3: Retrieving record");
        byte[] getResult = contract.evaluateTransaction("GetKYC", lifecycleId);
        JsonNode retrievedRecord = objectMapper.readTree(new String(getResult, StandardCharsets.UTF_8));
        assertEquals(lifecycleId, retrievedRecord.get("id").asText());
        assertEquals("PENDING", retrievedRecord.get("status").asText());
        logger.info("  Retrieved record with status: PENDING");

        // Step 4: Approve the record
        logger.info("Step 4: Approving record");
        byte[] verifyResult = contract.submitTransaction("VerifyKYC", lifecycleId, "APPROVE");
        JsonNode verifiedRecord = objectMapper.readTree(new String(verifyResult, StandardCharsets.UTF_8));
        assertEquals("VERIFIED", verifiedRecord.get("status").asText(), "Status should be VERIFIED after approval");
        logger.info("  Approved with status: VERIFIED");

        // Step 5: Expire the record
        logger.info("Step 5: Expiring record");
        byte[] expireResult = contract.submitTransaction("ExpireKYC", lifecycleId);
        JsonNode expiredRecord = objectMapper.readTree(new String(expireResult, StandardCharsets.UTF_8));
        assertEquals("EXPIRED", expiredRecord.get("status").asText(), "Status should be EXPIRED");
        logger.info("  Expired with status: EXPIRED");

        // Step 6: Get history
        logger.info("Step 6: Retrieving history");
        byte[] historyResult = contract.evaluateTransaction("GetKYCHistory", lifecycleId);
        JsonNode history = objectMapper.readTree(new String(historyResult, StandardCharsets.UTF_8));
        assertTrue(history.isArray() && history.size() >= 3, "History should have at least 3 entries");
        logger.info("  History entries: {}", history.size());

        logger.info("Complete KYC lifecycle test passed!");
    }
}
