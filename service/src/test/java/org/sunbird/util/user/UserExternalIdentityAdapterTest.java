package org.sunbird.util.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.user.UserDeclareEntity;

public class UserExternalIdentityAdapterTest {
  private ObjectMapper mapper = new ObjectMapper();

  @Test
  public void testConvertSelfDeclareFieldsToExternalIds() {
    Map<String, Object> selfDeclaredFields = getSelfDeclareFields();
    List<Map<String, String>> externalIds =
        UserExternalIdentityAdapter.convertSelfDeclareFieldsToExternalIds(selfDeclaredFields);
    String declaredEmail = "";
    String declaredPhone = "";
    for (Map<String, String> extIdMap : externalIds) {
      if (JsonKey.DECLARED_EMAIL.equals((String) extIdMap.get(JsonKey.ORIGINAL_ID_TYPE))) {
        declaredEmail = (String) extIdMap.get(JsonKey.ORIGINAL_EXTERNAL_ID);
      }
      if (JsonKey.DECLARED_PHONE.equals((String) extIdMap.get(JsonKey.ORIGINAL_ID_TYPE))) {
        declaredPhone = (String) extIdMap.get(JsonKey.ORIGINAL_EXTERNAL_ID);
      }
    }

    Assert.assertEquals("abc@tenant.com", declaredEmail);
    Assert.assertEquals("999999999", declaredPhone);
  }

  @Test
  public void testConvertExternalFieldsToSelfDeclareFields() {
    Map<String, Object> declaredFeilds = getSelfDeclareFields();
    List<Map<String, String>> externalIds =
        UserExternalIdentityAdapter.convertSelfDeclareFieldsToExternalIds(declaredFeilds);
    Map<String, Object> resultDeclaredFields =
        UserExternalIdentityAdapter.convertExternalFieldsToSelfDeclareFields(externalIds);
    Assert.assertEquals(
        declaredFeilds.get(JsonKey.USER_ID), resultDeclaredFields.get(JsonKey.USER_ID));
    Assert.assertEquals(
        ((Map<String, Object>) declaredFeilds.get(JsonKey.USER_INFO)).get(JsonKey.DECLARED_EMAIL),
        ((Map<String, Object>) resultDeclaredFields.get(JsonKey.USER_INFO))
            .get(JsonKey.DECLARED_EMAIL));
  }

  private Map<String, Object> getSelfDeclareFields() {
    UserDeclareEntity userDeclareEntity = new UserDeclareEntity();
    userDeclareEntity.setUserId("userid");
    userDeclareEntity.setOrgId("org");
    userDeclareEntity.setPersona(JsonKey.TEACHER_PERSONA);
    Map<String, Object> userInfo = new HashMap<>();
    userInfo.put(JsonKey.DECLARED_EMAIL, "abc@tenant.com");
    userInfo.put(JsonKey.DECLARED_PHONE, "999999999");
    userDeclareEntity.setUserInfo(userInfo);
    Map<String, Object> selfDeclaredMap = mapper.convertValue(userDeclareEntity, Map.class);
    return selfDeclaredMap;
  }
}
