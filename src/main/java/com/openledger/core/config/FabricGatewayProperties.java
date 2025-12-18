package com.openledger.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.HashMap;

/**
 * Configuration properties for Fabric Gateway connections.
 */
@Component
@ConfigurationProperties(prefix = "fabric")
public class FabricGatewayProperties {

    private Default defaults = new Default();
    private Gateway gateway = new Gateway();
    private Map<String, Organization> organizations = new HashMap<>();

    // Getters and setters
    public Default getDefault() { return defaults; }
    public void setDefault(Default defaults) { this.defaults = defaults; }

    public Gateway getGateway() { return gateway; }
    public void setGateway(Gateway gateway) { this.gateway = gateway; }

    public Map<String, Organization> getOrganizations() { return organizations; }
    public void setOrganizations(Map<String, Organization> organizations) { this.organizations = organizations; }

    public static class Default {
        private String channel = "mychannel";

        public String getChannel() { return channel; }
        public void setChannel(String channel) { this.channel = channel; }
    }

    public static class Gateway {
        private int evaluateTimeout = 5;
        private int endorseTimeout = 15;
        private int submitTimeout = 5;
        private int commitTimeout = 60;

        public int getEvaluateTimeout() { return evaluateTimeout; }
        public void setEvaluateTimeout(int evaluateTimeout) { this.evaluateTimeout = evaluateTimeout; }

        public int getEndorseTimeout() { return endorseTimeout; }
        public void setEndorseTimeout(int endorseTimeout) { this.endorseTimeout = endorseTimeout; }

        public int getSubmitTimeout() { return submitTimeout; }
        public void setSubmitTimeout(int submitTimeout) { this.submitTimeout = submitTimeout; }

        public int getCommitTimeout() { return commitTimeout; }
        public void setCommitTimeout(int commitTimeout) { this.commitTimeout = commitTimeout; }
    }

    public static class Organization {
        private String orgName;        // e.g., "org1.example.com" - used for crypto API
        private String peerEndpoint;   // e.g., "localhost:7051" - fallback if not from crypto API
        private String peerName;       // e.g., "peer0" - used for crypto API and TLS override

        public String getOrgName() { return orgName; }
        public void setOrgName(String orgName) { this.orgName = orgName; }

        public String getPeerEndpoint() { return peerEndpoint; }
        public void setPeerEndpoint(String peerEndpoint) { this.peerEndpoint = peerEndpoint; }

        public String getPeerName() { return peerName; }
        public void setPeerName(String peerName) { this.peerName = peerName; }
    }
}
