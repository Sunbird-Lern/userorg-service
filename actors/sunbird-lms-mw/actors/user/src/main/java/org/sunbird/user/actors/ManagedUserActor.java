package org.sunbird.user.actors;

import akka.dispatch.Mapper;
import akka.pattern.Patterns;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.actorutil.user.UserClient;
import org.sunbird.actorutil.user.impl.UserClientImpl;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.TelemetryEnvKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.UserFlagUtil;
import org.sunbird.learner.util.UserUtility;
import org.sunbird.learner.util.Util;
import org.sunbird.models.location.Location;
import org.sunbird.user.service.UserService;
import org.sunbird.user.service.impl.UserServiceImpl;
import org.sunbird.user.util.UserUtil;
import scala.concurrent.Future;

@ActorConfig(
  tasks = {"createUserV4", "createManagedUser", "getManagedUsers"},
  asyncTasks = {},
  dispatcher = "most-used-one-dispatcher"
)
public class ManagedUserActor extends UserBaseActor {
  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private UserClient userClient = UserClientImpl.getInstance();
  private UserService userService = UserServiceImpl.getInstance();
  private ElasticSearchService esUtil = EsClientFactory.getInstance(JsonKey.REST);
  private Util.DbInfo userOrgDb = Util.dbInfoMap.get(JsonKey.USER_ORG_DB);

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    String operation = request.getOperation();
    switch (operation) {
      case "createUserV4":
      case "createManagedUser": // managedUser creation new version
        createManagedUser(request);
        break;
      case "getManagedUsers": // managedUser search
        getManagedUsers(request);
        break;
      default:
        onReceiveUnsupportedOperation("ManagedUserActor");
    }
  }

  /**
   * This method will create managed user in user in cassandra and update to ES as well at same
   * time. Email and phone is not provided, name and managedBy is mandatory. BMGS or Location is
   * optional
   *
   * @param actorMessage
   */
  private void createManagedUser(Request actorMessage) {
    actorMessage.toLower();
    Map<String, Object> userMap = actorMessage.getRequest();
    populateLocationCodesFromProfileLocation(userMap);
    validateLocationCodes(actorMessage);

    String managedBy = (String) userMap.get(JsonKey.MANAGED_BY);
    logger.info(
        actorMessage.getRequestContext(),
        "validateUserId :: requestedId: " + actorMessage.getContext().get(JsonKey.REQUESTED_BY));
    String userId = (String) actorMessage.getContext().get(JsonKey.REQUESTED_BY);
    userMap.put(JsonKey.CREATED_BY, userId);
    // If user account isManagedUser (managedBy passed in request) should be same as context
    // user_id
    userService.validateUserId(actorMessage, managedBy, actorMessage.getRequestContext());

    // If managedUser limit is set, validate total number of managed users against it
    UserUtil.validateManagedUserLimit(managedBy, actorMessage.getRequestContext());
    processUserRequestV4(userMap, managedBy, actorMessage);
  }

  private void validateLocationCodes(Request userRequest) {
    Object locationCodes = userRequest.getRequest().get(JsonKey.LOCATION_CODES);
    validateLocationCodesDataType(locationCodes);
    if (CollectionUtils.isNotEmpty((List) locationCodes)) {
      List<Location> locationList = getLocationList(locationCodes, userRequest.getRequestContext());
      String stateCode = validateAndGetStateLocationCode(locationList);
      List<String> allowedLocationTypeList =
          getStateLocationTypeConfig(stateCode, userRequest.getRequestContext());
      List<String> set = new ArrayList<>();
      for (Location location : locationList) {
        // for create-MUA we allow locations upto district
        if ((location.getType().equals(JsonKey.STATE))
            || (location.getType().equals(JsonKey.DISTRICT))) {
          isValidLocationType(location.getType(), allowedLocationTypeList);
          set.add(location.getCode());
        }
      }
      userRequest.getRequest().put(JsonKey.LOCATION_CODES, set);
    }
  }

  private void processUserRequestV4(
      Map<String, Object> userMap, String managedBy, Request actorMessage) {
    UserUtil.setUserDefaultValueForV3(userMap, actorMessage.getRequestContext());
    removeUnwanted(userMap);
    UserUtil.toLower(userMap);
    userMap.put(
        JsonKey.ROOT_ORG_ID, DataCacheHandler.getConfigSettings().get(JsonKey.CUSTODIAN_ORG_ID));
    userMap.put(
        JsonKey.CHANNEL, DataCacheHandler.getConfigSettings().get(JsonKey.CUSTODIAN_ORG_CHANNEL));
    Map<String, Object> managedByInfo =
        UserUtil.validateManagedByUser(managedBy, actorMessage.getRequestContext());
    convertValidatedLocationCodesToIDs(userMap, actorMessage.getRequestContext());
    ignoreOrAcceptFrameworkData(userMap, managedByInfo, actorMessage.getRequestContext());
    String userId = ProjectUtil.generateUniqueId();
    userMap.put(JsonKey.ID, userId);
    userMap.put(JsonKey.USER_ID, userId);
    UserUtil.addMaskEmailAndMaskPhone(userMap);
    try {
      UserUtility.encryptUserData(userMap);
    } catch (Exception ex) {
      logger.error(actorMessage.getRequestContext(), ex.getMessage(), ex);
    }
    userMap.put(JsonKey.IS_DELETED, false);
    userMap.put(JsonKey.FLAGS_VALUE, UserFlagUtil.getFlagValue(JsonKey.STATE_VALIDATED, false));
    userMap.remove(JsonKey.PASSWORD);
    userMap.remove(JsonKey.DOB_VALIDATION_DONE);
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
          "ManagedUserActor:processUserRequestV4: User creation failure");
    }
    if ("kafka".equalsIgnoreCase(ProjectUtil.getConfigValue("sunbird_user_create_sync_type"))) {
      writeDataToKafka(esResponse);
      sender().tell(response, self());
    } else {
      Future<Response> future =
          esUtil
              .save(
                  ProjectUtil.EsType.user.getTypeName(),
                  (String) esResponse.get(JsonKey.USER_ID),
                  esResponse,
                  actorMessage.getRequestContext())
              .map(
                  new Mapper<>() {
                    @Override
                    public Response apply(String parameter) {
                      return response;
                    }
                  },
                  context().dispatcher());
      Patterns.pipe(future, getContext().dispatcher()).to(sender());
    }
    generateUserTelemetry(userMap, actorMessage, userId, JsonKey.CREATE);
  }

  private void ignoreOrAcceptFrameworkData(
      Map<String, Object> userRequestMap,
      Map<String, Object> userDbRecord,
      RequestContext context) {
    try {
      UserUtil.validateUserFrameworkData(userRequestMap, userDbRecord, context);
    } catch (ProjectCommonException pce) {
      // Could be that the framework id or value - is invalid, missing.
      userRequestMap.remove(JsonKey.FRAMEWORK);
    }
  }

  private Map<String, Object> saveUserOrgInfo(Map<String, Object> userMap, RequestContext context) {
    Map<String, Object> userOrgMap = UserUtil.createUserOrgRequestData(userMap);
    cassandraOperation.insertRecord(
        userOrgDb.getKeySpace(), userOrgDb.getTableName(), userOrgMap, context);

    return userOrgMap;
  }

  /**
   * Get managed user list for LUA uuid (JsonKey.ID) and fetch encrypted token for eac user from
   * admin utils if the JsonKey.WITH_TOKENS value sent in query param is true
   *
   * @param request Request
   */
  private void getManagedUsers(Request request) {
    // LUA uuid/ManagedBy Id
    String uuid = (String) request.get(JsonKey.ID);

    boolean withTokens = Boolean.valueOf((String) request.get(JsonKey.WITH_TOKENS));

    Map<String, Object> searchResult =
        userClient.searchManagedUser(
            getActorRef(ActorOperations.USER_SEARCH.getValue()),
            request,
            request.getRequestContext());
    List<Map<String, Object>> userList = (List) searchResult.get(JsonKey.CONTENT);

    List<Map<String, Object>> activeUserList = null;
    if (CollectionUtils.isNotEmpty(userList)) {
      activeUserList =
          userList
              .stream()
              .filter(o -> !BooleanUtils.isTrue((Boolean) o.get(JsonKey.IS_DELETED)))
              .collect(Collectors.toList());
    }
    if (withTokens && CollectionUtils.isNotEmpty(activeUserList)) {
      // Fetch encrypted token from admin utils
      Map<String, Object> encryptedTokenList =
          userService.fetchEncryptedToken(uuid, activeUserList, request.getRequestContext());
      // encrypted token for each managedUser in respList
      userService.appendEncryptedToken(
          encryptedTokenList, activeUserList, request.getRequestContext());
    }
    Map<String, Object> responseMap = new HashMap<>();
    if (CollectionUtils.isNotEmpty(activeUserList)) {
      responseMap.put(JsonKey.CONTENT, activeUserList);
      responseMap.put(JsonKey.COUNT, activeUserList.size());
    } else {
      responseMap.put(JsonKey.CONTENT, new ArrayList<>());
      responseMap.put(JsonKey.COUNT, 0);
    }
    Response response = new Response();
    response.put(JsonKey.RESPONSE, responseMap);
    sender().tell(response, self());
  }
}
