package org.sunbird.auth.verifier;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class JWTVerify {

    private static String SEPARATOR = ".";
    private static ObjectMapper mapper = new ObjectMapper();

    public static void main(String args[]) throws Exception {
        KeyManager.init();
        verifyRS256Token("eyJhbGciOiJSUzI1NiIsImtpZCI6ImRldmljZTAifQ.eyJwYXJlbnRJZCI6Im1hbmFnZWRfdXNlcl9VVUlEIiwic3ViIjoibG9nZ2VkX2luX3VzZXJfVVVJRDExMSIsImV4cCI6MTY1NDY3OTg3MiwiaWF0IjoxNTkxNjA3ODcyfQ.GqwIc9GO3MOCCWQTO3VrI3PnnEZOkof2jw4mjeE9SADQr9o6q-ohsW5DmeZOicKPp3AsWFlzEsTVWgI143lOo0yOPeBoZCa9Rrjog-t3HP0nD4seAdjqcA7e7RV7YAwc9JNviFTQCwz02-TkeervvSnlrBG4-ep5Fu-ne7vZDmuFoGqDdfJnPj5EAFCeiXTbORTzryxz8r1CI7UIJmwsCgKorpQE0cv9wgHZS2VdHIYed4n23oO_od3y4FjixUJj-8RE5eroIDuB2s8j2LxL7HHBUDpiB4buiUlllgsZsRMB56f3xARSXYIn5AsBG5xiSalO8lN1RhBvgzmL-rZobA");
    }
    
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

    public static boolean verifyRS256Token(String token) {
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
            String decodedSig =  String.valueOf(decodeFromBase64(signature));
            boolean isValid = CryptoUtil.verifyRSASign(payLoad, decodeFromBase64(signature), KeyManager.getPublicKey(keyId).getPublicKey(), "SHA256withRSA");
            System.out.println("Token is " + isValid);
            Map<String, String> dataMap = mapper.readValue(new String(decodeFromBase64(body)) , Map.class);
            if(dataMap.get("parentID").equals() && dataMap.get("sub").equals())
            return isValid;

        } catch (Exception ex) {
            return false;
        }
    }

    private static byte[] decodeFromBase64(String data) {
        return Base64Util.decode(data, 11);
    }
}
