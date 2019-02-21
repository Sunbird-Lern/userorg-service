package controllers.usermanagement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import controllers.BaseControllerTest;
import org.junit.Ignore;
import org.junit.Test;
import org.sunbird.common.responsecode.ResponseCode;
import play.mvc.Result;

@Ignore
public class UserTypeControllerTest extends BaseControllerTest {

  @Test
  public void testGetUserTypeSuccess() {
    Result result = performTest("/v1/user/type/list", "GET", null);
    assertEquals(getResponseCode(result), ResponseCode.success.getErrorCode().toLowerCase());
    assertTrue(getResponseStatus(result) == 200);
  }
}
