package org.sunbird.user.actors;

import akka.actor.ActorRef;
import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.actorutil.location.LocationClient;
import org.sunbird.actorutil.location.impl.LocationClientImpl;
import org.sunbird.actorutil.org.OrganisationClient;
import org.sunbird.actorutil.org.impl.OrganisationClientImpl;
import org.sunbird.actorutil.systemsettings.SystemSettingClient;
import org.sunbird.actorutil.systemsettings.impl.SystemSettingClientImpl;
import org.sunbird.actorutil.user.UserClient;
import org.sunbird.actorutil.user.impl.UserClientImpl;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.GeoLocationJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LocationActorOperation;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.StringFormatter;
import org.sunbird.common.models.util.TelemetryEnvKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.common.responsecode.ResponseMessage;
import org.sunbird.common.util.Matcher;
import org.sunbird.content.store.util.ContentStoreUtil;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.kafka.client.KafkaClient;
import org.sunbird.learner.organisation.external.identity.service.OrgExternalService;
import org.sunbird.learner.organisation.service.OrgService;
import org.sunbird.learner.organisation.service.impl.OrgServiceImpl;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.FormApiUtil;
import org.sunbird.learner.util.UserFlagUtil;
import org.sunbird.learner.util.UserUtility;
import org.sunbird.learner.util.Util;
import org.sunbird.models.location.Location;
import org.sunbird.models.organisation.Organisation;
import org.sunbird.models.user.User;
import org.sunbird.models.user.UserDeclareEntity;
import org.sunbird.models.user.org.UserOrg;
import org.sunbird.telemetry.util.TelemetryUtil;
import org.sunbird.user.dao.UserOrgDao;
import org.sunbird.user.dao.UserSelfDeclarationDao;
import org.sunbird.user.dao.impl.UserOrgDaoImpl;
import org.sunbird.user.dao.impl.UserSelfDeclarationDaoImpl;
import org.sunbird.user.service.UserService;
import org.sunbird.user.service.impl.UserServiceImpl;
import org.sunbird.user.util.UserActorOperations;
import org.sunbird.user.util.UserLookUp;
import org.sunbird.user.util.UserUtil;
import org.sunbird.validator.user.UserRequestValidator;
import scala.Tuple2;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

@ActorConfig(
  tasks = {"createUser", "updateUser", "createUserV3", "createUserV4", "getManagedUsers"},
  asyncTasks = {},
  dispatcher = "most-used-one-dispatcher"
)
public class UserManagementActor extends BaseActor {
  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private UserRequestValidator userRequestValidator = new UserRequestValidator();
  private static LocationClient locationClient = LocationClientImpl.getInstance();
  private UserService userService = UserServiceImpl.getInstance();
  private SystemSettingClient systemSettingClient = SystemSettingClientImpl.getInstance();
  private OrganisationClient organisationClient = new OrganisationClientImpl();
  private OrgExternalService orgExternalService = new OrgExternalService();
  private Util.DbInfo usrDbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
  private Util.DbInfo userOrgDb = Util.dbInfoMap.get(JsonKey.USER_ORG_DB);
  private ObjectMapper mapper = new ObjectMapper();
  private ActorRef systemSettingActorRef = null;
  private static ElasticSearchService esUtil = EsClientFactory.getInstance(JsonKey.REST);
  private UserClient userClient = new UserClientImpl();
  private static UserSelfDeclarationDao userSelfDeclarationDao =
      UserSelfDeclarationDaoImpl.getInstance();

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    cacheFrameworkFieldsConfig(request.getRequestContext());
    if (systemSettingActorRef == null) {
      systemSettingActorRef = getActorRef(ActorOperations.GET_SYSTEM_SETTING.getValue());
    }
    String operation = request.getOperation();
    switch (operation) {
      case "createUser": // create User [v1,v2,v3]
        createUser(request);
        break;
      case "updateUser":
        updateUser(request);
        break;
      case "createUserV3": // signup [/v1/user/signup]
        createUserV3(request);
        break;
      case "createUserV4": // managedUser creation
        createUserV4(request);
        break;
      case "getManagedUsers": // managedUser search
        getManagedUsers(request);
        break;
      default:
        onReceiveUnsupportedOperation("UserManagementActor");
    }
  }

  /**
   * This method will create user in user in cassandra and update to ES as well at same time.
   *
   * @param actorMessage
   */
  private void createUserV3(Request actorMessage) {
    logger.info(
        actorMessage.getRequestContext(), "UserManagementActor:createUserV3 method called.");
    createUserV3_V4(actorMessage, false);
  }
  /**
   * This method will create managed user in user in cassandra and update to ES as well at same
   * time. Email and phone is not provided, name and managedBy is mandatory. BMGS or Location is
   * optional
   *
   * @param actorMessage
   */
  private void createUserV4(Request actorMessage) {
    logger.info(
        actorMessage.getRequestContext(), "UserManagementActor:createUserV4 method called.");
    validateLocationCodes(actorMessage);
    createUserV3_V4(actorMessage, true);
  }

  private void createUserV3_V4(Request actorMessage, boolean isV4) {
    actorMessage.toLower();
    Map<String, Object> userMap = actorMessage.getRequest();
    String signupType =
        (String) actorMessage.getContext().get(JsonKey.SIGNUP_TYPE) != null
            ? (String) actorMessage.getContext().get(JsonKey.SIGNUP_TYPE)
            : "";
    String source =
        (String) actorMessage.getContext().get(JsonKey.REQUEST_SOURCE) != null
            ? (String) actorMessage.getContext().get(JsonKey.REQUEST_SOURCE)
            : "";

    String managedBy = (String) userMap.get(JsonKey.MANAGED_BY);
    String channel = DataCacheHandler.getConfigSettings().get(JsonKey.CUSTODIAN_ORG_CHANNEL);
    String rootOrgId = DataCacheHandler.getConfigSettings().get(JsonKey.CUSTODIAN_ORG_ID);
    userMap.put(JsonKey.ROOT_ORG_ID, rootOrgId);
    userMap.put(JsonKey.CHANNEL, channel);
    if (isV4) {
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
    }
    processUserRequestV3_V4(userMap, signupType, source, managedBy, actorMessage);
  }

  private void cacheFrameworkFieldsConfig(RequestContext context) {
    if (MapUtils.isEmpty(DataCacheHandler.getFrameworkFieldsConfig())) {
      Map<String, List<String>> frameworkFieldsConfig =
          systemSettingClient.getSystemSettingByFieldAndKey(
              getActorRef(ActorOperations.GET_SYSTEM_SETTING.getValue()),
              JsonKey.USER_PROFILE_CONFIG,
              JsonKey.FRAMEWORK,
              new TypeReference<Map<String, List<String>>>() {},
              context);
      DataCacheHandler.setFrameworkFieldsConfig(frameworkFieldsConfig);
    }
  }

  @SuppressWarnings("unchecked")
  private void updateUser(Request actorMessage) {
    Util.initializeContext(actorMessage, TelemetryEnvKey.USER);
    actorMessage.toLower();
    String callerId = (String) actorMessage.getContext().get(JsonKey.CALLER_ID);
    boolean updateUserSchoolOrg = false;
    Map<String, Object> userMap = actorMessage.getRequest();
    logger.info(actorMessage.getRequestContext(), "Incoming update request body: " + userMap);
    userRequestValidator.validateUpdateUserRequest(actorMessage);
    validateLocationCodes(actorMessage);
    // update externalIds provider from channel to orgId
    UserUtil.updateExternalIdsProviderWithOrgId(userMap, actorMessage.getRequestContext());
    Map<String, Object> userDbRecord =
        UserUtil.validateExternalIdsAndReturnActiveUser(userMap, actorMessage.getRequestContext());
    String managedById = (String) userDbRecord.get(JsonKey.MANAGED_BY);
    validateUserTypeAndSubType(
        actorMessage.getRequest(), userDbRecord, actorMessage.getRequestContext());
    if (StringUtils.isNotBlank(callerId)) {
      userService.validateUploader(actorMessage, actorMessage.getRequestContext());
    } else {
      userService.validateUserId(actorMessage, managedById, actorMessage.getRequestContext());
    }

    validateUserFrameworkData(userMap, userDbRecord, actorMessage.getRequestContext());
    // Check if the user is Custodian Org user
    boolean isCustodianOrgUser = isCustodianOrgUser(userMap, actorMessage.getRequestContext());
    encryptExternalDetails(userMap, userDbRecord);
    User user = mapper.convertValue(userMap, User.class);
    UserUtil.validateExternalIdsForUpdateUser(
        user, isCustodianOrgUser, actorMessage.getRequestContext());
    userMap.put(JsonKey.EXTERNAL_IDS, user.getExternalIds());
    updateLocationCodeToIds(
        (List<Map<String, String>>) userMap.get(JsonKey.EXTERNAL_IDS),
        actorMessage.getRequestContext());
    UserUtil.validateUserPhoneEmailAndWebPages(
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
    if (requestMap.containsKey(JsonKey.RECOVERY_EMAIL)
        && StringUtils.isBlank((String) requestMap.get(JsonKey.RECOVERY_EMAIL))) {
      requestMap.put(JsonKey.RECOVERY_EMAIL, null);
    }
    if (requestMap.containsKey(JsonKey.RECOVERY_PHONE)
        && StringUtils.isBlank((String) requestMap.get(JsonKey.RECOVERY_PHONE))) {
      requestMap.put(JsonKey.RECOVERY_PHONE, null);
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
    // As of now disallowing updating manageble user's phone/email, will le allowed in next release
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
      Organisation organisation = null;
      if (StringUtils.isNotEmpty((String) userMap.get(JsonKey.ORG_EXTERNAL_ID))) {
        OrganisationClient organisationClient = OrganisationClientImpl.getInstance();
        organisation =
            organisationClient.esGetOrgByExternalId(
                String.valueOf(userMap.get(JsonKey.ORG_EXTERNAL_ID)),
                null,
                actorMessage.getRequestContext());
        Map<String, Object> org =
            (Map<String, Object>) mapper.convertValue(organisation, Map.class);
        List<Map<String, Object>> orgList = new ArrayList();
        orgList.add(org);
        actorMessage.getRequest().put(JsonKey.ORGANISATIONS, orgList);
        updateUserSchoolOrg =
            (boolean) actorMessage.getRequest().get(JsonKey.UPDATE_USER_SCHOOL_ORG);
      }
      updateUserOrganisations(actorMessage);
      Map<String, Object> userRequest = new HashMap<>(userMap);
      userRequest.put(JsonKey.OPERATION_TYPE, JsonKey.UPDATE);
      resp = saveUserAttributes(userRequest, actorMessage.getRequestContext());
    } else {
      logger.info(
          actorMessage.getRequestContext(), "UserManagementActor:updateUser: User update failure");
    }
    response.put(
        JsonKey.ERRORS,
        ((Map<String, Object>) resp.getResult().get(JsonKey.RESPONSE)).get(JsonKey.ERRORS));
    sender().tell(response, self());
    // Managed-users should get ResetPassword Link
    if (resetPasswordLink) {
      sendResetPasswordLink(requestMap, actorMessage.getRequestContext());
    }
    if (null != resp) {
      Map<String, Object> completeUserDetails = new HashMap<>(userDbRecord);
      completeUserDetails.putAll(requestMap);
      saveUserDetailsToEs(completeUserDetails, actorMessage.getRequestContext());
    }
    Map<String, Object> targetObject = null;
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    targetObject =
        TelemetryUtil.generateTargetObject(
            (String) userMap.get(JsonKey.USER_ID), TelemetryEnvKey.USER, JsonKey.UPDATE, null);
    TelemetryUtil.telemetryProcessingCall(
        userMap, targetObject, correlatedObject, actorMessage.getContext());
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
      UserLookUp userLookUp = new UserLookUp();
      userLookUp.deleteRecords(reqList, requestContext);
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

  /**
   * This method will encrypt the declared-email and declared-phone in external-id-details
   *
   * @param userMap
   */
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

  @SuppressWarnings("unchecked")
  private void updateUserOrganisations(Request actorMessage) {
    logger.info(
        actorMessage.getRequestContext(), "UserManagementActor: updateUserOrganisation called");
    List<Map<String, Object>> orgList = null;
    if (null != actorMessage.getRequest().get(JsonKey.ORGANISATIONS)) {
      orgList = (List<Map<String, Object>>) actorMessage.getRequest().get(JsonKey.ORGANISATIONS);
    }
    String userId = (String) actorMessage.getRequest().get(JsonKey.USER_ID);
    String rootOrgId = getUserRootOrgId(userId, actorMessage.getRequestContext());
    List<Map<String, Object>> orgListDb =
        UserUtil.getAllUserOrgDetails(userId, actorMessage.getRequestContext());
    Map<String, Object> orgDbMap = new HashMap<>();
    if (CollectionUtils.isNotEmpty(orgListDb)) {
      orgListDb.forEach(org -> orgDbMap.put((String) org.get(JsonKey.ORGANISATION_ID), org));
    }

    if (!CollectionUtils.isEmpty(orgList)) {
      for (Map<String, Object> org : orgList) {
        createOrUpdateOrganisations(org, orgDbMap, actorMessage);
        updateUserSelfDeclaredData(actorMessage, org, userId);
      }
    }
    String requestedBy = (String) actorMessage.getContext().get(JsonKey.REQUESTED_BY);
    removeOrganisations(orgDbMap, rootOrgId, requestedBy, actorMessage.getRequestContext());
    logger.info(
        actorMessage.getRequestContext(),
        "UserManagementActor:updateUserOrganisations : " + "updateUserOrganisation Completed");
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

  private String getUserRootOrgId(String userId, RequestContext context) {
    User user = userService.getUserById(userId, context);
    return user.getRootOrgId();
  }

  @SuppressWarnings("unchecked")
  private void createOrUpdateOrganisations(
      Map<String, Object> org, Map<String, Object> orgDbMap, Request actorMessage) {
    UserOrgDao userOrgDao = UserOrgDaoImpl.getInstance();
    String userId = (String) actorMessage.getRequest().get(JsonKey.USER_ID);
    if (MapUtils.isNotEmpty(org)) {
      UserOrg userOrg = mapper.convertValue(org, UserOrg.class);
      String orgId =
          null != org.get(JsonKey.ORGANISATION_ID)
              ? (String) org.get(JsonKey.ORGANISATION_ID)
              : (String) org.get(JsonKey.ID);

      userOrg.setUserId(userId);
      userOrg.setDeleted(false);
      if (null != orgId && orgDbMap.containsKey(orgId)) {
        userOrg.setUpdatedDate(ProjectUtil.getFormattedDate());
        userOrg.setUpdatedBy((String) (actorMessage.getContext().get(JsonKey.REQUESTED_BY)));
        userOrg.setOrganisationId(
            (String) ((Map<String, Object>) orgDbMap.get(orgId)).get(JsonKey.ORGANISATION_ID));
        userOrgDao.updateUserOrg(userOrg, actorMessage.getRequestContext());
        orgDbMap.remove(orgId);
      } else {
        userOrg.setHashTagId((String) (org.get(JsonKey.HASHTAGID)));
        userOrg.setOrgJoinDate(ProjectUtil.getFormattedDate());
        userOrg.setAddedBy((String) actorMessage.getContext().get(JsonKey.REQUESTED_BY));
        userOrg.setId(ProjectUtil.getUniqueIdFromTimestamp(actorMessage.getEnv()));
        userOrg.setOrganisationId((String) (org.get(JsonKey.ID)));
        userOrgDao.createUserOrg(userOrg, actorMessage.getRequestContext());
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void removeOrganisations(
      Map<String, Object> orgDbMap, String rootOrgId, String requestedBy, RequestContext context) {
    Set<String> ids = orgDbMap.keySet();
    UserOrgDao userOrgDao = UserOrgDaoImpl.getInstance();
    ids.remove(rootOrgId);
    ObjectMapper mapper = new ObjectMapper();
    for (String id : ids) {
      UserOrg userOrg = mapper.convertValue(orgDbMap.get(id), UserOrg.class);
      userOrg.setDeleted(true);
      userOrg.setId((String) ((Map<String, Object>) orgDbMap.get(id)).get(JsonKey.ID));
      userOrg.setUpdatedDate(ProjectUtil.getFormattedDate());
      userOrg.setUpdatedBy(requestedBy);
      userOrg.setOrgLeftDate(ProjectUtil.getFormattedDate());
      userOrgDao.updateUserOrg(userOrg, context);
    }
  }

  // Check if the user is Custodian Org user
  private boolean isCustodianOrgUser(Map<String, Object> userMap, RequestContext context) {
    boolean isCustodianOrgUser = false;
    String custodianRootOrgId = DataCacheHandler.getConfigSettings().get(JsonKey.CUSTODIAN_ORG_ID);
    User user = userService.getUserById((String) userMap.get(JsonKey.USER_ID), context);
    if (StringUtils.isNotBlank(custodianRootOrgId)
        && user.getRootOrgId().equalsIgnoreCase(custodianRootOrgId)) {
      isCustodianOrgUser = true;
    }
    return isCustodianOrgUser;
  }

  private void ignoreOrAcceptFrameworkData(
      Map<String, Object> userRequestMap,
      Map<String, Object> userDbRecord,
      RequestContext context) {
    try {
      validateUserFrameworkData(userRequestMap, userDbRecord, context);
    } catch (ProjectCommonException pce) {
      // Could be that the framework id or value - is invalid, missing.
      userRequestMap.remove(JsonKey.FRAMEWORK);
    }
  }

  @SuppressWarnings("unchecked")
  private void validateUserFrameworkData(
      Map<String, Object> userRequestMap,
      Map<String, Object> userDbRecord,
      RequestContext context) {
    if (userRequestMap.containsKey(JsonKey.FRAMEWORK)) {
      Map<String, Object> framework = (Map<String, Object>) userRequestMap.get(JsonKey.FRAMEWORK);
      List<String> frameworkIdList;
      if (framework.get(JsonKey.ID) instanceof String) {
        String frameworkIdString = (String) framework.remove(JsonKey.ID);
        frameworkIdList = new ArrayList<>();
        frameworkIdList.add(frameworkIdString);
        framework.put(JsonKey.ID, frameworkIdList);
      } else {
        frameworkIdList = (List<String>) framework.get(JsonKey.ID);
      }
      userRequestMap.put(JsonKey.FRAMEWORK, framework);
      List<String> frameworkFields =
          DataCacheHandler.getFrameworkFieldsConfig().get(JsonKey.FIELDS);
      List<String> frameworkMandatoryFields =
          DataCacheHandler.getFrameworkFieldsConfig().get(JsonKey.MANDATORY_FIELDS);
      userRequestValidator.validateMandatoryFrameworkFields(
          userRequestMap, frameworkFields, frameworkMandatoryFields);
      Map<String, Object> rootOrgMap =
          Util.getOrgDetails((String) userDbRecord.get(JsonKey.ROOT_ORG_ID), context);
      String hashtagId = (String) rootOrgMap.get(JsonKey.HASHTAGID);

      verifyFrameworkId(hashtagId, frameworkIdList, context);
      Map<String, List<Map<String, String>>> frameworkCachedValue =
          getFrameworkDetails(frameworkIdList.get(0), context);
      ((Map<String, Object>) userRequestMap.get(JsonKey.FRAMEWORK)).remove(JsonKey.ID);
      userRequestValidator.validateFrameworkCategoryValues(userRequestMap, frameworkCachedValue);
      ((Map<String, Object>) userRequestMap.get(JsonKey.FRAMEWORK))
          .put(JsonKey.ID, frameworkIdList);
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
    // channel update is not allowed
    userMap.remove(JsonKey.CHANNEL);
  }

  /**
   * Method to create the new user , Username should be unique .
   *
   * @param actorMessage Request
   */
  private void createUser(Request actorMessage) {
    Util.initializeContext(actorMessage, TelemetryEnvKey.USER);
    actorMessage.toLower();
    Map<String, Object> userMap = actorMessage.getRequest();
    String callerId = (String) actorMessage.getContext().get(JsonKey.CALLER_ID);
    String version = (String) actorMessage.getContext().get(JsonKey.VERSION);
    if (StringUtils.isNotBlank(version)
        && (JsonKey.VERSION_2.equalsIgnoreCase(version)
            || JsonKey.VERSION_3.equalsIgnoreCase(version))) {
      userRequestValidator.validateCreateUserV2Request(actorMessage);
      if (StringUtils.isNotBlank(callerId)) {
        userMap.put(JsonKey.ROOT_ORG_ID, actorMessage.getContext().get(JsonKey.ROOT_ORG_ID));
      }
    } else {
      userRequestValidator.validateCreateUserV1Request(actorMessage);
    }
    validateLocationCodes(actorMessage);
    validateChannelAndOrganisationId(userMap, actorMessage.getRequestContext());
    validatePrimaryAndRecoveryKeys(userMap);

    // remove these fields from req
    userMap.remove(JsonKey.ENC_EMAIL);
    userMap.remove(JsonKey.ENC_PHONE);
    actorMessage.getRequest().putAll(userMap);
    // Util.getUserProfileConfig(systemSettingActorRef);
    boolean isCustodianOrg = false;
    if (StringUtils.isBlank(callerId)) {
      userMap.put(JsonKey.CREATED_BY, actorMessage.getContext().get(JsonKey.REQUESTED_BY));
      try {
        if (StringUtils.isBlank((String) userMap.get(JsonKey.CHANNEL))
            && StringUtils.isBlank((String) userMap.get(JsonKey.ROOT_ORG_ID))) {
          String channel =
              userService.getCustodianChannel(
                  userMap, systemSettingActorRef, actorMessage.getRequestContext());
          String rootOrgId =
              userService.getRootOrgIdFromChannel(channel, actorMessage.getRequestContext());
          userMap.put(JsonKey.ROOT_ORG_ID, rootOrgId);
          userMap.put(JsonKey.CHANNEL, channel);
          isCustodianOrg = true;
        }
      } catch (Exception ex) {
        logger.error(actorMessage.getRequestContext(), ex.getMessage(), ex);
        sender().tell(ex, self());
        return;
      }
    }
    if (userMap.containsKey(JsonKey.ORG_EXTERNAL_ID)) {
      String orgExternalId = (String) userMap.get(JsonKey.ORG_EXTERNAL_ID);
      String channel = (String) userMap.get(JsonKey.CHANNEL);
      String orgId =
          orgExternalService.getOrgIdFromOrgExternalIdAndProvider(
              orgExternalId, channel, actorMessage.getRequestContext());
      if (StringUtils.isBlank(orgId)) {
        logger.info(
            actorMessage.getRequestContext(),
            "UserManagementActor:createUser: No organisation with orgExternalId = "
                + orgExternalId
                + " and channel = "
                + channel);
        ProjectCommonException.throwClientErrorException(
            ResponseCode.invalidParameterValue,
            MessageFormat.format(
                ResponseCode.invalidParameterValue.getErrorMessage(),
                orgExternalId,
                JsonKey.ORG_EXTERNAL_ID));
      }
      if (userMap.containsKey(JsonKey.ORGANISATION_ID)
          && !orgId.equals(userMap.get(JsonKey.ORGANISATION_ID))) {
        logger.info(
            actorMessage.getRequestContext(),
            "UserManagementActor:createUser Mismatch of organisation from orgExternalId="
                + orgExternalId
                + " and channel="
                + channel
                + " as organisationId="
                + orgId
                + " and request organisationId="
                + userMap.get(JsonKey.ORGANISATION_ID));
        throwParameterMismatchException(JsonKey.ORG_EXTERNAL_ID, JsonKey.ORGANISATION_ID);
      }
      userMap.remove(JsonKey.ORG_EXTERNAL_ID);
      userMap.put(JsonKey.ORGANISATION_ID, orgId);

      // Fetch locationids of the suborg and update the location of sso user
      if (!isCustodianOrg) {
        OrgService orgService = OrgServiceImpl.getInstance();
        Map<String, Object> orgMap = orgService.getOrgById(orgId, actorMessage.getRequestContext());
        if (MapUtils.isNotEmpty(orgMap)) {
          userMap.put(JsonKey.LOCATION_IDS, orgMap.get(JsonKey.LOCATION_IDS));
        }
      }
    }
    processUserRequest(userMap, callerId, actorMessage);
  }

  private void validateChannelAndOrganisationId(
      Map<String, Object> userMap, RequestContext context) {
    String organisationId = (String) userMap.get(JsonKey.ORGANISATION_ID);
    String requestedChannel = (String) userMap.get(JsonKey.CHANNEL);
    String subOrgRootOrgId = "";
    if (StringUtils.isNotBlank(organisationId)) {
      Organisation organisation = organisationClient.esGetOrgById(organisationId, context);
      if (null == organisation) {
        ProjectCommonException.throwClientErrorException(ResponseCode.invalidOrgData);
      }
      if (organisation.isRootOrg()) {
        subOrgRootOrgId = organisation.getId();
        if (StringUtils.isNotBlank(requestedChannel)
            && !requestedChannel.equalsIgnoreCase(organisation.getChannel())) {
          throwParameterMismatchException(JsonKey.CHANNEL, JsonKey.ORGANISATION_ID);
        }
        userMap.put(JsonKey.CHANNEL, organisation.getChannel());
      } else {
        subOrgRootOrgId = organisation.getRootOrgId();
        Organisation subOrgRootOrg = organisationClient.esGetOrgById(subOrgRootOrgId, context);
        if (null != subOrgRootOrg) {
          if (StringUtils.isNotBlank(requestedChannel)
              && !requestedChannel.equalsIgnoreCase(subOrgRootOrg.getChannel())) {
            throwParameterMismatchException(JsonKey.CHANNEL, JsonKey.ORGANISATION_ID);
          }
          userMap.put(JsonKey.CHANNEL, subOrgRootOrg.getChannel());
        }
      }
      userMap.put(JsonKey.ROOT_ORG_ID, subOrgRootOrgId);
    }
    String rootOrgId = "";
    if (StringUtils.isNotBlank(requestedChannel)) {
      rootOrgId = userService.getRootOrgIdFromChannel(requestedChannel, context);
      if (StringUtils.isNotBlank(subOrgRootOrgId) && !rootOrgId.equalsIgnoreCase(subOrgRootOrgId)) {
        throwParameterMismatchException(JsonKey.CHANNEL, JsonKey.ORGANISATION_ID);
      }
      userMap.put(JsonKey.ROOT_ORG_ID, rootOrgId);
    }
  }

  private void throwParameterMismatchException(String... param) {
    ProjectCommonException.throwClientErrorException(
        ResponseCode.parameterMismatch,
        MessageFormat.format(
            ResponseCode.parameterMismatch.getErrorMessage(), StringFormatter.joinByComma(param)));
  }

  private void processUserRequestV3_V4(
      Map<String, Object> userMap,
      String signupType,
      String source,
      String managedBy,
      Request actorMessage) {
    UserUtil.setUserDefaultValueForV3(userMap, actorMessage.getRequestContext());
    UserUtil.toLower(userMap);
    if (StringUtils.isEmpty(managedBy)) {
      UserLookUp userLookUp = new UserLookUp();
      // check phone and uniqueness using user look table
      userLookUp.checkPhoneUniqueness(
          (String) userMap.get(JsonKey.PHONE), actorMessage.getRequestContext());
      userLookUp.checkEmailUniqueness(
          (String) userMap.get(JsonKey.EMAIL), actorMessage.getRequestContext());
    } else {
      String channel = DataCacheHandler.getConfigSettings().get(JsonKey.CUSTODIAN_ORG_CHANNEL);
      String rootOrgId = DataCacheHandler.getConfigSettings().get(JsonKey.CUSTODIAN_ORG_ID);
      userMap.put(JsonKey.ROOT_ORG_ID, rootOrgId);
      userMap.put(JsonKey.CHANNEL, channel);
      Map<String, Object> managedByInfo =
          UserUtil.validateManagedByUser(managedBy, actorMessage.getRequestContext());
      convertValidatedLocationCodesToIDs(userMap, actorMessage.getRequestContext());
      ignoreOrAcceptFrameworkData(userMap, managedByInfo, actorMessage.getRequestContext());
    }
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
    Map<String, Boolean> userFlagsMap = new HashMap<>();
    userFlagsMap.put(JsonKey.STATE_VALIDATED, false);
    if (StringUtils.isEmpty(managedBy)) {
      userFlagsMap.put(
          JsonKey.EMAIL_VERIFIED,
          (Boolean)
              (userMap.get(JsonKey.EMAIL_VERIFIED) != null
                  ? userMap.get(JsonKey.EMAIL_VERIFIED)
                  : false));
      userFlagsMap.put(
          JsonKey.PHONE_VERIFIED,
          (Boolean)
              (userMap.get(JsonKey.PHONE_VERIFIED) != null
                  ? userMap.get(JsonKey.PHONE_VERIFIED)
                  : false));
    }
    int userFlagValue = userFlagsToNum(userFlagsMap);
    userMap.put(JsonKey.FLAGS_VALUE, userFlagValue);
    final String password = (String) userMap.get(JsonKey.PASSWORD);
    userMap.remove(JsonKey.PASSWORD);
    Response response =
        cassandraOperation.insertRecord(
            usrDbInfo.getKeySpace(),
            usrDbInfo.getTableName(),
            userMap,
            actorMessage.getRequestContext());
    insertIntoUserLookUp(userMap, actorMessage.getRequestContext());
    response.put(JsonKey.USER_ID, userMap.get(JsonKey.ID));
    Map<String, Object> esResponse = new HashMap<>();
    if (JsonKey.SUCCESS.equalsIgnoreCase((String) response.get(JsonKey.RESPONSE))) {
      Map<String, Object> orgMap = saveUserOrgInfo(userMap, actorMessage.getRequestContext());
      esResponse = Util.getUserDetails(userMap, orgMap, actorMessage.getRequestContext());
    } else {
      logger.info(
          actorMessage.getRequestContext(),
          "UserManagementActor:processUserRequest: User creation failure");
    }
    if ("kafka".equalsIgnoreCase(ProjectUtil.getConfigValue("sunbird_user_create_sync_type"))) {
      saveUserToKafka(esResponse);
      sender().tell(response, self());
    } else {
      Future<Boolean> kcFuture =
          Futures.future(
              new Callable<Boolean>() {

                @Override
                public Boolean call() {
                  try {
                    Map<String, Object> updatePasswordMap = new HashMap<String, Object>();
                    updatePasswordMap.put(JsonKey.ID, (String) userMap.get(JsonKey.ID));
                    updatePasswordMap.put(JsonKey.PASSWORD, password);
                    logger.info(
                        actorMessage.getRequestContext(),
                        "Update password value passed "
                            + password
                            + " --"
                            + (String) userMap.get(JsonKey.ID));
                    return UserUtil.updatePassword(
                        updatePasswordMap, actorMessage.getRequestContext());
                  } catch (Exception e) {
                    logger.error(
                        actorMessage.getRequestContext(),
                        "Error occurred during update password : " + e.getMessage(),
                        e);
                    return false;
                  }
                }
              },
              getContext().dispatcher());
      Future<Response> future =
          saveUserToES(esResponse, actorMessage.getRequestContext())
              .zip(kcFuture)
              .map(
                  new Mapper<Tuple2<String, Boolean>, Response>() {

                    @Override
                    public Response apply(Tuple2<String, Boolean> parameter) {
                      boolean updatePassResponse = parameter._2;
                      logger.info(
                          actorMessage.getRequestContext(),
                          "UserManagementActor:processUserRequest: Response from update password call "
                              + updatePassResponse);
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

    processTelemetry(userMap, signupType, source, userId, actorMessage.getContext());
  }

  private void processTelemetry(
      Map<String, Object> userMap,
      String signupType,
      String source,
      String userId,
      Map<String, Object> context) {
    Map<String, Object> targetObject = null;
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    Map<String, String> rollUp = new HashMap<>();
    rollUp.put("l1", (String) userMap.get(JsonKey.ROOT_ORG_ID));
    context.put(JsonKey.ROLLUP, rollUp);
    targetObject =
        TelemetryUtil.generateTargetObject(
            (String) userMap.get(JsonKey.ID), TelemetryEnvKey.USER, JsonKey.CREATE, null);
    TelemetryUtil.generateCorrelatedObject(userId, TelemetryEnvKey.USER, null, correlatedObject);
    if (StringUtils.isNotBlank(signupType)) {
      TelemetryUtil.generateCorrelatedObject(
          signupType, StringUtils.capitalize(JsonKey.SIGNUP_TYPE), null, correlatedObject);
    } else {
      logger.info("UserManagementActor:processUserRequest: No signupType found");
    }
    if (StringUtils.isNotBlank(source)) {
      TelemetryUtil.generateCorrelatedObject(
          source, StringUtils.capitalize(JsonKey.REQUEST_SOURCE), null, correlatedObject);
    } else {
      logger.info("UserManagementActor:processUserRequest: No source found");
    }

    TelemetryUtil.telemetryProcessingCall(userMap, targetObject, correlatedObject, context);
  }

  private Map<String, Object> saveUserOrgInfo(Map<String, Object> userMap, RequestContext context) {
    Map<String, Object> userOrgMap = createUserOrgRequestData(userMap);
    cassandraOperation.insertRecord(
        userOrgDb.getKeySpace(), userOrgDb.getTableName(), userOrgMap, context);

    return userOrgMap;
  }

  private Response insertIntoUserLookUp(Map<String, Object> userMap, RequestContext context) {
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> lookUp = new HashMap<>();
    if (userMap.get(JsonKey.PHONE) != null) {
      lookUp.put(JsonKey.TYPE, JsonKey.PHONE);
      lookUp.put(JsonKey.USER_ID, userMap.get(JsonKey.ID));
      lookUp.put(JsonKey.VALUE, userMap.get(JsonKey.PHONE));
      list.add(lookUp);
    }
    if (userMap.get(JsonKey.EMAIL) != null) {
      lookUp = new HashMap<>();
      lookUp.put(JsonKey.TYPE, JsonKey.EMAIL);
      lookUp.put(JsonKey.USER_ID, userMap.get(JsonKey.ID));
      lookUp.put(JsonKey.VALUE, userMap.get(JsonKey.EMAIL));
      list.add(lookUp);
    }
    if (CollectionUtils.isNotEmpty((List) userMap.get(JsonKey.EXTERNAL_IDS))) {
      Map<String, Object> externalId =
          ((List<Map<String, Object>>) userMap.get(JsonKey.EXTERNAL_IDS))
              .stream()
              .filter(
                  x -> ((String) x.get(JsonKey.ID_TYPE)).equals((String) x.get(JsonKey.PROVIDER)))
              .findFirst()
              .orElse(null);
      if (MapUtils.isNotEmpty(externalId)) {
        lookUp = new HashMap<>();
        lookUp.put(JsonKey.TYPE, JsonKey.USER_LOOKUP_FILED_EXTERNAL_ID);
        lookUp.put(JsonKey.USER_ID, userMap.get(JsonKey.ID));
        // provider is the orgId, not the channel
        lookUp.put(
            JsonKey.VALUE, externalId.get(JsonKey.ID) + "@" + externalId.get(JsonKey.PROVIDER));
        list.add(lookUp);
      }
    }
    if (userMap.get(JsonKey.USERNAME) != null) {
      lookUp = new HashMap<>();
      lookUp.put(JsonKey.TYPE, JsonKey.USER_LOOKUP_FILED_USER_NAME);
      lookUp.put(JsonKey.USER_ID, userMap.get(JsonKey.ID));
      lookUp.put(JsonKey.VALUE, userMap.get(JsonKey.USERNAME));
      list.add(lookUp);
    }
    Response response = null;
    if (CollectionUtils.isNotEmpty(list)) {
      UserLookUp userLookUp = new UserLookUp();
      response = userLookUp.insertRecords(list, context);
    }
    return response;
  }

  private Map<String, Object> createUserOrgRequestData(Map<String, Object> userMap) {
    Map<String, Object> userOrgMap = new HashMap<String, Object>();
    userOrgMap.put(JsonKey.ID, ProjectUtil.getUniqueIdFromTimestamp(1));
    userOrgMap.put(JsonKey.HASHTAGID, userMap.get(JsonKey.ROOT_ORG_ID));
    userOrgMap.put(JsonKey.USER_ID, userMap.get(JsonKey.USER_ID));
    userOrgMap.put(JsonKey.ORGANISATION_ID, userMap.get(JsonKey.ROOT_ORG_ID));
    userOrgMap.put(JsonKey.ORG_JOIN_DATE, ProjectUtil.getFormattedDate());
    userOrgMap.put(JsonKey.IS_DELETED, false);
    userOrgMap.put(JsonKey.ROLES, userMap.get(JsonKey.ROLES));
    return userOrgMap;
  }

  @SuppressWarnings("unchecked")
  private void processUserRequest(Map<String, Object> userMap, String callerId, Request request) {
    Map<String, Object> requestMap = null;
    UserUtil.setUserDefaultValue(userMap, callerId, request.getRequestContext());
    ObjectMapper mapper = new ObjectMapper();
    // Update external ids provider with OrgId
    UserUtil.updateExternalIdsProviderWithOrgId(userMap, request.getRequestContext());
    User user = mapper.convertValue(userMap, User.class);
    UserUtil.validateExternalIds(user, JsonKey.CREATE, request.getRequestContext());
    userMap.put(JsonKey.EXTERNAL_IDS, user.getExternalIds());
    UserUtil.validateUserPhoneEmailAndWebPages(user, JsonKey.CREATE, request.getRequestContext());
    convertValidatedLocationCodesToIDs(userMap, request.getRequestContext());
    UserUtil.toLower(userMap);
    String userId = ProjectUtil.generateUniqueId();
    userMap.put(JsonKey.ID, userId);
    userMap.put(JsonKey.USER_ID, userId);
    UserUtil.addMaskEmailAndMaskPhone(userMap);
    requestMap = UserUtil.encryptUserData(userMap);
    Map<String, Object> userLookUpData = new HashMap<>(requestMap);
    removeUnwanted(requestMap);
    requestMap.put(JsonKey.IS_DELETED, false);
    Map<String, Boolean> userFlagsMap = new HashMap<>();
    // checks if the user is belongs to state and sets a validation flag
    setStateValidation(requestMap, userFlagsMap, request.getRequestContext());
    userFlagsMap.put(JsonKey.EMAIL_VERIFIED, (Boolean) userMap.get(JsonKey.EMAIL_VERIFIED));
    userFlagsMap.put(JsonKey.PHONE_VERIFIED, (Boolean) userMap.get(JsonKey.PHONE_VERIFIED));
    int userFlagValue = userFlagsToNum(userFlagsMap);
    requestMap.put(JsonKey.FLAGS_VALUE, userFlagValue);
    Response response = null;
    boolean isPasswordUpdated = false;
    try {
      response =
          cassandraOperation.insertRecord(
              usrDbInfo.getKeySpace(),
              usrDbInfo.getTableName(),
              requestMap,
              request.getRequestContext());
      insertIntoUserLookUp(userLookUpData, request.getRequestContext());
      isPasswordUpdated = UserUtil.updatePassword(userMap, request.getRequestContext());

    } finally {
      if (response == null) {
        response = new Response();
      }
      response.put(JsonKey.USER_ID, userMap.get(JsonKey.ID));
      if (!isPasswordUpdated) {
        response.put(JsonKey.ERROR_MSG, ResponseMessage.Message.ERROR_USER_UPDATE_PASSWORD);
      }
    }
    Response resp = null;
    if (((String) response.get(JsonKey.RESPONSE)).equalsIgnoreCase(JsonKey.SUCCESS)) {
      Map<String, Object> userRequest = new HashMap<>();
      userRequest.putAll(userMap);
      userRequest.put(JsonKey.OPERATION_TYPE, JsonKey.CREATE);
      userRequest.put(JsonKey.CALLER_ID, callerId);
      resp = saveUserAttributes(userRequest, request.getRequestContext());
    } else {
      logger.info(
          request.getRequestContext(),
          "UserManagementActor:processUserRequest: User creation failure");
    }
    // Enable this when you want to send full response of user attributes
    Map<String, Object> esResponse = new HashMap<>();
    esResponse.putAll((Map<String, Object>) resp.getResult().get(JsonKey.RESPONSE));
    esResponse.putAll(requestMap);
    response.put(
        JsonKey.ERRORS,
        ((Map<String, Object>) resp.getResult().get(JsonKey.RESPONSE)).get(JsonKey.ERRORS));

    Response syncResponse = new Response();
    syncResponse.putAll(response.getResult());

    if (null != resp && userMap.containsKey("sync") && (boolean) userMap.get("sync")) {
      Map<String, Object> userDetails = Util.getUserDetails(userId, request.getRequestContext());
      Future<Response> future =
          saveUserToES(userDetails, request.getRequestContext())
              .map(
                  new Mapper<String, Response>() {
                    @Override
                    public Response apply(String parameter) {
                      return syncResponse;
                    }
                  },
                  context().dispatcher());
      Patterns.pipe(future, getContext().dispatcher()).to(sender());
    } else {
      if (null != resp) {
        saveUserDetailsToEs(esResponse, request.getRequestContext());
      }
      /*The pattern of this call was incorrect that it tells the ES actor after sending a response. In a high load system,
      this could be fatal, due to this it was  throw an error that the user is not found . so shifted this line after saving to ES */
      sender().tell(response, self());
    }
    requestMap.put(JsonKey.PASSWORD, userMap.get(JsonKey.PASSWORD));
    if (StringUtils.isNotBlank(callerId)) {
      sendEmailAndSms(requestMap, request.getRequestContext());
    }
    generateUserTelemetry(userMap, request, userId);
  }

  private void generateUserTelemetry(Map<String, Object> userMap, Request request, String userId) {
    Request telemetryReq = new Request();
    telemetryReq.getRequest().put("userMap", userMap);
    telemetryReq.getRequest().put("userId", userId);
    telemetryReq.setContext(request.getContext());
    telemetryReq.setOperation("generateUserTelemetry");
    tellToAnother(telemetryReq);
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

  private void setStateValidation(
      Map<String, Object> requestMap, Map<String, Boolean> userBooleanMap, RequestContext context) {
    String rootOrgId = (String) requestMap.get(JsonKey.ROOT_ORG_ID);
    String custodianRootOrgId = DataCacheHandler.getConfigSettings().get(JsonKey.CUSTODIAN_ORG_ID);
    // if the user is creating for non-custodian(i.e state) the value is set as true else false
    userBooleanMap.put(JsonKey.STATE_VALIDATED, !custodianRootOrgId.equals(rootOrgId));
  }

  private Map<String, Boolean> updatedUserFlagsMap(
      Map<String, Object> userMap, Map<String, Object> userDbRecord, RequestContext context) {
    Map<String, Boolean> userBooleanMap = new HashMap<>();
    setUserFlagValue(userDbRecord, JsonKey.EMAIL, JsonKey.EMAIL_VERIFIED);
    setUserFlagValue(userDbRecord, JsonKey.PHONE, JsonKey.PHONE_VERIFIED);
    boolean emailVerified =
        (boolean)
            (userMap.containsKey(JsonKey.EMAIL_VERIFIED)
                ? userMap.get(JsonKey.EMAIL_VERIFIED)
                : userDbRecord.get(JsonKey.EMAIL_VERIFIED));
    boolean phoneVerified =
        (boolean)
            (userMap.containsKey(JsonKey.PHONE_VERIFIED)
                ? userMap.get(JsonKey.PHONE_VERIFIED)
                : userDbRecord.get(JsonKey.PHONE_VERIFIED));
    // for existing users, it won't contain state-validation
    // adding in release-2.4.0
    // userDbRecord- record from es.
    if (!userDbRecord.containsKey(JsonKey.STATE_VALIDATED)) {
      setStateValidation(userDbRecord, userBooleanMap, context);
    } else {
      userBooleanMap.put(
          JsonKey.STATE_VALIDATED, (boolean) userDbRecord.get(JsonKey.STATE_VALIDATED));
    }
    userBooleanMap.put(JsonKey.EMAIL_VERIFIED, emailVerified);
    userBooleanMap.put(JsonKey.PHONE_VERIFIED, phoneVerified);
    return userBooleanMap;
  }

  /**
   * This method set the default value of the user-flag if it is not present in userDbRecord
   *
   * @param userDbRecord
   * @param flagType
   * @param verifiedFlagType
   * @return
   */
  public Map<String, Object> setUserFlagValue(
      Map<String, Object> userDbRecord, String flagType, String verifiedFlagType) {
    if (userDbRecord.get(flagType) != null
        && (userDbRecord.get(verifiedFlagType) == null
            || (boolean) userDbRecord.get(verifiedFlagType))) {
      userDbRecord.put(verifiedFlagType, true);
    } else {
      userDbRecord.put(verifiedFlagType, false);
    }
    return userDbRecord;
  }

  @SuppressWarnings("unchecked")
  private void convertValidatedLocationCodesToIDs(
      Map<String, Object> userMap, RequestContext context) {
    if (userMap.containsKey(JsonKey.LOCATION_IDS)
        && CollectionUtils.isEmpty((List<String>) userMap.get(JsonKey.LOCATION_IDS))) {
      userMap.remove(JsonKey.LOCATION_IDS);
    }
    if (!userMap.containsKey(JsonKey.LOCATION_IDS)
        && userMap.containsKey(JsonKey.LOCATION_CODES)
        && !CollectionUtils.isEmpty((List<String>) userMap.get(JsonKey.LOCATION_CODES))) {
      List<String> locationIdList =
          locationClient.getRelatedLocationIds(
              getActorRef(LocationActorOperation.GET_RELATED_LOCATION_IDS.getValue()),
              (List<String>) userMap.get(JsonKey.LOCATION_CODES),
              context);
      if (locationIdList != null && !locationIdList.isEmpty()) {
        userMap.put(JsonKey.LOCATION_IDS, locationIdList);
        userMap.remove(JsonKey.LOCATION_CODES);
      } else {
        ProjectCommonException.throwClientErrorException(
            ResponseCode.invalidParameterValue,
            MessageFormat.format(
                ResponseCode.invalidParameterValue.getErrorMessage(),
                JsonKey.LOCATION_CODES,
                userMap.get(JsonKey.LOCATION_CODES)));
      }
    }
  }

  private void sendEmailAndSms(Map<String, Object> userMap, RequestContext context) {
    // sendEmailAndSms
    Request EmailAndSmsRequest = new Request();
    EmailAndSmsRequest.getRequest().putAll(userMap);
    EmailAndSmsRequest.setRequestContext(context);
    EmailAndSmsRequest.setOperation(UserActorOperations.PROCESS_ONBOARDING_MAIL_AND_SMS.getValue());
    tellToAnother(EmailAndSmsRequest);
  }

  private void sendResetPasswordLink(Map<String, Object> userMap, RequestContext context) {
    Request EmailAndSmsRequest = new Request();
    EmailAndSmsRequest.getRequest().putAll(userMap);
    EmailAndSmsRequest.setRequestContext(context);
    EmailAndSmsRequest.setOperation(
        UserActorOperations.PROCESS_PASSWORD_RESET_MAIL_AND_SMS.getValue());
    tellToAnother(EmailAndSmsRequest);
  }

  private Future<String> saveUserToES(Map<String, Object> completeUserMap, RequestContext context) {

    return esUtil.save(
        ProjectUtil.EsType.user.getTypeName(),
        (String) completeUserMap.get(JsonKey.USER_ID),
        completeUserMap,
        context);
  }

  private void saveUserToKafka(Map<String, Object> completeUserMap) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      String event = mapper.writeValueAsString(completeUserMap);
      // user_events
      KafkaClient.send(event, ProjectUtil.getConfigValue("sunbird_user_create_sync_topic"));
    } catch (Exception ex) {
      ex.printStackTrace();
    }
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

  private Response saveUserAttributes(Map<String, Object> userMap, RequestContext context) {
    Request request = new Request();
    request.setRequestContext(context);
    request.setOperation(UserActorOperations.SAVE_USER_ATTRIBUTES.getValue());
    request.getRequest().putAll(userMap);
    logger.info(context, "UserManagementActor:saveUserAttributes");
    try {
      Timeout t = new Timeout(Duration.create(10, TimeUnit.SECONDS));
      ActorRef actorRef = getActorRef(UserActorOperations.SAVE_USER_ATTRIBUTES.getValue());
      Future<Object> future = Patterns.ask(actorRef, request, t);
      return (Response) Await.result(future, t.duration());
    } catch (Exception e) {
      logger.error(context, e.getMessage(), e);
    }
    return null;
  }

  private void removeUnwanted(Map<String, Object> reqMap) {
    reqMap.remove(JsonKey.ADDRESS);
    reqMap.remove(JsonKey.EDUCATION);
    reqMap.remove(JsonKey.JOB_PROFILE);
    reqMap.remove(JsonKey.ORGANISATION);
    reqMap.remove(JsonKey.REGISTERED_ORG);
    reqMap.remove(JsonKey.ROOT_ORG);
    reqMap.remove(JsonKey.IDENTIFIER);
    reqMap.remove(JsonKey.ORGANISATIONS);
    reqMap.remove(JsonKey.IS_DELETED);
    reqMap.remove(JsonKey.EXTERNAL_ID);
    reqMap.remove(JsonKey.ID_TYPE);
    reqMap.remove(JsonKey.EXTERNAL_ID_TYPE);
    reqMap.remove(JsonKey.PROVIDER);
    reqMap.remove(JsonKey.EXTERNAL_ID_PROVIDER);
    reqMap.remove(JsonKey.EXTERNAL_IDS);
    reqMap.remove(JsonKey.ORGANISATION_ID);
  }

  public static void verifyFrameworkId(
      String hashtagId, List<String> frameworkIdList, RequestContext context) {
    List<String> frameworks = DataCacheHandler.getHashtagIdFrameworkIdMap().get(hashtagId);
    String frameworkId = frameworkIdList.get(0);
    if (frameworks != null && frameworks.contains(frameworkId)) {
      return;
    } else {
      Map<String, List<Map<String, String>>> frameworkDetails =
          getFrameworkDetails(frameworkId, context);
      if (frameworkDetails == null)
        throw new ProjectCommonException(
            ResponseCode.errorNoFrameworkFound.getErrorCode(),
            ResponseCode.errorNoFrameworkFound.getErrorMessage(),
            ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
    }
  }

  public static Map<String, List<Map<String, String>>> getFrameworkDetails(
      String frameworkId, RequestContext context) {
    if (DataCacheHandler.getFrameworkCategoriesMap().get(frameworkId) == null) {
      handleGetFrameworkDetails(frameworkId, context);
    }
    return DataCacheHandler.getFrameworkCategoriesMap().get(frameworkId);
  }

  @SuppressWarnings("unchecked")
  private static void handleGetFrameworkDetails(String frameworkId, RequestContext context) {
    Map<String, Object> response = ContentStoreUtil.readFramework(frameworkId, context);
    Map<String, List<Map<String, String>>> frameworkCacheMap = new HashMap<>();
    List<String> supportedfFields = DataCacheHandler.getFrameworkFieldsConfig().get(JsonKey.FIELDS);
    Map<String, Object> result = (Map<String, Object>) response.get(JsonKey.RESULT);
    if (MapUtils.isNotEmpty(result)) {
      Map<String, Object> frameworkDetails = (Map<String, Object>) result.get(JsonKey.FRAMEWORK);
      if (MapUtils.isNotEmpty(frameworkDetails)) {
        List<Map<String, Object>> frameworkCategories =
            (List<Map<String, Object>>) frameworkDetails.get(JsonKey.CATEGORIES);
        if (CollectionUtils.isNotEmpty(frameworkCategories)) {
          for (Map<String, Object> frameworkCategoriesValue : frameworkCategories) {
            String frameworkField = (String) frameworkCategoriesValue.get(JsonKey.CODE);
            if (supportedfFields.contains(frameworkField)) {
              List<Map<String, String>> listOfFields = new ArrayList<>();
              List<Map<String, Object>> frameworkTermList =
                  (List<Map<String, Object>>) frameworkCategoriesValue.get(JsonKey.TERMS);
              if (CollectionUtils.isNotEmpty(frameworkTermList)) {
                for (Map<String, Object> frameworkTerm : frameworkTermList) {
                  String id = (String) frameworkTerm.get(JsonKey.IDENTIFIER);
                  String name = (String) frameworkTerm.get(JsonKey.NAME);
                  Map<String, String> writtenValue = new HashMap<>();
                  writtenValue.put(JsonKey.ID, id);
                  writtenValue.put(JsonKey.NAME, name);
                  listOfFields.add(writtenValue);
                }
              }
              if (StringUtils.isNotBlank(frameworkField)
                  && CollectionUtils.isNotEmpty(listOfFields))
                frameworkCacheMap.put(frameworkField, listOfFields);
            }
            if (MapUtils.isNotEmpty(frameworkCacheMap))
              DataCacheHandler.updateFrameworkCategoriesMap(frameworkId, frameworkCacheMap);
          }
        }
      }
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
    validatePrimaryAndRecoveryKeys(userReqMap);
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
            getActorRef(ActorOperations.COMPOSITE_SEARCH.getValue()),
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
      responseMap.put(JsonKey.CONTENT, new ArrayList<Map<String, Object>>());
      responseMap.put(JsonKey.COUNT, 0);
    }
    Response response = new Response();
    response.put(JsonKey.RESPONSE, responseMap);
    sender().tell(response, self());
  }

  private void validateUserTypeAndSubType(
      Map<String, Object> userMap, Map<String, Object> userDbRecord, RequestContext context) {
    if (null != userMap.get(JsonKey.USER_TYPE)) {
      List<String> locationCodes = (List<String>) userMap.get(JsonKey.LOCATION_CODES);
      List<Location> locations = new ArrayList<>();
      if (CollectionUtils.isEmpty(locationCodes)) {
        // Get location code from user records locations Ids
        List<String> locationIds = (List<String>) userDbRecord.get(JsonKey.LOCATION_IDS);
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
        // If location is null or empty .Vlidate with default config
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
  }

  private void validateLocationCodes(Request userRequest) {
    Object locationCodes = userRequest.getRequest().get(JsonKey.LOCATION_CODES);
    if ((locationCodes != null) && !(locationCodes instanceof List)) {
      throw new ProjectCommonException(
          ResponseCode.dataTypeError.getErrorCode(),
          ProjectUtil.formatMessage(
              ResponseCode.dataTypeError.getErrorMessage(), JsonKey.LOCATION_CODES, JsonKey.LIST),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    if (locationCodes != null) {
      // As of now locationCode can take array of only locationcodes and map of locationCodes which
      // include type and code of the location
      String stateCode = null;
      List<String> set = new ArrayList<>();
      List<Location> locationList = new ArrayList<>();
      if (((List) locationCodes).get(0) instanceof String) {
        List<String> locations = (List<String>) locationCodes;
        locationList =
            locationClient.getLocationsByCodes(
                getActorRef(LocationActorOperation.SEARCH_LOCATION.getValue()),
                locations,
                userRequest.getRequestContext());
        for (Location location : locationList) {
          if (JsonKey.STATE.equals(location.getType())) {
            stateCode = location.getCode();
          }
        }
      } else {
        locationList = createLocationLists((List<Map<String, String>>) locationCodes);
        for (Location location : locationList) {
          if (JsonKey.STATE.equals(location.getType())) {
            stateCode = location.getCode();
          }
        }
      }
      // Throw an exception if location codes update is not passed with state code
      if (StringUtils.isBlank(stateCode)) {
        throw new ProjectCommonException(
            ResponseCode.invalidParameterValue.getErrorCode(),
            ProjectUtil.formatMessage(
                ResponseCode.invalidParameterValue.getErrorMessage(), JsonKey.LOCATION_CODES),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
      Map<String, List<String>> locationTypeConfigMap = DataCacheHandler.getLocationTypeConfig();
      if (MapUtils.isEmpty(locationTypeConfigMap)
          || CollectionUtils.isEmpty(locationTypeConfigMap.get(stateCode))) {
        Map<String, Object> userProfileConfigMap =
            FormApiUtil.getProfileConfig(stateCode, userRequest.getRequestContext());
        // If config is not available check the default profile config
        if (MapUtils.isEmpty(userProfileConfigMap) && !JsonKey.DEFAULT_PERSONA.equals(stateCode)) {
          stateCode = JsonKey.DEFAULT_PERSONA;
          if (CollectionUtils.isEmpty(locationTypeConfigMap.get(stateCode))) {
            userProfileConfigMap =
                FormApiUtil.getProfileConfig(stateCode, userRequest.getRequestContext());
            List<String> locationTypeList =
                FormApiUtil.getLocationTypeConfigMap(userProfileConfigMap);
            locationTypeConfigMap.put(stateCode, locationTypeList);
          }
        } else {
          List<String> locationTypeList =
              FormApiUtil.getLocationTypeConfigMap(userProfileConfigMap);
          locationTypeConfigMap.put(stateCode, locationTypeList);
        }
      }
      List<String> typeList = locationTypeConfigMap.get(stateCode);
      for (Location location : locationList) {
        isValidLocationType(location.getType(), typeList);
        if (!location.getType().equals(JsonKey.LOCATION_TYPE_SCHOOL)) {
          set.add(location.getCode());
        } else {
          userRequest.getRequest().put(JsonKey.ORG_EXTERNAL_ID, location.getCode());
          userRequest.getRequest().put(JsonKey.UPDATE_USER_SCHOOL_ORG, true);
        }
      }
      userRequest.getRequest().put(JsonKey.LOCATION_CODES, set);
    }
  }

  /**
   * This method will validate location type
   *
   * @param type
   * @return
   */
  public static boolean isValidLocationType(String type, List<String> typeList) {
    if (null != type && !typeList.contains(type.toLowerCase())) {
      throw new ProjectCommonException(
          ResponseCode.invalidValue.getErrorCode(),
          ProjectUtil.formatMessage(
              ResponseCode.invalidValue.getErrorMessage(),
              GeoLocationJsonKey.LOCATION_TYPE,
              type,
              typeList),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    return true;
  }

  private List<Location> createLocationLists(List<Map<String, String>> locationCodes) {
    List<Location> locations = new ArrayList<>();
    for (Map<String, String> locationMap : locationCodes) {
      Location location = new Location();
      location.setCode(locationMap.get(JsonKey.CODE));
      location.setType(locationMap.get(JsonKey.TYPE));
      locations.add(location);
    }
    return locations;
  }
}
