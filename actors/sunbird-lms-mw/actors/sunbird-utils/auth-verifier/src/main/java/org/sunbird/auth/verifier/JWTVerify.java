package org.sunbird.auth.verifier;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;

import java.util.Map;

public class JWTVerify {

    private static String SEPARATOR = ".";
    private static ObjectMapper mapper = new ObjectMapper();
    
    public static boolean verifyAuthFortoken(String token, String requestedByUserID, String requestedForUserID) {
        boolean isValid = false;
        try {
            String[] tokenElements = token.split("\\.");
            String header = tokenElements[0];
            String body = tokenElements[1];
            String signature = tokenElements[2];
            String payLoad = header + SEPARATOR + body;
            Map<Object, Object> headerData = mapper.readValue(new String(decodeFromBase64(header)) , Map.class);
            String keyId = headerData.get("kid").toString();
            ProjectLogger.log("JWTVerify:verifyAuthFortoken: keyId: "+keyId,
              LoggerEnum.INFO.name());
            //{"parentId":"managed_user_UUID","sub":"logged_in_user_UUID111","exp":1654679872,"iat":1591607872}
            isValid = org.sunbird.auth.verifier.CryptoUtil.verifyRSASign(payLoad, decodeFromBase64(signature), org.sunbird.auth.verifier.KeyManager.getPublicKey(keyId).getPublicKey(), "SHA256withRSA");
            ProjectLogger.log("JWTVerify:verifyAuthFortoken Token is: " + isValid,
              LoggerEnum.INFO.name());
            Map<String, String> dataMap = mapper.readValue(new String(decodeFromBase64(body)) , Map.class);
            //need to check the keys
            if(!(isValid && dataMap.get("parentID").equals(requestedByUserID) && dataMap.get("sub").equals(requestedForUserID)))
                isValid = false;
        } catch (Exception ex) {
            return false;
        }
        return isValid;
    }

    private static byte[] decodeFromBase64(String data) {
        return Base64Util.decode(data, 11);
    }
}
