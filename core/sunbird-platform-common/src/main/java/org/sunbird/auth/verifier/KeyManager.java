package org.sunbird.auth.verifier;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.common.PropertiesCache;

/**
 * Manages the loading and retrieval of Public Keys for token verification.
 */
public class KeyManager {

    private static final LoggerUtil logger = new LoggerUtil(KeyManager.class);
    private static final PropertiesCache propertiesCache = PropertiesCache.getInstance();
    private static final Map<String, KeyData> keyMap = new HashMap<>();

    /**
     * Initializes the KeyManager by loading public keys from the configured base path.
     */
    public static void init() {
        String basePath = propertiesCache.getProperty(JsonKey.ACCESS_TOKEN_PUBLICKEY_BASEPATH);
        logger.info("KeyManager:init: Starting public key loading from base path: " + basePath);
        
        try (Stream<Path> walk = Files.walk(Paths.get(basePath))) {
            List<String> result =
                    walk.filter(Files::isRegularFile).map(x -> x.toString()).collect(Collectors.toList());
            
            result.forEach(
                    file -> {
                        try {
                            StringBuilder contentBuilder = new StringBuilder();
                            Path path = Paths.get(file);
                            Files.lines(path, StandardCharsets.UTF_8)
                                    .forEach(contentBuilder::append);
                            
                            KeyData keyData =
                                    new KeyData(
                                            path.getFileName().toString(), loadPublicKey(contentBuilder.toString()));
                            keyMap.put(path.getFileName().toString(), keyData);
                            logger.info("KeyManager:init: Loaded key: " + path.getFileName().toString());
                        } catch (Exception e) {
                            logger.error("KeyManager:init: Exception in reading public key file: " + file, e);
                        }
                    });
        } catch (Exception e) {
            logger.error("KeyManager:init: Exception in loading public keys base directory", e);
        }
    }

    /**
     * Retrieves the KeyData for a given Key ID.
     * @param keyId The Key ID.
     * @return The KeyData object, or null if not found.
     */
    public static KeyData getPublicKey(String keyId) {
        return keyMap.get(keyId);
    }

    /**
     * Parses a string representation of a public key into a PublicKey object.
     * @param key The public key string (PEM format).
     * @return The PublicKey object.
     * @throws Exception If parsing fails.
     */
    public static PublicKey loadPublicKey(String key) throws Exception {
        String publicKey = new String(key.getBytes(), StandardCharsets.UTF_8);
        publicKey = publicKey.replaceAll("(-+BEGIN PUBLIC KEY-+)", "");
        publicKey = publicKey.replaceAll("(-+END PUBLIC KEY-+)", "");
        publicKey = publicKey.replaceAll("[\\r\\n]+", "");
        byte[] keyBytes = Base64Util.decode(publicKey.getBytes(StandardCharsets.UTF_8), Base64Util.DEFAULT);

        X509EncodedKeySpec X509publicKey = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(X509publicKey);
    }
}
