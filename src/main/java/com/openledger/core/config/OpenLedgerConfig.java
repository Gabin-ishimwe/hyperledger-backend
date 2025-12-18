package com.openledger.core.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configuration class for OpenLedger core components.
 */
@Configuration
public class OpenLedgerConfig {

    /**
     * Provides RestClient.Builder bean for HTTP client operations.
     * Can be overridden by application-specific configurations.
     */
    @Bean
    @ConditionalOnMissingBean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
