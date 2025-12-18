package com.openledger.blockchain.client;

import com.openledger.blockchain.dto.CryptoMaterials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Service for fetching cryptographic materials from external crypto service.
 * Java refactor of the NestJS CryptoService using Spring RestClient.
 */
@Service
public class CryptoClient {

    private final RestClient restClient;
    private final String baseUrl;

    public CryptoClient(RestClient.Builder builder,
                       @Value("${crypto.service.url}") String baseUrl) {
        this.restClient = builder.build();
        this.baseUrl = baseUrl;
    }

    /**
     * Fetch cryptographic materials for a specific organization, user, and peer.
     *
     * @param org The organization identifier
     * @param user The user identifier
     * @param peer The peer identifier
     * @return CryptoMaterials containing certificates and keys
     * @throws RuntimeException if the crypto API returns an error
     */
    public CryptoMaterials fetchCrypto(String org, String user, String peer) {
        return restClient.get()
            .uri(baseUrl + "/api/crypto/gateway/{org}/{user}/{peer}", org, user, peer)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, res) -> {
                throw new RuntimeException("Crypto API Error: " + res.getStatusCode());
            })
            .body(CryptoMaterials.class);
    }
}
