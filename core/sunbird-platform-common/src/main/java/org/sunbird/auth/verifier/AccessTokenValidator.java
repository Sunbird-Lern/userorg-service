package org.sunbird.auth.verifier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.common.util.Time;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;

public class AccessTokenValidator {

    private static final LoggerUtil logger = new LoggerUtil(AccessTokenValidator.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String sso_url = System.getenv(JsonKey.SUNBIRD_SSO_URL);
    // Preserving the typo RELAM if it exists in JsonKey, but usually it should be REALM. 
    // Assuming original code was correct about the constant name.
    private static final String realm = System.getenv(JsonKey.SUNBIRD_SSO_RELAM);

    /**
     * Validates the access token. Checks signature and expiration.
     *
     * @param token The JWT access token string.
     * @param requestContext Context for logging/tracing.
     * @return Map containing the token claims if valid, empty map otherwise.
     * @throws JsonProcessingException if token parsing fails.
     */
    public static Map<String, Object> validateToken(String token, Map<String, Object> requestContext)
            throws JsonProcessingException {
        return validateToken(token, requestContext, true);
    }

    /**
     * Validates the access token, with optional expiration check.
     *
     * @param token The JWT access token string.
     * @param checkActive If true, checks the 'exp' claim.
     * @return Map containing the token claims if valid, empty map otherwise.
     * @throws JsonProcessingException if token parsing fails.
     */
    public static Map<String, Object> validateToken(String token, boolean checkActive) throws JsonProcessingException {
        return validateToken(token, null, checkActive);
    }

    /**
     * Validates the access token with expiration check enabled (default).
     *
     * @param token The JWT access token string.
     * @return Map containing the token claims if valid, empty map otherwise.
     * @throws JsonProcessingException if token parsing fails.
     */
    public static Map<String, Object> validateToken(String token) throws JsonProcessingException {
        return validateToken(token, null, true);
    }

    /**
     * Internal method to validate the token.
     * 
     * <p>This method performs the following steps:
     * <ol>
     *   <li>Splits the token into header, body, and signature.</li>
     *   <li>Decodes the header to retrieve the Key ID (kid).</li>
     *   <li>Verifies the RSA signature using the public key associated with the kid.</li>
     *   <li>If the signature is valid, decodes the body.</li>
     *   <li>Optionally checks if the token has expired.</li>
     * </ol>
     *
     * @param token The JWT token string.
     * @param requestContext The request context (can be null).
     * @param checkExpiry Whether to validate the 'exp' claim.
     * @return The token body as a Map if valid; otherwise, an empty Map.
     * @throws JsonProcessingException If the header or body cannot be parsed as JSON.
     */
    private static Map<String, Object> validateToken(String token, Map<String, Object> requestContext, boolean checkExpiry)
            throws JsonProcessingException {
        String[] tokenElements = token.split("\\.");
        // Basic JWT format check
        if (tokenElements.length != 3) {
            logger.info("Invalid token format: " + token);
             return Collections.emptyMap();
        }
        
        String header = tokenElements[0];
        String body = tokenElements[1];
        String signature = tokenElements[2];
        String payLoad = header + JsonKey.DOT_SEPARATOR + body;

        // Decode header to get Key ID
        Map<Object, Object> headerData =
                mapper.readValue(new String(decodeFromBase64(header), StandardCharsets.UTF_8), Map.class);
        String keyId = headerData.get("kid").toString();

        // Verify Signature
        boolean isValid = CryptoUtil.verifyRSASign(
                payLoad,
                decodeFromBase64(signature),
                KeyManager.getPublicKey(keyId).getPublicKey(),
                JsonKey.SHA_256_WITH_RSA);

        if (isValid) {
            Map<String, Object> tokenBody =
                    mapper.readValue(new String(decodeFromBase64(body), StandardCharsets.UTF_8), Map.class);
            
            if (checkExpiry) {
                boolean isExp = isExpired((Integer) tokenBody.get("exp"));
                if (isExp) {
                    logger.info("AccessTokenValidator: Token expired. Context: " + requestContext);
                    return Collections.emptyMap();
                }
            }
            return tokenBody;
        }
        return Collections.emptyMap();
    }

    /**
     * Managed user token verification.
     * Validates the token and ensures the requested user IDs match the token claims.
     *
     * @param managedEncToken The managed token string.
     * @param requestedByUserId User ID of the requester (must match parent).
     * @param requestedForUserId User ID of the target user (must match sub).
     * @param loggingHeaders Headers for logging logic.
     * @return The managed user ID if valid, unauthorized otherwise.
     */
    public static String verifyManagedUserToken(
            String managedEncToken, String requestedByUserId, String requestedForUserId, String loggingHeaders) {
        return verifyManagedUserToken(managedEncToken, requestedByUserId, requestedForUserId, null, loggingHeaders);
    }

    /**
     * managedtoken is validated and requestedByUserID, requestedForUserID values are validated
     * aganist the managedEncToken
     *
     * @param managedEncToken
     * @param requestedByUserId
     * @param requestedForUserId
     * @param requestContext
     * @return
     */
    public static String verifyManagedUserToken(
            String managedEncToken,
            String requestedByUserId,
            String requestedForUserId,
            Map<String, Object> requestContext) {
        return verifyManagedUserToken(managedEncToken, requestedByUserId, requestedForUserId, requestContext, null);
    }

    public static String verifyManagedUserToken(String managedEncToken, String requestedByUserId) {
        return verifyManagedUserToken(managedEncToken, requestedByUserId, null, null, null);
    }

    private static String verifyManagedUserToken(
            String managedEncToken,
            String requestedByUserId,
            String requestedForUserId,
            Map<String, Object> requestContext,
            String loggingHeaders) {
        String managedFor = JsonKey.UNAUTHORIZED;
        try {
            Map<String, Object> payload;
            if (requestContext != null) {
                payload = validateToken(managedEncToken, requestContext);
            } else {
                payload = validateToken(managedEncToken, true);
            }

            if (MapUtils.isNotEmpty(payload)) {
                String parentId = (String) payload.get(JsonKey.PARENT_ID);
                String muaId = (String) payload.get(JsonKey.SUB);
                
                String logMsg = String.format(
                        "AccessTokenValidator:verifyManagedUserToken: Parent: %s, ManagedBy: %s, RequestedBy: %s",
                        parentId, muaId, requestedByUserId);

                if (StringUtils.isNotEmpty(requestedForUserId)) {
                    logMsg += ", RequestedFor: " + requestedForUserId;
                }
                if (requestContext != null) {
                    logMsg += ", Context: " + requestContext;
                }

                logger.info(logMsg);

                boolean isValid = parentId.equalsIgnoreCase(requestedByUserId);
                if (StringUtils.isNotEmpty(requestedForUserId) && !muaId.equalsIgnoreCase(requestedForUserId)) {
                   logger.info(String.format(
                           "AccessTokenValidator:verifyManagedUserToken: Mismatch! RequestedFor: %s, ManagedBy: %s, Headers: %s",
                           requestedForUserId, muaId, loggingHeaders));
                    // If requestedForUserId is present, it MUST match muaId for the token to be valid for that target
                    if (isValid) {
                         isValid = muaId.equalsIgnoreCase(requestedForUserId);
                    }
                }

                if (isValid) {
                    managedFor = muaId;
                }
            }
        } catch (Exception ex) {
            String errorMsg = "Exception in verifyManagedUserToken: Token : " + managedEncToken;
             if (requestContext != null) {
                 errorMsg += ", request context data :" + requestContext;
             }
            logger.error(errorMsg, ex);
        }
        return managedFor;
    }

    /**
     * Verifies the user access token.
     *
     * @param token The JWT access token.
     * @param checkActive Whether to check for token expiration.
     * @return The user ID from the token if valid, unauthorized otherwise.
     */
    public static String verifyUserToken(String token, boolean checkActive) {
        return verifyUserToken(token, null, checkActive);
    }

    /**
     * Verifies the user access token.
     *
     * @param token The JWT access token.
     * @param requestContext Context for logging/tracing.
     * @return The user ID from the token if valid, unauthorized otherwise.
     */
    public static String verifyUserToken(String token, Map<String, Object> requestContext) {
        return verifyUserToken(token, requestContext, true);
    }

    /**
     * Verifies the user access token with default expiration check.
     *
     * @param token The JWT access token.
     * @return The user ID from the token if valid, unauthorized otherwise.
     */
    public static String verifyUserToken(String token) {
        return verifyUserToken(token, null, true);
    }

    private static String verifyUserToken(String token, Map<String, Object> requestContext, boolean checkActive) {
        String userId = JsonKey.UNAUTHORIZED;
        try {
            Map<String, Object> payload;
            if (requestContext != null) {
                payload = validateToken(token, requestContext);
                logger.debug(
                        String.format("AccessTokenValidator:verifyUserToken: Payload: %s, Context: %s",
                                payload, requestContext));
            } else {
                payload = validateToken(token, checkActive);
            }

            if (MapUtils.isNotEmpty(payload) && checkIss((String) payload.get("iss"))) {
                userId = (String) payload.get(JsonKey.SUB);
                if (StringUtils.isNotBlank(userId)) {
                    int pos = userId.lastIndexOf(":");
                    userId = userId.substring(pos + 1);
                }
            }
        } catch (Exception ex) {
            String errorMsg = "Exception in verifyUserAccessToken: Token : " + token;
             if (requestContext != null) {
                 errorMsg += ", request context data :" + requestContext;
             }
            logger.error(errorMsg, ex);
        }
        
        if (JsonKey.UNAUTHORIZED.equalsIgnoreCase(userId) && requestContext != null) {
             logger.info(
                  String.format("AccessTokenValidator:verifyUserToken: Invalid Token. Context: %s", requestContext));
        }
        
        return userId;
    }

    /**
     * Verifies the user token against a specific source URL.
     *
     * @param token The JWT access token string.
     * @param url The source URL (SSO URL). If null, defaults to environment SUNBIRD_SSO_URL.
     * @param requestContext Context for logging/tracing.
     * @return The userId from the token if valid, otherwise JsonKey.UNAUTHORIZED.
     */
    public static String verifySourceUserToken(String token, String url, Map<String, Object> requestContext) {
        String userId = JsonKey.UNAUTHORIZED;
        try {
            Map<String, Object> payload = validateToken(token, requestContext);
            if (requestContext != null) {
                logger.debug(
                        String.format("AccessTokenValidator:verifySourceUserToken: Payload: %s, Context: %s",
                                payload, requestContext));
            }

            if (MapUtils.isNotEmpty(payload) && checkSourceIss((String) payload.get("iss"), url)) {
                userId = (String) payload.get(JsonKey.SUB);
                if (StringUtils.isNotBlank(userId)) {
                    int pos = userId.lastIndexOf(":");
                    userId = userId.substring(pos + 1);
                }
            }
        } catch (Exception ex) {
            String errorMsg = "Exception in verifySourceUserToken: Token : " + token;
             if (requestContext != null) {
                 errorMsg += ", request context data :" + requestContext;
             }
            logger.error(errorMsg, ex);
        }

        if (JsonKey.UNAUTHORIZED.equalsIgnoreCase(userId) && requestContext != null) {
            logger.info(
                    String.format("AccessTokenValidator:verifySourceUserToken: Invalid Source Token. Context: %s", requestContext));
        }
        return userId;
    }

    private static boolean checkSourceIss(String iss, String url) {
        String ssoUrl = (url != null ? url : sso_url);
        String realmUrl = ssoUrl + "realms/" + realm;
        return (realmUrl.equalsIgnoreCase(iss));
    }

    private static boolean checkIss(String iss) {
        String realmUrl = sso_url + "realms/" + realm;
        return (realmUrl.equalsIgnoreCase(iss));
    }

    private static boolean isExpired(Integer expiration) {
        return (Time.currentTime() > expiration);
    }

    private static byte[] decodeFromBase64(String data) {
        return Base64Util.decode(data, 11);
    }
}
