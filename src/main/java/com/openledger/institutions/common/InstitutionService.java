package com.openledger.institutions.common;

import java.time.LocalDate;
import java.util.Map;

/**
 * Base interface for all institution services.
 * Defines common operations that all financial institution integrations must implement.
 *
 * TODO: Implement concrete services for each institution (BK, MTN, Equity)
 */
public interface InstitutionService {

    /**
     * Get the institution identifier.
     *
     * @return Institution ID (e.g., "BK", "MTN", "EQUITY")
     */
    String getInstitutionId();

    /**
     * Get transactions from the blockchain for this institution.
     *
     * @param fromDate Start date for query
     * @param toDate End date for query
     * @param limit Maximum number of records
     * @param offset Pagination offset
     * @return Map containing transaction data
     */
    Map<String, Object> getTransactions(LocalDate fromDate, LocalDate toDate, Integer limit, Integer offset);

    /**
     * Get account balances from the blockchain for this institution.
     *
     * @param accountId Optional specific account ID
     * @return Map containing balance data
     */
    Map<String, Object> getBalances(String accountId);

    /**
     * Get network status and connectivity information.
     *
     * @return Map containing status information
     */
    Map<String, Object> getNetworkStatus();

    /**
     * Check if the institution service is healthy.
     *
     * @return true if healthy, false otherwise
     */
    boolean isHealthy();
}
