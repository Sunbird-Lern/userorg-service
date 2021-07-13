package controllers.usermanagement.validator;

import java.util.HashMap;
import java.util.Map;
import org.junit.*;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;

public class ShadowUserMigrateReqValidatorTest {

  private Request request;
  private ShadowUserMigrateReqValidator shadowUserMigrateReqValidator;

  @Before
  public void setUp() throws Exception {
    request = new Request();
  }

  @Test
  public void testMigrateReqWithoutParamExternalIdWhenActionIsAccept() {
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.USER_ID, "abc");
    reqMap.put(JsonKey.ACTION, "accept");
    reqMap.put(JsonKey.CHANNEL, "TN");
    request.setRequest(reqMap);
    shadowUserMigrateReqValidator = ShadowUserMigrateReqValidator.getInstance(request, "abc");
    try {
      shadowUserMigrateReqValidator.validate();
    } catch (Exception e) {
      Assert.assertEquals("Mandatory parameter userExtId is missing.", e.getMessage());
    }
  }

  @Test
  public void testMigrateReqWithoutParamExternalIdWhenActionIsReject() {
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.USER_ID, "abc");
    reqMap.put(JsonKey.ACTION, "reject");
    reqMap.put(JsonKey.CHANNEL, "TN");
    request.setRequest(reqMap);
    shadowUserMigrateReqValidator = ShadowUserMigrateReqValidator.getInstance(request, "abc");
    try {
      shadowUserMigrateReqValidator.validate();
      Assert.assertTrue(true);
    } catch (Exception e) {
      Assert.assertTrue(false);
    }
  }

  @Test
  public void testMigrateReqWithoutMandatoryParamAction() {
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.USER_ID, "abc");
    reqMap.put(JsonKey.USER_EXT_ID, "abc_ext_id");
    reqMap.put(JsonKey.CHANNEL, "TN");
    request.setRequest(reqMap);
    shadowUserMigrateReqValidator = ShadowUserMigrateReqValidator.getInstance(request, "abc");
    try {
      shadowUserMigrateReqValidator.validate();
    } catch (Exception e) {
      Assert.assertEquals(
          "Invalid value supplied for parameter action.Supported values are [accept, reject]",
          e.getMessage());
    }
  }

  @Test
  public void testMigrateReqWithoutMandatoryParamUserId() {
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.CHANNEL, "TN");
    reqMap.put(JsonKey.ACTION, "reject");
    reqMap.put(JsonKey.USER_EXT_ID, "abc_ext_id");
    request.setRequest(reqMap);
    shadowUserMigrateReqValidator = ShadowUserMigrateReqValidator.getInstance(request, "abc");
    try {
      shadowUserMigrateReqValidator.validate();
    } catch (Exception e) {
      Assert.assertEquals("Mandatory parameter userId is missing.", e.getMessage());
    }
  }

  @Test
  public void testMigrateReqWithInvalidValueAction() {
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.USER_ID, "abc");
    reqMap.put(JsonKey.USER_EXT_ID, "abc_ext_id");
    reqMap.put(JsonKey.CHANNEL, "TN");
    reqMap.put(JsonKey.ACTION, "action_incorrect_value");
    request.setRequest(reqMap);
    shadowUserMigrateReqValidator = ShadowUserMigrateReqValidator.getInstance(request, "abc");
    try {
      shadowUserMigrateReqValidator.validate();
    } catch (Exception e) {
      Assert.assertEquals(
          "Invalid value supplied for parameter action.Supported values are [accept, reject]",
          e.getMessage());
    }
  }

  @Test
  public void testMigrateReqWithDiffCallerId() {
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.USER_EXT_ID, "abc_ext_id");
    reqMap.put(JsonKey.USER_ID, "abc");
    reqMap.put(JsonKey.CHANNEL, "TN");
    reqMap.put(JsonKey.ACTION, "accept");
    request.setRequest(reqMap);
    shadowUserMigrateReqValidator = ShadowUserMigrateReqValidator.getInstance(request, "abcD");
    try {
      shadowUserMigrateReqValidator.validate();
      Assert.assertTrue(false);
    } catch (Exception e) {
      Assert.assertEquals(
          "Invalid value abc for parameter userId. Please provide a valid value.", e.getMessage());
    }
  }

  @Test()
  public void testMigrateReqSuccess() {
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.USER_EXT_ID, "abc_ext_id");
    reqMap.put(JsonKey.USER_ID, "abc");
    reqMap.put(JsonKey.CHANNEL, "TN");
    reqMap.put(JsonKey.ACTION, "accept");
    request.setRequest(reqMap);
    shadowUserMigrateReqValidator = ShadowUserMigrateReqValidator.getInstance(request, "abc");
    shadowUserMigrateReqValidator.validate();
  }
}
