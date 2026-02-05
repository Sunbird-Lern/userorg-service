package org.sunbird.util.user;

import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.when;

import com.google.gson.JsonObject;
import org.jclouds.json.Json;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.util.DataCacheHandler;

import java.util.HashMap;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({
        "javax.management.*",
        "javax.net.ssl.*",
        "javax.security.*",
        "jdk.internal.reflect.*",
        "javax.crypto.*"
})
@PrepareForTest({
        DataCacheHandler.class
})
public class UserExtendedProfileSchemaValidatorTest {

    @Before
    public void beforeEachTest() {
        PowerMockito.mockStatic(DataCacheHandler.class);
        String extendedProfileConfig =
                "{\"profileDetails.json\":{\"$schema\":\"http://json-schema.org/draft-07/schema\",\"type\":\"object\",\"definitions\":{\"@type\":{\"type\":\"string\"},\"MaritalStatus\":{\"type\":\"string\",\"enum\":[\"Married\",\"Single\"]},\"NonEmptyStr\":{\"type\":\"string\",\"pattern\":\"\\\\A(?!\\\\s*\\\\Z).+\"}},\"properties\":{\"personalDetails\":{\"type\":\"object\",\"title\":\"The Personal Details Schema\",\"required\":[\"firstname\",\"surname\",\"maritalStatus\"],\"properties\":{\"firstname\":{\"$id\":\"#/properties/firstname\",\"$ref\":\"#/definitions/NonEmptyStr\",\"$comment\":\"User first name\"},\"surname\":{\"$id\":\"#/properties/surname\",\"$ref\":\"#/definitions/NonEmptyStr\",\"$comment\":\"User surname\"},\"maritalStatus\":{\"$id\":\"#/properties/maritalStatus\",\"$ref\":\"#/definitions/MaritalStatus\",\"$comment\":\"Marital Status\"}}}},\"title\":\"profileDetails\",\"required\":[\"personalDetails\"]}}";

        Map<String, String> configMap = new HashMap<>();
        configMap.put(JsonKey.EXTENDED_PROFILE_SCHEMA_CONFIG, extendedProfileConfig);
        when(DataCacheHandler.getConfigSettings()).thenReturn(configMap);
        UserExtendedProfileSchemaValidator.loadSchemas();
    }

    @Test
    public void loadSchemaTestSuccessful() {
        UserExtendedProfileSchemaValidator.loadSchemas();
    }

    @Test
    public void loadSchemaTestFailure() {
        String extendedProfileConfig = "[{\"key\":\"value\"}]";

        Map<String, String> configMap = new HashMap<>();
        configMap.put(JsonKey.EXTENDED_PROFILE_SCHEMA_CONFIG, extendedProfileConfig);
        when(DataCacheHandler.getConfigSettings()).thenReturn(configMap);
        try {
            UserExtendedProfileSchemaValidator.loadSchemas();
        } catch(ProjectCommonException pe) {
            assertTrue(pe.getResponseCode() == ResponseCode.extendUserProfileNotLoaded);
        }
    }

    @Test
    public void validateSchema() {
        JSONObject profileObject = new JSONObject();
        JSONObject personalDetails = new JSONObject();
        personalDetails.put("firstname", "firstname");
        personalDetails.put("surname", "surname");
        personalDetails.put("maritalStatus", "Single");
        profileObject.put("personalDetails", personalDetails);

        try {
            assertTrue(UserExtendedProfileSchemaValidator.validate("profileDetails.json", profileObject));
        } catch(Exception e) {
            assertTrue("Failed to validate schema.", false);
        }
    }

    @Test
    public void validateSchemaWithInvalidObject() {
        try {
            UserExtendedProfileSchemaValidator.validate("profileDetails.json", null);
        } catch(Exception e) {
            assertTrue("Received Exception when parsing object..", true);
        }
    }

    @Test
    public void validateSchemaWithInvalidType() {
        JSONObject profileObject = new JSONObject();
        JSONObject personalDetails = new JSONObject();
        personalDetails.put("firstname", "firstname");
        personalDetails.put("surname", "surname");
        personalDetails.put("maritalStatus", "Single");
        profileObject.put("personalDetails", personalDetails);

        try {
            UserExtendedProfileSchemaValidator.validate("myProfile", profileObject);
        } catch(Exception e) {
            assertTrue("Failed to validate schema.", true);
        }
    }

}
