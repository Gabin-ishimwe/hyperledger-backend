package com.openledger.common.fabric.registry;

import com.openledger.common.fabric.crypto.CryptoClient;
import com.openledger.common.fabric.crypto.CryptoMaterials;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Identity;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.Signer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing Fabric Gateway instances with dynamic identity fetching.
 * Caches Gateway instances and handles connection to institution-specific peers.
 */
@Component
public class GatewayRegistry {

    private final CryptoClient cryptoClient;
    private final FabricIdentityHelper identityHelper;
    private final Map<String, Gateway> gatewayCache = new ConcurrentHashMap<>();
    
    @Value("${fabric.default.channel:mychannel}")
    private String defaultChannel;

    public GatewayRegistry(CryptoClient cryptoClient, FabricIdentityHelper identityHelper) {
        this.cryptoClient = cryptoClient;
        this.identityHelper = identityHelper;
    }

    /**
     * Get or create a Gateway for the specified organization, user, and peer.
     * 
     * @param org Organization identifier
     * @param user User identifier
     * @param peer Peer identifier
     * @param peerEndpoint Peer gRPC endpoint URL
     * @return Gateway instance
     */
    public Gateway getGateway(String org, String user, String peer, String peerEndpoint) {
        String cacheKey = String.format("%s-%s-%s", org, user, peer);
        
        return gatewayCache.computeIfAbsent(cacheKey, key -> {
            try {
                return createGateway(org, user, peer, peerEndpoint);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create gateway for " + cacheKey, e);
            }
        });
    }

    /**
     * Get a Network from the Gateway for the specified channel.
     * 
     * @param org Organization identifier
     * @param user User identifier
     * @param peer Peer identifier
     * @param peerEndpoint Peer gRPC endpoint URL
     * @param channel Channel name (optional, uses default if null)
     * @return Network instance
     */
    public Network getNetwork(String org, String user, String peer, String peerEndpoint, String channel) {
        Gateway gateway = getGateway(org, user, peer, peerEndpoint);
        String channelName = channel != null ? channel : defaultChannel;
        return gateway.getNetwork(channelName);
    }

    /**
     * Create a new Gateway instance with dynamic identity fetching.
     */
    private Gateway createGateway(String org, String user, String peer, String peerEndpoint) throws Exception {
        // Fetch crypto materials from external service
        CryptoMaterials cryptoMaterials = cryptoClient.fetchCrypto(org, user, peer);
        
        // Convert to Fabric objects
        Identity identity = identityHelper.createIdentity(
            cryptoMaterials.certificate(), 
            cryptoMaterials.mspId()
        );
        Signer signer = identityHelper.createSigner(cryptoMaterials.privateKey());

        // Create Gateway builder
        Gateway.Builder builder = Gateway.newInstance()
            .identity(identity)
            .signer(signer)
            .connection(peerEndpoint);

        // Add TLS CA cert if provided
        if (cryptoMaterials.tlsCaCert() != null && !cryptoMaterials.tlsCaCert().trim().isEmpty()) {
            builder = builder.tlsCaCertificate(cryptoMaterials.tlsCaCert().getBytes());
        }

        return builder.connect();
    }

    /**
     * Close and remove a Gateway from the cache.
     * 
     * @param org Organization identifier
     * @param user User identifier
     * @param peer Peer identifier
     */
    public void closeGateway(String org, String user, String peer) {
        String cacheKey = String.format("%s-%s-%s", org, user, peer);
        Gateway gateway = gatewayCache.remove(cacheKey);
        if (gateway != null) {
            gateway.close();
        }
    }

    /**
     * Close all cached Gateways.
     */
    public void closeAll() {
        gatewayCache.values().forEach(Gateway::close);
        gatewayCache.clear();
    }
}
