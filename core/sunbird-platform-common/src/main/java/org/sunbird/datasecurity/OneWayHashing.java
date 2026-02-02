package org.sunbird.datasecurity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.sunbird.logging.LoggerUtil;

/**
 * Utility class for performing one-way data hashing.
 * Uses SHA-256 algorithm to hash input strings.
 */
public class OneWayHashing {

  public static LoggerUtil logger = new LoggerUtil(OneWayHashing.class);

  private OneWayHashing() {}

  /**
   * Encrypts (hashes) a value using SHA-256 algorithm.
   *
   * @param val The string value to hash.
   * @return The SHA-256 hash of the value in hexadecimal format, or an empty string if an error occurs.
   */
  public static String encryptVal(String val) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.update(val.getBytes(StandardCharsets.UTF_8));
      byte[] byteData = md.digest();
      // convert the byte to hex format
      StringBuilder sb = new StringBuilder();
      for (byte b : byteData) {
        sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
      }
      return sb.toString();
    } catch (Exception e) {
      logger.error("Error while encrypting", e);
    }
    return "";
  }
}
