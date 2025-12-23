package org.sunbird.actor.user;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.user.validator.UserCreateRequestValidator;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.dto.SearchDTO;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.location.Location;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.user.AssociationMechanism;
import org.sunbird.service.user.UserLookupService;
import org.sunbird.service.user.UserOrgService;
import org.sunbird.service.user.UserService;
import org.sunbird.service.user.impl.UserLookUpServiceImpl;
import org.sunbird.service.user.impl.UserOrgServiceImpl;
import org.sunbird.service.user.impl.UserServiceImpl;
import org.sunbird.telemetry.dto.TelemetryEnvKey;
import org.sunbird.util.*;
import org.sunbird.util.user.UserUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ManagedUserActor extends UserBaseActor {

  private final UserService userService = UserServiceImpl.getInstance();
  private final UserOrgService userOrgService = UserOrgServiceImpl.getInstance();
  private final UserLookupService userLookupService = UserLookUpServiceImpl.getInstance();
  private final UserCreateRequestValidator userCreateRequestValidator =
      new UserCreateRequestValidator();

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
        onReceiveUnsupportedOperation();
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
    if (userMap.containsKey(JsonKey.ORG_EXTERNAL_ID)) {
      userMap.remove(JsonKey.ORG_EXTERNAL_ID);
    }
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
    userCreateRequestValidator.validateLocationCodesDataType(locationCodes);
    if (CollectionUtils.isNotEmpty((List) locationCodes)) {
      List<Location> locationList = getLocationList(locationCodes, userRequest.getRequestContext());
      String stateCode = UserCreateRequestValidator.validateAndGetStateLocationCode(locationList);
      List<String> allowedLocationTypeList =
          getStateLocationTypeConfig(stateCode, userRequest.getRequestContext());
      List<String> set = new ArrayList<>();
      for (Location location : locationList) {
        // for create-MUA we allow locations upto district
        if ((location.getType().equals(JsonKey.STATE))
            || (location.getType().equals(JsonKey.DISTRICT))) {
          UserCreateRequestValidator.isValidLocationType(
              location.getType(), allowedLocationTypeList);
          set.add(location.getCode());
        }
      }
      userRequest.getRequest().put(JsonKey.LOCATION_CODES, set);
    }
  }

  private void processUserRequestV4(
      Map<String, Object> userMap, String managedBy, Request actorMessage) {
    UserUtil.setUserDefaultValue(userMap, actorMessage.getRequestContext());
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
    userLookupService.insertRecords(userMap, actorMessage.getRequestContext());
    response.put(JsonKey.USER_ID, userMap.get(JsonKey.ID));
    Map<String, Object> esResponse = new HashMap<>();
    if (JsonKey.SUCCESS.equalsIgnoreCase((String) response.get(JsonKey.RESPONSE))) {
      saveUserOrgInfo(userMap, actorMessage.getRequestContext());
      esResponse =
          userService.getUserDetailsForES(
              (String) userMap.get(JsonKey.ID), actorMessage.getRequestContext());
    } else {
      logger.info(
          actorMessage.getRequestContext(),
          "ManagedUserActor:processUserRequestV4: User creation failure");
    }
    if ("kafka".equalsIgnoreCase(ProjectUtil.getConfigValue("sunbird_user_create_sync_type"))) {
      writeDataToKafka(esResponse);
    } else {
      userService.saveUserToES(
          (String) esResponse.get(JsonKey.USER_ID), esResponse, actorMessage.getRequestContext());
    }
    sender().tell(response, sender());
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

  private void saveUserOrgInfo(Map<String, Object> userMap, RequestContext context) {
    Map<String, Object> userOrgMap = new HashMap<>();
    userOrgMap.put(JsonKey.HASHTAGID, userMap.get(JsonKey.ROOT_ORG_ID));
    userOrgMap.put(JsonKey.ID, userMap.get(JsonKey.USER_ID));
    userOrgMap.put(JsonKey.ORGANISATION_ID, userMap.get(JsonKey.ROOT_ORG_ID));
    userOrgMap.put(JsonKey.ORG_JOIN_DATE, ProjectUtil.getFormattedDate());
    userOrgMap.put(JsonKey.IS_DELETED, false);
    userOrgMap.put(JsonKey.ASSOCIATION_TYPE, AssociationMechanism.SELF_DECLARATION);
    userOrgService.registerUserToOrg(userOrgMap, context);
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
    Map<String, Object> searchRequestMap = new HashMap<>();
    Map<String, Object> filters = new HashMap<>();
    filters.put(JsonKey.MANAGED_BY, request.get(JsonKey.ID));
    searchRequestMap.put(JsonKey.FILTERS, filters);

    String sortByField = (String) request.get(JsonKey.SORTBY);
    if (StringUtils.isNotEmpty(sortByField)) {
      String order = (String) request.get(JsonKey.ORDER);
      Map<String, Object> sortBy = new HashMap<>();
      sortBy.put(sortByField, StringUtils.isEmpty(order) ? "asc" : order);
      searchRequestMap.put(JsonKey.SORT_BY, sortBy);
    }
    SearchDTO searchDTO = ElasticSearchHelper.createSearchDTO(searchRequestMap);
    Map<String, Object> searchResult =
        userService.searchUser(searchDTO, request.getRequestContext());
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
