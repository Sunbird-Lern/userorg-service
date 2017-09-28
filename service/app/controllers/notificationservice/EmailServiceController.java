package controllers.notificationservice;

import akka.util.Timeout;
import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestValidator;
import play.libs.F.Promise;
import play.mvc.Result;

public class EmailServiceController extends BaseController {

  /**
   * This method will add a new course entry into cassandra DB.
   * 
   * @return Promise<Result>
   */
  public Promise<Result> sendMail() {

    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("send Mail : =" + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      RequestValidator.validateSendMail(reqObj);
      reqObj.setOperation(ActorOperations.EMAIL_SERVICE.getValue());
      reqObj.setRequest_id(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.EMAIL_REQUEST, reqObj.getRequest());
      innerMap.put(JsonKey.REQUESTED_BY,ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
     
      return  actorResponseHandler(getRemoteActor(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

}
