package org.sunbird.auth.verifier;

import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Map;
import org.sunbird.logging.LoggerUtil;

public class CryptoUtil {
  private static final Charset US_ASCII = Charset.forName("US-ASCII");
  private static final LoggerUtil logger = new LoggerUtil(CryptoUtil.class);

  /**
   * Verifies the RSA signature.
   *
   * @param payLoad The string payload.
   * @param signature The signature bytes.
   * @param key The public key.
   * @param algorithm The signature algorithm (e.g., SHA256withRSA).
   * @return True if verification succeeds, false otherwise.
   */
  public static boolean verifyRSASign(
      String payLoad, byte[] signature, PublicKey key, String algorithm) {
    return verifyRSASign(payLoad, signature, key, algorithm, null);
  }

  /**
   * Verifies the RSA signature with logging context.
   *
   * @param payLoad The string payload.
   * @param signature The signature bytes.
   * @param key The public key.
   * @param algorithm The signature algorithm.
   * @param requestContext Context for logging (optional).
   * @return True if verification succeeds, false otherwise.
   */
  public static boolean verifyRSASign(
      String payLoad,
      byte[] signature,
      PublicKey key,
      String algorithm,
      Map<String, Object> requestContext) {
    Signature sign;
    try {
      sign = Signature.getInstance(algorithm);
      sign.initVerify(key);
      sign.update(payLoad.getBytes(US_ASCII));
      return sign.verify(signature);
    } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
      String msg = String.format("CryptoUtil:verifyRSASign: Exception occurred while token verification. Error: %s", e.getMessage());
      if (requestContext != null) {
        msg += ", Context: " + requestContext;
      }
      logger.error(msg, e);
      return false;
    }
  }
}
