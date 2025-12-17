package com.openledger.integration.institutions.bk;

import com.openledger.common.mapping.JsonataTransformer;
import com.openledger.common.mapping.MappingException;
import org.springframework.stereotype.Component;

/**
 * Mapper for transforming Bank of Kigali data to OpenLedger format.
 * Uses JsonataTransformer for programmatic transformation.
 */
@Component
public class BankMapper {

    private final JsonataTransformer jsonataTransformer;

    // JSONata expressions for transformation
    private static final String TRANSACTION_MAPPING = """
        {
            "transactions": $.data.transactions.{
                "id": transactionId,
                "type": "BANK_TRANSFER",
                "amount": amount,
                "currency": currency,
                "timestamp": timestamp,
                "fromAccount": fromAccount.accountNumber,
                "toAccount": toAccount.accountNumber,
                "description": description,
                "status": status,
                "institutionId": "BK",
                "reference": reference,
                "fees": fees ? fees : 0,
                "metadata": {
                    "channel": channel,
                    "location": location,
                    "originalData": $
                }
            }
        }
        """;

    private static final String BALANCE_MAPPING = """
        {
            "balances": $.data.accounts.{
                "accountId": accountNumber,
                "accountType": accountType,
                "balance": currentBalance,
                "currency": currency,
                "timestamp": lastUpdated,
                "institutionId": "BK",
                "status": status,
                "availableBalance": availableBalance,
                "metadata": {
                    "branchCode": branchCode,
                    "productCode": productCode,
                    "originalData": $
                }
            }
        }
        """;

    private static final String CUSTOMER_MAPPING = """
        {
            "customer": {
                "customerId": $.data.customerId,
                "name": $.data.personalInfo.fullName,
                "email": $.data.contactInfo.email,
                "phone": $.data.contactInfo.phone,
                "institutionId": "BK",
                "status": $.data.status,
                "accounts": $.data.accounts.accountNumber,
                "kyc": {
                    "level": $.data.kycLevel,
                    "verified": $.data.verified,
                    "documentType": $.data.identification.documentType,
                    "documentNumber": $.data.identification.documentNumber
                },
                "metadata": {
                    "customerType": $.data.customerType,
                    "registrationDate": $.data.registrationDate,
                    "originalData": $.data
                }
            }
        }
        """;

    public BankMapper(JsonataTransformer jsonataTransformer) {
        this.jsonataTransformer = jsonataTransformer;
    }

    /**
     * Transform Bank of Kigali transaction data to OpenLedger format.
     * 
     * @param bankTransactionJson Raw transaction JSON from bank API
     * @return Transformed JSON in OpenLedger format
     * @throws MappingException if transformation fails
     */
    public String mapTransactions(String bankTransactionJson) {
        try {
            return jsonataTransformer.transform(bankTransactionJson, TRANSACTION_MAPPING);
        } catch (Exception e) {
            throw new MappingException("Failed to map BK transactions", e);
        }
    }

    /**
     * Transform Bank of Kigali balance data to OpenLedger format.
     * 
     * @param bankBalanceJson Raw balance JSON from bank API
     * @return Transformed JSON in OpenLedger format
     * @throws MappingException if transformation fails
     */
    public String mapBalances(String bankBalanceJson) {
        try {
            return jsonataTransformer.transform(bankBalanceJson, BALANCE_MAPPING);
        } catch (Exception e) {
            throw new MappingException("Failed to map BK balances", e);
        }
    }

    /**
     * Transform Bank of Kigali customer data to OpenLedger format.
     * 
     * @param bankCustomerJson Raw customer JSON from bank API
     * @return Transformed JSON in OpenLedger format
     * @throws MappingException if transformation fails
     */
    public String mapCustomer(String bankCustomerJson) {
        try {
            return jsonataTransformer.transform(bankCustomerJson, CUSTOMER_MAPPING);
        } catch (Exception e) {
            throw new MappingException("Failed to map BK customer", e);
        }
    }

    /**
     * Transform generic Bank of Kigali data using custom JSONata expression.
     * 
     * @param bankJson Raw JSON from bank API
     * @param customMapping Custom JSONata expression
     * @return Transformed JSON
     * @throws MappingException if transformation fails
     */
    public String mapCustom(String bankJson, String customMapping) {
        try {
            return jsonataTransformer.transform(bankJson, customMapping);
        } catch (Exception e) {
            throw new MappingException("Failed to map BK data with custom mapping", e);
        }
    }
}
