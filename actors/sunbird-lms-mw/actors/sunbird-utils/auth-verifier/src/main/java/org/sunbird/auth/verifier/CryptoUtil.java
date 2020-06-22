package org.sunbird.auth.verifier;

import java.nio.charset.Charset;
import java.security.*;

public class CryptoUtil {
    private static final Charset US_ASCII = Charset.forName("US-ASCII");

    public static boolean verifyRSASign(String payLoad, byte[] signature, PublicKey key, String algorithm) {
        Signature sign;
        try {
            sign = Signature.getInstance(algorithm);
            sign.initVerify(key);
            sign.update(payLoad.getBytes(US_ASCII));
            return sign.verify(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            return false;
        }
    }

}
