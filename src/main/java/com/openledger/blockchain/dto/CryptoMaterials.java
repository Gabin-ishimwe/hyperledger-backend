package com.openledger.blockchain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Record representing cryptographic materials fetched from the crypto service.
 * Maps to the nested JSON response from /api/crypto/gateway/:orgName/:userName/:peerName
 *
 * Response structure:
 * {
 *   "identity": { "certificate": "...", "privateKey": "...", "mspId": "..." },
 *   "tls": { "caCert": "...", "peerEndpoint": "..." },
 *   "metadata": { "userName": "...", "orgName": "...", "peerName": "...", "generatedAt": "..." }
 * }
 */
public record CryptoMaterials(
    @JsonProperty("identity")
    Identity identity,

    @JsonProperty("tls")
    Tls tls,

    @JsonProperty("metadata")
    Metadata metadata
) {

    /**
     * Identity information containing certificate, private key, and MSP ID.
     */
    public record Identity(
        @JsonProperty("certificate")
        String certificate,

        @JsonProperty("privateKey")
        String privateKey,

        @JsonProperty("mspId")
        String mspId
    ) {}

    /**
     * TLS information containing CA certificate and peer endpoint.
     */
    public record Tls(
        @JsonProperty("caCert")
        String caCert,

        @JsonProperty("peerEndpoint")
        String peerEndpoint
    ) {}

    /**
     * Metadata about the crypto materials request.
     */
    public record Metadata(
        @JsonProperty("userName")
        String userName,

        @JsonProperty("orgName")
        String orgName,

        @JsonProperty("peerName")
        String peerName,

        @JsonProperty("generatedAt")
        String generatedAt
    ) {}

    // Convenience methods for backward compatibility and easier access

    public String certificate() {
        return identity != null ? identity.certificate() : null;
    }

    public String privateKey() {
        return identity != null ? identity.privateKey() : null;
    }

    public String mspId() {
        return identity != null ? identity.mspId() : null;
    }

    public String tlsCaCert() {
        return tls != null ? tls.caCert() : null;
    }

    public String peerEndpoint() {
        return tls != null ? tls.peerEndpoint() : null;
    }
}
