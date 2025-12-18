package com.openledger.institutions.common;

/**
 * Base interface for institution API connectors.
 * Defines common operations for connecting to external financial institution APIs.
 *
 * TODO: Implement concrete connectors for each institution (BK, MTN, Equity)
 */
public interface InstitutionConnector {

    /**
     * Get the institution identifier.
     *
     * @return Institution ID (e.g., "BK", "MTN", "EQUITY")
     */
    String getInstitutionId();

    /**
     * Fetch transactions from the institution's API.
     *
     * @param fromDate Start date for query
     * @param toDate End date for query
     * @param limit Maximum number of records
     * @return Raw JSON response from the API
     */
    String fetchTransactions(String fromDate, String toDate, Integer limit);

    /**
     * Fetch account balances from the institution's API.
     *
     * @param accountId Optional specific account ID
     * @return Raw JSON response from the API
     */
    String fetchAccountBalances(String accountId);

    /**
     * Test connectivity to the institution's API.
     *
     * @return true if connection is successful
     */
    boolean testConnection();
}
