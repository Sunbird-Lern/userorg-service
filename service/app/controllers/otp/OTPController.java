package controllers.otp;

import controllers.BaseController;
import controllers.otp.validator.OTPRequestValidator;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import play.libs.F.Promise;
import play.mvc.Result;

public class OTPController extends BaseController {

  public Promise<Result> generateOTP() {
    return handleRequest(
        ActorOperations.GENERATE_OTP.getValue(),
        request().body().asJson(),
        (request) -> {
          new OTPRequestValidator().validateGenerateOTPRequest((Request) request);
          return null;
        },
        getAllRequestHeaders(request()));
  }

}
