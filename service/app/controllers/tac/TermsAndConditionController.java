package controllers.tac;

import controllers.BaseController;
import controllers.tac.validator.TACRequestValidator;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import play.libs.F.Promise;
import play.mvc.Result;

public class TermsAndConditionController extends BaseController {

  /**
   * Method to get the TAC
   *
   * @return Promise<Result>
   */
  public Promise<Result> getTAC() {
    return handleRequest(ActorOperations.GET_TAC.getValue());
  }
  /**
   * Method to accept the TAC
   *
   * @return Promise<Result>
   */
  public Promise<Result> acceptTAC() {
    return handleRequest(
        ActorOperations.ACCEPT_TAC.getValue(),
        request().body().asJson(),
        (request) -> {
          new TACRequestValidator().validateTACRequest((Request) request);
          return null;
        });
  }
}
