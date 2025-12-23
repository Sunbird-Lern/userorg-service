package org.sunbird.actor.user;

import org.apache.pekko.actor.ActorRef;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.user.validator.UserCreateRequestValidator;
import org.sunbird.actor.user.validator.UserRequestValidator;
import org.sunbird.dao.user.UserOrgDao;
import org.sunbird.dao.user.UserSelfDeclarationDao;
import org.sunbird.dao.user.impl.UserOrgDaoImpl;
import org.sunbird.dao.user.impl.UserSelfDeclarationDaoImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.location.Location;
import org.sunbird.model.user.User;
import org.sunbird.model.user.UserDeclareEntity;
import org.sunbird.model.user.UserOrg;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.organisation.OrgService;
import org.sunbird.service.organisation.impl.OrgServiceImpl;
import org.sunbird.service.user.AssociationMechanism;
import org.sunbird.service.user.ExtendedUserProfileService;
import org.sunbird.service.user.UserService;
import org.sunbird.service.user.impl.ExtendedUserProfileServiceImpl;
import org.sunbird.service.user.impl.UserServiceImpl;
import org.sunbird.telemetry.dto.TelemetryEnvKey;
import org.sunbird.util.*;
import org.sunbird.util.user.ProfileUtil;
import org.sunbird.util.user.UserUtil;

public class UserUpdateActor extends UserBaseActor {

  private final UserRequestValidator userRequestValidator = new UserRequestValidator();
  private final ObjectMapper mapper = new ObjectMapper();
  private final UserService userService = UserServiceImpl.getInstance();
  private final OrgService orgService = OrgServiceImpl.getInstance();
  private final UserSelfDeclarationDao userSelfDeclarationDao =
      UserSelfDeclarationDaoImpl.getInstance();
  private ExtendedUserProfileService userProfileService =
      ExtendedUserProfileServiceImpl.getInstance();

  @Inject
  @Named("user_profile_update_actor")
  private ActorRef userProfileUpdateActor;

  @Inject
  @Named("location_actor")
  private ActorRef locationActor;

  @Inject
  @Named("background_job_manager_actor")
  private ActorRef backgroundJobManager;

  @Inject
  @Named("user_on_boarding_notification_actor")
  private ActorRef userOnBoardingNotificationActor;

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    String operation = request.getOperation();
    switch (operation) {
      case "updateUser":
      case "updateUserV2":
      case "updateUserV3":
        updateUser(request);
        break;
      default:
        onReceiveUnsupportedOperation();
    }
  }

  private void updateUser(Request actorMessage) {
    actorMessage.toLower();
    String callerId = (String) actorMessage.getContext().get(JsonKey.CALLER_ID);
    Map<String, Object> userMap = actorMessage.getRequest();
    logger.info(actorMessage.getRequestContext(), "Incoming update request body: " + userMap);
    userRequestValidator.validateUpdateUserRequest(actorMessage);
    if (null != actorMessage.getRequest().get(JsonKey.PROFILE_DETAILS)) {
      userProfileService.validateProfile(actorMessage);
      convertProfileObjToString(actorMessage);
    }
    // validate organisationId if passed in requestBody
    String organisationId = (String) userMap.get(JsonKey.ORGANISATION_ID);
    if (StringUtils.isNotBlank(organisationId)) {
      Map<String, Object> org =
          orgService.getOrgById(organisationId, actorMessage.getRequestContext());
      if (MapUtils.isEmpty(org)) {
        throw new ProjectCommonException(
            ResponseCode.invalidParameterValue,
            MessageFormat.format(
                ResponseCode.invalidParameterValue.getErrorMessage(),
                organisationId,
                JsonKey.ORGANISATION_ID),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
    }
    // update externalIds provider from channel to orgId
    UserUtil.updateExternalIdsProviderWithOrgId(userMap, actorMessage.getRequestContext());
    Map<String, Object> userDbRecord =
        UserUtil.validateExternalIdsAndReturnActiveUser(userMap, actorMessage.getRequestContext());
    if (actorMessage.getOperation().equalsIgnoreCase(ActorOperations.UPDATE_USER.getValue())) {
      userMap.remove(JsonKey.PROFILE_LOCATION);
    } else {
      populateLocationCodesFromProfileLocation(userMap);
    }
    validateAndGetLocationCodes(actorMessage);
    if (actorMessage.getOperation().equalsIgnoreCase(ActorOperations.UPDATE_USER.getValue())) {
      userMap.remove(JsonKey.PROFILE_USERTYPES);
      userMap.remove(JsonKey.PROFILE_USERTYPE);
      validateUserTypeAndSubType(
          actorMessage.getRequest(), userDbRecord, actorMessage.getRequestContext());
    } else if (actorMessage
        .getOperation()
        .equalsIgnoreCase(ActorOperations.UPDATE_USER_V2.getValue())) {
      userMap.remove(JsonKey.PROFILE_USERTYPES);
      populateUserTypeAndSubType(userMap);
      validateUserTypeAndSubType(
          actorMessage.getRequest(), userDbRecord, actorMessage.getRequestContext());
    } else if (actorMessage
        .getOperation()
        .equalsIgnoreCase(ActorOperations.UPDATE_USER_V3.getValue())) {
      userMap.remove(JsonKey.PROFILE_USERTYPE);
      userMap.remove(JsonKey.USER_TYPE);
      userMap.remove(JsonKey.USER_SUB_TYPE);
      if (userMap.containsKey(JsonKey.PROFILE_USERTYPES)) {
        List<Map<String, Object>> userTypeAndSubTypes =
            (List<Map<String, Object>>) userMap.get(JsonKey.PROFILE_USERTYPES);

        List<Map<String, Object>> distinctUserTypeAndSubTypes =
            userTypeAndSubTypes
                .stream()
                .filter(s -> filterFunction(s))
                .filter(
                    distinctByValue(map -> map.get(JsonKey.TYPE) + "_" + map.get(JsonKey.SUB_TYPE)))
                .collect(Collectors.toList());

        Map<String, Object> userTypeAndSubType = distinctUserTypeAndSubTypes.get(0);
        if (MapUtils.isNotEmpty(userTypeAndSubType)) {
          userMap.put(JsonKey.USER_TYPE, userTypeAndSubType.get(JsonKey.TYPE));
          userMap.put(JsonKey.USER_SUB_TYPE, userTypeAndSubType.get(JsonKey.SUB_TYPE));
        }
        validateUserTypeAndSubType(
            actorMessage.getRequest(), userDbRecord, actorMessage.getRequestContext());
        try {
          userMap.put(
              JsonKey.PROFILE_USERTYPES, mapper.writeValueAsString(distinctUserTypeAndSubTypes));
        } catch (Exception ex) {
          logger.error(actorMessage.getRequestContext(), "Exception while mapping", ex);
          ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
        }
      }
    }
    String managedById = (String) userDbRecord.get(JsonKey.MANAGED_BY);
    if (StringUtils.isNotBlank(callerId)) {
      userService.validateUploader(actorMessage, actorMessage.getRequestContext());
    } else {
      userService.validateUserId(actorMessage, managedById, actorMessage.getRequestContext());
    }
    UserUtil.validateUserFrameworkData(userMap, userDbRecord, actorMessage.getRequestContext());
    // Check if the user is Custodian Org user
    boolean isCustodianOrgUser = isCustodianOrgUser((String) userDbRecord.get(JsonKey.ROOT_ORG_ID));
    encryptExternalDetails(userMap, userDbRecord);
    User user = mapper.convertValue(userMap, User.class);
    UserUtil.validateExternalIdsForUpdateUser(
        user, isCustodianOrgUser, actorMessage.getRequestContext());
    userMap.put(JsonKey.EXTERNAL_IDS, user.getExternalIds());
    updateLocationCodeToIds(
        (List<Map<String, String>>) userMap.get(JsonKey.EXTERNAL_IDS),
        actorMessage.getRequestContext());
    UserUtil.validateUserPhoneAndEmailUniqueness(
        user, JsonKey.UPDATE, actorMessage.getRequestContext());
    // not allowing user to update the status,provider,userName
    removeFieldsFrmReq(userMap);
    convertValidatedLocationCodesToIDs(userMap, actorMessage.getRequestContext());
    userMap.put(JsonKey.UPDATED_DATE, ProjectUtil.getFormattedDate());
    if (StringUtils.isBlank(callerId)) {
      userMap.put(JsonKey.UPDATED_BY, actorMessage.getContext().get(JsonKey.REQUESTED_BY));
    }
    UserUtil.addMaskEmailAndMaskPhone(userMap);
    Map<String, Object> requestMap = UserUtil.encryptUserData(userMap);
    validateRecoveryEmailPhone(userDbRecord, userMap);
    Map<String, Object> userLookUpData = new HashMap<>(requestMap);
    removeUnwanted(requestMap);
    if (requestMap.containsKey(JsonKey.TNC_ACCEPTED_ON)) {
      requestMap.put(
          JsonKey.TNC_ACCEPTED_ON, new Timestamp((Long) requestMap.get(JsonKey.TNC_ACCEPTED_ON)));
    }
    // update userSubType to null if userType is changed and subType are not provided
    if (requestMap.containsKey(JsonKey.USER_TYPE)
        && !requestMap.containsKey(JsonKey.USER_SUB_TYPE)) {
      requestMap.put(JsonKey.USER_SUB_TYPE, null);
    }

    Map<String, Boolean> userBooleanMap = updatedUserFlagsMap(userDbRecord);
    int userFlagValue = userFlagsToNum(userBooleanMap);
    requestMap.put(JsonKey.FLAGS_VALUE, userFlagValue);
    boolean resetPasswordLink = false;
    if (StringUtils.isNotEmpty(managedById)
        && ((StringUtils.isNotEmpty((String) requestMap.get(JsonKey.EMAIL))
            || (StringUtils.isNotEmpty((String) requestMap.get(JsonKey.PHONE)))))) {
      requestMap.put(JsonKey.MANAGED_BY, null);
      resetPasswordLink = true;
    }

    Response response = userService.updateUser(requestMap, actorMessage.getRequestContext());
    userLookupService.insertRecords(userLookUpData, actorMessage.getRequestContext());
    removeUserLookupEntry(userLookUpData, userDbRecord, actorMessage.getRequestContext());
    if (StringUtils.isNotBlank(callerId)) {
      userMap.put(JsonKey.ROOT_ORG_ID, actorMessage.getContext().get(JsonKey.ROOT_ORG_ID));
    }
    Response resp = null;
    if (((String) response.get(JsonKey.RESPONSE)).equalsIgnoreCase(JsonKey.SUCCESS)) {
      List<Map<String, Object>> orgList = new ArrayList();
      if (StringUtils.isNotEmpty((String) userMap.get(JsonKey.ORG_EXTERNAL_ID))
          || (StringUtils.isNotEmpty((String) userMap.get(JsonKey.ORGANISATION_ID)))) {
        Map<String, Object> filters = new HashMap<>();
        if ((StringUtils.isNotEmpty((String) userMap.get(JsonKey.ORG_EXTERNAL_ID)))) {
          filters.put(JsonKey.EXTERNAL_ID, userMap.get(JsonKey.ORG_EXTERNAL_ID));
        } else {
          filters.put(JsonKey.ID, userMap.get(JsonKey.ORGANISATION_ID));
        }
        filters.put(JsonKey.STATUS, ProjectUtil.Status.ACTIVE.getValue());
        if (StringUtils.isNotEmpty((String) userMap.get(JsonKey.STATE_ID))) {
          filters.put(
              String.join(".", JsonKey.ORG_LOCATION, JsonKey.ID), userMap.get(JsonKey.STATE_ID));
        } else {
          logger.info(
              actorMessage.getRequestContext(), "profileLocation is empty in user update request.");
          List<Map<String, String>> profileLocation =
              (List<Map<String, String>>) userDbRecord.get(JsonKey.PROFILE_LOCATION);
          profileLocation
              .stream()
              .forEach(
                  loc -> {
                    String locType = loc.get(JsonKey.TYPE);
                    if (JsonKey.STATE.equalsIgnoreCase(locType)) {
                      filters.put(
                          String.join(".", JsonKey.ORG_LOCATION, JsonKey.ID), loc.get(JsonKey.ID));
                    }
                  });
        }
        logger.info(
            actorMessage.getRequestContext(),
            "fetching org by orgExternalId and orgLocationId : " + filters);
        List<Map<String, Object>> organisations =
            orgService.organisationSearch(filters, actorMessage.getRequestContext());
        if (organisations.size() == 0 || organisations.size() > 1) {
          logger.info(
              actorMessage.getRequestContext(),
              "Got empty or more than one search result by orgExternalId and orgLocationId : "
                  + filters);
        } else {
          Map<String, Object> org = organisations.get(0);
          if (MapUtils.isNotEmpty(org)) {
            orgList.add(org);
          }
          actorMessage.getRequest().put(JsonKey.ORGANISATIONS, orgList);
        }
      }
      // SB-25162 - call only if profile location is passed with or without school OR if
      // organisationlist is not empty
      if (CollectionUtils.isNotEmpty(orgList) || userMap.containsKey(JsonKey.PROFILE_LOCATION)) {
        actorMessage.getRequest().put(JsonKey.ROOT_ORG_ID, userDbRecord.get(JsonKey.ROOT_ORG_ID));
        updateUserOrganisations(actorMessage);
      }

      Map<String, Object> userRequest = new HashMap<>(userMap);
      userRequest.put(JsonKey.OPERATION_TYPE, JsonKey.UPDATE);
      userRequest.put(JsonKey.CALLER_ID, callerId);

      resp =
          userService.saveUserAttributes(
              userRequest, userProfileUpdateActor, actorMessage.getRequestContext());
    } else {
      logger.info(
          actorMessage.getRequestContext(), "UserUpdateActor:updateUser: User update failure");
    }
    if (null != resp) {
      response.put(
          JsonKey.ERRORS,
          ((Map<String, Object>) resp.getResult().get(JsonKey.RESPONSE)).get(JsonKey.ERRORS));
    }
    sender().tell(response, self());
    if (resetPasswordLink) {
      sendResetPasswordLink(requestMap, actorMessage.getRequestContext());
    }
    if (null != resp) {
      Map<String, Object> completeUserDetails = new HashMap<>(userDbRecord);
      completeUserDetails.putAll(requestMap);
      saveUserDetailsToEs(completeUserDetails, actorMessage.getRequestContext());
    }
    generateUserTelemetry(
        userMap, actorMessage, (String) userMap.get(JsonKey.USER_ID), JsonKey.UPDATE);
  }

  private <T> Predicate<T> distinctByValue(Function<? super T, ?> keyExtractor) {
    Map<Object, Boolean> seen = new ConcurrentHashMap<>();
    return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
  }

  private boolean filterFunction(Map<String, Object> map) {
    return map.get(JsonKey.TYPE) != null;
  }

  private void validateUserTypeAndSubType(
      Map<String, Object> userMap, Map<String, Object> userDbRecord, RequestContext context) {
    if (null != userMap.get(JsonKey.USER_TYPE)) {
      List<String> locationCodes = (List<String>) userMap.get(JsonKey.LOCATION_CODES);
      List<Location> locations = new ArrayList<>();
      if (CollectionUtils.isEmpty(locationCodes)) {
        // userDbRecord is record from ES , so it contains complete user data and profileLocation as
        // List<Map<String, String>>
        List<Map<String, String>> profLocList =
            (List<Map<String, String>>) userDbRecord.get(JsonKey.PROFILE_LOCATION);
        List<String> locationIds = null;
        if (CollectionUtils.isNotEmpty(profLocList)) {
          locationIds =
              profLocList.stream().map(m -> m.get(JsonKey.ID)).collect(Collectors.toList());
        }
        // Get location code from user records locations Ids
        logger.info(
            context,
            String.format(
                "Locations for userId:%s is:%s", userMap.get(JsonKey.USER_ID), locationIds));
        if (CollectionUtils.isNotEmpty(locationIds)) {
          locations = searchLocationByCodesOrIds(JsonKey.ID, locationIds, context);
        }
      } else {
        logger.info(
            context,
            String.format(
                "Locations for userId:%s is:%s", userMap.get(JsonKey.USER_ID), locationCodes));
        locations = searchLocationByCodesOrIds(JsonKey.CODE, locationCodes, context);
      }
      if (CollectionUtils.isNotEmpty(locations)) {
        String stateCode = null;
        for (Location location : locations) {
          if (JsonKey.STATE.equals(location.getType())) {
            stateCode = location.getCode();
          }
        }
        logger.info(context, String.format("Validating UserType for state code:%s", stateCode));
        if (StringUtils.isNotBlank(stateCode)) {
          // Validate UserType and UserSubType configure based on user state config else user
          // default config
          validateUserTypeAndSubType(userMap, context, stateCode);
        }
      } else {
        // If location is null or empty .Validate with default config
        logger.info(
            context,
            String.format("Validating UserType for state code:%s", JsonKey.DEFAULT_PERSONA));
        validateUserTypeAndSubType(userMap, context, JsonKey.DEFAULT_PERSONA);
      }
    }
  }

  private List<Location> searchLocationByCodesOrIds(
      String codeOrId, List<String> locationCodesOrIds, RequestContext context) {
    Map<String, Object> filters = new HashMap<>();
    Map<String, Object> searchRequestMap = new HashMap<>();
    filters.put(codeOrId, locationCodesOrIds);
    searchRequestMap.put(JsonKey.FILTERS, filters);
    Response searchResponse = locationService.searchLocation(searchRequestMap, context);
    List<Map<String, Object>> responseList =
        (List<Map<String, Object>>) searchResponse.getResult().get(JsonKey.RESPONSE);
    return responseList
        .stream()
        .map(s -> mapper.convertValue(s, Location.class))
        .collect(Collectors.toList());
  }

  private void validateUserTypeAndSubType(
      Map<String, Object> userMap, RequestContext context, String stateCode) {
    String stateCodeConfig = userRequestValidator.validateUserType(userMap, stateCode, context);
    userRequestValidator.validateUserSubType(userMap, stateCodeConfig, context);
    // after all validations set userType and userSubtype to profileUsertype
    populateProfileUserType(userMap, context);
  }

  private boolean isCustodianOrgUser(String userRootOrgId) {
    String custodianRootOrgId = DataCacheHandler.getConfigSettings().get(JsonKey.CUSTODIAN_ORG_ID);
    if (StringUtils.isNotBlank(custodianRootOrgId) && StringUtils.isNotBlank(userRootOrgId)) {
      return userRootOrgId.equalsIgnoreCase(custodianRootOrgId);
    }
    return false;
  }

  private void encryptExternalDetails(
      Map<String, Object> userMap, Map<String, Object> userDbRecords) {
    List<Map<String, Object>> extList =
        (List<Map<String, Object>>) userMap.get(JsonKey.EXTERNAL_IDS);
    if (!(extList == null || extList.isEmpty())) {
      extList.forEach(
          map -> {
            try {
              String idType = (String) map.get(JsonKey.ID_TYPE);
              switch (idType) {
                case JsonKey.DECLARED_EMAIL:
                case JsonKey.DECLARED_PHONE:
                  /* Check whether email and phone contains mask value, if mask then copy the
                      encrypted value from user table
                  * */
                  if (UserUtility.isMasked((String) map.get(JsonKey.ID))) {
                    if (idType.equals(JsonKey.DECLARED_EMAIL)) {
                      map.put(JsonKey.ID, userDbRecords.get(JsonKey.EMAIL));
                    } else {
                      map.put(JsonKey.ID, userDbRecords.get(JsonKey.PHONE));
                    }
                  } else {
                    // If not masked encrypt the plain text
                    map.put(JsonKey.ID, UserUtility.encryptData((String) map.get(JsonKey.ID)));
                  }
                  break;
                default: // do nothing
              }

            } catch (Exception e) {
              logger.error("Error in encrypting in the external id details", e);
              ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
            }
          });
    }
  }

  private void updateLocationCodeToIds(
      List<Map<String, String>> externalIds, RequestContext context) {
    List<String> locCodeLst = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(externalIds)) {
      externalIds.forEach(
          externalIdMap -> {
            if (externalIdMap.containsValue(JsonKey.DECLARED_STATE)
                || externalIdMap.containsValue(JsonKey.DECLARED_DISTRICT)) {
              locCodeLst.add(externalIdMap.get(JsonKey.ID));
            }
          });
      logger.info(
          context, "updateLocationCodeToIds : Searching location for location codes " + locCodeLst);
      List<Location> locationIdList = searchLocationByCodesOrIds(JsonKey.CODE, locCodeLst, context);
      if (CollectionUtils.isNotEmpty(locationIdList)) {
        locationIdList.forEach(
            location ->
                externalIds.forEach(
                    externalIdMap -> {
                      if (externalIdMap.containsValue(JsonKey.DECLARED_STATE)
                          || externalIdMap.containsValue(JsonKey.DECLARED_DISTRICT)) {
                        if (location.getCode().equals(externalIdMap.get(JsonKey.ID))) {
                          externalIdMap.put(JsonKey.ID, location.getId());
                          externalIdMap.put(JsonKey.ORIGINAL_EXTERNAL_ID, location.getId());
                        }
                      }
                    }));
      }
    }
  }

  private void removeFieldsFrmReq(Map<String, Object> userMap) {
    userMap.remove(JsonKey.ENC_EMAIL);
    userMap.remove(JsonKey.ENC_PHONE);
    userMap.remove(JsonKey.STATUS);
    userMap.remove(JsonKey.PROVIDER);
    userMap.remove(JsonKey.USERNAME);
    userMap.remove(JsonKey.ROOT_ORG_ID);
    userMap.remove(JsonKey.LOGIN_ID);
    userMap.remove(JsonKey.ROLES);
    userMap.remove(JsonKey.CHANNEL);
    userMap.remove(JsonKey.MASKED_PHONE);
    userMap.remove(JsonKey.MASKED_EMAIL);
  }

  private void validateRecoveryEmailPhone(
      Map<String, Object> userDbRecord, Map<String, Object> userReqMap) {
    String userPrimaryPhone = (String) userDbRecord.get(JsonKey.PHONE);
    String userPrimaryEmail = (String) userDbRecord.get(JsonKey.EMAIL);
    String recoveryEmail = (String) userReqMap.get(JsonKey.RECOVERY_EMAIL);
    String recoveryPhone = (String) userReqMap.get(JsonKey.RECOVERY_PHONE);
    if (StringUtils.isNotBlank(recoveryEmail)
        && Matcher.matchIdentifiers(userPrimaryEmail, recoveryEmail)) {
      throwRecoveryParamsMatchException(JsonKey.EMAIL, JsonKey.RECOVERY_EMAIL);
    }
    if (StringUtils.isNotBlank(recoveryPhone)
        && Matcher.matchIdentifiers(userPrimaryPhone, recoveryPhone)) {
      throwRecoveryParamsMatchException(JsonKey.PHONE, JsonKey.RECOVERY_PHONE);
    }
    UserCreateRequestValidator.validatePrimaryEmailOrPhone(userDbRecord, userReqMap);
    UserCreateRequestValidator.validatePrimaryAndRecoveryKeys(userReqMap);
  }

  private void throwRecoveryParamsMatchException(String type, String recoveryType) {
    ProjectCommonException.throwClientErrorException(
        ResponseCode.recoveryParamsMatchException,
        MessageFormat.format(
            ResponseCode.recoveryParamsMatchException.getErrorMessage(), recoveryType, type));
  }

  private Map<String, Boolean> updatedUserFlagsMap(Map<String, Object> userDbRecord) {
    Map<String, Boolean> userBooleanMap = new HashMap<>();
    if (!userDbRecord.containsKey(JsonKey.STATE_VALIDATED)) {
      setStateValidation(userDbRecord, userBooleanMap);
    } else {
      userBooleanMap.put(
          JsonKey.STATE_VALIDATED, (boolean) userDbRecord.get(JsonKey.STATE_VALIDATED));
    }
    return userBooleanMap;
  }

  private void setStateValidation(
      Map<String, Object> requestMap, Map<String, Boolean> userBooleanMap) {
    String rootOrgId = (String) requestMap.get(JsonKey.ROOT_ORG_ID);
    String custodianRootOrgId = DataCacheHandler.getConfigSettings().get(JsonKey.CUSTODIAN_ORG_ID);
    // if the user is creating for non-custodian(i.e state) the value is set as true else false
    userBooleanMap.put(JsonKey.STATE_VALIDATED, !custodianRootOrgId.equals(rootOrgId));
  }

  private int userFlagsToNum(Map<String, Boolean> userBooleanMap) {
    int userFlagValue = 0;
    Set<Map.Entry<String, Boolean>> mapEntry = userBooleanMap.entrySet();
    for (Map.Entry<String, Boolean> entry : mapEntry) {
      if (StringUtils.isNotEmpty(entry.getKey())) {
        userFlagValue += UserFlagUtil.getFlagValue(entry.getKey(), entry.getValue());
      }
    }
    return userFlagValue;
  }

  private void removeUserLookupEntry(
      Map<String, Object> userLookUpData,
      Map<String, Object> userDbRecord,
      RequestContext requestContext) {
    List<Map<String, String>> reqList = new ArrayList<>();
    if (UserUtil.isEmailOrPhoneDiff(userLookUpData, userDbRecord, JsonKey.EMAIL)) {
      String email = (String) userDbRecord.get(JsonKey.EMAIL);
      Map<String, String> lookupMap = new LinkedHashMap<>();
      lookupMap.put(JsonKey.TYPE, JsonKey.EMAIL);
      lookupMap.put(JsonKey.VALUE, email);
      reqList.add(lookupMap);
    }
    if (UserUtil.isEmailOrPhoneDiff(userLookUpData, userDbRecord, JsonKey.PHONE)) {
      String phone = (String) userDbRecord.get(JsonKey.PHONE);
      Map<String, String> lookupMap = new LinkedHashMap<>();
      lookupMap.put(JsonKey.TYPE, JsonKey.PHONE);
      lookupMap.put(JsonKey.VALUE, phone);
      reqList.add(lookupMap);
    }
    if (CollectionUtils.isNotEmpty(reqList)) {
      userLookupService.deleteRecords(reqList, requestContext);
    }
  }

  private void updateUserOrganisations(Request actorMessage) {
    logger.info(actorMessage.getRequestContext(), "UserUpdateActor: updateUserOrganisation called");
    String userId = (String) actorMessage.getRequest().get(JsonKey.USER_ID);
    String rootOrgId = (String) actorMessage.getRequest().remove(JsonKey.ROOT_ORG_ID);
    List<Map<String, Object>> userOrgListDb =
        UserUtil.getUserOrgDetails(false, userId, actorMessage.getRequestContext());
    Map<String, Object> userOrgDbMap = new HashMap<>();
    if (CollectionUtils.isNotEmpty(userOrgListDb)) {
      userOrgListDb.forEach(
          userOrg -> userOrgDbMap.put((String) userOrg.get(JsonKey.ORGANISATION_ID), userOrg));
    }
    List<Map<String, Object>> orgList = null;
    if (null != actorMessage.getRequest().get(JsonKey.ORGANISATIONS)) {
      orgList = (List<Map<String, Object>>) actorMessage.getRequest().get(JsonKey.ORGANISATIONS);
    }
    if (CollectionUtils.isNotEmpty(orgList)) {
      for (Map<String, Object> org : orgList) {
        createOrUpdateOrganisations(org, userOrgDbMap, actorMessage);
        updateUserSelfDeclaredData(actorMessage, org, userId);
      }
    }
    String requestedBy = (String) actorMessage.getContext().get(JsonKey.REQUESTED_BY);
    removeOrganisations(userOrgDbMap, rootOrgId, requestedBy, actorMessage.getRequestContext());
    logger.info(
        actorMessage.getRequestContext(),
        "UserUpdateActor:updateUserOrganisations : " + "updateUserOrganisation Completed");
  }

  private void createOrUpdateOrganisations(
      Map<String, Object> org, Map<String, Object> userOrgDbMap, Request actorMessage) {
    UserOrgDao userOrgDao = UserOrgDaoImpl.getInstance();
    String userId = (String) actorMessage.getRequest().get(JsonKey.USER_ID);
    if (MapUtils.isNotEmpty(org)) {
      UserOrg userOrg =
          mapper.convertValue(org, UserOrg.class); // setting userOrg fields from org details
      String orgId =
          null != org.get(JsonKey.ORGANISATION_ID)
              ? (String) org.get(JsonKey.ORGANISATION_ID)
              : (String) org.get(JsonKey.ID);

      userOrg.setUserId(userId);
      userOrg.setDeleted(false);
      if (null != orgId && userOrgDbMap.containsKey(orgId)) {
        userOrg.setUpdatedDate(ProjectUtil.getFormattedDate());
        userOrg.setUpdatedBy((String) (actorMessage.getContext().get(JsonKey.REQUESTED_BY)));
        userOrg.setOrganisationId(
            (String) ((Map<String, Object>) userOrgDbMap.get(orgId)).get(JsonKey.ORGANISATION_ID));
        AssociationMechanism associationMechanism = new AssociationMechanism();
        if (null != userOrgDbMap.get(JsonKey.ASSOCIATION_TYPE)) {
          associationMechanism.setAssociationType(
              (int) ((Map<String, Object>) userOrgDbMap.get(orgId)).get(JsonKey.ASSOCIATION_TYPE));
        }
        associationMechanism.appendAssociationType(AssociationMechanism.SELF_DECLARATION);
        userOrg.setAssociationType(associationMechanism.getAssociationType());
        userOrgDao.updateUserOrg(userOrg, actorMessage.getRequestContext());
        userOrgDbMap.remove(orgId);
      } else {
        userOrg.setHashTagId((String) (org.get(JsonKey.HASHTAGID)));
        userOrg.setOrgJoinDate(ProjectUtil.getFormattedDate());
        userOrg.setAddedBy((String) actorMessage.getContext().get(JsonKey.REQUESTED_BY));
        userOrg.setId(ProjectUtil.getUniqueIdFromTimestamp(actorMessage.getEnv()));
        userOrg.setOrganisationId((String) (org.get(JsonKey.ID)));
        userOrg.setAssociationType(AssociationMechanism.SELF_DECLARATION);
        userOrgDao.createUserOrg(userOrg, actorMessage.getRequestContext());
      }
    }
  }

  private void removeOrganisations(
      Map<String, Object> userOrgDbMap,
      String rootOrgId,
      String requestedBy,
      RequestContext context) {
    Set<String> ids = userOrgDbMap.keySet();
    UserOrgDao userOrgDao = UserOrgDaoImpl.getInstance();
    ids.remove(rootOrgId);
    ObjectMapper mapper = new ObjectMapper();
    for (String id : ids) {
      UserOrg userOrg = mapper.convertValue(userOrgDbMap.get(id), UserOrg.class);
      userOrg.setDeleted(true);
      userOrg.setId((String) ((Map<String, Object>) userOrgDbMap.get(id)).get(JsonKey.ID));
      userOrg.setUpdatedDate(ProjectUtil.getFormattedDate());
      userOrg.setUpdatedBy(requestedBy);
      userOrg.setOrgLeftDate(ProjectUtil.getFormattedDate());
      userOrgDao.updateUserOrg(userOrg, context);
    }
  }

  private void updateUserSelfDeclaredData(Request actorMessage, Map org, String userId) {
    List<Map<String, Object>> declredDetails =
        userSelfDeclarationDao.getUserSelfDeclaredFields(userId, actorMessage.getRequestContext());
    if (!CollectionUtils.isEmpty(declredDetails)) {
      UserDeclareEntity userDeclareEntity =
          mapper.convertValue(declredDetails.get(0), UserDeclareEntity.class);
      Map declaredInfo = userDeclareEntity.getUserInfo();
      if (StringUtils.isEmpty((String) declaredInfo.get(JsonKey.DECLARED_SCHOOL_UDISE_CODE))
          || !org.get(JsonKey.EXTERNAL_ID)
              .equals(declaredInfo.get(JsonKey.DECLARED_SCHOOL_UDISE_CODE))) {
        declaredInfo.put(JsonKey.DECLARED_SCHOOL_UDISE_CODE, org.get(JsonKey.EXTERNAL_ID));
        declaredInfo.put(JsonKey.DECLARED_SCHOOL_NAME, org.get(JsonKey.ORG_NAME));
        userSelfDeclarationDao.upsertUserSelfDeclaredFields(
            userDeclareEntity, actorMessage.getRequestContext());
      }
    }
  }

  private void sendResetPasswordLink(Map<String, Object> userMap, RequestContext context) {
    Request EmailAndSmsRequest = new Request();
    EmailAndSmsRequest.getRequest().putAll(userMap);
    EmailAndSmsRequest.setRequestContext(context);
    EmailAndSmsRequest.setOperation(ActorOperations.PROCESS_PASSWORD_RESET_MAIL_AND_SMS.getValue());
    userOnBoardingNotificationActor.tell(EmailAndSmsRequest, self());
  }

  private void saveUserDetailsToEs(Map<String, Object> completeUserMap, RequestContext context) {
    Request userRequest = new Request();
    userRequest.setRequestContext(context);
    userRequest.setOperation(ActorOperations.UPDATE_USER_INFO_ELASTIC.getValue());
    userRequest.getRequest().put(JsonKey.ID, completeUserMap.get(JsonKey.ID));
    logger.info(context, "UserUpdateActor:saveUserDetailsToEs: Trigger sync of user details to ES");
    if (null != backgroundJobManager) {
      backgroundJobManager.tell(userRequest, self());
    }
  }

  private void convertProfileObjToString(Request actorMessage) {
    // ProfileObject is available - add 'osid' and then convert it to String.
    try {
      Map profileObject = (Map) actorMessage.getRequest().get(JsonKey.PROFILE_DETAILS);
      ProfileUtil.appendIdToReferenceObjects(profileObject);
      String profileStr = mapper.writeValueAsString(profileObject);
      actorMessage.getRequest().put(JsonKey.PROFILE_DETAILS, profileStr);
    } catch (Exception e) {
      throw new ProjectCommonException(
          ResponseCode.invalidValue,
          ProjectUtil.formatMessage(
              ResponseCode.invalidValue.getErrorMessage(), JsonKey.PROFILE_DETAILS),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }
}
