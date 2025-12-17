package com.openledger.api.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for OpenLedger API.
 */
@Configuration
public class ApiConfig {

    /**
     * Configure OpenAPI documentation.
     */
    @Bean
    public OpenAPI openLedgerOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("OpenLedger API")
                .description("Blockchain-based Financial Platform API for Rwanda's Banking Sector")
                .version("1.0.0")
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .externalDocs(new ExternalDocumentation()
                .description("OpenLedger Documentation")
                .url("https://github.com/openledger/docs"));
    }
}
