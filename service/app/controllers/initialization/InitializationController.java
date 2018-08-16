package controllers.initialization;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import java.util.HashMap;
import java.util.Map;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.Request;
import org.sunbird.validator.initialisation.InitialisationRequestValidator;
import play.libs.F.Promise;
import play.mvc.Result;

/**
 * InitializationController This class contains controller methods for System Initialisation
 *
 * @author Loganathan Shanmugam
 */
public class InitializationController extends BaseController {
  private static final InitialisationRequestValidator initialisationRequestValidator =
      new InitialisationRequestValidator();
  /**
   * This method will do the initialisation process to create the first rootOrg in the system.
   *
   * @return returns the response result contains the id of the first rootOrg created or gives error
   *     response if already initalised
   */
  @SuppressWarnings("unchecked")
  public Promise<Result> initaliseRootOrg() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log(
          "InitializationController: initaliseRootOrg called" + requestData,
          LoggerEnum.INFO.name());
      Request reqObj =
          createAndInitRequest(ActorOperations.SYSTEM_INIT_ROOT_ORG.getValue(), requestData);
      initialisationRequestValidator.validateCreateFirstRootOrg(reqObj);
      HashMap<String, Object> innerMap = new HashMap<>();
      Map<String, Object> orgData = reqObj.getRequest();
      innerMap.put(JsonKey.ORGANISATION, orgData);
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }
}
