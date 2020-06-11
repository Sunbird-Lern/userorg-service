package org.sunbird.auth.verifier;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;

import java.util.Map;

public class ManagedTokenVerificator {
    
    private static ObjectMapper mapper = new ObjectMapper();
    
    /** managedtoken is validated and requestedByUserID, requestedForUserID values are validated aganist the managedToken
     * @param managedToken
     * @param requestedByUserID
     * @param requestedForUserID
     * @return
     */
    public static boolean verify(String managedToken, String requestedByUserID, String requestedForUserID) {
        boolean isValid = false;
        try {
            String[] tokenElements = managedToken.split("\\.");
            String header = tokenElements[0];
            String body = tokenElements[1];
            String signature = tokenElements[2];
            String payLoad = header + JsonKey.DOT_SEPARATOR + body;
            Map<Object, Object> headerData = mapper.readValue(new String(decodeFromBase64(header)) , Map.class);
            String keyId = headerData.get("kid").toString();
            ProjectLogger.log("ManagedTokenVerificator:verify: keyId: "+keyId,
              LoggerEnum.INFO.name());
            //{"parentId":"managed_user_UUID","sub":"logged_in_user_UUID111","exp":1654679872,"iat":1591607872}
            isValid = org.sunbird.auth.verifier.CryptoUtil.verifyRSASign(payLoad, decodeFromBase64(signature), org.sunbird.auth.verifier.KeyManager.getPublicKey(keyId).getPublicKey(), JsonKey.SHA_256_WITH_RSA);
            ProjectLogger.log("ManagedTokenVerificator:verify Token is: " + isValid,
              LoggerEnum.INFO.name());
            Map<String, String> dataMap = mapper.readValue(new String(decodeFromBase64(body)) , Map.class);
            //need to check the keys
            ProjectLogger.log("ManagedTokenVerificator: parent uuid: " + dataMap.get(JsonKey.PARENT_ID) +
              "managedBy uuid: " + dataMap.get(JsonKey.SUB), LoggerEnum.INFO.name());
            if(!(isValid && dataMap.get(JsonKey.PARENT_ID).equalsIgnoreCase(requestedByUserID) && dataMap.get(JsonKey.SUB).equalsIgnoreCase(requestedForUserID))) {
                isValid = false;
            }
        } catch (Exception ex) {
            return false;
        }
        return isValid;
    }

    private static byte[] decodeFromBase64(String data) {
        return Base64Util.decode(data, 11);
    }
}
