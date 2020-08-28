package util;

import play.libs.typedmap.TypedKey;
import play.mvc.Http;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

public class Common {

    public static Map<String, String[]> getRequestHeadersInArray(Map<String, List<String>> requestHeaders) {
        Map<String, String[]> requestHeadersArray = new HashMap();
        requestHeaders.entrySet().forEach(entry -> {
            requestHeadersArray.put(entry.getKey(), (String[]) entry.getValue().toArray());
        });
        return requestHeadersArray;
    }
    public static String getFromRequest(Http.Request httpReq, TypedKey<?> attribute){
        String attributeValue = null;
        if (httpReq.attrs() != null && httpReq.attrs().containsKey(attribute)) {
            attributeValue = (String) httpReq.attrs().get(attribute);
        }
        return attributeValue;
    }
}