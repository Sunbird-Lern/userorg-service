package org.sunbird.user.actors;

import static org.sunbird.learner.util.Util.isNotNull;

import akka.actor.ActorRef;
import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import akka.pattern.Patterns;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.actorutil.location.impl.LocationClientImpl;
import org.sunbird.actorutil.systemsettings.SystemSettingClient;
import org.sunbird.actorutil.systemsettings.impl.SystemSettingClientImpl;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.*;
import org.sunbird.common.models.util.ProjectUtil.EsType;
import org.sunbird.common.models.util.datasecurity.EncryptionService;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.dto.SearchDTO;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.UserUtility;
import org.sunbird.learner.util.Util;
import org.sunbird.models.location.Location;
import org.sunbird.user.service.UserExternalIdentityService;
import org.sunbird.user.service.UserService;
import org.sunbird.user.service.impl.UserExternalIdentityServiceImpl;
import org.sunbird.user.service.impl.UserServiceImpl;
import org.sunbird.user.util.UserUtil;
import scala.Tuple2;
import scala.concurrent.Await;
import scala.concurrent.Future;

@ActorConfig(
  tasks = {
    "getUserDetailsByLoginId",
    "getUserProfile",
    "getUserProfileV2",
    "getUserProfileV3",
    "getUserByKey",
    "checkUserExistence",
    "checkUserExistenceV2"
  },
  asyncTasks = {},
  dispatcher = "most-used-one-dispatcher"
)
public class UserProfileReadActor extends BaseActor {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private EncryptionService encryptionService =
      org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getEncryptionServiceInstance(
          null);
  private Util.DbInfo geoLocationDbInfo = Util.dbInfoMap.get(JsonKey.GEO_LOCATION_DB);
  private ActorRef systemSettingActorRef = null;
  private ElasticSearchService esUtil = EsClientFactory.getInstance(JsonKey.REST);
  private UserService userService = UserServiceImpl.getInstance();
  private UserExternalIdentityService userExternalIdentityService =
      new UserExternalIdentityServiceImpl();

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    if (systemSettingActorRef == null) {
      systemSettingActorRef = getActorRef(ActorOperations.GET_SYSTEM_SETTING.getValue());
    }
    String operation = request.getOperation();
    switch (operation) {
      case "getUserProfile":
        getUserProfile(request);
        break;
      case "getUserProfileV2":
        getUserProfileV2(request);
        break;
      case "getUserProfileV3":
        getUserProfileV3(request);
        break;
      case "getUserDetailsByLoginId":
        getUserDetailsByLoginId(request);
        break;
      case "getUserByKey":
        getKey(request);
        break;
      case "checkUserExistence":
        checkUserExistence(request);
        break;
      case "checkUserExistenceV2":
        checkUserExistenceV2(request);
        break;
      default:
        onReceiveUnsupportedOperation("UserProfileReadActor");
    }
  }

  /**
   * Method to get user profile (version 1).
   *
   * @param actorMessage Request containing user ID
   */
  private void getUserProfile(Request actorMessage) {
    Response response = getUserProfileData(actorMessage);
    sender().tell(response, self());
  }

  private void getUserProfileV3(Request actorMessage) {
    Response response = getUserProfileData(actorMessage);
    sender().tell(response, self());
  }

  private Response getUserProfileData(Request actorMessage) {
    Map<String, Object> userMap = actorMessage.getRequest();
    String id = (String) userMap.get(JsonKey.USER_ID);
    String userId;
    String provider = (String) actorMessage.getContext().get(JsonKey.PROVIDER);
    String idType = (String) actorMessage.getContext().get(JsonKey.ID_TYPE);
    boolean withTokens =
        Boolean.valueOf((String) actorMessage.getContext().get(JsonKey.WITH_TOKENS));
    String managedToken = (String) actorMessage.getContext().get(JsonKey.MANAGED_TOKEN);
    boolean showMaskedData = false;
    if (!StringUtils.isEmpty(provider)) {
      if (StringUtils.isEmpty(idType)) {
        userId = null;
        ProjectCommonException.throwClientErrorException(
            ResponseCode.mandatoryParamsMissing,
            MessageFormat.format(
                ResponseCode.mandatoryParamsMissing.getErrorMessage(), JsonKey.ID_TYPE));
      } else {
        userId =
            userExternalIdentityService.getUserV1(
                id, provider, idType, actorMessage.getRequestContext());
        if (userId == null) {
          ProjectCommonException.throwClientErrorException(
              ResponseCode.externalIdNotFound,
              ProjectUtil.formatMessage(
                  ResponseCode.externalIdNotFound.getErrorMessage(), id, idType, provider));
        }
        showMaskedData = true;
      }

    } else {
      userId = id;
      showMaskedData = false;
    }
    Map<String, Object> result = null;

    Future<Map<String, Object>> resultF =
        esUtil.getDataByIdentifier(
            ProjectUtil.EsType.user.getTypeName(), userId, actorMessage.getRequestContext());
    try {
      Object object = Await.result(resultF, ElasticSearchHelper.timeout.duration());
      if (object != null) {
        result = (Map<String, Object>) object;
      }
    } catch (Exception e) {
      logger.error(
          actorMessage.getRequestContext(),
          String.format(
              "%s:%s:User not found with provided id == %s and error %s",
              this.getClass().getSimpleName(), "getUserProfileData", e.getMessage()),
          e);
    }

    // check user found or not
    if (result == null || result.size() == 0) {
      throw new ProjectCommonException(
          ResponseCode.userNotFound.getErrorCode(),
          ResponseCode.userNotFound.getErrorMessage(),
          ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
    }
    // check whether is_deletd true or false
    if (ProjectUtil.isNotNull(result)
        && result.containsKey(JsonKey.IS_DELETED)
        && ProjectUtil.isNotNull(result.get(JsonKey.IS_DELETED))
        && (Boolean) result.get(JsonKey.IS_DELETED)) {
      ProjectCommonException.throwClientErrorException(ResponseCode.userAccountlocked);
    }
    Map<String, Object> rootOrgResult =
        fetchRootOrganisation(result, actorMessage.getRequestContext());
    result.put(JsonKey.ROOT_ORG, rootOrgResult);
    // having check for removing private filed from user , if call user and response
    // user data id is not same.
    String requestedById =
        (String) actorMessage.getContext().getOrDefault(JsonKey.REQUESTED_BY, "");
    String managedForId = (String) actorMessage.getContext().getOrDefault(JsonKey.MANAGED_FOR, "");
    String managedBy = (String) result.get(JsonKey.MANAGED_BY);
    logger.info(
        actorMessage.getRequestContext(),
        "requested By and requested user id == "
            + requestedById
            + "  "
            + (String) userId
            + " managedForId= "
            + managedForId
            + " managedBy "
            + managedBy
            + " showMaskedData= "
            + showMaskedData);
    if (StringUtils.isNotEmpty(managedBy) && !managedBy.equals(requestedById)) {
      ProjectCommonException.throwUnauthorizedErrorException();
    }

    try {
      if (!((userId).equalsIgnoreCase(requestedById) || userId.equalsIgnoreCase(managedForId))
          && !showMaskedData) {

        result = removeUserPrivateField(result);
      } else {
        logger.info(
            actorMessage.getRequestContext(),
            "Response with externalIds and complete profile details");
        // if version is 3 , then read declarations from user_declarations table
        String version = (String) actorMessage.getContext().get(JsonKey.VERSION);
        if (StringUtils.isNotEmpty(version) && version.equals(JsonKey.VERSION_3)) {
          if (null != actorMessage.getContext().get(JsonKey.FIELDS)) {
            String requestFields = (String) actorMessage.getContext().get(JsonKey.FIELDS);
            if (requestFields.contains(JsonKey.DECLARATIONS)) {
              List<Map<String, Object>> declarations =
                  fetchUserDeclarations(userId, actorMessage.getRequestContext());
              result.put(JsonKey.DECLARATIONS, declarations);
            }
            if (requestFields.contains(JsonKey.EXTERNAL_IDS)) {
              logger.info(
                  actorMessage.getRequestContext(),
                  "Get external Ids explicitly from usr_external_identity for v3");
              List<Map<String, String>> resExternalIds =
                  fetchExternalIds(userId, result, actorMessage.getRequestContext());
              result.put(JsonKey.EXTERNAL_IDS, resExternalIds);
            }
          }
        } else {
          // fetch user external identity
          logger.info(
              actorMessage.getRequestContext(),
              "Get external Ids from both declarations and usr_external_identity for merge them");
          List<Map<String, String>> dbResExternalIds =
              fetchUserExternalIdentity(userId, result, actorMessage.getRequestContext());
          result.put(JsonKey.EXTERNAL_IDS, dbResExternalIds);
        }
      }
    } catch (Exception e) {
      logger.error(
          actorMessage.getRequestContext(),
          "Error in UserProfileReadActor: getUserProfileData: error message " + e.getMessage(),
          e);
      ProjectCommonException.throwServerErrorException(ResponseCode.userDataEncryptionError);
    }
    if (null != actorMessage.getContext().get(JsonKey.FIELDS)) {
      String requestFields = (String) actorMessage.getContext().get(JsonKey.FIELDS);
      addExtraFieldsInUserProfileResponse(result, requestFields, actorMessage.getRequestContext());
    } else {
      result.remove(JsonKey.MISSING_FIELDS);
      result.remove(JsonKey.COMPLETENESS);
    }

    Response response = new Response();

    if (null != result) {
      UserUtility.decryptUserDataFrmES(result);
      updateTnc(result);
      // loginId is used internally for checking the duplicate user
      result.remove(JsonKey.LOGIN_ID);
      result.remove(JsonKey.ENC_EMAIL);
      result.remove(JsonKey.ENC_PHONE);

      if (withTokens && StringUtils.isNotEmpty(managedBy) && MapUtils.isNotEmpty(result)) {
        if (StringUtils.isEmpty(managedToken)) {
          logger.info(
              actorMessage.getRequestContext(),
              "UserProfileReadActor: getUserProfileData: calling token generation for: " + userId);
          List<Map<String, Object>> userList = new ArrayList<Map<String, Object>>();
          userList.add(result);
          // Fetch encrypted token from admin utils
          Map<String, Object> encryptedTokenList =
              userService.fetchEncryptedToken(
                  managedBy, userList, actorMessage.getRequestContext());
          // encrypted token for each managedUser in respList
          userService.appendEncryptedToken(
              encryptedTokenList, userList, actorMessage.getRequestContext());
          result = userList.get(0);
        } else {
          result.put(JsonKey.MANAGED_TOKEN, managedToken);
        }
      }

      response.put(JsonKey.RESPONSE, result);
    } else {
      result = new HashMap<>();
      response.put(JsonKey.RESPONSE, result);
    }
    return response;
  }

  private List<Map<String, String>> fetchExternalIds(
      String userId, Map<String, Object> user, RequestContext context) {
    try {
      logger.info(context, "Get external Ids explicitly from usr_external_identity for v3");
      List<Map<String, String>> resExternalIds =
          userExternalIdentityService.getUserExternalIds(userId, context);
      decryptUserExternalIds(resExternalIds, context);
      if (CollectionUtils.isNotEmpty(resExternalIds)) {
        String rootOrgId = (String) user.get(JsonKey.ROOT_ORG_ID);
        if (StringUtils.isNotBlank(rootOrgId)) {
          String orgId = resExternalIds.get(0).get(JsonKey.PROVIDER);
          if (orgId.equalsIgnoreCase(rootOrgId)) {
            String provider = (String) user.get(JsonKey.CHANNEL);
            resExternalIds
                .stream()
                .forEach(
                    s -> {
                      if (s.get(JsonKey.PROVIDER) != null
                          && s.get(JsonKey.PROVIDER).equals(s.get(JsonKey.ID_TYPE))) {
                        s.put(JsonKey.ID_TYPE, provider);
                      }
                      s.put(JsonKey.PROVIDER, provider);
                    });
          } else {
            UserUtil.updateExternalIdsWithProvider(resExternalIds, context);
          }
        } else {
          UserUtil.updateExternalIdsWithProvider(resExternalIds, context);
        }
      }
      return resExternalIds;
    } catch (Exception ex) {
      logger.error(context, ex.getMessage(), ex);
    }
    return new ArrayList<>();
  }

  /**
   * fetch declared info from user_declaration table
   *
   * @param userId
   * @return
   */
  private List<Map<String, Object>> fetchUserDeclarations(String userId, RequestContext context) {
    Map<String, Object> req = new HashMap<>();
    req.put(JsonKey.USER_ID, userId);
    Response response =
        cassandraOperation.getRecordById(
            JsonKey.SUNBIRD, JsonKey.USR_DECLARATION_TABLE, req, context);
    List<Map<String, Object>> resExternalIds;
    List<Map<String, Object>> finalRes = new ArrayList<>();
    if (null != response && null != response.getResult()) {
      resExternalIds = (List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE);
      if (CollectionUtils.isNotEmpty(resExternalIds)) {
        resExternalIds.forEach(
            item -> {
              Map<String, Object> declaration = new HashMap<>();
              Map<String, String> declaredFields =
                  (Map<String, String>) item.get(JsonKey.USER_INFO);
              if (MapUtils.isNotEmpty(declaredFields)) {
                decryptDeclarationFields(declaredFields, context);
              }
              declaration.put(JsonKey.STATUS, (String) item.get(JsonKey.STATUS));
              declaration.put(JsonKey.ERROR_TYPE, (String) item.get(JsonKey.ERROR_TYPE));
              declaration.put(JsonKey.ORG_ID, (String) item.get(JsonKey.ORG_ID));
              declaration.put(JsonKey.PERSONA, (String) item.get(JsonKey.PERSONA));
              declaration.put(JsonKey.INFO, declaredFields);
              finalRes.add(declaration);
            });
      }
    }
    return finalRes;
  }

  private Map<String, String> decryptDeclarationFields(
      Map<String, String> declaredFields, RequestContext context) {
    if (declaredFields.containsKey(JsonKey.DECLARED_EMAIL)) {
      declaredFields.put(
          JsonKey.DECLARED_EMAIL,
          UserUtil.getDecryptedData(declaredFields.get(JsonKey.DECLARED_EMAIL), context));
    }
    if (declaredFields.containsKey(JsonKey.DECLARED_PHONE)) {
      declaredFields.put(
          JsonKey.DECLARED_PHONE,
          UserUtil.getDecryptedData(declaredFields.get(JsonKey.DECLARED_PHONE), context));
    }
    return declaredFields;
  }

  private List<Map<String, String>> fetchUserExternalIdentity(
      String userId, Map<String, Object> user, RequestContext context) {
    try {
      List<Map<String, String>> dbResExternalIds = UserUtil.getExternalIds(userId, context);

      decryptUserExternalIds(dbResExternalIds, context);
      // update orgId to provider in externalIds
      String rootOrgId = (String) user.get(JsonKey.ROOT_ORG_ID);
      if (CollectionUtils.isNotEmpty(dbResExternalIds) && StringUtils.isNotBlank(rootOrgId)) {
        String orgId = dbResExternalIds.get(0).get(JsonKey.PROVIDER);
        if (orgId.equalsIgnoreCase(rootOrgId)) {
          String provider = (String) user.get(JsonKey.CHANNEL);
          dbResExternalIds
              .stream()
              .forEach(
                  s -> {
                    if (s.get(JsonKey.PROVIDER) != null
                        && s.get(JsonKey.PROVIDER).equals(s.get(JsonKey.ID_TYPE))) {
                      s.put(JsonKey.ID_TYPE, provider);
                    }
                    s.put(JsonKey.PROVIDER, provider);
                  });
        } else {
          UserUtil.updateExternalIdsWithProvider(dbResExternalIds, context);
        }
      } else {
        UserUtil.updateExternalIdsWithProvider(dbResExternalIds, context);
      }
      return dbResExternalIds;
    } catch (Exception ex) {
      logger.error(context, ex.getMessage(), ex);
    }
    return new ArrayList<>();
  }

  private void decryptUserExternalIds(
      List<Map<String, String>> dbResExternalIds, RequestContext context) {
    if (CollectionUtils.isNotEmpty(dbResExternalIds)) {
      dbResExternalIds
          .stream()
          .forEach(
              s -> {
                if (StringUtils.isNotBlank(s.get(JsonKey.ORIGINAL_EXTERNAL_ID))
                    && StringUtils.isNotBlank(s.get(JsonKey.ORIGINAL_ID_TYPE))
                    && StringUtils.isNotBlank(s.get(JsonKey.ORIGINAL_PROVIDER))) {
                  if (JsonKey.DECLARED_EMAIL.equals(s.get(JsonKey.ORIGINAL_ID_TYPE))
                      || JsonKey.DECLARED_PHONE.equals(s.get(JsonKey.ORIGINAL_ID_TYPE))) {

                    String decrytpedOriginalExternalId =
                        UserUtil.getDecryptedData(s.get(JsonKey.ORIGINAL_EXTERNAL_ID), context);
                    s.put(JsonKey.ID, decrytpedOriginalExternalId);

                  } else if (JsonKey.DECLARED_DISTRICT.equals(s.get(JsonKey.ORIGINAL_ID_TYPE))
                      || JsonKey.DECLARED_STATE.equals(s.get(JsonKey.ORIGINAL_ID_TYPE))) {
                    LocationClientImpl locationClient = new LocationClientImpl();
                    Location location =
                        locationClient.getLocationById(
                            getActorRef(LocationActorOperation.SEARCH_LOCATION.getValue()),
                            s.get(JsonKey.ORIGINAL_EXTERNAL_ID),
                            context);
                    s.put(
                        JsonKey.ID,
                        (location == null
                            ? s.get(JsonKey.ORIGINAL_EXTERNAL_ID)
                            : location.getCode()));
                  } else {
                    s.put(JsonKey.ID, s.get(JsonKey.ORIGINAL_EXTERNAL_ID));
                  }
                  s.put(JsonKey.ID_TYPE, s.get(JsonKey.ORIGINAL_ID_TYPE));
                  s.put(JsonKey.PROVIDER, s.get(JsonKey.ORIGINAL_PROVIDER));
                } else {
                  s.put(JsonKey.ID, s.get(JsonKey.EXTERNAL_ID));
                }

                s.remove(JsonKey.EXTERNAL_ID);
                s.remove(JsonKey.ORIGINAL_EXTERNAL_ID);
                s.remove(JsonKey.ORIGINAL_ID_TYPE);
                s.remove(JsonKey.ORIGINAL_PROVIDER);
                s.remove(JsonKey.CREATED_BY);
                s.remove(JsonKey.LAST_UPDATED_BY);
                s.remove(JsonKey.LAST_UPDATED_ON);
                s.remove(JsonKey.CREATED_ON);
                s.remove(JsonKey.USER_ID);
                s.remove(JsonKey.SLUG);
              });
    }
  }

  /**
   * This method will remove user private field from response map
   *
   * @param responseMap Map<String,Object>
   */
  private Map<String, Object> removeUserPrivateField(Map<String, Object> responseMap) {
    logger.info("Start removing User private field==");
    for (int i = 0; i < ProjectUtil.excludes.length; i++) {
      responseMap.remove(ProjectUtil.excludes[i]);
    }
    logger.info("All private filed removed=");
    return responseMap;
  }

  private Map<String, Object> fetchRootOrganisation(
      Map<String, Object> result, RequestContext context) {
    try {
      if (StringUtils.isNotBlank((String) result.get(JsonKey.ROOT_ORG_ID))) {
        String rootOrgId = (String) result.get(JsonKey.ROOT_ORG_ID);
        Util.DbInfo OrgDb = Util.dbInfoMap.get(JsonKey.ORG_DB);
        Response response =
            cassandraOperation.getRecordById(
                OrgDb.getKeySpace(), OrgDb.getTableName(), rootOrgId, context);
        List<Map<String, Object>> responseList =
            (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
        if (CollectionUtils.isNotEmpty(responseList)) {
          return responseList.get(0);
        }
      }
    } catch (Exception ex) {
      logger.error(context, ex.getMessage(), ex);
    }
    return null;
  }

  private Future<Map<String, Object>> fetchRootAndRegisterOrganisation(
      Map<String, Object> result, RequestContext context) {
    try {
      if (isNotNull(result.get(JsonKey.ROOT_ORG_ID))) {
        String rootOrgId = (String) result.get(JsonKey.ROOT_ORG_ID);
        return esUtil.getDataByIdentifier(
            ProjectUtil.EsType.organisation.getTypeName(), rootOrgId, context);
      }
    } catch (Exception ex) {
      logger.error(context, ex.getMessage(), ex);
    }
    return null;
  }

  private void addExtraFieldsInUserProfileResponse(
      Map<String, Object> result, String fields, RequestContext context) {
    if (!StringUtils.isBlank(fields)) {
      result.put(JsonKey.LAST_LOGIN_TIME, Long.parseLong("0"));
      if (!fields.contains(JsonKey.COMPLETENESS)) {
        result.remove(JsonKey.COMPLETENESS);
      }
      if (!fields.contains(JsonKey.MISSING_FIELDS)) {
        result.remove(JsonKey.MISSING_FIELDS);
      }
      if (fields.contains(JsonKey.TOPIC)) {
        // fetch the topic details of all user associated orgs and append in the result
        fetchTopicOfAssociatedOrgs(result, context);
      }
      if (fields.contains(JsonKey.ORGANISATIONS)) {
        updateUserOrgInfo((List) result.get(JsonKey.ORGANISATIONS), context);
      }
      if (fields.contains(JsonKey.ROLES)) {
        result.put(JsonKey.ROLE_LIST, DataCacheHandler.getUserReadRoleList());
      }
      if (fields.contains(JsonKey.LOCATIONS)) {
        result.put(
            JsonKey.USER_LOCATIONS,
            getUserLocations((List<String>) result.get(JsonKey.LOCATION_IDS), context));
        result.remove(JsonKey.LOCATION_IDS);
      }
    }
  }

  private void fetchTopicOfAssociatedOrgs(Map<String, Object> result, RequestContext context) {
    Set<String> topicSet = new HashSet<>();
    List<Map<String, Object>> userOrgList =
        (List<Map<String, Object>>) result.get(JsonKey.ORGANISATIONS);
    if (CollectionUtils.isNotEmpty(userOrgList)) {
      List<String> orgIdList =
          userOrgList
              .stream()
              .map(m -> (String) m.get(JsonKey.ORGANISATION_ID))
              .distinct()
              .collect(Collectors.toList());

      // fetch all org details from cassandra ...
      if (CollectionUtils.isNotEmpty(orgIdList)) {
        Util.DbInfo OrgDb = Util.dbInfoMap.get(JsonKey.ORG_DB);
        List<String> orgfields = new ArrayList<>();
        orgfields.add(JsonKey.ID);
        orgfields.add(JsonKey.LOCATION_ID);
        Response userOrgResponse =
            cassandraOperation.getPropertiesValueById(
                OrgDb.getKeySpace(), OrgDb.getTableName(), orgIdList, orgfields, context);
        List<Map<String, Object>> userOrgResponseList =
            (List<Map<String, Object>>) userOrgResponse.get(JsonKey.RESPONSE);

        if (CollectionUtils.isNotEmpty(userOrgResponseList)) {
          List<String> locationIdList = new ArrayList<>();
          for (Map<String, Object> org : userOrgResponseList) {
            String locId = (String) org.get(JsonKey.LOCATION_ID);
            if (StringUtils.isNotBlank(locId)) {
              locationIdList.add(locId);
            }
          }
          if (CollectionUtils.isNotEmpty(locationIdList)) {
            List<String> geoLocationFields = new ArrayList<>();
            geoLocationFields.add(JsonKey.TOPIC);
            Response geoLocationResponse =
                cassandraOperation.getPropertiesValueById(
                    geoLocationDbInfo.getKeySpace(),
                    geoLocationDbInfo.getTableName(),
                    locationIdList,
                    geoLocationFields,
                    context);
            List<Map<String, Object>> geoLocationResponseList =
                (List<Map<String, Object>>) geoLocationResponse.get(JsonKey.RESPONSE);
            List<String> topicList =
                geoLocationResponseList
                    .stream()
                    .map(m -> (String) m.get(JsonKey.TOPIC))
                    .distinct()
                    .collect(Collectors.toList());
            topicSet.addAll(topicList);
          }
        }
      }
    }
    result.put(JsonKey.TOPICS, topicSet);
  }

  private void updateUserOrgInfo(List<Map<String, Object>> userOrgs, RequestContext context) {
    Map<String, Map<String, Object>> orgInfoMap = fetchAllOrgById(userOrgs, context);
    Map<String, Map<String, Object>> locationInfoMap = fetchAllLocationsById(orgInfoMap, context);
    prepUserOrgInfoWithAdditionalData(userOrgs, orgInfoMap, locationInfoMap);
  }

  private Map<String, Map<String, Object>> fetchAllOrgById(
      List<Map<String, Object>> userOrgs, RequestContext context) {
    List<String> orgIds =
        userOrgs
            .stream()
            .map(m -> (String) m.get(JsonKey.ORGANISATION_ID))
            .distinct()
            .collect(Collectors.toList());
    if (CollectionUtils.isNotEmpty(orgIds)) {
      List<String> fields =
          Arrays.asList(
              JsonKey.ORG_NAME,
              JsonKey.CHANNEL,
              JsonKey.HASHTAGID,
              JsonKey.LOCATION_IDS,
              JsonKey.ID);
      Util.DbInfo OrgDb = Util.dbInfoMap.get(JsonKey.ORG_DB);
      Response userOrgResponse =
          cassandraOperation.getPropertiesValueById(
              OrgDb.getKeySpace(), OrgDb.getTableName(), orgIds, fields, context);
      List<Map<String, Object>> userOrgResponseList =
          (List<Map<String, Object>>) userOrgResponse.get(JsonKey.RESPONSE);
      return userOrgResponseList
          .stream()
          .collect(
              Collectors.toMap(
                  obj -> {
                    return (String) obj.get("id");
                  },
                  val -> val));
    } else {
      return new HashMap<>();
    }
  }

  private Map<String, Map<String, Object>> fetchAllLocationsById(
      Map<String, Map<String, Object>> orgInfoMap, RequestContext context) {

    Set<String> locationSet = new HashSet<>();
    for (Map<String, Object> org : orgInfoMap.values()) {
      List<String> locationIds = (List<String>) org.get(JsonKey.LOCATION_IDS);
      if (CollectionUtils.isNotEmpty(locationIds)) {
        locationIds.forEach(
            locId -> {
              if (StringUtils.isNotBlank(locId)) {
                locationSet.add(locId);
              }
            });
      }
    }
    if (CollectionUtils.isNotEmpty(locationSet)) {
      List<String> locList = new ArrayList<>(locationSet);
      List<String> locationFields =
          Arrays.asList(JsonKey.CODE, JsonKey.NAME, JsonKey.TYPE, JsonKey.PARENT_ID, JsonKey.ID);

      Response locationResponse =
          cassandraOperation.getPropertiesValueById(
              "sunbird", "location", locList, locationFields, context);
      List<Map<String, Object>> locationResponseList =
          (List<Map<String, Object>>) locationResponse.get(JsonKey.RESPONSE);

      Map<String, Map<String, Object>> locationInfoMap =
          locationResponseList
              .stream()
              .collect(
                  Collectors.toMap(
                      obj -> {
                        return (String) obj.get("id");
                      },
                      val -> val));
      return locationInfoMap;
    } else {
      return new HashMap<>();
    }
  }

  private void prepUserOrgInfoWithAdditionalData(
      List<Map<String, Object>> userOrgs,
      Map<String, Map<String, Object>> orgInfoMap,
      Map<String, Map<String, Object>> locationInfoMap) {
    for (Map<String, Object> usrOrg : userOrgs) {
      Map<String, Object> orgInfo = orgInfoMap.get(usrOrg.get(JsonKey.ORGANISATION_ID));
      usrOrg.put(JsonKey.ORG_NAME, orgInfo.get(JsonKey.ORG_NAME));
      usrOrg.put(JsonKey.CHANNEL, orgInfo.get(JsonKey.CHANNEL));
      usrOrg.put(JsonKey.HASHTAGID, orgInfo.get(JsonKey.HASHTAGID));
      usrOrg.put(JsonKey.LOCATION_IDS, orgInfo.get(JsonKey.LOCATION_IDS));
      if (MapUtils.isNotEmpty(locationInfoMap)) {
        usrOrg.put(
            JsonKey.LOCATIONS,
            prepLocationFields((List<String>) orgInfo.get(JsonKey.LOCATION_IDS), locationInfoMap));
      }
    }
  }

  private List<Map<String, Object>> prepLocationFields(
      List<String> locationIds, Map<String, Map<String, Object>> locationInfoMap) {
    List<Map<String, Object>> retList = new ArrayList<>();
    if (locationIds != null) {
      for (String locationId : locationIds) {
        retList.add(locationInfoMap.get(locationId));
      }
    }
    return retList;
  }

  /**
   * Method to get user profile (version 2).
   *
   * @param actorMessage Request containing user ID
   */
  private void getUserProfileV2(Request actorMessage) {
    Response response = getUserProfileData(actorMessage);
    SystemSettingClient systemSetting = new SystemSettingClientImpl();
    Object excludedFieldList =
        systemSetting.getSystemSettingByFieldAndKey(
            systemSettingActorRef,
            JsonKey.USER_PROFILE_CONFIG,
            JsonKey.SUNBIRD_USER_PROFILE_READ_EXCLUDED_FIELDS,
            new TypeReference<List<String>>() {},
            actorMessage.getRequestContext());
    if (excludedFieldList != null) {
      removeExcludedFieldsFromUserProfileResponse(
          (Map<String, Object>) response.get(JsonKey.RESPONSE), (List<String>) excludedFieldList);
    } else {
      logger.info(
          actorMessage.getRequestContext(),
          "UserProfileReadActor:getUserProfileV2: System setting userProfileConfig.read.excludedFields not configured.");
    }
    sender().tell(response, self());
  }

  private void removeExcludedFieldsFromUserProfileResponse(
      Map<String, Object> response, List<String> excludeFields) {
    if (CollectionUtils.isNotEmpty(excludeFields)) {
      for (String key : excludeFields) {
        response.remove(key);
      }
    }
  }

  private void getUserDetailsByLoginId(Request actorMessage) {
    actorMessage.toLower();
    Map<String, Object> userMap = actorMessage.getRequest();
    if (null != userMap.get(JsonKey.LOGIN_ID)) {
      String loginId = (String) userMap.get(JsonKey.LOGIN_ID);
      try {
        loginId =
            encryptionService.encryptData(
                (String) userMap.get(JsonKey.LOGIN_ID), actorMessage.getRequestContext());
      } catch (Exception e) {
        logger.error(actorMessage.getRequestContext(), e.getMessage(), e);
        ProjectCommonException exception =
            new ProjectCommonException(
                ResponseCode.userDataEncryptionError.getErrorCode(),
                ResponseCode.userDataEncryptionError.getErrorMessage(),
                ResponseCode.SERVER_ERROR.getResponseCode());
        sender().tell(exception, self());
        return;
      }
      SearchDTO searchDto = new SearchDTO();
      Map<String, Object> filter = new HashMap<>();
      filter.put(JsonKey.LOGIN_ID, loginId);
      searchDto.getAdditionalProperties().put(JsonKey.FILTERS, filter);
      Future<Map<String, Object>> esResponseF =
          esUtil.search(
              searchDto, ProjectUtil.EsType.user.getTypeName(), actorMessage.getRequestContext());
      Map<String, Object> esResponse =
          (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(esResponseF);
      List<Map<String, Object>> userList =
          (List<Map<String, Object>>) esResponse.get(JsonKey.CONTENT);
      Map<String, Object> result = null;
      if (null != userList && !userList.isEmpty()) {
        result = userList.get(0);
      } else {
        throw new ProjectCommonException(
            ResponseCode.userNotFound.getErrorCode(),
            ResponseCode.userNotFound.getErrorMessage(),
            ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
      }
      // String username = ssoManager.getUsernameById((String) result.get(JsonKey.USER_ID));
      // result.put(JsonKey.USERNAME, username);
      sendResponse(actorMessage, result);

    } else {
      ProjectCommonException exception =
          new ProjectCommonException(
              ResponseCode.userNotFound.getErrorCode(),
              ResponseCode.userNotFound.getErrorMessage(),
              ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
      sender().tell(exception, self());
      return;
    }
  }

  private void sendResponse(Request actorMessage, Map<String, Object> result) {
    if (result == null || result.size() == 0) {
      throw new ProjectCommonException(
          ResponseCode.userNotFound.getErrorCode(),
          ResponseCode.userNotFound.getErrorMessage(),
          ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
    }

    // check whether is_deletd true or false
    if (ProjectUtil.isNotNull(result)
        && result.containsKey(JsonKey.IS_DELETED)
        && ProjectUtil.isNotNull(result.get(JsonKey.IS_DELETED))
        && (Boolean) result.get(JsonKey.IS_DELETED)) {
      throw new ProjectCommonException(
          ResponseCode.userAccountlocked.getErrorCode(),
          ResponseCode.userAccountlocked.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    Future<Map<String, Object>> future =
        fetchRootAndRegisterOrganisation(result, actorMessage.getRequestContext());
    Future<Response> response =
        future.map(
            new Mapper<Map<String, Object>, Response>() {
              @Override
              public Response apply(Map<String, Object> responseMap) {
                logger.info(
                    actorMessage.getRequestContext(),
                    "UserProfileReadActor:handle user profile read async call ");
                result.put(JsonKey.ROOT_ORG, responseMap);
                Response response = new Response();
                handleUserCallAsync(result, response, actorMessage);
                return response;
              }
            },
            getContext().dispatcher());
    Patterns.pipe(response, getContext().dispatcher()).to(sender());
  }

  private void handleUserCallAsync(
      Map<String, Object> result, Response response, Request actorMessage) {
    // having check for removing private filed from user , if call user and response
    // user data id is not same.
    String requestedById =
        (String) actorMessage.getContext().getOrDefault(JsonKey.REQUESTED_BY, "");
    logger.info(
        actorMessage.getRequestContext(),
        "requested By and requested user id == "
            + requestedById
            + "  "
            + (String) result.get(JsonKey.USER_ID));

    try {
      if (!(((String) result.get(JsonKey.USER_ID)).equalsIgnoreCase(requestedById))) {
        result = removeUserPrivateField(result);
      } else {
        // fetch user external identity
        List<Map<String, String>> dbResExternalIds =
            fetchUserExternalIdentity(requestedById, result, actorMessage.getRequestContext());
        result.put(JsonKey.EXTERNAL_IDS, dbResExternalIds);
      }
    } catch (Exception e) {
      logger.error(actorMessage.getRequestContext(), e.getMessage(), e);
      ProjectCommonException exception =
          new ProjectCommonException(
              ResponseCode.userDataEncryptionError.getErrorCode(),
              ResponseCode.userDataEncryptionError.getErrorMessage(),
              ResponseCode.SERVER_ERROR.getResponseCode());
      sender().tell(exception, self());
      return;
    }

    if (null != result) {
      // remove email and phone no from response
      result.remove(JsonKey.ENC_EMAIL);
      result.remove(JsonKey.ENC_PHONE);
      updateTnc(result);
      if (null != actorMessage.getRequest().get(JsonKey.FIELDS)) {
        List<String> requestFields = (List<String>) actorMessage.getRequest().get(JsonKey.FIELDS);
        if (requestFields != null) {
          addExtraFieldsInUserProfileResponse(
              result, String.join(",", requestFields), actorMessage.getRequestContext());
        } else {
          result.remove(JsonKey.MISSING_FIELDS);
          result.remove(JsonKey.COMPLETENESS);
        }
      } else {
        result.remove(JsonKey.MISSING_FIELDS);
        result.remove(JsonKey.COMPLETENESS);
      }
      response.put(JsonKey.RESPONSE, result);
      UserUtility.decryptUserDataFrmES(result);
    } else {
      result = new HashMap<>();
      response.put(JsonKey.RESPONSE, result);
    }
  }

  private void updateTnc(Map<String, Object> userSearchMap) {
    Map<String, Object> tncConfigMap = null;
    try {
      String tncValue = DataCacheHandler.getConfigSettings().get(JsonKey.TNC_CONFIG);
      ObjectMapper mapper = new ObjectMapper();
      tncConfigMap = mapper.readValue(tncValue, Map.class);

    } catch (Exception e) {
      logger.error(
          "UserProfileReadActor:updateTncInfo: Exception occurred while getting system setting for"
              + JsonKey.TNC_CONFIG
              + e.getMessage(),
          e);
    }

    if (MapUtils.isNotEmpty(tncConfigMap)) {
      try {
        String tncLatestVersion = (String) tncConfigMap.get(JsonKey.LATEST_VERSION);
        userSearchMap.put(JsonKey.TNC_LATEST_VERSION, tncLatestVersion);
        String tncUserAcceptedVersion = (String) userSearchMap.get(JsonKey.TNC_ACCEPTED_VERSION);
        String tncUserAcceptedOn = (String) userSearchMap.get(JsonKey.TNC_ACCEPTED_ON);
        if (StringUtils.isEmpty(tncUserAcceptedVersion)
            || !tncUserAcceptedVersion.equalsIgnoreCase(tncLatestVersion)
            || StringUtils.isEmpty(tncUserAcceptedOn)) {
          userSearchMap.put(JsonKey.PROMPT_TNC, true);
        } else {
          userSearchMap.put(JsonKey.PROMPT_TNC, false);
        }

        if (tncConfigMap.containsKey(tncLatestVersion)) {
          String url = (String) ((Map) tncConfigMap.get(tncLatestVersion)).get(JsonKey.URL);
          logger.info("UserProfileReadActor:updateTncInfo: url = " + url);
          userSearchMap.put(JsonKey.TNC_LATEST_VERSION_URL, url);
        } else {
          userSearchMap.put(JsonKey.PROMPT_TNC, false);
          logger.info(
              "UserProfileReadActor:updateTncInfo: TnC version URL is missing from configuration");
        }
      } catch (Exception e) {
        logger.error(
            "UserProfileReadActor:updateTncInfo: Exception occurred with error message = "
                + e.getMessage(),
            e);
        ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
      }
    }
  }

  private List<Map<String, Object>> getUserLocations(
      List<String> locationIds, RequestContext context) {
    if (CollectionUtils.isNotEmpty(locationIds)) {
      List<String> locationFields =
          Arrays.asList(JsonKey.CODE, JsonKey.NAME, JsonKey.TYPE, JsonKey.PARENT_ID, JsonKey.ID);
      List<String> orgfields = new ArrayList<>();
      orgfields.add(JsonKey.ID);
      orgfields.add(JsonKey.LOCATION_ID);
      Response locationResponse =
          cassandraOperation.getPropertiesValueById(
              "sunbird", "location", locationIds, locationFields, context);
      List<Map<String, Object>> locationResponseList =
          (List<Map<String, Object>>) locationResponse.get(JsonKey.RESPONSE);
      return locationResponseList;
    }
    return new ArrayList<>();
  }

  private Future<Response> checkUserExists(Request request, boolean isV1) {
    Future<Map<String, Object>> userFuture;
    String key = (String) request.get(JsonKey.KEY);
    if (JsonKey.PHONE.equalsIgnoreCase(key)
        || JsonKey.EMAIL.equalsIgnoreCase(key)
        || JsonKey.USERNAME.equalsIgnoreCase(key)) {
      String value = (String) request.get(JsonKey.VALUE);
      String userId =
          getUserIdByUserLookUp(
              key.toLowerCase(), StringUtils.lowerCase(value), request.getRequestContext());
      if (StringUtils.isBlank(userId)) {
        return Futures.future(
            () -> {
              Response resp = new Response();
              resp.put(JsonKey.EXISTS, false);
              return resp;
            },
            getContext().dispatcher());
      }
      userFuture =
          esUtil.getDataByIdentifier(
              EsType.user.getTypeName(), userId, request.getRequestContext());
      return userFuture.map(
          new Mapper<Map<String, Object>, Response>() {
            @Override
            public Response apply(Map<String, Object> response) {
              Response resp = new Response();
              resp.put(JsonKey.EXISTS, true);
              if (!isV1) {
                resp.put(JsonKey.ID, response.get(JsonKey.USER_ID));
                String name = (String) response.get(JsonKey.FIRST_NAME);
                if (StringUtils.isNotEmpty((String) response.get(JsonKey.LAST_NAME))) {
                  name += " " + response.get(JsonKey.LAST_NAME);
                }
                resp.put(JsonKey.NAME, name);
              }
              String logMsg = String.format("userExists %s ", request.get(JsonKey.VALUE));
              logger.info(request.getRequestContext(), logMsg);
              return resp;
            }
          },
          getContext().dispatcher());

    } else {
      userFuture = userSearchDetails(request);
      return userFuture.map(
          new Mapper<Map<String, Object>, Response>() {
            @Override
            public Response apply(Map<String, Object> responseMap) {
              List<Map<String, Object>> respList = (List) responseMap.get(JsonKey.CONTENT);
              long size = respList.size();
              boolean isExists = (size > 0);

              Response resp = new Response();
              resp.put(JsonKey.EXISTS, isExists);

              if (isExists && !isV1) {
                Map<String, Object> response = respList.get(0);
                resp.put(JsonKey.EXISTS, true);
                resp.put(JsonKey.ID, response.get(JsonKey.USER_ID));
                String name = (String) response.get(JsonKey.FIRST_NAME);
                if (StringUtils.isNotEmpty((String) response.get(JsonKey.LAST_NAME))) {
                  name += " " + response.get(JsonKey.LAST_NAME);
                }
                resp.put(JsonKey.NAME, name);
              }

              String logMsg =
                  String.format(
                      "userExists %s results size = %d", request.get(JsonKey.VALUE), size);
              logger.info(request.getRequestContext(), logMsg);
              return resp;
            }
          },
          getContext().dispatcher());
    }
  }

  private void checkUserExistence(Request request) {
    Future<Response> userResponse = checkUserExists(request, true);
    Patterns.pipe(userResponse, getContext().dispatcher()).to(sender());
  }

  private void checkUserExistenceV2(Request request) {
    Future<Response> userResponse = checkUserExists(request, false);
    Patterns.pipe(userResponse, getContext().dispatcher()).to(sender());
  }

  private Future<Map<String, Object>> userSearchDetails(Request request) {
    Map<String, Object> searchMap = new WeakHashMap<>();
    String value = (String) request.get(JsonKey.VALUE);
    String encryptedValue = null;
    try {
      encryptedValue =
          encryptionService.encryptData(StringUtils.lowerCase(value), request.getRequestContext());
    } catch (Exception var11) {
      throw new ProjectCommonException(
          ResponseCode.userDataEncryptionError.getErrorCode(),
          ResponseCode.userDataEncryptionError.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    searchMap.put((String) request.get(JsonKey.KEY), encryptedValue);
    logger.info(
        request.getRequestContext(),
        "UserProfileReadActor:checkUserExistence: search map prepared " + searchMap);
    SearchDTO searchDTO = new SearchDTO();
    searchDTO.getAdditionalProperties().put(JsonKey.FILTERS, searchMap);
    Future<Map<String, Object>> esFuture =
        esUtil.search(searchDTO, EsType.user.getTypeName(), request.getRequestContext());
    return esFuture;
  }

  private String getUserIdByUserLookUp(String type, String value, RequestContext context) {
    try {
      value = encryptionService.encryptData(value, context);
    } catch (Exception e) {
      logger.info(context, "Exception occurred while encrypting email/phone " + e);
    }
    Util.DbInfo userLookUp = Util.dbInfoMap.get(JsonKey.USER_LOOKUP);
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.TYPE, type);
    reqMap.put(JsonKey.VALUE, value);
    Response response =
        cassandraOperation.getRecordsByCompositeKey(
            userLookUp.getKeySpace(), userLookUp.getTableName(), reqMap, context);
    List<Map<String, Object>> userMapList =
        (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    if (CollectionUtils.isNotEmpty(userMapList)) {
      Map<String, Object> userMap = userMapList.get(0);
      return (String) userMap.get(JsonKey.USER_ID);
    }
    return "";
  }

  private void getKey(Request actorMessage) {
    String key = (String) actorMessage.getRequest().get(JsonKey.KEY);
    String value = (String) actorMessage.getRequest().get(JsonKey.VALUE);
    if (JsonKey.LOGIN_ID.equalsIgnoreCase(key)
        || JsonKey.EMAIL.equalsIgnoreCase(key)
        || JsonKey.USERNAME.equalsIgnoreCase(key)) {
      value = value.toLowerCase();
    }
    Future<Map<String, Object>> userResponse;
    Future<Map<String, Object>> futureResponse;
    if (JsonKey.PHONE.equalsIgnoreCase(key)
        || JsonKey.EMAIL.equalsIgnoreCase(key)
        || JsonKey.USERNAME.equalsIgnoreCase(key)) {
      String userId =
          getUserIdByUserLookUp(key.toLowerCase(), value, actorMessage.getRequestContext());
      if (StringUtils.isBlank(userId)) {
        isUserExists(new HashMap<>());
      }
      futureResponse =
          esUtil.getDataByIdentifier(
              EsType.user.getTypeName(), userId, actorMessage.getRequestContext());
      userResponse =
          futureResponse.map(
              new Mapper<Map<String, Object>, Map<String, Object>>() {
                @Override
                public Map<String, Object> apply(Map<String, Object> userMap) {
                  isUserExists(userMap);
                  userMap.put(JsonKey.EMAIL, userMap.get(JsonKey.MASKED_EMAIL));
                  userMap.put(JsonKey.PHONE, userMap.get(JsonKey.MASKED_PHONE));
                  isUserAccountDeleted(userMap);
                  return userMap;
                }
              },
              getContext().dispatcher());

    } else {
      String encryptedValue = null;
      try {
        encryptedValue = encryptionService.encryptData(value, actorMessage.getRequestContext());
      } catch (Exception e) {
        ProjectCommonException exception =
            new ProjectCommonException(
                ResponseCode.userDataEncryptionError.getErrorCode(),
                ResponseCode.userDataEncryptionError.getErrorMessage(),
                ResponseCode.SERVER_ERROR.getResponseCode());
        sender().tell(exception, self());
        return;
      }
      Map<String, Object> searchMap = new WeakHashMap<>();
      searchMap.put(key, encryptedValue);
      SearchDTO searchDTO = new SearchDTO();
      searchDTO.getAdditionalProperties().put(JsonKey.FILTERS, searchMap);

      futureResponse =
          esUtil.search(searchDTO, EsType.user.getTypeName(), actorMessage.getRequestContext());

      userResponse =
          futureResponse.map(
              new Mapper<Map<String, Object>, Map<String, Object>>() {
                @Override
                public Map<String, Object> apply(Map<String, Object> responseMap) {
                  logger.info(
                      actorMessage.getRequestContext(),
                      "SearchHandlerActor:handleUserSearchAsyncRequest user search call ");
                  List<Map<String, Object>> respList = (List) responseMap.get(JsonKey.CONTENT);
                  isUserExists(respList);
                  Map<String, Object> userMap = respList.get(0);
                  userMap.put(JsonKey.EMAIL, userMap.get(JsonKey.MASKED_EMAIL));
                  userMap.put(JsonKey.PHONE, userMap.get(JsonKey.MASKED_PHONE));
                  isUserAccountDeleted(userMap);
                  return userMap;
                }
              },
              getContext().dispatcher());
    }

    handleUserSearchAsyncRequest(userResponse, actorMessage);
  }

  private void handleUserSearchAsyncRequest(
      Future<Map<String, Object>> userResponse, Request actorMessage) {
    Future<Object> orgResponse =
        userResponse.map(
            new Mapper<Map<String, Object>, Object>() {
              @Override
              public Object apply(Map<String, Object> parameter) {
                Map<String, Object> esOrgMap = new HashMap<>();
                esOrgMap =
                    (Map<String, Object>)
                        ElasticSearchHelper.getResponseFromFuture(
                            fetchRootAndRegisterOrganisation(
                                parameter, actorMessage.getRequestContext()));
                return esOrgMap;
              }
            },
            getContext().dispatcher());

    Future<Map<String, Object>> userOrgResponse =
        userResponse
            .zip(orgResponse)
            .map(
                new Mapper<Tuple2<Map<String, Object>, Object>, Map<String, Object>>() {
                  @Override
                  public Map<String, Object> apply(Tuple2<Map<String, Object>, Object> parameter) {
                    Map<String, Object> userMap = parameter._1;
                    userMap.put(JsonKey.ROOT_ORG, (Map<String, Object>) parameter._2);
                    userMap.remove(JsonKey.ENC_EMAIL);
                    userMap.remove(JsonKey.ENC_PHONE);
                    String requestedById =
                        (String) actorMessage.getContext().getOrDefault(JsonKey.REQUESTED_BY, "");
                    if (!(((String) userMap.get(JsonKey.USER_ID))
                        .equalsIgnoreCase(requestedById))) {
                      userMap = removeUserPrivateField(userMap);
                    }
                    return userMap;
                  }
                },
                getContext().dispatcher());

    Future<Map<String, Object>> externalIdFuture =
        userResponse.map(
            new Mapper<Map<String, Object>, Map<String, Object>>() {
              @Override
              public Map<String, Object> apply(Map<String, Object> result) {
                Map<String, Object> extIdMap = new HashMap<>();
                String requestedById =
                    (String) actorMessage.getContext().getOrDefault(JsonKey.REQUESTED_BY, "");
                if (((String) result.get(JsonKey.USER_ID)).equalsIgnoreCase(requestedById)) {
                  List<Map<String, String>> dbResExternalIds =
                      fetchUserExternalIdentity(
                          (String) result.get(JsonKey.USER_ID),
                          result,
                          actorMessage.getRequestContext());
                  extIdMap.put(JsonKey.EXTERNAL_IDS, dbResExternalIds);
                  return extIdMap;
                }
                return extIdMap;
              }
            },
            getContext().dispatcher());

    Future<Map<String, Object>> tncFuture =
        userOrgResponse.map(
            new Mapper<Map<String, Object>, Map<String, Object>>() {
              @Override
              public Map<String, Object> apply(Map<String, Object> result) {
                updateTnc(result);
                if (null != actorMessage.getRequest().get(JsonKey.FIELDS)) {
                  List<String> requestFields =
                      (List<String>) actorMessage.getRequest().get(JsonKey.FIELDS);
                  if (requestFields != null) {
                    addExtraFieldsInUserProfileResponse(
                        result, String.join(",", requestFields), actorMessage.getRequestContext());
                  } else {
                    result.remove(JsonKey.MISSING_FIELDS);
                    result.remove(JsonKey.COMPLETENESS);
                  }
                } else {
                  result.remove(JsonKey.MISSING_FIELDS);
                  result.remove(JsonKey.COMPLETENESS);
                }
                UserUtility.decryptUserDataFrmES(result);
                return result;
              }
            },
            getContext().dispatcher());

    Future<Response> sumFuture =
        externalIdFuture
            .zip(tncFuture)
            .map(
                new Mapper<Tuple2<Map<String, Object>, Map<String, Object>>, Response>() {
                  @Override
                  public Response apply(
                      Tuple2<Map<String, Object>, Map<String, Object>> parameter) {
                    Map<String, Object> externalIdMap = parameter._1;
                    logger.info(
                        actorMessage.getRequestContext(), "the externalId map is " + externalIdMap);
                    Map<String, Object> tncMap = parameter._2;
                    tncMap.putAll(externalIdMap);
                    Response response = new Response();
                    response.put(JsonKey.RESPONSE, tncMap);
                    return response;
                  }
                },
                getContext().dispatcher());

    Patterns.pipe(sumFuture, getContext().dispatcher()).to(sender());
  }

  private void isUserAccountDeleted(Map<String, Object> responseMap) {
    if (BooleanUtils.isTrue((Boolean) responseMap.get(JsonKey.IS_DELETED))) {
      throw new ProjectCommonException(
          ResponseCode.userAccountlocked.getErrorCode(),
          ResponseCode.userAccountlocked.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  private void isUserExists(List<Map<String, Object>> respList) {
    if (null == respList || respList.size() == 0) {
      throw new ProjectCommonException(
          ResponseCode.userNotFound.getErrorCode(),
          ResponseCode.userNotFound.getErrorMessage(),
          ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
    }
  }

  private void isUserExists(Map<String, Object> respMap) {
    if (MapUtils.isEmpty(respMap)) {
      throw new ProjectCommonException(
          ResponseCode.userNotFound.getErrorCode(),
          ResponseCode.userNotFound.getErrorMessage(),
          ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
    }
  }
}
