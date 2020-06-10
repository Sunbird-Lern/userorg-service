package org.sunbird.auth.verifier;

import com.fasterxml.jackson.databind.ObjectMapper;

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
            System.out.println(keyId);
            //{"parentId":"managed_user_UUID","sub":"logged_in_user_UUID111","exp":1654679872,"iat":1591607872}
            isValid = CryptoUtil.verifyRSASign(payLoad, decodeFromBase64(signature), KeyManager.getPublicKey(keyId).getPublicKey(), "SHA256withRSA");
            System.out.println("Token is " + isValid);
            Map<String, String> dataMap = mapper.readValue(new String(decodeFromBase64(body)) , Map.class);
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
