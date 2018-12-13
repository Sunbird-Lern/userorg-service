package controllers.otp;

import controllers.BaseController;
import controllers.otp.validator.OtpRequestValidator;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import play.libs.F.Promise;
import play.mvc.Result;

public class OtpController extends BaseController {

  public Promise<Result> generateOTP() {
    return handleRequest(
        ActorOperations.GENERATE_OTP.getValue(),
        request().body().asJson(),
        (request) -> {
          new OtpRequestValidator().validateGenerateOTPRequest((Request) request);
          return null;
        },
        getAllRequestHeaders(request()));
  }

  public Promise<Result> verifyOTP() {
    return handleRequest(
        ActorOperations.VERIFY_OTP.getValue(),
        request().body().asJson(),
        (request) -> {
          new OtpRequestValidator().validateVerifyOTPRequest((Request) request);
          return null;
        },
        getAllRequestHeaders(request()));
  }
}
