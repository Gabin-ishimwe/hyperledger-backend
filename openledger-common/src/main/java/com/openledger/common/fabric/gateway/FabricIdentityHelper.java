package com.openledger.common.fabric.gateway;

import org.hyperledger.fabric.client.identity.Identities;
import org.hyperledger.fabric.client.identity.Identity;
import org.hyperledger.fabric.client.identity.Signer;
import org.hyperledger.fabric.client.identity.Signers;
import org.hyperledger.fabric.client.identity.X509Identity;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Utility class for converting PEM strings from the crypto API into Fabric Identity and Signer objects.
 * Updated to use the new Fabric Gateway SDK v2.x API.
 */
@Component
public class FabricIdentityHelper {

    /**
     * Create a Fabric Identity from PEM-encoded certificate and MSP ID.
     * 
     * @param certificatePem PEM-encoded certificate string
     * @param mspId MSP identifier
     * @return Fabric Identity object
     */
    public Identity createIdentity(String certificatePem, String mspId) {
        try {
            X509Certificate certificate = Identities.readX509Certificate(new StringReader(certificatePem));
            return new X509Identity(mspId, certificate);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create identity from certificate", e);
        }
    }

    /**
     * Create a Fabric Signer from PEM-encoded private key.
     * 
     * @param privateKeyPem PEM-encoded private key string
     * @return Fabric Signer object
     */
    public Signer createSigner(String privateKeyPem) {
        try {
            PrivateKey privateKey = Identities.readPrivateKey(new StringReader(privateKeyPem));
            return Signers.newPrivateKeySigner(privateKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create signer from private key", e);
        }
    }

}
