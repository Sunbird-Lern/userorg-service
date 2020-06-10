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

    private static boolean verifyRS256Token(String token) {
        try {
            String[] tokenElements = token.split("\\.");
            String header = tokenElements[0];
            String body = tokenElements[1];
            String signature = tokenElements[2];

            String payLoad = header + SEPARATOR + body;
            Map<Object, Object> headerData = mapper.readValue(new String(decodeFromBase64(header)) , Map.class);
            String keyId = headerData.get("kid").toString();
            System.out.println(keyId);
            boolean isValid = CryptoUtil.verifyRSASign(payLoad, decodeFromBase64(signature), KeyManager.getPublicKey(keyId).getPublicKey(), "SHA256withRSA");
            System.out.println("Token is " + isValid);
            return isValid;

        } catch (Exception ex) {
            return false;
        }
    }

    private static byte[] decodeFromBase64(String data) {
        return Base64Util.decode(data, 11);
    }
}
