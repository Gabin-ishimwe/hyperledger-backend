package com.openledger.common.fabric.crypto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Record representing cryptographic materials fetched from the crypto service.
 * Maps to the JSON response containing certificate, private key, MSP ID, and optional TLS CA certificate.
 */
public record CryptoMaterials(
    @JsonProperty("certificate")
    String certificate,
    
    @JsonProperty("privateKey") 
    String privateKey,
    
    @JsonProperty("mspId")
    String mspId,
    
    @JsonProperty("tlsCaCert")
    String tlsCaCert
) {}
