package org.sunbird.service.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import com.typesafe.config.ConfigFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.organisation.validator.OrgTypeValidator;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.response.ResponseMessage;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.operations.userorg.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.location.LocationService;
import org.sunbird.service.location.LocationServiceImpl;
import org.sunbird.service.organisation.OrgService;
import org.sunbird.service.organisation.impl.OrgServiceImpl;
import org.sunbird.service.user.impl.*;
import org.sunbird.util.*;
import org.sunbird.common.ProjectUtil;
import org.sunbird.util.user.UserTncUtil;
import org.sunbird.util.user.UserUtil;

public class UserProfileReadService {

  private final LoggerUtil logger = new LoggerUtil(UserProfileReadService.class);
  private final UserService userService = UserServiceImpl.getInstance();
  private final OrgService orgService = OrgServiceImpl.getInstance();
  private final UserRoleService userRoleService = UserRoleServiceImpl.getInstance();
  private final UserOrgService userOrgService = UserOrgServiceImpl.getInstance();
  private final LocationService locationService = LocationServiceImpl.getInstance();
  private final UserSelfDeclarationService userSelfDeclarationService =
      UserSelfDeclarationServiceImpl.getInstance();
  private final UserExternalIdentityService userExternalIdentityService =
    UserExternalIdentityServiceImpl.getInstance();
  private final ObjectMapper mapper = new ObjectMapper();

  public Response getUserProfileData(Request actorMessage) {
    String id = (String) actorMessage.getRequest().get(JsonKey.USER_ID);
    String idType = (String) actorMessage.getContext().get(JsonKey.ID_TYPE);
    String provider = (String) actorMessage.getContext().get(JsonKey.PROVIDER);
    boolean isPrivate = (boolean) actorMessage.getContext().get(JsonKey.PRIVATE);
    String readVersion = actorMessage.getOperation();
    String userId;
    // Check whether its normal read by id call or read by externalId call
    validateProviderAndIdType(provider, idType);
    if (StringUtils.isNotBlank(provider)) {
      userId = getUserIdByExternalId(actorMessage, id, idType, provider);
    } else {
      userId = id;
    }
    Map<String, Object> result =
        validateUserIdAndGetUserDetails(userId, actorMessage.getRequestContext());
    appendUserTypeAndLocation(result, actorMessage);
    Map<String, Object> rootOrg =
        orgService.getOrgById(
            (String) result.get(JsonKey.ROOT_ORG_ID), actorMessage.getRequestContext());
    if (MapUtils.isNotEmpty(rootOrg)
        && (readVersion.equalsIgnoreCase(ActorOperations.GET_USER_PROFILE_V4.getValue())
            || readVersion.equalsIgnoreCase(ActorOperations.GET_USER_PROFILE_V5.getValue()))) {
      Util.getOrgDefaultValue().keySet().forEach(rootOrg::remove);
      Util.getUserDefaultValue().keySet().forEach(result::remove);
    } else {
      result.putAll(Util.getUserDefaultValue());
    }

    OrgTypeValidator.getInstance().updateOrganisationTypeFlags(rootOrg);
    result.put(JsonKey.ROOT_ORG, rootOrg);
    Map<String, List<String>> userOrgRoles = null;
    List<Map<String, Object>> userRolesList =
        userRoleService.getUserRoles(userId, actorMessage.getRequestContext());
    if (readVersion.equalsIgnoreCase(ActorOperations.GET_USER_PROFILE_V5.getValue())) {
      result.put(JsonKey.ROLES, userRolesList);
    } else {
      result.remove(JsonKey.ROLES);
      userOrgRoles = getUserOrgRoles(userRolesList);
    }
    result.put(
        JsonKey.ORGANISATIONS,
        fetchUserOrgList(
            (String) result.get(JsonKey.ID), userOrgRoles, actorMessage.getRequestContext()));
    String requestedById =
        (String) actorMessage.getContext().getOrDefault(JsonKey.REQUESTED_BY, "");
    String managedForId = (String) actorMessage.getContext().getOrDefault(JsonKey.MANAGED_FOR, "");
    String managedBy = (String) result.get(JsonKey.MANAGED_BY);
    logger.debug(
        actorMessage.getRequestContext(),
        "requested By and requested user id == "
            + requestedById
            + "  "
            + userId
            + " managedForId= "
            + managedForId
            + " managedBy "
            + managedBy);
    if(ConfigFactory.load().getBoolean(JsonKey.AUTH_ENABLED)) {
      if (!isPrivate && StringUtils.isNotEmpty(managedBy) && !managedBy.equals(requestedById)) {
        ProjectCommonException.throwUnauthorizedErrorException();
      }
    }
    getManagedToken(actorMessage, userId, result, managedBy);
    String requestFields = (String) actorMessage.getContext().get(JsonKey.FIELDS);
    if (StringUtils.isNotBlank(userId)
        && (userId.equalsIgnoreCase(requestedById) || userId.equalsIgnoreCase(managedForId))
        && StringUtils.isBlank(requestFields)) {
      result.put(
          JsonKey.EXTERNAL_IDS,
          fetchUserExternalIdentity(userId, result, true, actorMessage.getRequestContext()));
    }
    if (StringUtils.isNotBlank((String) actorMessage.getContext().get(JsonKey.FIELDS))) {
      addExtraFieldsInUserProfileResponse(result, requestFields, actorMessage.getRequestContext());
    }
    String encEmail = (String) result.get(JsonKey.EMAIL);
    String encPhone = (String) result.get(JsonKey.PHONE);

    UserUtility.decryptUserDataFrmES(result);
    // Its used for Private user read api to display encoded email and encoded phone in api response
    if (isPrivate) {
      result.put((JsonKey.ENC_PHONE), encPhone);
      result.put((JsonKey.ENC_EMAIL), encEmail);
    }
    updateTnc(result);
    if (null != result.get(JsonKey.ALL_TNC_ACCEPTED)) {
      result.put(
          JsonKey.ALL_TNC_ACCEPTED,
          UserTncUtil.convertTncStringToJsonMap(
              (Map<String, String>) result.get(JsonKey.ALL_TNC_ACCEPTED)));
    }
    addFlagValue(result);
    appendMinorFlag(result);
    // For Backward compatibility , In ES we were sending identifier field
    result.put(JsonKey.IDENTIFIER, userId);

    Response response = new Response();
    response.put(JsonKey.RESPONSE, result);
    return response;
  }

  private Map<String, List<String>> getUserOrgRoles(List<Map<String, Object>> userRolesList) {
    Map<String, List<String>> userOrgRoles = new HashMap<>();
    for (Map userRole : userRolesList) {
      List<Map<String, String>> scopeMap = (List<Map<String, String>>) userRole.get(JsonKey.SCOPE);
      if (CollectionUtils.isNotEmpty(scopeMap)) {
        for (Map scope : scopeMap) {
          String orgId = (String) scope.get(JsonKey.ORGANISATION_ID);
          String role = (String) userRole.get(JsonKey.ROLE);
          if (userOrgRoles.containsKey(orgId)) {
            List<String> roles = userOrgRoles.get(orgId);
            roles.add(role);
            userOrgRoles.put(orgId, roles);
          } else {
            userOrgRoles.put(orgId, new ArrayList(Arrays.asList(role)));
          }
        }
      }
    }
    return userOrgRoles;
  }

  public void appendUserTypeAndLocation(Map<String, Object> result, Request actorMessage) {
    List<Map<String, Object>> userTypeDetailsList = new ArrayList<>();
    try {
      if (StringUtils.isNotEmpty((String) result.get(JsonKey.PROFILE_USERTYPES))) {
        userTypeDetailsList =
            mapper.readValue(
                (String) result.get(JsonKey.PROFILE_USERTYPES), new TypeReference<>() {});
      }
    } catch (Exception e) {
      logger.error(
          actorMessage.getRequestContext(),
          "Exception because of mapper read value" + result.get(JsonKey.PROFILE_USERTYPES),
          e);
    }
    Map<String, Object> userTypeDetails = new HashMap<>();
    try {
      if (StringUtils.isNotEmpty((String) result.get(JsonKey.PROFILE_USERTYPE))) {
        userTypeDetails =
            mapper.readValue(
                (String) result.get(JsonKey.PROFILE_USERTYPE), new TypeReference<>() {});
      }
    } catch (Exception e) {
      logger.error(
          actorMessage.getRequestContext(),
          "Exception because of mapper read value" + result.get(JsonKey.PROFILE_USERTYPE),
          e);
    }

    List<Map<String, String>> userLocList = new ArrayList<>();
    List<String> locationIds = new ArrayList<>();
    try {
      if (StringUtils.isNotEmpty((String) result.get(JsonKey.PROFILE_LOCATION))) {
        userLocList =
            mapper.readValue(
                (String) result.get(JsonKey.PROFILE_LOCATION), new TypeReference<>() {});
        if (CollectionUtils.isNotEmpty(userLocList)) {
          locationIds =
              userLocList.stream().map(m -> m.get(JsonKey.ID)).collect(Collectors.toList());
        }
      }
    } catch (Exception ex) {
      logger.error(
          actorMessage.getRequestContext(),
          "Exception occurred while mapping " + result.get(JsonKey.PROFILE_LOCATION),
          ex);
    }
    if (actorMessage
        .getOperation()
        .equalsIgnoreCase(ActorOperations.GET_USER_PROFILE_V3.getValue())) {
      if (MapUtils.isNotEmpty(userTypeDetails)) {
        result.put(JsonKey.USER_TYPE, userTypeDetails.get(JsonKey.TYPE));
        result.put(JsonKey.USER_SUB_TYPE, userTypeDetails.get(JsonKey.SUB_TYPE));
      } else {
        result.put(JsonKey.USER_TYPE, null);
        result.put(JsonKey.USER_SUB_TYPE, null);
      }
      result.put(JsonKey.LOCATION_IDS, locationIds);
    } else {
      result.remove(JsonKey.USER_TYPE);
      result.remove(JsonKey.USER_SUB_TYPE);
      result.remove(JsonKey.LOCATION_IDS);
    }
    result.put(JsonKey.PROFILE_USERTYPE, userTypeDetails);
    result.put(JsonKey.PROFILE_USERTYPES, userTypeDetailsList);
    result.put(JsonKey.PROFILE_LOCATION, userLocList);
  }

  private void appendMinorFlag(Map<String, Object> result) {
    String dob = (String) result.get(JsonKey.DOB);
    if (StringUtils.isNotEmpty(dob)) {
      int year = Integer.parseInt(dob.split("-")[0]);
      LocalDate currentDate = LocalDate.now();
      int currentYear = currentDate.getYear();
      // reason for keeping 19 instead of 18 is, all dob's will be saving with 12-31 appending to
      // the year so 18 will be completed in the jan 1st
      // for eg: 2004-12-31 will become major after 2023 jan 1st.
      boolean isMinor = currentYear - year <= 19;
      result.put(JsonKey.IS_MINOR, isMinor);
    }
  }

  private void addFlagValue(Map<String, Object> userDetails) {
    int flagsValue = Integer.parseInt(userDetails.get(JsonKey.FLAGS_VALUE).toString());
    Map<String, Boolean> userFlagMap = UserFlagUtil.assignUserFlagValues(flagsValue);
    userDetails.putAll(userFlagMap);
  }

  private Map<String, Object> getManagedToken(
      Request actorMessage, String userId, Map<String, Object> result, String managedBy) {
    boolean withTokens =
        Boolean.parseBoolean((String) actorMessage.getContext().get(JsonKey.WITH_TOKENS));

    if (withTokens && StringUtils.isNotEmpty(managedBy)) {
      String managedToken = (String) actorMessage.getContext().get(JsonKey.MANAGED_TOKEN);
      if (StringUtils.isEmpty(managedToken)) {
        logger.debug(
            actorMessage.getRequestContext(),
            "UserProfileReadService: getUserProfileData: calling token generation for: " + userId);
        List<Map<String, Object>> userList = new ArrayList<>();
        userList.add(result);
        // Fetch encrypted token from admin utils
        Map<String, Object> encryptedTokenList =
            userService.fetchEncryptedToken(managedBy, userList, actorMessage.getRequestContext());
        // encrypted token for each managedUser in respList
        userService.appendEncryptedToken(
            encryptedTokenList, userList, actorMessage.getRequestContext());
        result = userList.get(0);
      } else {
        result.put(JsonKey.MANAGED_TOKEN, managedToken);
      }
    }
    return result;
  }

  private List<Map<String, Object>> fetchUserOrgList(
      String userId, Map<String, List<String>> userOrgRoles, RequestContext requestContext) {
    List<Map<String, Object>> usrOrgList = new ArrayList<>();
    List<Map<String, Object>> userOrgList =
        userOrgService.getUserOrgListByUserId(userId, requestContext);
    for (Map<String, Object> userOrg : userOrgList) {
      Boolean isDeleted = (Boolean) userOrg.get(JsonKey.IS_DELETED);
      if (null == isDeleted || (!isDeleted.booleanValue())) {
        updateAssociationMechanism(userOrg);
        userOrg.remove(JsonKey.ROLES);
        String organisationId = (String) userOrg.get(JsonKey.ORGANISATION_ID);
        if (MapUtils.isNotEmpty(userOrgRoles) && userOrgRoles.containsKey(organisationId)) {
          userOrg.put(JsonKey.ROLES, userOrgRoles.get(organisationId));
        }
        usrOrgList.add(userOrg);
      }
    }
    return usrOrgList;
  }

  private void updateAssociationMechanism(Map<String, Object> userOrg) {
    AssociationMechanism associationMechanism = new AssociationMechanism();
    if (null != userOrg.get(JsonKey.ASSOCIATION_TYPE)) {
      int associationType = (int) userOrg.get(JsonKey.ASSOCIATION_TYPE);
      associationMechanism.setAssociationType(associationType);
      userOrg.put(JsonKey.IS_SSO, associationMechanism.isAssociationType(AssociationMechanism.SSO));
      userOrg.put(
          JsonKey.IS_SELF_DECLARATION,
          associationMechanism.isAssociationType(AssociationMechanism.SELF_DECLARATION));
      userOrg.put(
          JsonKey.IS_SYSTEM_UPLOAD,
          associationMechanism.isAssociationType(AssociationMechanism.SYSTEM_UPLOAD));
    }
  }

  private Map<String, Object> validateUserIdAndGetUserDetails(
      String userId, RequestContext context) {
    Map<String, Object> user = userService.getUserDetailsById(userId, context);
    // check whether user active or not
    Boolean isDeleted = (Boolean) user.get(JsonKey.IS_DELETED);
    if (null != isDeleted && isDeleted) {
      ProjectCommonException.throwClientErrorException(ResponseCode.userAccountlocked);
    }
    removeUserPrivateField(user);
    return user;
  }

  private String getUserIdByExternalId(
      Request actorMessage, String id, String idType, String provider) {
    String userId =
        userExternalIdentityService.getUserV1(
            id, provider, idType, actorMessage.getRequestContext());
    if (StringUtils.isBlank(userId)) {
      throw new ProjectCommonException(
          ResponseCode.resourceNotFound,
          ProjectUtil.formatMessage(
              ResponseMessage.Message.EXTERNALID_NOT_FOUND, id, idType, provider),
          ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
    }
    return userId;
  }

  private void validateProviderAndIdType(String provider, String idType) {
    if (StringUtils.isNotBlank(provider) && StringUtils.isBlank(idType)) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.mandatoryParamsMissing,
          MessageFormat.format(
              ResponseCode.mandatoryParamsMissing.getErrorMessage(), JsonKey.ID_TYPE));
    }
  }

  private void removeUserPrivateField(Map<String, Object> responseMap) {
    for (int i = 0; i < ProjectUtil.excludes.length; i++) {
      responseMap.remove(ProjectUtil.excludes[i]);
    }
    responseMap.remove(JsonKey.ADDRESS);
  }

  public void updateTnc(Map<String, Object> userMap) {
    Map<String, Object> tncConfigMap = null;
    try {
      String tncValue = DataCacheHandler.getConfigSettings().get(JsonKey.TNC_CONFIG);
      tncConfigMap = mapper.readValue(tncValue, Map.class);

    } catch (Exception e) {
      logger.error(
          "UserProfileReadService:updateTncInfo: Exception occurred while getting system setting for"
              + JsonKey.TNC_CONFIG
              + e.getMessage(),
          e);
    }

    if (MapUtils.isNotEmpty(tncConfigMap)) {
      try {
        String tncLatestVersion = (String) tncConfigMap.get(JsonKey.LATEST_VERSION);
        userMap.put(JsonKey.TNC_LATEST_VERSION, tncLatestVersion);
        String tncUserAcceptedVersion = (String) userMap.get(JsonKey.TNC_ACCEPTED_VERSION);
        Object tncUserAcceptedOn = userMap.get(JsonKey.TNC_ACCEPTED_ON);
        userMap.put(JsonKey.PROMPT_TNC, false);
        String url = (String) ((Map) tncConfigMap.get(tncLatestVersion)).get(JsonKey.URL);
        logger.debug("UserProfileReadService:updateTncInfo: url = " + url);
        userMap.put(JsonKey.TNC_LATEST_VERSION_URL, url);
        if ((StringUtils.isEmpty(tncUserAcceptedVersion)
                || !tncUserAcceptedVersion.equalsIgnoreCase(tncLatestVersion)
                || (null == tncUserAcceptedOn))
            && (tncConfigMap.containsKey(tncLatestVersion))) {
          userMap.put(JsonKey.PROMPT_TNC, true);
        }
      } catch (Exception e) {
        logger.error(
            "UserProfileReadService:updateTncInfo: Exception occurred with error message = "
                + e.getMessage(),
            e);
      }
    }
  }

  public List<Map<String, String>> fetchUserExternalIdentity(
      String userId, Map<String, Object> user, boolean mergeDeclarations, RequestContext context) {
    try {
      List<Map<String, String>> dbResExternalIds =
          userExternalIdentityService.getExternalIds(userId, mergeDeclarations, context);
      // update orgId to provider in externalIds
      String rootOrgId = (String) user.get(JsonKey.ROOT_ORG_ID);
      String provider = (String) user.get(JsonKey.CHANNEL);
      updateExternalIdsOrgIdWithProvider(dbResExternalIds, rootOrgId, provider, context);
      return dbResExternalIds;
    } catch (Exception ex) {
      logger.error(
          context, "Exception occurred while fetching user externalId. " + ex.getMessage(), ex);
    }
    return new ArrayList<>();
  }

  private void updateExternalIdsOrgIdWithProvider(
      List<Map<String, String>> dbResExternalIds,
      String rootOrgId,
      String provider,
      RequestContext context) {
    if (CollectionUtils.isNotEmpty(dbResExternalIds)
        && StringUtils.isNotBlank(rootOrgId)
        && StringUtils.isNotBlank(dbResExternalIds.get(0).get(JsonKey.PROVIDER))
        && ((dbResExternalIds.get(0).get(JsonKey.PROVIDER)).equalsIgnoreCase(rootOrgId))) {
      dbResExternalIds.forEach(
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
  }

  public void addExtraFieldsInUserProfileResponse(
      Map<String, Object> result, String fields, RequestContext context) {
    if (!StringUtils.isBlank(fields)) {
      result.put(JsonKey.LAST_LOGIN_TIME, Long.parseLong("0"));
      if (fields.contains(JsonKey.TOPIC)) {
        result.put(JsonKey.TOPICS, new HashSet<>());
      }
      if (fields.contains(JsonKey.ROLES)) {
        result.put(JsonKey.ROLE_LIST, DataCacheHandler.getUserReadRoleList());
      }
      if (fields.contains(JsonKey.ORGANISATIONS)) {
        updateUserOrgInfo((List) result.get(JsonKey.ORGANISATIONS), context);
      }
      if (fields.contains(JsonKey.LOCATIONS)) {
        List<Map<String, String>> userLocList =
            (List<Map<String, String>>) result.get(JsonKey.PROFILE_LOCATION);
        if (CollectionUtils.isNotEmpty(userLocList)) {
          List<String> locationIds =
              userLocList.stream().map(m -> m.get(JsonKey.ID)).collect(Collectors.toList());
          List<Map<String, Object>> userLocations = getUserLocations(locationIds, context);
          if (CollectionUtils.isNotEmpty(userLocations)) {
            result.put(JsonKey.USER_LOCATIONS, userLocations);
            // For adding school, request need to have fields=locations,organisations, as externalid
            // id is populated with this request only
            if (fields.contains(JsonKey.ORGANISATIONS)) {
              try {
                addSchoolLocation(result, context);
              } catch (Exception e) {
                logger.error("Not able to fetch school details in user read - user location", e);
              }
            }
            result.remove(JsonKey.LOCATION_IDS);
            result.remove(JsonKey.PROFILE_LOCATION);
          }
        }
      }
      if (fields.contains(JsonKey.DECLARATIONS)) {
        List<Map<String, Object>> declarations =
            userSelfDeclarationService.fetchUserDeclarations(
                (String) result.get(JsonKey.ID), context);
        result.put(JsonKey.DECLARATIONS, declarations);
      }
      if (CollectionUtils.isEmpty((List<Map<String, String>>) result.get(JsonKey.EXTERNAL_IDS))
          && fields.contains(JsonKey.EXTERNAL_IDS)) {
        List<Map<String, String>> resExternalIds =
            fetchUserExternalIdentity((String) result.get(JsonKey.ID), result, false, context);
        result.put(JsonKey.EXTERNAL_IDS, resExternalIds);
      }
    }
  }

  private void addSchoolLocation(Map<String, Object> result, RequestContext context) {
    String rootOrgId = (String) result.get(JsonKey.ROOT_ORG_ID);
    List<Map<String, Object>> organisations =
        (List<Map<String, Object>>) result.get(JsonKey.ORGANISATIONS);
    List<Map<String, Object>> userLocation =
        (List<Map<String, Object>>) result.get(JsonKey.USER_LOCATIONS);
    // inorder to add school, user should have sub-org and attached to locations hierarchy as parent
    // block/cluster
    if (CollectionUtils.isNotEmpty(organisations)
        && organisations.size() > 1
        && userLocation.size() >= 3) {
      for (int i = 0; i < organisations.size(); i++) {
        String organisationId = (String) organisations.get(i).get(JsonKey.ORGANISATION_ID);
        if (StringUtils.isNotBlank(organisationId) && !organisationId.equalsIgnoreCase(rootOrgId)) {
          if (StringUtils.isNotBlank((String) organisations.get(i).get(JsonKey.ORG_NAME))
              && StringUtils.isNotBlank((String) organisations.get(i).get(JsonKey.EXTERNAL_ID))) {
            Map<String, Object> schoolLocation = new HashMap<>();
            schoolLocation.put(JsonKey.NAME, organisations.get(i).get(JsonKey.ORG_NAME));
            schoolLocation.put(JsonKey.TYPE, JsonKey.LOCATION_TYPE_SCHOOL);
            schoolLocation.put(JsonKey.CODE, organisations.get(i).get(JsonKey.EXTERNAL_ID));
            schoolLocation.put(JsonKey.ID, organisationId);
            schoolLocation.put(JsonKey.PARENT_ID, "");
            userLocation.add(schoolLocation);
          } else {
            logger.info(context, "School details are blank for orgId = " + organisationId);
          }
        }
      }
    }
  }

  private List<Map<String, Object>> getUserLocations(
      List<String> locationIds, RequestContext context) {
    if (CollectionUtils.isNotEmpty(locationIds)) {
      List<String> locationFields =
          Arrays.asList(JsonKey.CODE, JsonKey.NAME, JsonKey.TYPE, JsonKey.PARENT_ID, JsonKey.ID);
      return locationService.getLocationsByIds(locationIds, locationFields, context);
    }
    return new ArrayList<>();
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
              JsonKey.ORG_LOCATION,
              JsonKey.LOCATION_IDS,
              JsonKey.ID,
              JsonKey.EXTERNAL_ID,
              JsonKey.ORGANISATION_TYPE);
      List<Map<String, Object>> userOrgResponseList =
          orgService.getOrgByIds(orgIds, fields, context);
      if (CollectionUtils.isNotEmpty(userOrgResponseList)) {
        return userOrgResponseList
            .stream()
            .collect(Collectors.toMap(obj -> (String) obj.get("id"), val -> val));
      }
    }
    return new HashMap<>();
  }

  private Map<String, Map<String, Object>> fetchAllLocationsById(
      Map<String, Map<String, Object>> orgInfoMap, RequestContext context) {
    Set<String> locationSet = new HashSet<>();
    for (Map<String, Object> org : orgInfoMap.values()) {
      List<String> locationIds = null;
      List<Map<String, String>> orgLocList = (List) org.get(JsonKey.ORG_LOCATION);
      if (CollectionUtils.isNotEmpty(orgLocList)) {
        locationIds = orgLocList.stream().map(m -> m.get(JsonKey.ID)).collect(Collectors.toList());
        org.put(JsonKey.ORG_LOCATION, orgLocList);
      } else {
        org.put(JsonKey.ORG_LOCATION, new ArrayList<>());
      }
      if (CollectionUtils.isNotEmpty(locationIds)) {
        locationIds.forEach(
            locId -> {
              if (StringUtils.isNotBlank(locId)) {
                locationSet.add(locId);
              }
            });
      }
      org.put(JsonKey.LOCATION_IDS, locationIds);
    }
    if (CollectionUtils.isNotEmpty(locationSet)) {
      List<String> locList = new ArrayList<>(locationSet);
      List<Map<String, Object>> locationResponseList =
          locationService.getLocationsByIds(locList, null, context);
      return locationResponseList
          .stream()
          .collect(Collectors.toMap(obj -> (String) obj.get("id"), val -> val));
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
      if (MapUtils.isNotEmpty(orgInfo)) {
        usrOrg.put(JsonKey.ORG_NAME, orgInfo.get(JsonKey.ORG_NAME));
        usrOrg.put(JsonKey.CHANNEL, orgInfo.get(JsonKey.CHANNEL));
        usrOrg.put(JsonKey.HASHTAGID, orgInfo.get(JsonKey.HASHTAGID));
        usrOrg.put(JsonKey.LOCATION_IDS, orgInfo.get(JsonKey.LOCATION_IDS));
        usrOrg.put(JsonKey.ORG_LOCATION, orgInfo.get(JsonKey.ORG_LOCATION));
        usrOrg.put(JsonKey.EXTERNAL_ID, orgInfo.get(JsonKey.EXTERNAL_ID));
        if (null != orgInfo.get(JsonKey.ORGANISATION_TYPE)) {
          int orgType = (int) orgInfo.get(JsonKey.ORGANISATION_TYPE);
          boolean isSchool =
              (orgType == OrgTypeValidator.getInstance().getValueByType(JsonKey.ORG_TYPE_SCHOOL)) ? true : false;
          usrOrg.put(JsonKey.IS_SCHOOL, isSchool);
        }
        if (MapUtils.isNotEmpty(locationInfoMap)) {
          usrOrg.put(
              JsonKey.LOCATIONS,
              prepLocationFields(
                  (List<String>) orgInfo.get(JsonKey.LOCATION_IDS), locationInfoMap));
        }
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
}
