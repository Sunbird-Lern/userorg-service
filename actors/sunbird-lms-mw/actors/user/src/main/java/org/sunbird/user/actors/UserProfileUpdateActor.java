package org.sunbird.user.actors;

import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import akka.pattern.Patterns;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.actorutil.InterServiceCommunication;
import org.sunbird.actorutil.InterServiceCommunicationFactory;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.Request;
import org.sunbird.user.util.UserActorOperations;
import scala.concurrent.Future;

@ActorConfig(
  tasks = {"saveUserAttributes"},
  asyncTasks = {"saveUserAttributes"}
)
public class UserProfileUpdateActor extends BaseActor {

  private static InterServiceCommunication interServiceCommunication =
      InterServiceCommunicationFactory.getInstance();

  @Override
  public void onReceive(Request request) throws Throwable {
    if (UserActorOperations.SAVE_USER_ATTRIBUTES
        .getValue()
        .equalsIgnoreCase(request.getOperation())) {
      saveUserAttributes(request);
    } else {
      onReceiveUnsupportedOperation("UserAttributesProcessingActor");
    }
  }

  private void saveUserAttributes(Request request) {
    Map<String, Object> userMap = request.getRequest();
    String operationType = (String) userMap.get(JsonKey.OPERATION_TYPE);
    userMap.remove(JsonKey.OPERATION_TYPE);
    List<Future<Object>> futures = getFutures(userMap, operationType);
    Future<Iterable<Object>> futuresSequence = Futures.sequence(futures, getContext().dispatcher());
    Future<Response> consolidatedFutureResponse = getConsolidatedFutureResponse(futuresSequence);
    Patterns.pipe(consolidatedFutureResponse, getContext().dispatcher()).to(sender());
  }

  private Future<Response> getConsolidatedFutureResponse(Future<Iterable<Object>> futuresSequence) {
    return futuresSequence.map(
        new Mapper<Iterable<Object>, Response>() {
          Map<String, Object> map = new HashMap<>();
          List<Object> errorList = new ArrayList<>();

          @Override
          public Response apply(Iterable<Object> futureResult) {
            for (Object object : futureResult) {
              if (object instanceof Response) {
                Response response = (Response) object;
                Map<String, Object> result = response.getResult();
                String key = (String) result.get(JsonKey.KEY);
                if (StringUtils.isNotBlank(key)) {
                  map.put(key, result.get(key));
                }
                @SuppressWarnings("unchecked")
                List<String> errMsgList = (List<String>) result.get(JsonKey.ERROR_MSG);
                if (CollectionUtils.isNotEmpty(errMsgList)) {
                  for (String err : errMsgList) {
                    Map<String, Object> map = new HashMap<>();
                    map.put(JsonKey.ATTRIBUTE, key);
                    map.put(JsonKey.MESSAGE, err);
                    errorList.add(map);
                  }
                }
              } else if (object instanceof ProjectCommonException) {
                errorList.add(((ProjectCommonException) object).getMessage());
              } else if (object instanceof Exception) {
                errorList.add(((Exception) object).getMessage());
              }
            }
            map.put(JsonKey.ERRORS, errorList);
            Response response = new Response();
            response.put(JsonKey.RESPONSE, map);
            return response;
          }
        },
        getContext().dispatcher());
  }

  @SuppressWarnings("unchecked")
  private List<Future<Object>> getFutures(Map<String, Object> userMap, String operationType) {
    List<Future<Object>> futures = new ArrayList<>();

    if (userMap.containsKey(JsonKey.ADDRESS)
        && CollectionUtils.isNotEmpty((List<Map<String, Object>>) userMap.get(JsonKey.ADDRESS))) {
      futures.add(saveAddress(userMap, operationType));
    }

    if (userMap.containsKey(JsonKey.EDUCATION)
        && CollectionUtils.isNotEmpty((List<Map<String, Object>>) userMap.get(JsonKey.EDUCATION))) {
      futures.add(saveEducation(userMap, operationType));
    }

    if (userMap.containsKey(JsonKey.JOB_PROFILE)
        && CollectionUtils.isNotEmpty(
            (List<Map<String, Object>>) userMap.get(JsonKey.JOB_PROFILE))) {
      futures.add(saveJobProfile(userMap, operationType));
    }

    if (CollectionUtils.isNotEmpty((List<Map<String, String>>) userMap.get(JsonKey.EXTERNAL_IDS))) {
      futures.add(saveUserExternalIds(userMap));
    }

    if (StringUtils.isNotBlank((String) userMap.get(JsonKey.ORGANISATION_ID))
        || StringUtils.isNotBlank((String) userMap.get(JsonKey.ROOT_ORG_ID))) {
      futures.add(saveUserOrgDetails(userMap, operationType));
    }

    return futures;
  }

  private Future<Object> saveAddress(Map<String, Object> userMap, String operationType) {
    String actorOperation = UserActorOperations.UPDATE_USER_ADDRESS.getValue();

    if (JsonKey.CREATE.equalsIgnoreCase(operationType)) {
      actorOperation = UserActorOperations.INSERT_USER_ADDRESS.getValue();
    }

    return saveUserAttributes(userMap, actorOperation);
  }

  private Future<Object> saveEducation(Map<String, Object> userMap, String operationType) {
    String actorOperation = UserActorOperations.UPDATE_USER_EDUCATION.getValue();

    if (JsonKey.CREATE.equalsIgnoreCase(operationType)) {
      actorOperation = UserActorOperations.INSERT_USER_EDUCATION.getValue();
    }

    return saveUserAttributes(userMap, actorOperation);
  }

  private Future<Object> saveJobProfile(Map<String, Object> userMap, String operationType) {
    String actorOperation = UserActorOperations.UPDATE_USER_JOB_PROFILE.getValue();

    if (JsonKey.CREATE.equalsIgnoreCase(operationType)) {
      actorOperation = UserActorOperations.INSERT_USER_JOB_PROFILE.getValue();
    }

    return saveUserAttributes(userMap, actorOperation);
  }

  private Future<Object> saveUserExternalIds(Map<String, Object> userMap) {
    return saveUserAttributes(
        userMap, UserActorOperations.UPSERT_USER_EXTERNAL_IDENTITY_DETAILS.getValue());
  }

  private Future<Object> saveUserOrgDetails(Map<String, Object> userMap, String operationType) {
    String actorOperation = UserActorOperations.UPDATE_USER_ORG_DETAILS.getValue();

    if (JsonKey.CREATE.equalsIgnoreCase(operationType)) {
      actorOperation = UserActorOperations.INSERT_USER_ORG_DETAILS.getValue();
    }

    return saveUserAttributes(userMap, actorOperation);
  }

  private Future<Object> saveUserAttributes(Map<String, Object> userMap, String actorOperation) {
    try {
      Request request = new Request();
      request.getRequest().putAll(userMap);
      request.setOperation(actorOperation);
      return interServiceCommunication.getFuture(getActorRef(actorOperation), request);
    } catch (Exception ex) {
      ProjectLogger.log(
          "UserProfileUpdateActor:saveUserAttributes: Exception occurred with error message = "
              + ex.getMessage(),
          ex);
    }
    return null;
  }
}
