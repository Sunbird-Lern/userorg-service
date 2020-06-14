package org.sunbird.auth.verifier;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;

import java.util.HashMap;
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
            Map<String, String> map = new HashMap<String, String>();
            map.put("accessv1_key1","accessv1_key_public1");
            map.put("accessv1_key2","accessv1_key_public2");
            map.put("accessv1_key3","accessv1_key_public3");
            map.put("accessv1_key4","accessv1_key_public4");
            map.put("accessv1_key5","accessv1_key_public5");
            map.put("accessv1_key6","accessv1_key_public6");
            map.put("accessv1_key7","accessv1_key_public7");
            map.put("accessv1_key8","accessv1_key_public8");
            map.put("accessv1_key9","accessv1_key_public9");
            map.put("accessv1_key10","accessv1_key_public10");
            keyId = map.get(keyId);
            ProjectLogger.log("ManagedTokenValidator: key modified value: " + keyId, LoggerEnum.INFO.name());
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
