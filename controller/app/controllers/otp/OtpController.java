package controllers.otp;

import org.apache.pekko.actor.ActorRef;
import controllers.BaseController;
import controllers.otp.validator.OtpRequestValidator;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Named;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import play.mvc.Http;
import play.mvc.Result;

public class OtpController extends BaseController {

  @Inject
  @Named("otp_actor")
  private ActorRef otpActor;

  public CompletionStage<Result> generateOTP(Http.Request httpRequest) {
    return handleRequest(
        otpActor,
        ActorOperations.GENERATE_OTP.getValue(),
        httpRequest.body().asJson(),
        (request) -> {
          new OtpRequestValidator().validateGenerateOtpRequest((Request) request);
          return null;
        },
        getAllRequestHeaders(httpRequest),
        httpRequest);
  }

  public CompletionStage<Result> verifyOTP(Http.Request httpRequest) {
    return handleRequest(
        otpActor,
        ActorOperations.VERIFY_OTP.getValue(),
        httpRequest.body().asJson(),
        (request) -> {
          new OtpRequestValidator().validateVerifyOtpRequest((Request) request);
          return null;
        },
        getAllRequestHeaders(httpRequest),
        httpRequest);
  }
}
