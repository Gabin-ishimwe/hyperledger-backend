package com.openledger.common.fabric.registry;

import org.hyperledger.fabric.gateway.Identity;
import org.hyperledger.fabric.gateway.Signer;
import org.hyperledger.fabric.gateway.Signers;
import org.hyperledger.fabric.gateway.X509Identity;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * Utility class for converting PEM strings from the crypto API into Fabric Identity and Signer objects.
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
            X509Certificate certificate = parseCertificate(certificatePem);
            return new X509Identity(mspId, certificate);
        } catch (CertificateException e) {
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
            PrivateKey privateKey = parsePrivateKey(privateKeyPem);
            return Signers.newPrivateKeySigner(privateKey);
        } catch (InvalidKeyException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Failed to create signer from private key", e);
        }
    }

    /**
     * Parse X509 certificate from PEM string.
     */
    private X509Certificate parseCertificate(String certificatePem) throws CertificateException {
        String cleanPem = cleanPemString(certificatePem);
        byte[] certificateBytes = Base64.getDecoder().decode(cleanPem);
        
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) factory.generateCertificate(
            new ByteArrayInputStream(certificateBytes)
        );
    }

    /**
     * Parse private key from PEM string.
     */
    private PrivateKey parsePrivateKey(String privateKeyPem) 
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        String cleanPem = cleanPemString(privateKeyPem);
        byte[] keyBytes = Base64.getDecoder().decode(cleanPem);
        
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("EC"); // Assuming EC keys
        return keyFactory.generatePrivate(keySpec);
    }

    /**
     * Remove PEM headers/footers and whitespace from PEM string.
     */
    private String cleanPemString(String pem) {
        return pem
            .replaceAll("-----BEGIN[^-]*-----", "")
            .replaceAll("-----END[^-]*-----", "")
            .replaceAll("\\s+", "");
    }
}
