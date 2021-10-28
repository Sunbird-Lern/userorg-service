package controllers.otp.validator;

import org.junit.Test;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;

public class OtpRequestValidatorTest {

  @Test(expected = ProjectCommonException.class)
  public void testValidateGenerateOtpRequest() {
    Request otpRequest = new Request();
    otpRequest.getRequest().put(JsonKey.KEY, "xyz@xyz.com");
    otpRequest.getRequest().put(JsonKey.TYPE, "email");
    otpRequest.getRequest().put(JsonKey.TEMPLATE_ID, "invalidTemplateID");
    OtpRequestValidator otpRequestValidator = new OtpRequestValidator();
    otpRequestValidator.validateGenerateOtpRequest(otpRequest);
  }
}
