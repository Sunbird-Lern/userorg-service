package util;

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
}