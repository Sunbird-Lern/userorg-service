package org.sunbird.auth.verifier;

import java.security.PublicKey;

/**
 * Pojo for Key Data.
 */
public class KeyData {
    private String keyId;
    private PublicKey publicKey;

    /**
     * Constructor
     * @param keyId Key Id
     * @param publicKey Public Key
     */
    public KeyData(String keyId, PublicKey publicKey) {
        this.keyId = keyId;
        this.publicKey = publicKey;
    }

    /**
     * Get Key Id
     * @return keyId
     */
    public String getKeyId(){
        return keyId;
    }

    /**
     * Set Key Id
     * @param keyId Key Id
     */
    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    /**
     * Get Public Key
     * @return publicKey
     */
    public PublicKey getPublicKey() {
        return publicKey;
    }

    /**
     * Set Public Key
     * @param publicKey Public Key
     */
    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }
}
