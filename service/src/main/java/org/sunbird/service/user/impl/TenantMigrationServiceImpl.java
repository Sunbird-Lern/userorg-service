package org.sunbird.service.user.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.organisation.OrgExternalService;
import org.sunbird.service.organisation.OrgService;
import org.sunbird.service.organisation.impl.OrgExternalServiceImpl;
import org.sunbird.service.organisation.impl.OrgServiceImpl;
import org.sunbird.service.user.TenantMigrationService;
import org.sunbird.service.user.UserOrgService;
import org.sunbird.service.user.UserService;
import org.sunbird.keycloak.SSOManager;
import org.sunbird.keycloak.SSOServiceFactory;
import org.sunbird.util.DataCacheHandler;
import org.sunbird.common.ProjectUtil;
import org.sunbird.utils.StringFormatter;

public class TenantMigrationServiceImpl implements TenantMigrationService {

  public final LoggerUtil logger = new LoggerUtil(this.getClass());
  private final UserService userService = UserServiceImpl.getInstance();
  private static TenantMigrationService tenantMigrationService = null;
  private final UserOrgService userOrgService = UserOrgServiceImpl.getInstance();

  public static TenantMigrationService getInstance() {
    if (tenantMigrationService == null) {
      tenantMigrationService = new TenantMigrationServiceImpl();
    }
    return tenantMigrationService;
  }

  public void validateChannelAndGetRootOrgId(Request request) {
    String rootOrgId = "";
    String channel = (String) request.getRequest().get(JsonKey.CHANNEL);
    if (StringUtils.isNotBlank(channel)) {
      rootOrgId =
          OrgServiceImpl.getInstance()
              .getRootOrgIdFromChannel(channel, request.getRequestContext());
      request.getRequest().put(JsonKey.ROOT_ORG_ID, rootOrgId);
    }
  }

  public void validateUserCustodianOrgId(String rootOrgId) {
    String custodianOrgId = DataCacheHandler.getConfigSettings().get(JsonKey.CUSTODIAN_ORG_ID);
    if (!rootOrgId.equalsIgnoreCase(custodianOrgId)) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.parameterMismatch,
          MessageFormat.format(
              ResponseCode.parameterMismatch.getErrorMessage(),
              "user rootOrgId and custodianOrgId"));
    }
  }

  public String validateOrgExternalIdOrOrgIdAndGetOrgId(
      Map<String, Object> migrateReq, RequestContext context) {
    logger.debug(context, "TenantMigrationActor:validateOrgExternalIdOrOrgIdAndGetOrgId called.");
    String orgId = "";
    if (StringUtils.isNotBlank((String) migrateReq.get(JsonKey.ORG_ID))
        || StringUtils.isNotBlank((String) migrateReq.get(JsonKey.ORG_EXTERNAL_ID))) {
      if (StringUtils.isNotBlank((String) migrateReq.get(JsonKey.ORG_ID))) {
        orgId = (String) migrateReq.get(JsonKey.ORG_ID);
        OrgService orgService = OrgServiceImpl.getInstance();
        Map<String, Object> result = orgService.getOrgById(orgId, context);
        if (MapUtils.isEmpty(result)) {
          logger.debug(
              context,
              "TenantMigrationActor:validateOrgExternalIdOrOrgIdAndGetOrgId called. OrgId is Invalid");
          ProjectCommonException.throwClientErrorException(ResponseCode.invalidParameter,
            MessageFormat.format(
              ResponseCode.invalidParameter.getErrorMessage(),
              JsonKey.ORG_ID));
        } else {
          String reqOrgRootOrgId = (String) result.get(JsonKey.ROOT_ORG_ID);
          if (StringUtils.isNotBlank(reqOrgRootOrgId)
              && !reqOrgRootOrgId.equalsIgnoreCase((String) migrateReq.get(JsonKey.ROOT_ORG_ID))) {
            ProjectCommonException.throwClientErrorException(
                ResponseCode.parameterMismatch,
                MessageFormat.format(
                    ResponseCode.parameterMismatch.getErrorMessage(),
                    StringFormatter.joinByComma(JsonKey.CHANNEL, JsonKey.ORG_ID)));
          } else {
            if (MapUtils.isNotEmpty(result)) {
              fetchLocationIds(context, migrateReq, result);
            }
          }
        }
      } else if (StringUtils.isNotBlank((String) migrateReq.get(JsonKey.ORG_EXTERNAL_ID))) {
        OrgExternalService orgExternalService = new OrgExternalServiceImpl();
        orgId =
            orgExternalService.getOrgIdFromOrgExternalIdAndProvider(
                (String) migrateReq.get(JsonKey.ORG_EXTERNAL_ID),
                (String) migrateReq.get(JsonKey.CHANNEL),
                context);
        if (StringUtils.isBlank(orgId)) {
          logger.debug(
              context,
              "TenantMigrationActor:validateOrgExternalIdOrOrgIdAndGetOrgId called. OrgExternalId is Invalid");
          ProjectCommonException.throwClientErrorException(
              ResponseCode.invalidParameterValue,
              MessageFormat.format(
                  ResponseCode.invalidParameterValue.getErrorMessage(),
                  migrateReq.get(JsonKey.ORG_EXTERNAL_ID),
                  JsonKey.ORG_EXTERNAL_ID));
        } else {
          // Fetch locationids of the suborg and update the location of sso user
          OrgService orgService = OrgServiceImpl.getInstance();
          Map<String, Object> orgMap = orgService.getOrgById(orgId, context);
          if (MapUtils.isNotEmpty(orgMap)) {
            fetchLocationIds(context, migrateReq, orgMap);
          }
        }
      }
    }
    return orgId;
  }

  public Response migrateUser(Map<String, Object> migrateUserDetails, RequestContext context) {
    return userService.updateUser(migrateUserDetails, context);
  }

  public void deactivateUserFromKC(String userId, RequestContext context) {
    try {
      Map<String, Object> userDbMap = new HashMap<>();
      userDbMap.put(JsonKey.USER_ID, userId);
      String status = getSSOManager().deactivateUser(userDbMap, context);
      logger.debug(
          context,
          "TenantMigrationActor:deactivateUserFromKC:user status in deactivating Keycloak"
              + status);
    } catch (Exception e) {
      logger.error(
          context,
          "TenantMigrationActor:deactivateUserFromKC:Error occurred while deactivating user from  Keycloak",
          e);
    }
  }

  private SSOManager getSSOManager() {
    return SSOServiceFactory.getInstance();
  }

  public Response updateUserOrg(Request request, List<Map<String, Object>> userOrgList) {
    logger.debug(request.getRequestContext(), "TenantMigrationActor:updateUserOrg called.");
    Response response = new Response();
    Boolean isSoftDelete = (Boolean) request.get(JsonKey.SOFT_DELETE_PREVIOUS_ORG);
    if (CollectionUtils.isNotEmpty(userOrgList)) {
      if (null != isSoftDelete && isSoftDelete) {
        softDeleteOldUserOrgMapping(userOrgList, request.getRequestContext());
      } else {
        deleteOldUserOrgMapping(userOrgList, request.getRequestContext());
      }
    }
    Map<String, Object> userDetails = request.getRequest();
    // add mapping root org
    createUserOrgRequestAndUpdate(
        (String) userDetails.get(JsonKey.USER_ID),
        (String) userDetails.get(JsonKey.ROOT_ORG_ID),
        request.getRequestContext());
    String orgId = (String) userDetails.get(JsonKey.ORG_ID);
    if (StringUtils.isNotBlank(orgId)
        && !((String) userDetails.get(JsonKey.ROOT_ORG_ID)).equalsIgnoreCase(orgId)) {
      try {
        createUserOrgRequestAndUpdate(
            (String) userDetails.get(JsonKey.USER_ID), orgId, request.getRequestContext());
        logger.debug(
            request.getRequestContext(),
            "TenantMigrationActor:updateUserOrg user org data got updated.");
      } catch (Exception ex) {
        logger.error(
            request.getRequestContext(),
            "TenantMigrationActor:updateUserOrg:Exception occurred while updating user Org.",
            ex);
        List<Map<String, Object>> errMsgList = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        map.put(JsonKey.ERROR_MSG, ex.getMessage());
        errMsgList.add(map);
        response.getResult().put(JsonKey.ERRORS, errMsgList);
      }
    }
    return response;
  }

  private void deleteOldUserOrgMapping(
      List<Map<String, Object>> userOrgList, RequestContext context) {
    userOrgService.deleteUserOrgMapping(userOrgList, context);
  }

  private void createUserOrgRequestAndUpdate(String userId, String orgId, RequestContext context) {
    Map<String, Object> userOrgRequest = new HashMap<>();
    userOrgRequest.put(JsonKey.ID, userId);
    userOrgRequest.put(JsonKey.HASHTAGID, orgId);
    userOrgRequest.put(JsonKey.ORGANISATION_ID, orgId);
    List<String> roles = new ArrayList<>();
    roles.add(ProjectUtil.UserRole.PUBLIC.getValue());
    userOrgRequest.put(JsonKey.ROLES, roles);
    userOrgService.registerUserToOrg(userOrgRequest, context);
  }

  private void fetchLocationIds(
      RequestContext context, Map<String, Object> migrateReq, Map<String, Object> orgMap) {
    List orgLocation = (List) orgMap.get(JsonKey.ORG_LOCATION);
    if (CollectionUtils.isNotEmpty(orgLocation)) {
      try {
        ObjectMapper mapper = new ObjectMapper();
        migrateReq.put(JsonKey.PROFILE_LOCATION, mapper.writeValueAsString(orgLocation));
      } catch (Exception e) {
        logger.info(context, "Exception occurred while converting orgLocation to String.");
      }
    }
  }

  private void softDeleteOldUserOrgMapping(
          List<Map<String, Object>> userOrgList, RequestContext context) {
    userOrgService.softDeleteOldUserOrgMapping(userOrgList, context);
  }
}
