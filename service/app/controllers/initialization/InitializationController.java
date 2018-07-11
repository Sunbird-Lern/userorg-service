/** */
package controllers.initialization;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.ProjectUtil.EsType;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestValidator;
import play.libs.F.Promise;
import play.mvc.Result;

/**
 *
 *
 * @author Loganathan Shanmugam
 */
public class InitializationController extends BaseController {
  /**
   * This method will do the initialisation process to create the first rootOrg in the system.
   *
   * @return Promise<Result>
   */
  @SuppressWarnings("unchecked")
  public Promise<Result> initalizeSystem() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log(
          "InitializationController: initalizeSystem called" + requestData , LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      ProjectUtil.updateMapSomeValueTOLowerCase(reqObj);
      reqObj.put("remoteAddress",request().remoteAddress());
      RequestValidator.validateCreateFirstRootOrg(reqObj);
      reqObj.setOperation(ActorOperations.CREATE_FIRST_ROOTORG.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
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