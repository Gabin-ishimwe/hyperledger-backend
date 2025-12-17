package com.openledger.integration.institutions.bk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Connector for Bank of Kigali APIs.
 * Handles external API calls to fetch bank data.
 */
@Component
public class BankConnector {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String bankApiUrl;
    private final String apiKey;

    public BankConnector(RestClient.Builder restClientBuilder,
                        @Value("${institutions.bk.api.url}") String bankApiUrl,
                        @Value("${institutions.bk.api.key}") String apiKey) {
        this.restClient = restClientBuilder
            .baseUrl(bankApiUrl)
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .build();
        this.objectMapper = new ObjectMapper();
        this.bankApiUrl = bankApiUrl;
        this.apiKey = apiKey;
    }

    /**
     * Fetch transactions from Bank of Kigali API.
     * 
     * @param fromDate Start date for transaction query
     * @param toDate End date for transaction query
     * @param limit Maximum number of transactions to fetch
     * @return Raw JSON response as string
     */
    public String fetchTransactions(String fromDate, String toDate, Integer limit) {
        return restClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/v1/transactions")
                .queryParam("fromDate", fromDate)
                .queryParam("toDate", toDate)
                .queryParam("limit", limit)
                .build())
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, res) -> {
                throw new RuntimeException("Bank API Error: " + res.getStatusCode());
            })
            .body(String.class);
    }

    /**
     * Fetch account balances from Bank of Kigali API.
     * 
     * @param accountId Specific account ID (optional)
     * @return Raw JSON response as string
     */
    public String fetchAccountBalances(String accountId) {
        return restClient.get()
            .uri(uriBuilder -> {
                var builder = uriBuilder.path("/api/v1/accounts");
                if (accountId != null) {
                    builder.path("/{accountId}/balance").build(accountId);
                } else {
                    builder.path("/balances").build();
                }
                return builder.build();
            })
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, res) -> {
                throw new RuntimeException("Bank API Error: " + res.getStatusCode());
            })
            .body(String.class);
    }

    /**
     * Fetch customer information from Bank of Kigali API.
     * 
     * @param customerId Customer ID to fetch
     * @return Raw JSON response as string
     */
    public String fetchCustomerInfo(String customerId) {
        return restClient.get()
            .uri("/api/v1/customers/{customerId}", customerId)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, res) -> {
                throw new RuntimeException("Bank API Error: " + res.getStatusCode());
            })
            .body(String.class);
    }

    /**
     * Test connectivity to the bank API.
     * 
     * @return true if connection is successful
     */
    public boolean testConnection() {
        try {
            restClient.get()
                .uri("/api/v1/health")
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new RuntimeException("Health check failed: " + res.getStatusCode());
                })
                .toBodilessEntity();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
