package controllers.organisationmanagement;

import org.apache.pekko.actor.ActorRef;
import controllers.BaseController;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Named;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.validator.orgvalidator.KeyManagementValidator;
import play.mvc.Http;
import play.mvc.Result;

/** this Class is responsible for managing the signIn and enc keys for organisation */
public class KeyManagementController extends BaseController {

  @Inject
  @Named("org_management_actor")
  private ActorRef organisationManagementActor;

  /**
   * this action method will validate and save the enc and signIn keys into organisation db.
   *
   * @return Result
   */
  public CompletionStage<Result> assignKeys(Http.Request httpRequest) {
    return handleRequest(
        organisationManagementActor,
        ActorOperations.ASSIGN_KEYS.getValue(),
        httpRequest.body().asJson(),
        orgRequest -> {
          KeyManagementValidator.getInstance((Request) orgRequest).validate();
          return null;
        },
        null,
        null,
        true,
        httpRequest);
  }
}
