package com.openledger.blockchain.client;

import com.openledger.core.config.FabricGatewayProperties;
import com.openledger.blockchain.dto.CryptoMaterials;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.TlsChannelCredentials;
import org.hyperledger.fabric.client.*;
import org.hyperledger.fabric.client.identity.Identity;
import org.hyperledger.fabric.client.identity.Signer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Enhanced Fabric Gateway Service that follows the reference pattern.
 * Integrates with the crypto service for dynamic identity fetching while
 * using the new Fabric Gateway SDK v2.x API.
 */
@Service
public class FabricGatewayService implements DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(FabricGatewayService.class);

    private final CryptoClient cryptoClient;
    private final FabricIdentityHelper identityHelper;
    private final FabricGatewayProperties properties;

    // Cache for gRPC channels and Gateway instances
    private final Map<String, ManagedChannel> channelCache = new ConcurrentHashMap<>();
    private final Map<String, Gateway> gatewayCache = new ConcurrentHashMap<>();

    public FabricGatewayService(CryptoClient cryptoClient,
                                FabricIdentityHelper identityHelper,
                                FabricGatewayProperties properties) {
        this.cryptoClient = cryptoClient;
        this.identityHelper = identityHelper;
        this.properties = properties;
    }

    /**
     * Get or create a Gateway connection for the specified organization and user.
     *
     * @param orgMspId Organization MSP ID (e.g., "Org1MSP", "BankOfKigaliMSP")
     * @param userId User identifier
     * @param peerId Peer identifier (optional, defaults to peer0)
     * @return Gateway instance
     */
    public Gateway getGateway(String orgMspId, String userId, String peerId) {
        String effectivePeerId = peerId != null ? peerId : "peer0";
        String cacheKey = String.format("%s-%s-%s", orgMspId, userId, effectivePeerId);

        return gatewayCache.computeIfAbsent(cacheKey, key -> {
            try {
                return createGateway(orgMspId, userId, effectivePeerId);
            } catch (Exception e) {
                logger.error("Failed to create gateway for {}", cacheKey, e);
                throw new RuntimeException("Failed to create gateway for " + cacheKey, e);
            }
        });
    }

    /**
     * Get a Network from the Gateway for the specified channel.
     *
     * @param orgMspId Organization MSP ID
     * @param userId User identifier
     * @param peerId Peer identifier (optional)
     * @param channelName Channel name (optional, uses default if null)
     * @return Network instance
     */
    public Network getNetwork(String orgMspId, String userId, String peerId, String channelName) {
        Gateway gateway = getGateway(orgMspId, userId, peerId);
        String channel = channelName != null ? channelName : properties.getDefault().getChannel();
        return gateway.getNetwork(channel);
    }

    /**
     * Get a Contract from the Network for the specified chaincode.
     *
     * @param orgMspId Organization MSP ID
     * @param userId User identifier
     * @param peerId Peer identifier (optional)
     * @param channelName Channel name (optional)
     * @param contractName Chaincode/contract name
     * @return Contract instance
     */
    public Contract getContract(String orgMspId, String userId, String peerId,
                                String channelName, String contractName) {
        Network network = getNetwork(orgMspId, userId, peerId, channelName);
        return network.getContract(contractName);
    }

    /**
     * Create a new Gateway instance with dynamic identity fetching and gRPC channel management.
     */
    private Gateway createGateway(String orgMspId, String userId, String peerId) throws Exception {
        logger.info("Creating Gateway for org: {}, user: {}, peer: {}", orgMspId, userId, peerId);

        // Get organization configuration
        FabricGatewayProperties.Organization orgConfig = properties.getOrganizations().get(orgMspId);
        if (orgConfig == null) {
            throw new IllegalArgumentException("No configuration found for organization: " + orgMspId);
        }

        // Validate required configuration
        if (orgConfig.getOrgName() == null || orgConfig.getOrgName().isBlank()) {
            throw new IllegalArgumentException("orgName not configured for organization: " + orgMspId);
        }

        // Use peerName from config if peerId is not specified, otherwise use peerId
        String effectivePeerName = (peerId != null && !peerId.isBlank()) ? peerId : orgConfig.getPeerName();
        if (effectivePeerName == null || effectivePeerName.isBlank()) {
            effectivePeerName = "peer0"; // default
        }

        // Fetch crypto materials from external service using correct API parameters:
        // /api/crypto/gateway/:orgName/:userName/:peerName
        CryptoMaterials cryptoMaterials = cryptoClient.fetchCrypto(
                orgConfig.getOrgName(),  // e.g., "org1.example.com"
                userId,                   // e.g., "User1"
                effectivePeerName         // e.g., "peer0"
        );
        logger.debug("Fetched crypto materials for org: {}, user: {}, peer: {}",
                orgConfig.getOrgName(), userId, effectivePeerName);

        // Convert to Fabric objects using the helper
        Identity identity = identityHelper.createIdentity(cryptoMaterials.certificate(), cryptoMaterials.mspId());
        Signer signer = identityHelper.createSigner(cryptoMaterials.privateKey());

        // Use peer endpoint from crypto response, fallback to config
        String peerEndpoint = cryptoMaterials.peerEndpoint();
        if (peerEndpoint == null || peerEndpoint.isBlank()) {
            peerEndpoint = orgConfig.getPeerEndpoint();
            logger.warn("No peerEndpoint in crypto response, using config: {}", peerEndpoint);
        }

        // Create or reuse gRPC channel using endpoint from crypto materials
        ManagedChannel channel = getOrCreateChannel(orgMspId, peerEndpoint, cryptoMaterials);

        // Create Gateway with proper timeouts following reference pattern
        Gateway gateway = Gateway.newInstance()
                .identity(identity)
                .signer(signer)
                .hash(Hash.SHA256)
                .connection(channel)
                // Configure timeouts from properties
                .evaluateOptions(options -> options.withDeadlineAfter(
                        properties.getGateway().getEvaluateTimeout(), TimeUnit.SECONDS))
                .endorseOptions(options -> options.withDeadlineAfter(
                        properties.getGateway().getEndorseTimeout(), TimeUnit.SECONDS))
                .submitOptions(options -> options.withDeadlineAfter(
                        properties.getGateway().getSubmitTimeout(), TimeUnit.SECONDS))
                .commitStatusOptions(options -> options.withDeadlineAfter(
                        properties.getGateway().getCommitTimeout(), TimeUnit.SECONDS))
                .connect();

        logger.info("Successfully created Gateway for {}/{}/{}", orgMspId, userId, effectivePeerName);
        return gateway;
    }

    /**
     * Get or create a gRPC channel for the organization.
     *
     * @param orgMspId Organization MSP ID for caching
     * @param peerEndpoint Peer endpoint from crypto materials or config (e.g., "peer0.org1.example.com:7051")
     * @param cryptoMaterials Crypto materials containing TLS CA cert and metadata
     */
    private ManagedChannel getOrCreateChannel(String orgMspId,
                                              String peerEndpoint,
                                              CryptoMaterials cryptoMaterials) {
        return channelCache.computeIfAbsent(orgMspId, key -> {
            try {
                logger.info("Creating gRPC channel for org: {} to endpoint: {}", orgMspId, peerEndpoint);

                // Create TLS credentials
                TlsChannelCredentials.Builder credentialsBuilder = TlsChannelCredentials.newBuilder();

                // Add TLS CA certificate if provided
                String tlsCaCert = cryptoMaterials.tlsCaCert();
                if (tlsCaCert != null && !tlsCaCert.trim().isEmpty()) {
                    credentialsBuilder.trustManager(new ByteArrayInputStream(tlsCaCert.getBytes()));
                } else {
                    logger.warn("No TLS CA certificate provided for org: {}, using system trust store", orgMspId);
                }

                // Build TLS override authority from metadata (e.g., "peer0.org1.example.com")
                String tlsOverrideAuthority = buildTlsOverrideAuthority(cryptoMaterials);

                // Create gRPC channel
                ManagedChannel channel = Grpc.newChannelBuilder(peerEndpoint, credentialsBuilder.build())
                        .overrideAuthority(tlsOverrideAuthority)
                        .build();

                logger.info("Successfully created gRPC channel for org: {} with authority: {}", orgMspId, tlsOverrideAuthority);
                return channel;
            } catch (Exception e) {
                logger.error("Failed to create gRPC channel for org: {}", orgMspId, e);
                throw new RuntimeException("Failed to create gRPC channel for " + orgMspId, e);
            }
        });
    }

    /**
     * Build TLS override authority from crypto materials metadata.
     * Format: peerName.orgName (e.g., "peer0.org1.example.com")
     */
    private String buildTlsOverrideAuthority(CryptoMaterials cryptoMaterials) {
        if (cryptoMaterials.metadata() != null) {
            String peerName = cryptoMaterials.metadata().peerName();
            String orgName = cryptoMaterials.metadata().orgName();
            if (peerName != null && orgName != null) {
                return peerName + "." + orgName;
            }
        }
        // Fallback: extract from peerEndpoint (remove port)
        String endpoint = cryptoMaterials.peerEndpoint();
        if (endpoint != null && endpoint.contains(":")) {
            return endpoint.substring(0, endpoint.lastIndexOf(':'));
        }
        return endpoint;
    }

    /**
     * Close and remove a specific Gateway from the cache.
     */
    public void closeGateway(String orgMspId, String userId, String peerId) {
        String cacheKey = String.format("%s-%s-%s", orgMspId, userId, peerId != null ? peerId : "peer0");
        Gateway gateway = gatewayCache.remove(cacheKey);
        if (gateway != null) {
            try {
                gateway.close();
                logger.info("Closed Gateway for {}", cacheKey);
            } catch (Exception e) {
                logger.error("Error closing Gateway for {}", cacheKey, e);
            }
        }
    }

    /**
     * Close a specific organization's gRPC channel.
     */
    public void closeChannel(String orgMspId) {
        ManagedChannel channel = channelCache.remove(orgMspId);
        if (channel != null && !channel.isShutdown()) {
            try {
                channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
                logger.info("Closed gRPC channel for org: {}", orgMspId);
            } catch (Exception e) {
                logger.error("Error closing gRPC channel for org: {}", orgMspId, e);
            }
        }
    }

    /**
     * Health check method to verify connectivity to a specific organization.
     */
    public boolean isHealthy(String orgMspId, String userId, String peerId) {
        try {
            Gateway gateway = getGateway(orgMspId, userId, peerId);
            // Simple test - try to get the default network
            Network network = gateway.getNetwork(properties.getDefault().getChannel());
            return network != null;
        } catch (Exception e) {
            logger.warn("Health check failed for {}/{}/{}", orgMspId, userId, peerId, e);
            return false;
        }
    }

    /**
     * Cleanup method called when the service is destroyed.
     */
    @Override
    public void destroy() {
        logger.info("Shutting down FabricGatewayService...");

        // Close all gateways
        gatewayCache.values().forEach(gateway -> {
            try {
                gateway.close();
            } catch (Exception e) {
                logger.error("Error closing gateway during shutdown", e);
            }
        });
        gatewayCache.clear();

        // Close all channels
        channelCache.values().forEach(channel -> {
            if (!channel.isShutdown()) {
                try {
                    channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    logger.error("Error closing channel during shutdown", e);
                }
            }
        });
        channelCache.clear();

        logger.info("FabricGatewayService shutdown complete");
    }
}
