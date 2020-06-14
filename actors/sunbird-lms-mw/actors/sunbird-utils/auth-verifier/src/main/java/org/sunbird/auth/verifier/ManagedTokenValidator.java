package org.sunbird.auth.verifier;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;

import java.util.Map;

public class ManagedTokenValidator {
    
    private static ObjectMapper mapper = new ObjectMapper();
    
    /** managedtoken is validated and requestedByUserID, requestedForUserID values are validated aganist the managedEncToken
     * @param managedEncToken
     * @param requestedByUserId
     * @param requestedForUserId
     * @return
     */
    public static String verify(String managedEncToken, String requestedByUserId, String requestedForUserId) {
        boolean isValid = false;
        String managedFor = JsonKey.UNAUTHORIZED;
        try {
            String[] tokenElements = managedEncToken.split("\\.");
            String header = tokenElements[0];
            String body = tokenElements[1];
            String signature = tokenElements[2];
            String payLoad = header + JsonKey.DOT_SEPARATOR + body;
            Map<Object, Object> headerData = mapper.readValue(new String(decodeFromBase64(header)) , Map.class);
            String keyId = headerData.get("kid").toString();
            ProjectLogger.log("ManagedTokenValidator:verify: keyId: "+keyId,
              LoggerEnum.INFO.name());
            Map<String, String> dataMap = mapper.readValue(new String(decodeFromBase64(body)) , Map.class);
            String parentId = dataMap.get(JsonKey.PARENT_ID);
            String sub = dataMap.get(JsonKey.SUB);
            ProjectLogger.log("ManagedTokenValidator: parent uuid: " + parentId +
              " managedBy uuid: " + sub + " requestedByUserID: "+ requestedByUserId + " requestedForUserId: "+ requestedForUserId, LoggerEnum.INFO.name());
            isValid = CryptoUtil.verifyRSASign(payLoad, decodeFromBase64(signature), KeyManager.getPublicKey(keyId).getPublicKey(), JsonKey.SHA_256_WITH_RSA);
            isValid &=  parentId.equalsIgnoreCase(requestedByUserId) && sub.equalsIgnoreCase(requestedForUserId);
            if(isValid) {
                managedFor = sub;
            }
        } catch (Exception ex) {
            ProjectLogger.log("Exception in ManagedTokenValidator: verify ",LoggerEnum.ERROR);
            ex.printStackTrace();
        }
        
        return managedFor;
    }

    private static byte[] decodeFromBase64(String data) {
        return Base64Util.decode(data, 11);
    }
}
