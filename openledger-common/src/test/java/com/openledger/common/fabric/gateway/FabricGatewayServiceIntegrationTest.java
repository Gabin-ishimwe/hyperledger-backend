package com.openledger.common.fabric.gateway;

import com.openledger.common.config.FabricGatewayProperties;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.Network;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for FabricGatewayService.
 *
 * These tests require a running Fabric network and crypto service.
 * Run with: mvn test -Dtest=FabricGatewayServiceIntegrationTest
 *
 * Configure the following environment variables or application-test.yml:
 * - CRYPTO_SERVICE_URL: URL of the crypto service
 * - ORG1_PEER_ENDPOINT: Peer endpoint for Org1
 * - FABRIC_CHANNEL: Channel name
 */
@SpringBootTest
@ActiveProfiles("test")
class FabricGatewayServiceIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(FabricGatewayServiceIntegrationTest.class);

    @Autowired
    private FabricGatewayService fabricGatewayService;

    @Autowired
    private FabricGatewayProperties properties;

    private static final String TEST_ORG_MSP_ID = "Org1MSP";
    private static final String TEST_USER_ID = "User1";
    private static final String TEST_PEER_ID = "peer0";
    private static final String TEST_CHAINCODE = "basic";

    @BeforeEach
    void setUp() {
        logger.info("=== Starting FabricGatewayService Integration Test ===");
        logger.info("Test configuration:");
        logger.info("  Org MSP ID: {}", TEST_ORG_MSP_ID);
        logger.info("  User ID: {}", TEST_USER_ID);
        logger.info("  Peer ID: {}", TEST_PEER_ID);
        logger.info("  Default Channel: {}", properties.getDefault().getChannel());
    }

    @AfterEach
    void tearDown() {
        logger.info("=== Cleaning up after test ===");
        fabricGatewayService.closeGateway(TEST_ORG_MSP_ID, TEST_USER_ID, TEST_PEER_ID);
    }

    @Test
    @DisplayName("Should create gateway connection successfully")
    void testGetGateway() {
        logger.info("Testing gateway creation...");

        Gateway gateway = fabricGatewayService.getGateway(TEST_ORG_MSP_ID, TEST_USER_ID, TEST_PEER_ID);

        assertNotNull(gateway, "Gateway should not be null");
        logger.info("Gateway created successfully: {}", gateway);
    }

    @Test
    @DisplayName("Should get network from gateway")
    void testGetNetwork() {
        logger.info("Testing network retrieval...");

        Network network = fabricGatewayService.getNetwork(TEST_ORG_MSP_ID, TEST_USER_ID, TEST_PEER_ID, null);

        assertNotNull(network, "Network should not be null");
        logger.info("Network retrieved successfully: {}", network);
    }

    @Test
    @DisplayName("Should get contract from network")
    void testGetContract() {
        logger.info("Testing contract retrieval...");

        Contract contract = fabricGatewayService.getContract(
                TEST_ORG_MSP_ID, TEST_USER_ID, TEST_PEER_ID, null, TEST_CHAINCODE);

        assertNotNull(contract, "Contract should not be null");
        logger.info("Contract retrieved successfully: {}", contract);
    }

    @Test
    @DisplayName("Should report healthy when connection is valid")
    void testIsHealthy() {
        logger.info("Testing health check...");

        boolean healthy = fabricGatewayService.isHealthy(TEST_ORG_MSP_ID, TEST_USER_ID, TEST_PEER_ID);

        assertTrue(healthy, "Service should be healthy with valid connection");
        logger.info("Health check passed: {}", healthy);
    }

    @Test
    @DisplayName("Should cache gateway instances")
    void testGatewayCaching() {
        logger.info("Testing gateway caching...");

        Gateway gateway1 = fabricGatewayService.getGateway(TEST_ORG_MSP_ID, TEST_USER_ID, TEST_PEER_ID);
        Gateway gateway2 = fabricGatewayService.getGateway(TEST_ORG_MSP_ID, TEST_USER_ID, TEST_PEER_ID);

        assertSame(gateway1, gateway2, "Should return cached gateway instance");
        logger.info("Gateway caching works correctly");
    }

    @Test
    @DisplayName("Should evaluate transaction on chaincode")
    void testEvaluateTransaction() throws Exception {
        logger.info("Testing evaluate transaction...");

        Contract contract = fabricGatewayService.getContract(
                TEST_ORG_MSP_ID, TEST_USER_ID, TEST_PEER_ID, null, TEST_CHAINCODE);

        // Try to query all assets (common function in basic chaincode)
        byte[] result = contract.evaluateTransaction("GetAllAssets");

        assertNotNull(result, "Transaction result should not be null");
        String resultStr = new String(result);
        logger.info("Evaluate transaction result: {}", resultStr);
    }

    @Test
    @DisplayName("Should close gateway and allow recreation")
    void testCloseAndRecreateGateway() {
        logger.info("Testing gateway close and recreation...");

        // Create initial gateway
        Gateway gateway1 = fabricGatewayService.getGateway(TEST_ORG_MSP_ID, TEST_USER_ID, TEST_PEER_ID);
        assertNotNull(gateway1);

        // Close it
        fabricGatewayService.closeGateway(TEST_ORG_MSP_ID, TEST_USER_ID, TEST_PEER_ID);
        logger.info("Gateway closed");

        // Create new gateway
        Gateway gateway2 = fabricGatewayService.getGateway(TEST_ORG_MSP_ID, TEST_USER_ID, TEST_PEER_ID);
        assertNotNull(gateway2);
        assertNotSame(gateway1, gateway2, "Should create new gateway after close");
        logger.info("New gateway created successfully after close");
    }

    @Test
    @DisplayName("Should handle invalid organization gracefully")
    void testInvalidOrganization() {
        logger.info("Testing invalid organization handling...");

        assertThrows(RuntimeException.class, () -> {
            fabricGatewayService.getGateway("InvalidMSP", TEST_USER_ID, TEST_PEER_ID);
        }, "Should throw exception for invalid organization");

        logger.info("Invalid organization handled correctly");
    }

    @Test
    @DisplayName("Should use default peer when peerId is null")
    void testDefaultPeerId() {
        logger.info("Testing default peer ID...");

        Gateway gateway = fabricGatewayService.getGateway(TEST_ORG_MSP_ID, TEST_USER_ID, null);

        assertNotNull(gateway, "Gateway should be created with default peer");
        logger.info("Default peer ID works correctly");
    }
}
