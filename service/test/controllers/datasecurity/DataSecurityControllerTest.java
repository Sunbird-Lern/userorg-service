package controllers.datasecurity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import controllers.BaseControllerTest;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.jetty.http.HttpMethods;
import org.junit.Test;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;
import play.mvc.Result;

public class DataSecurityControllerTest extends BaseControllerTest {

  private static final String USER_ID = "someUserId";
  private static final String ENCRYPT_DATA_URL = "/v1/user/data/encrypt";
  private static final String DECRYPT_DATA_URL = "/v1/user/data/decrypt";

  @Test
  public void testEncryptDataFailureWithoutUserIds() {
    Result result =
        performTest(
            ENCRYPT_DATA_URL, HttpMethods.POST, createInvalidEncryptionDecryptionRequest(false));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testEncryptDataFailureWithInvalidUserIdsDataType() {
    Result result =
        performTest(
            ENCRYPT_DATA_URL, HttpMethods.POST, createInvalidEncryptionDecryptionRequest(true));
    assertEquals(getResponseCode(result), ResponseCode.dataTypeError.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testDecryptDataFailureWithoutUserIds() {
    Result result =
        performTest(
            DECRYPT_DATA_URL, HttpMethods.POST, createInvalidEncryptionDecryptionRequest(false));
    assertEquals(getResponseCode(result), ResponseCode.mandatoryParamsMissing.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  @Test
  public void testDecryptDataFailureWithInvalidUserIdsDataType() {
    Result result =
        performTest(
            DECRYPT_DATA_URL, HttpMethods.POST, createInvalidEncryptionDecryptionRequest(true));
    assertEquals(getResponseCode(result), ResponseCode.dataTypeError.getErrorCode());
    assertTrue(getResponseStatus(result) == 400);
  }

  private Map createInvalidEncryptionDecryptionRequest(boolean isUserIdsPresent) {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> innerMap = new HashMap<>();
    if (isUserIdsPresent) {
      innerMap.put(JsonKey.USER_IDs, USER_ID);
    }
    requestMap.put(JsonKey.REQUEST, innerMap);
    return requestMap;
  }
}
