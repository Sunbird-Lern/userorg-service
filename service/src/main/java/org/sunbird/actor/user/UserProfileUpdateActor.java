package org.sunbird.actor.user;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.dispatch.Futures;
import org.apache.pekko.dispatch.Mapper;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.user.UserDeclareEntity;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.util.user.UserUtil;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

public class UserProfileUpdateActor extends BaseActor {

  @Inject
  @Named("user_org_management_actor")
  private ActorRef userOrgManagementActor;

  @Inject
  @Named("user_external_identity_management_actor")
  private ActorRef userExternalIdManagementActor;

  @Inject
  @Named("user_self_declaration_management_actor")
  private ActorRef userSelfDeclarationManagementActor;

  @Override
  public void onReceive(Request request) throws Throwable {
    if (ActorOperations.SAVE_USER_ATTRIBUTES.getValue().equalsIgnoreCase(request.getOperation())) {
      saveUserAttributes(request);
    } else {
      onReceiveUnsupportedOperation();
    }
  }

  private void saveUserAttributes(Request request) {
    Map<String, Object> userMap = request.getRequest();
    String operationType = (String) userMap.remove(JsonKey.OPERATION_TYPE);
    List<Future<Object>> futures = getFutures(userMap, operationType, request.getRequestContext());
    Future<Iterable<Object>> futuresSequence = Futures.sequence(futures, getContext().dispatcher());
    Future<Response> consolidatedFutureResponse = getConsolidatedFutureResponse(futuresSequence);
    Patterns.pipe(consolidatedFutureResponse, getContext().dispatcher()).to(sender());
  }

  private Future<Response> getConsolidatedFutureResponse(Future<Iterable<Object>> futuresSequence) {
    return futuresSequence.map(
        new Mapper<>() {
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

  private List<Future<Object>> getFutures(
      Map<String, Object> userMap, String operationType, RequestContext context) {
    List<Future<Object>> futures = new ArrayList<>();
    String callerId = (String) userMap.remove(JsonKey.CALLER_ID);
    if (CollectionUtils.isNotEmpty((List<Map<String, String>>) userMap.get(JsonKey.EXTERNAL_IDS))) {
      List<Map<String, String>> externalIds =
          (List<Map<String, String>>) userMap.get(JsonKey.EXTERNAL_IDS);
      List<Map<String, String>> userExternalIds = new ArrayList<>();
      List<Map<String, String>> userSelfDeclaredFields = new ArrayList<>();
      for (Map<String, String> extIdMap : externalIds) {
        if (extIdMap.get(JsonKey.ID_TYPE).equalsIgnoreCase(extIdMap.get(JsonKey.PROVIDER))) {
          userExternalIds.add(extIdMap);
        } else {
          userSelfDeclaredFields.add(extIdMap);
        }
      }
      userMap.remove(JsonKey.EXTERNAL_IDS);
      if (CollectionUtils.isNotEmpty(userSelfDeclaredFields)) {
        futures.add(saveUserSelfDeclareExternalIds(userMap, userSelfDeclaredFields, context));
      }
      if (CollectionUtils.isNotEmpty(userExternalIds)) {
        futures.add(saveUserExternalIds(userMap, userExternalIds, context));
      }
    }

    if (StringUtils.isNotBlank((String) userMap.get(JsonKey.ORGANISATION_ID))
        || StringUtils.isNotBlank((String) userMap.get(JsonKey.ROOT_ORG_ID))) {
      futures.add(saveUserOrgDetails(userMap, callerId, operationType, context));
    }

    return futures;
  }

  private Future<Object> saveUserSelfDeclareExternalIds(
      Map<String, Object> userMap, List<Map<String, String>> externalIds, RequestContext context) {
    List<UserDeclareEntity> selfDeclaredFields =
        UserUtil.transformExternalIdsToSelfDeclaredRequest(externalIds, userMap);
    userMap.put(JsonKey.DECLARATIONS, selfDeclaredFields);
    return saveUserAttributes(
        userMap,
        userSelfDeclarationManagementActor,
        ActorOperations.UPSERT_USER_SELF_DECLARATIONS.getValue(),
        context);
  }

  private Future<Object> saveUserExternalIds(
      Map<String, Object> userMap, List<Map<String, String>> externalIds, RequestContext context) {
    userMap.put(JsonKey.EXTERNAL_IDS, externalIds);
    return saveUserAttributes(
        userMap,
        userExternalIdManagementActor,
        ActorOperations.UPSERT_USER_EXTERNAL_IDENTITY_DETAILS.getValue(),
        context);
  }

  private Future<Object> saveUserOrgDetails(
      Map<String, Object> userMap, String callerId, String operationType, RequestContext context) {
    String actorOperation = ActorOperations.UPDATE_USER_ORG_DETAILS.getValue();

    if (JsonKey.CREATE.equalsIgnoreCase(operationType)) {
      actorOperation = ActorOperations.INSERT_USER_ORG_DETAILS.getValue();
    }
    Map<String, Object> reqMap = new HashMap<>(userMap);
    reqMap.put(JsonKey.CALLER_ID, callerId);
    return saveUserAttributes(reqMap, userOrgManagementActor, actorOperation, context);
  }

  private Future<Object> saveUserAttributes(
      Map<String, Object> userMap,
      ActorRef actorRef,
      String actorOperation,
      RequestContext context) {
    try {
      Request request = new Request();
      request.setRequestContext(context);
      request.getRequest().putAll(userMap);
      request.setOperation(actorOperation);
      Timeout t = new Timeout(Duration.create(10, TimeUnit.SECONDS));
      return Patterns.ask(actorRef, request, t);
    } catch (Exception ex) {
      logger.error(
          context,
          "UserProfileUpdateActor:saveUserAttributes: Exception occurred with error message = "
              + ex.getMessage(),
          ex);
    }
    return null;
  }
}
