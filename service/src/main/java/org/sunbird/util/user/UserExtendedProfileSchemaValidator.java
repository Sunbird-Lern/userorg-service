package org.sunbird.util.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.util.DataCacheHandler;

import java.util.HashMap;
import java.util.Map;

public class UserExtendedProfileSchemaValidator {
    private static final String PRIMARY_EMAIL_FIELD = "primaryEmail";

    private static LoggerUtil logger = new LoggerUtil(UserExtendedProfileSchemaValidator.class);
    private static Map<String, String> schemas = new HashMap<>();

    public static void loadSchemas() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String schemaConfig = DataCacheHandler.getConfigSettings().get(JsonKey.EXTENDED_PROFILE_SCHEMA_CONFIG);
            TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};
            Map<String, Object> schemaMap = mapper.readValue(schemaConfig, typeRef);
            for (Map.Entry entry : schemaMap.entrySet()) {
                schemas.put(entry.getKey().toString(), mapper.writeValueAsString(entry.getValue()));
            }
        } catch (Exception e) {
            logger.error("UserExtendedProfileSchemaValidator.loadSchemas :: failed to load schemas.", e);
            throw new ProjectCommonException(
                    ResponseCode.extendUserProfileNotLoaded,
                    ResponseCode.extendUserProfileNotLoaded.getErrorMessage(),
                    ResponseCode.extendUserProfileNotLoaded.getResponseCode());
        }
        logger.info( String.format("schemas size :- " + schemas.size()));
    }

    public static boolean validate(String entityType, JSONObject payload) throws Exception {
        Schema schema = getEntitySchema(entityType);
        try {
            schema.validate(payload);
            payload.put(JsonKey.MANDATORY_FIELDS_EXISTS, Boolean.TRUE);
        } catch (ValidationException e) {
            if (e.getAllMessages().toString().contains(PRIMARY_EMAIL_FIELD)) {
                throw new Exception(e.getAllMessages().toString());
            } else {
                logger.error("Mandatory attributes are not present", e);
                payload.put(JsonKey.MANDATORY_FIELDS_EXISTS, Boolean.FALSE);
            }
        }
        return true;
    }

    private static Schema getEntitySchema(String entityType) throws Exception {
        Schema schema;
        try {
            if(schemas.isEmpty()) {
                loadSchemas();
            }
            String definitionContent = schemas.get(entityType);
            JSONObject rawSchema = new JSONObject(definitionContent);
            SchemaLoader schemaLoader = SchemaLoader.builder()
                    .schemaJson(rawSchema).build();
            schema = schemaLoader.load().build();
        } catch (Exception ioe) {
            logger.error("UserExtendedProfileSchemaValidator.getEntitySchema :: failed to validate entityType : " + entityType, ioe);
            throw new Exception("can't validate, " + entityType + ": schema has a problem!");
        }
        return schema;
    }
}
