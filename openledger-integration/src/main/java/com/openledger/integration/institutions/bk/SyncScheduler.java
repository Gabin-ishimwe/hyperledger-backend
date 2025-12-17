package com.openledger.integration.institutions.bk;

import com.openledger.common.fabric.registry.GatewayRegistry;
import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Scheduler for synchronizing Bank of Kigali data with the blockchain ledger.
 * Periodically triggers pull/map/ledger-write flow.
 */
@Component
public class SyncScheduler {

    private static final Logger logger = LoggerFactory.getLogger(SyncScheduler.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final BankConnector bankConnector;
    private final BankMapper bankMapper;
    private final GatewayRegistry gatewayRegistry;

    @Value("${institutions.bk.org:BankOfKigaliMSP}")
    private String bkOrgId;

    @Value("${institutions.bk.user:admin}")
    private String bkUserId;

    @Value("${institutions.bk.peer:peer0.bk.openledger.com}")
    private String bkPeerId;

    @Value("${institutions.bk.peer.endpoint:grpcs://peer0.bk.openledger.com:7051}")
    private String bkPeerEndpoint;

    @Value("${institutions.bk.channel:openledger-channel}")
    private String bkChannel;

    @Value("${institutions.bk.chaincode:openledger}")
    private String bkChaincode;

    public SyncScheduler(BankConnector bankConnector, 
                        BankMapper bankMapper,
                        GatewayRegistry gatewayRegistry) {
        this.bankConnector = bankConnector;
        this.bankMapper = bankMapper;
        this.gatewayRegistry = gatewayRegistry;
    }

    /**
     * Scheduled task to sync transactions.
     * Runs every 5 minutes.
     */
    @Scheduled(fixedDelayString = "${sync.bk.transactions.interval:300000}")
    public void syncTransactions() {
        logger.info("Starting BK transaction sync...");
        
        try {
            // Calculate date range (last 24 hours)
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime yesterday = now.minusDays(1);
            
            String fromDate = yesterday.format(DATE_FORMATTER);
            String toDate = now.format(DATE_FORMATTER);

            // Fetch from bank API
            String bankTransactions = bankConnector.fetchTransactions(fromDate, toDate, 1000);
            logger.debug("Fetched transactions from BK API: {}", bankTransactions);

            // Transform to ledger format
            String mappedTransactions = bankMapper.mapTransactions(bankTransactions);
            logger.debug("Mapped transactions: {}", mappedTransactions);

            // Write to ledger
            writeToLedger("transactions", mappedTransactions);
            
            logger.info("BK transaction sync completed successfully");
            
        } catch (Exception e) {
            logger.error("Error during BK transaction sync", e);
        }
    }

    /**
     * Scheduled task to sync account balances.
     * Runs every 15 minutes.
     */
    @Scheduled(fixedDelayString = "${sync.bk.balances.interval:900000}")
    public void syncBalances() {
        logger.info("Starting BK balance sync...");
        
        try {
            // Fetch from bank API
            String bankBalances = bankConnector.fetchAccountBalances(null);
            logger.debug("Fetched balances from BK API: {}", bankBalances);

            // Transform to ledger format
            String mappedBalances = bankMapper.mapBalances(bankBalances);
            logger.debug("Mapped balances: {}", mappedBalances);

            // Write to ledger
            writeToLedger("balances", mappedBalances);
            
            logger.info("BK balance sync completed successfully");
            
        } catch (Exception e) {
            logger.error("Error during BK balance sync", e);
        }
    }

    /**
     * Manual sync trigger for testing purposes.
     */
    public void manualSync() {
        logger.info("Manual BK sync triggered");
        syncTransactions();
        syncBalances();
    }

    /**
     * Write data to the blockchain ledger.
     * 
     * @param dataType Type of data being written (transactions, balances, etc.)
     * @param data JSON data to write
     */
    private void writeToLedger(String dataType, String data) {
        try {
            Network network = gatewayRegistry.getNetwork(bkOrgId, bkUserId, bkPeerId, bkPeerEndpoint, bkChannel);
            Contract contract = network.getContract(bkChaincode);

            // Submit transaction to ledger
            byte[] result = contract.submitTransaction(
                "writeData",
                bkOrgId,
                dataType,
                data,
                String.valueOf(System.currentTimeMillis())
            );

            logger.info("Successfully wrote {} to ledger. Result: {}", dataType, new String(result));

        } catch (Exception e) {
            logger.error("Failed to write {} to ledger", dataType, e);
            throw new RuntimeException("Ledger write failed for " + dataType, e);
        }
    }

    /**
     * Check sync health and connectivity.
     * 
     * @return true if all systems are healthy
     */
    public boolean checkHealth() {
        try {
            // Test bank API connectivity
            boolean bankConnected = bankConnector.testConnection();
            logger.info("Bank API connection: {}", bankConnected ? "OK" : "FAILED");

            // Test ledger connectivity
            Network network = gatewayRegistry.getNetwork(bkOrgId, bkUserId, bkPeerId, bkPeerEndpoint, bkChannel);
            Contract contract = network.getContract(bkChaincode);
            byte[] result = contract.evaluateTransaction("ping");
            boolean ledgerConnected = result != null;
            logger.info("Ledger connection: {}", ledgerConnected ? "OK" : "FAILED");

            return bankConnected && ledgerConnected;

        } catch (Exception e) {
            logger.error("Health check failed", e);
            return false;
        }
    }
}
