package org.sunbird.user.actors;

import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import akka.pattern.Patterns;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.TelemetryEnvKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseMessage;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.UserFlagUtil;
import org.sunbird.learner.util.UserUtility;
import org.sunbird.learner.util.Util;
import org.sunbird.user.service.UserLookupService;
import org.sunbird.user.service.UserService;
import org.sunbird.user.service.impl.UserLookUpServiceImpl;
import org.sunbird.user.service.impl.UserServiceImpl;
import org.sunbird.user.util.UserUtil;
import scala.Tuple2;
import scala.concurrent.Future;

@ActorConfig(
  tasks = {
    "createUserV3",
    "createSSUUser",
  },
  asyncTasks = {},
  dispatcher = "most-used-one-dispatcher"
)
public class SSUUserCreateActor extends UserBaseActor {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private UserService userService = UserServiceImpl.getInstance();
  private Util.DbInfo userOrgDb = Util.dbInfoMap.get(JsonKey.USER_ORG_DB);
  private UserLookupService userLookupService = UserLookUpServiceImpl.getInstance();
  private ElasticSearchService esUtil = EsClientFactory.getInstance(JsonKey.REST);

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    String operation = request.getOperation();
    switch (operation) {
      case "createUserV3":
      case "createSSUUser":
        createSSUUser(request);
        break;
      default:
        onReceiveUnsupportedOperation("SSUUserCreateActor");
    }
  }

  /**
   * This method will create user in user in cassandra and update to ES as well at same time.
   *
   * @param actorMessage
   */
  private void createSSUUser(Request actorMessage) {
    actorMessage.toLower();
    Map<String, Object> userMap = actorMessage.getRequest();
    userMap.put(
        JsonKey.ROOT_ORG_ID, DataCacheHandler.getConfigSettings().get(JsonKey.CUSTODIAN_ORG_ID));
    userMap.put(
        JsonKey.CHANNEL, DataCacheHandler.getConfigSettings().get(JsonKey.CUSTODIAN_ORG_CHANNEL));
    if (actorMessage.getOperation().equalsIgnoreCase(ActorOperations.CREATE_SSU_USER.getValue())) {
      populateUserTypeAndSubType(userMap);
    }
    populateProfileUserType(userMap, actorMessage.getRequestContext());
    processSSUUser(userMap, actorMessage);
  }

  private void processSSUUser(Map<String, Object> userMap, Request actorMessage) {
    UserUtil.setUserDefaultValueForV3(userMap, actorMessage.getRequestContext());
    removeUnwanted(userMap);
    UserUtil.toLower(userMap);
    // check phone and uniqueness using user look table
    userLookupService.checkPhoneUniqueness(
        (String) userMap.get(JsonKey.PHONE), actorMessage.getRequestContext());
    userLookupService.checkEmailUniqueness(
        (String) userMap.get(JsonKey.EMAIL), actorMessage.getRequestContext());
    UserUtil.addMaskEmailAndMaskPhone(userMap);
    try {
      UserUtility.encryptUserData(userMap);
    } catch (Exception ex) {
      logger.error(actorMessage.getRequestContext(), ex.getMessage(), ex);
    }
    userMap.put(JsonKey.IS_DELETED, false);
    userMap.put(JsonKey.FLAGS_VALUE, UserFlagUtil.getFlagValue(JsonKey.STATE_VALIDATED, false));
    final String password = (String) userMap.get(JsonKey.PASSWORD);
    userMap.remove(JsonKey.PASSWORD);
    userMap.remove(JsonKey.DOB_VALIDATION_DONE);
    String userId = ProjectUtil.generateUniqueId();
    userMap.put(JsonKey.ID, userId);
    userMap.put(JsonKey.USER_ID, userId);
    Response response = userService.createUser(userMap, actorMessage.getRequestContext());
    insertIntoUserLookUp(userMap, actorMessage.getRequestContext());
    response.put(JsonKey.USER_ID, userMap.get(JsonKey.ID));
    Map<String, Object> esResponse = new HashMap<>();
    if (JsonKey.SUCCESS.equalsIgnoreCase((String) response.get(JsonKey.RESPONSE))) {
      Map<String, Object> orgMap = saveUserOrgInfo(userMap, actorMessage.getRequestContext());
      esResponse = Util.getUserDetails(userMap, orgMap, actorMessage.getRequestContext());
    } else {
      logger.info(
          actorMessage.getRequestContext(),
          "SSUUserCreateActor:processSSUUser: User creation failure");
    }
    if ("kafka".equalsIgnoreCase(ProjectUtil.getConfigValue("sunbird_user_create_sync_type"))) {
      writeDataToKafka(esResponse);
      sender().tell(response, self());
    } else {
      Future<Boolean> kcFuture =
          Futures.future(
              (Callable<Boolean>)
                  () -> {
                    try {
                      Map<String, Object> updatePasswordMap = new HashMap<>();
                      updatePasswordMap.put(JsonKey.ID, userMap.get(JsonKey.ID));
                      updatePasswordMap.put(JsonKey.PASSWORD, password);
                      return UserUtil.updatePassword(
                          updatePasswordMap, actorMessage.getRequestContext());
                    } catch (Exception e) {
                      logger.error(
                          actorMessage.getRequestContext(),
                          "Error occurred during update password : " + e.getMessage(),
                          e);
                      return false;
                    }
                  },
              getContext().dispatcher());
      Future<Response> future =
          esUtil
              .save(
                  ProjectUtil.EsType.user.getTypeName(),
                  (String) esResponse.get(JsonKey.USER_ID),
                  esResponse,
                  actorMessage.getRequestContext())
              .zip(kcFuture)
              .map(
                  new Mapper<>() {
                    @Override
                    public Response apply(Tuple2<String, Boolean> parameter) {
                      boolean updatePassResponse = parameter._2;
                      if (!updatePassResponse) {
                        response.put(
                            JsonKey.ERROR_MSG, ResponseMessage.Message.ERROR_USER_UPDATE_PASSWORD);
                      }
                      return response;
                    }
                  },
                  getContext().dispatcher());
      Patterns.pipe(future, getContext().dispatcher()).to(sender());
    }
    generateUserTelemetry(userMap, actorMessage, userId, JsonKey.CREATE);
  }

  private Map<String, Object> saveUserOrgInfo(Map<String, Object> userMap, RequestContext context) {
    Map<String, Object> userOrgMap = UserUtil.createUserOrgRequestData(userMap);
    cassandraOperation.insertRecord(
        userOrgDb.getKeySpace(), userOrgDb.getTableName(), userOrgMap, context);

    return userOrgMap;
  }
}
