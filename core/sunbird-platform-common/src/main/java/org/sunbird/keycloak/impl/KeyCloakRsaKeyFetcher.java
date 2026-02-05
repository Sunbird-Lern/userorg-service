package org.sunbird.keycloak.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.common.PropertiesCache;

/**
 * Class to fetch SSO public key from Keycloak server using 'certs' HTTP API call.
 */
public class KeyCloakRsaKeyFetcher {
  private static LoggerUtil logger = new LoggerUtil(KeyCloakRsaKeyFetcher.class);

  private static final String MODULUS = "modulusBase64";
  private static final String EXPONENT = "exponentBase64";

  /**
   * Fetches the public key from Keycloak based on the provided base URL and realm.
   *
   * @param url The Keycloak base URL.
   * @param realm The Keycloak realm name.
   * @return The PublicKey used to verify user access tokens, or null if retrieval fails.
   */
  public PublicKey getPublicKeyFromKeyCloak(String url, String realm) {
    try {
      Map<String, String> valueMap = null;
      Decoder urlDecoder = Base64.getUrlDecoder();
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      String publicKeyString = requestKeyFromKeycloak(url, realm);
      if (publicKeyString != null) {
        valueMap = getValuesFromJson(publicKeyString);
        if (valueMap != null) {
          BigInteger modulus = new BigInteger(1, urlDecoder.decode(valueMap.get(MODULUS)));
          BigInteger publicExponent = new BigInteger(1, urlDecoder.decode(valueMap.get(EXPONENT)));
          PublicKey key = keyFactory.generatePublic(new RSAPublicKeySpec(modulus, publicExponent));
          saveToCache(key);
          return key;
        }
      }
    } catch (Exception e) {
      logger.error(
          "KeyCloakRsaKeyFetcher:getPublicKeyFromKeyCloak: Exception occurred with message = "
              + e.getMessage(),
          e);
    }
    return null;
  }

  /**
   * Saves the public key string value to the PropertiesCache.
   *
   * @param key The Public key to save.
   */
  private void saveToCache(PublicKey key) {
    byte[] encodedPublicKey = key.getEncoded();
    String publicKey = Base64.getEncoder().encodeToString(encodedPublicKey);
    PropertiesCache cache = PropertiesCache.getInstance();
    cache.saveConfigProperty(JsonKey.SSO_PUBLIC_KEY, publicKey);
  }

  /**
   * Connects to the Keycloak server using an API call to get the public key.
   *
   * @param url The Keycloak base URL.
   * @param realm The Keycloak realm name.
   * @return The public key JSON response string, or null if validation fails.
   */
  private String requestKeyFromKeycloak(String url, String realm) {
    HttpClient client = HttpClientBuilder.create().build();
    HttpGet request = new HttpGet(url + "realms/" + realm + "/protocol/openid-connect/certs");

    try {
      HttpResponse response = client.execute(request);
      HttpEntity entity = response.getEntity();

      if (entity != null) {
        return EntityUtils.toString(entity);
      } else {
        logger.info(
            "KeyCloakRsaKeyFetcher:requestKeyFromKeycloak: Not able to fetch SSO public key from keycloak server");
      }
    } catch (IOException e) {
      logger.error(
          "KeyCloakRsaKeyFetcher:requestKeyFromKeycloak: Exception occurred with message = "
              + e.getMessage(),
          e);
    }
    return null;
  }

  /**
   * Extracts values (modulus and exponent) from the public key JSON string.
   *
   * @param response The public key JSON response string.
   * @return A Map containing the modulus and exponent, or null if parsing fails.
   */
  private Map<String, String> getValuesFromJson(String response) {
    ObjectMapper mapper = new ObjectMapper();
    Map<String, String> values = new HashMap<>();
    try {
      JsonNode res = mapper.readTree(response);
      JsonNode keys = res.get("keys");
      if (keys != null) {
        JsonNode value = keys.get(0);
        values.put(MODULUS, value.get("n").asText());
        values.put(EXPONENT, value.get("e").asText());
      }
    } catch (Exception e) {
      logger.error(
          "KeyCloakRsaKeyFetcher:getValuesFromJson: Exception occurred with message = "
              + e.getMessage(),
          e);
      return null;
    }

    return values;
  }
}
