package org.sunbird.user.actors;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.actorutil.org.OrganisationClient;
import org.sunbird.actorutil.org.impl.OrganisationClientImpl;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LocationActorOperation;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.TelemetryEnvKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.common.util.Matcher;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.UserFlagUtil;
import org.sunbird.learner.util.UserUtility;
import org.sunbird.learner.util.Util;
import org.sunbird.models.location.Location;
import org.sunbird.models.organisation.Organisation;
import org.sunbird.models.user.User;
import org.sunbird.models.user.UserDeclareEntity;
import org.sunbird.models.user.org.UserOrg;
import org.sunbird.user.dao.UserOrgDao;
import org.sunbird.user.dao.UserSelfDeclarationDao;
import org.sunbird.user.dao.impl.UserOrgDaoImpl;
import org.sunbird.user.dao.impl.UserSelfDeclarationDaoImpl;
import org.sunbird.user.service.AssociationMechanism;
import org.sunbird.user.service.UserService;
import org.sunbird.user.service.impl.UserServiceImpl;
import org.sunbird.user.util.UserActorOperations;
import org.sunbird.user.util.UserUtil;
import org.sunbird.validator.user.UserRequestValidator;

@ActorConfig(
  tasks = {"updateUser", "updateUserV2"},
  asyncTasks = {},
  dispatcher = "most-used-one-dispatcher"
)
public class UserUpdateActor extends UserBaseActor {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private UserRequestValidator userRequestValidator = new UserRequestValidator();
  private ObjectMapper mapper = new ObjectMapper();
  private UserService userService = UserServiceImpl.getInstance();
  private Util.DbInfo usrDbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
  private UserSelfDeclarationDao userSelfDeclarationDao = UserSelfDeclarationDaoImpl.getInstance();

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    String operation = request.getOperation();
    switch (operation) {
      case "updateUser":
      case "updateUserV2":
        updateUser(request);
        break;
      default:
        onReceiveUnsupportedOperation("UserUpdateActor");
    }
  }

  private void updateUser(Request actorMessage) {
    actorMessage.toLower();
    String callerId = (String) actorMessage.getContext().get(JsonKey.CALLER_ID);
    Map<String, Object> userMap = actorMessage.getRequest();
    logger.info(actorMessage.getRequestContext(), "Incoming update request body: " + userMap);
    userRequestValidator.validateUpdateUserRequest(actorMessage);
    // update externalIds provider from channel to orgId
    UserUtil.updateExternalIdsProviderWithOrgId(userMap, actorMessage.getRequestContext());
    Map<String, Object> userDbRecord =
        UserUtil.validateExternalIdsAndReturnActiveUser(userMap, actorMessage.getRequestContext());
    String managedById = (String) userDbRecord.get(JsonKey.MANAGED_BY);
    if (actorMessage.getOperation().equalsIgnoreCase(ActorOperations.UPDATE_USER_V2.getValue())) {
      populateUserTypeAndSubType(userMap);
      populateLocationCodesFromProfileLocation(userMap);
    } else {
      userMap.remove(JsonKey.PROFILE_LOCATION);
      userMap.remove(JsonKey.PROFILE_USERTYPE);
    }
    validateAndGetLocationCodes(actorMessage);
    validateUserTypeAndSubType(
        actorMessage.getRequest(), userDbRecord, actorMessage.getRequestContext());
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

    Map<String, Boolean> userBooleanMap =
        updatedUserFlagsMap(userMap, userDbRecord, actorMessage.getRequestContext());
    int userFlagValue = userFlagsToNum(userBooleanMap);
    requestMap.put(JsonKey.FLAGS_VALUE, userFlagValue);
    boolean resetPasswordLink = false;
    if (StringUtils.isNotEmpty(managedById)
        && ((StringUtils.isNotEmpty((String) requestMap.get(JsonKey.EMAIL))
            || (StringUtils.isNotEmpty((String) requestMap.get(JsonKey.PHONE)))))) {
      requestMap.put(JsonKey.MANAGED_BY, null);
      resetPasswordLink = true;
    }

    Response response =
        cassandraOperation.updateRecord(
            usrDbInfo.getKeySpace(),
            usrDbInfo.getTableName(),
            requestMap,
            actorMessage.getRequestContext());
    insertIntoUserLookUp(userLookUpData, actorMessage.getRequestContext());
    removeUserLookupEntry(userLookUpData, userDbRecord, actorMessage.getRequestContext());
    if (StringUtils.isNotBlank(callerId)) {
      userMap.put(JsonKey.ROOT_ORG_ID, actorMessage.getContext().get(JsonKey.ROOT_ORG_ID));
    }
    Response resp = null;
    if (((String) response.get(JsonKey.RESPONSE)).equalsIgnoreCase(JsonKey.SUCCESS)) {
      if (StringUtils.isNotEmpty((String) userMap.get(JsonKey.ORG_EXTERNAL_ID))) {
        OrganisationClient organisationClient = OrganisationClientImpl.getInstance();
        Map<String, Object> filters = new HashMap<>();
        filters.put(JsonKey.EXTERNAL_ID, userMap.get(JsonKey.ORG_EXTERNAL_ID));
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
        List<Organisation> organisations =
            organisationClient.esSearchOrgByFilter(filters, actorMessage.getRequestContext());
        if (organisations.size() == 0 || organisations.size() > 1) {
          logger.info(
              actorMessage.getRequestContext(),
              "Got empty search result by orgExternalId and orgLocationId : " + filters);
        } else {
          Map<String, Object> org =
              (Map<String, Object>) mapper.convertValue(organisations.get(0), Map.class);
          List<Map<String, Object>> orgList = new ArrayList();
          if (MapUtils.isNotEmpty(org)) {
            orgList.add(org);
          }
          actorMessage.getRequest().put(JsonKey.ORGANISATIONS, orgList);
          actorMessage.getRequest().put(JsonKey.ROOT_ORG_ID, userDbRecord.get(JsonKey.ROOT_ORG_ID));
          updateUserOrganisations(actorMessage);
        }
      }
      Map<String, Object> userRequest = new HashMap<>(userMap);
      userRequest.put(JsonKey.OPERATION_TYPE, JsonKey.UPDATE);
      userRequest.put(JsonKey.CALLER_ID, callerId);

      resp =
          userService.saveUserAttributes(
              userRequest,
              getActorRef(UserActorOperations.SAVE_USER_ATTRIBUTES.getValue()),
              actorMessage.getRequestContext());
    } else {
      logger.info(
          actorMessage.getRequestContext(), "UserManagementActor:updateUser: User update failure");
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
          locations =
              locationClient.getLocationByIds(
                  getActorRef(LocationActorOperation.SEARCH_LOCATION.getValue()),
                  locationIds,
                  context);
        }
      } else {
        locations =
            locationClient.getLocationsByCodes(
                getActorRef(LocationActorOperation.SEARCH_LOCATION.getValue()),
                locationCodes,
                context);
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

  private void validateUserTypeAndSubType(
      Map<String, Object> userMap, RequestContext context, String stateCode) {
    String stateCodeConfig = userRequestValidator.validateUserType(userMap, stateCode, context);
    userRequestValidator.validateUserSubType(userMap, stateCodeConfig);
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
              throw new ProjectCommonException(
                  ResponseCode.dataEncryptionError.getErrorCode(),
                  ResponseCode.dataEncryptionError.getErrorMessage(),
                  ResponseCode.dataEncryptionError.getResponseCode());
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
      List<Location> locationIdList =
          locationClient.getLocationByCodes(
              getActorRef(LocationActorOperation.GET_RELATED_LOCATION_IDS.getValue()),
              locCodeLst,
              context);
      if (CollectionUtils.isNotEmpty(locationIdList)) {
        locationIdList.forEach(
            location -> {
              externalIds.forEach(
                  externalIdMap -> {
                    if (externalIdMap.containsValue(JsonKey.DECLARED_STATE)
                        || externalIdMap.containsValue(JsonKey.DECLARED_DISTRICT)) {
                      if (location.getCode().equals(externalIdMap.get(JsonKey.ID))) {
                        externalIdMap.put(JsonKey.ID, location.getId());
                        externalIdMap.put(JsonKey.ORIGINAL_EXTERNAL_ID, location.getId());
                      }
                    }
                  });
            });
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
    validatePrimaryEmailOrPhone(userDbRecord, userReqMap);
  }

  private void validatePrimaryEmailOrPhone(
      Map<String, Object> userDbRecord, Map<String, Object> userReqMap) {
    String userPrimaryPhone = (String) userReqMap.get(JsonKey.PHONE);
    String userPrimaryEmail = (String) userReqMap.get(JsonKey.EMAIL);
    String recoveryEmail = (String) userDbRecord.get(JsonKey.RECOVERY_EMAIL);
    String recoveryPhone = (String) userDbRecord.get(JsonKey.RECOVERY_PHONE);
    if (StringUtils.isNotBlank(userPrimaryEmail)
        && Matcher.matchIdentifiers(userPrimaryEmail, recoveryEmail)) {
      throwRecoveryParamsMatchException(JsonKey.EMAIL, JsonKey.RECOVERY_EMAIL);
    }
    if (StringUtils.isNotBlank(userPrimaryPhone)
        && Matcher.matchIdentifiers(userPrimaryPhone, recoveryPhone)) {
      throwRecoveryParamsMatchException(JsonKey.PHONE, JsonKey.RECOVERY_PHONE);
    }
  }

  private void validatePrimaryAndRecoveryKeys(Map<String, Object> userReqMap) {
    String userPhone = (String) userReqMap.get(JsonKey.PHONE);
    String userEmail = (String) userReqMap.get(JsonKey.EMAIL);
    String userRecoveryEmail = (String) userReqMap.get(JsonKey.RECOVERY_EMAIL);
    String userRecoveryPhone = (String) userReqMap.get(JsonKey.RECOVERY_PHONE);
    if (StringUtils.isNotBlank(userEmail)
        && Matcher.matchIdentifiers(userEmail, userRecoveryEmail)) {
      throwRecoveryParamsMatchException(JsonKey.EMAIL, JsonKey.RECOVERY_EMAIL);
    }
    if (StringUtils.isNotBlank(userPhone)
        && Matcher.matchIdentifiers(userPhone, userRecoveryPhone)) {
      throwRecoveryParamsMatchException(JsonKey.PHONE, JsonKey.RECOVERY_PHONE);
    }
  }

  private void throwRecoveryParamsMatchException(String type, String recoveryType) {
    logger.info(
        "UserManagementActor:throwParamMatchException:".concat(recoveryType + "")
            + "should not same as primary ".concat(type + ""));
    ProjectCommonException.throwClientErrorException(
        ResponseCode.recoveryParamsMatchException,
        MessageFormat.format(
            ResponseCode.recoveryParamsMatchException.getErrorMessage(), recoveryType, type));
  }

  private Map<String, Boolean> updatedUserFlagsMap(
      Map<String, Object> userMap, Map<String, Object> userDbRecord, RequestContext context) {
    Map<String, Boolean> userBooleanMap = new HashMap<>();

    // for existing users, it won't contain state-validation
    // adding in release-2.4.0
    // userDbRecord- record from es.
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
    logger.info(
        actorMessage.getRequestContext(), "UserManagementActor: updateUserOrganisation called");
    List<Map<String, Object>> orgList = null;
    if (null != actorMessage.getRequest().get(JsonKey.ORGANISATIONS)) {
      orgList = (List<Map<String, Object>>) actorMessage.getRequest().get(JsonKey.ORGANISATIONS);
    }
    if (CollectionUtils.isNotEmpty(orgList)) {
      String userId = (String) actorMessage.getRequest().get(JsonKey.USER_ID);
      String rootOrgId = (String) actorMessage.getRequest().remove(JsonKey.ROOT_ORG_ID);
      List<Map<String, Object>> userOrgListDb =
          UserUtil.getUserOrgDetails(false, userId, actorMessage.getRequestContext());
      Map<String, Object> userOrgDbMap = new HashMap<>();
      if (CollectionUtils.isNotEmpty(userOrgListDb)) {
        userOrgListDb.forEach(
            userOrg -> userOrgDbMap.put((String) userOrg.get(JsonKey.ORGANISATION_ID), userOrg));
      }

      for (Map<String, Object> org : orgList) {
        createOrUpdateOrganisations(org, userOrgDbMap, actorMessage);
        updateUserSelfDeclaredData(actorMessage, org, userId);
      }

      String requestedBy = (String) actorMessage.getContext().get(JsonKey.REQUESTED_BY);
      removeOrganisations(userOrgDbMap, rootOrgId, requestedBy, actorMessage.getRequestContext());
      logger.info(
          actorMessage.getRequestContext(),
          "UserManagementActor:updateUserOrganisations : " + "updateUserOrganisation Completed");
    }
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
    EmailAndSmsRequest.setOperation(
        UserActorOperations.PROCESS_PASSWORD_RESET_MAIL_AND_SMS.getValue());
    tellToAnother(EmailAndSmsRequest);
  }

  private void saveUserDetailsToEs(Map<String, Object> completeUserMap, RequestContext context) {
    Request userRequest = new Request();
    userRequest.setRequestContext(context);
    userRequest.setOperation(ActorOperations.UPDATE_USER_INFO_ELASTIC.getValue());
    userRequest.getRequest().put(JsonKey.ID, completeUserMap.get(JsonKey.ID));
    logger.info(
        context, "UserManagementActor:saveUserDetailsToEs: Trigger sync of user details to ES");
    tellToAnother(userRequest);
  }
}
