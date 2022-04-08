package org.sunbird.util.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ProfileUtil {
    public static final ObjectMapper mapper = new ObjectMapper();
    private static final LoggerUtil logger = new LoggerUtil(ProfileUtil.class);

    public static Map<String,Object> toMap(String jsonString) {
        try {
            TypeReference<HashMap<String, Object>> typeRef
                    = new TypeReference<HashMap<String, Object>>() {};
            return mapper.readValue(jsonString, typeRef);
        } catch (Exception e) {
            logger.error( "ProfileUtil Exception " , e);
        }
        return null;
    }

    public static void appendIdToReferenceObjects(Map<String, Object> profile) {
        for (Map.Entry<String, Object> entry : profile.entrySet()) {
            if (entry.getValue() instanceof ArrayList) {
                List<Object> valueList = (List<Object>) entry.getValue();
                for(Object entryObj : valueList) {
                    if (entryObj instanceof HashMap) {
                        Map<String, Object> entryMap = (Map<String, Object>) entryObj;
                        if (entryMap.get(JsonKey.OSID) == null) {
                            entryMap.put(JsonKey.OSID, UUID.randomUUID().toString());
                        }
                    }
                }
            } else if (entry.getValue() instanceof HashMap) {
                appendIdToReferenceObjects((Map<String, Object>) entry.getValue());
            }
        }
        if(profile.get(JsonKey.OSID) == null) {
            profile.put(JsonKey.OSID, UUID.randomUUID().toString());
        }
    }
}
