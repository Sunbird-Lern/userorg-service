package org.sunbird.service.user.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.user.validator.UserCreateRequestValidator;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.response.ResponseMessage;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.model.organisation.Organisation;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.organisation.OrgExternalService;
import org.sunbird.service.organisation.OrgService;
import org.sunbird.service.organisation.impl.OrgExternalServiceImpl;
import org.sunbird.service.organisation.impl.OrgServiceImpl;
import org.sunbird.service.user.SSOUserService;
import org.sunbird.service.user.UserLookupService;
import org.sunbird.service.user.UserService;
import org.sunbird.util.DataCacheHandler;
import org.sunbird.common.ProjectUtil;
import org.sunbird.utils.StringFormatter;
import org.sunbird.util.user.UserUtil;

public class SSOUserServiceImpl implements SSOUserService {

  private final LoggerUtil logger = new LoggerUtil(SSOUserServiceImpl.class);

  private static SSOUserService ssoUserService = null;
  private final OrgExternalService orgExternalService = OrgExternalServiceImpl.getInstance();
  private final UserService userService = UserServiceImpl.getInstance();
  private final UserLookupService userLookupService = UserLookUpServiceImpl.getInstance();
  private final OrgService orgService = OrgServiceImpl.getInstance();

  public static SSOUserService getInstance() {
    if (ssoUserService == null) {
      ssoUserService = new SSOUserServiceImpl();
    }
    return ssoUserService;
  }

  public Map validateOrgIdAndPrimaryRecoveryKeys(
      Map<String, Object> userMap, Request actorMessage) {
    validateChannelAndOrganisationId(userMap, actorMessage.getRequestContext());
    UserCreateRequestValidator.validatePrimaryAndRecoveryKeys(userMap);
    populateProfileUserType(userMap, actorMessage.getRequestContext());
    // remove these fields from req
    userMap.remove(JsonKey.ENC_EMAIL);
    userMap.remove(JsonKey.ENC_PHONE);
    actorMessage.getRequest().putAll(userMap);

    boolean isCustodianOrg = false;
    if (StringUtils.isBlank((String) actorMessage.getContext().get(JsonKey.CALLER_ID))) {
      userMap.put(JsonKey.CREATED_BY, actorMessage.getContext().get(JsonKey.REQUESTED_BY));
      if (StringUtils.isBlank((String) userMap.get(JsonKey.CHANNEL))
          && StringUtils.isBlank((String) userMap.get(JsonKey.ROOT_ORG_ID))) {
        userMap.put(
            JsonKey.ROOT_ORG_ID,
            DataCacheHandler.getConfigSettings().get(JsonKey.CUSTODIAN_ORG_ID));
        userMap.put(
            JsonKey.CHANNEL,
            DataCacheHandler.getConfigSettings().get(JsonKey.CUSTODIAN_ORG_CHANNEL));
        isCustodianOrg = true;
      }
    }
    if (null != userMap.get(JsonKey.ORG_EXTERNAL_ID)
        && StringUtils.isNotBlank((String) userMap.get(JsonKey.ORG_EXTERNAL_ID))) {
      String orgId = validateExternalIdAndGetOrgId(userMap, actorMessage.getRequestContext());
      userMap.put(JsonKey.ORGANISATION_ID, orgId);

      // Fetch locationids of the suborg and update the location of sso user
      if (!isCustodianOrg) {
        OrgService orgService = OrgServiceImpl.getInstance();
        Map<String, Object> orgMap = orgService.getOrgById(orgId, actorMessage.getRequestContext());
        if (MapUtils.isNotEmpty(orgMap)) {
          userMap.put(JsonKey.PROFILE_LOCATION, orgMap.get(JsonKey.ORG_LOCATION));
        }
      }
    }
    return userMap;
  }

  public Response createUserAndPassword(
      Map<String, Object> requestMap, Map<String, Object> userMap, Request request) {
    Response response = null;
    boolean isPasswordUpdated = false;
    Map<String, Object> userLookUpData = new HashMap<>(userMap);
    try {
      response = userService.createUser(requestMap, request.getRequestContext());
      userLookupService.insertRecords(userLookUpData, request.getRequestContext());
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
    return response;
  }

  private void validateChannelAndOrganisationId(
      Map<String, Object> userMap, RequestContext context) {
    String requestedOrgId = (String) userMap.get(JsonKey.ORGANISATION_ID);
    String requestedChannel = (String) userMap.get(JsonKey.CHANNEL);
    String fetchedRootOrgIdByChannel = "";
    if (StringUtils.isNotBlank(requestedChannel)) {
      fetchedRootOrgIdByChannel = orgService.getRootOrgIdFromChannel(requestedChannel, context);
      if (StringUtils.isBlank(fetchedRootOrgIdByChannel)) {
        throw new ProjectCommonException(
            ResponseCode.invalidParameterValue,
            ProjectUtil.formatMessage(
                ResponseCode.invalidParameterValue.getErrorMessage(),
                requestedChannel,
                JsonKey.CHANNEL),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
      userMap.put(JsonKey.ROOT_ORG_ID, fetchedRootOrgIdByChannel);
    }
    Organisation fetchedOrgById;
    if (StringUtils.isNotBlank(requestedOrgId)) {
      fetchedOrgById = orgService.getOrgObjById(requestedOrgId, context);
      if (null == fetchedOrgById) {
        ProjectCommonException.throwClientErrorException(
            ResponseCode.invalidParameter,
            MessageFormat.format(
                ResponseCode.invalidParameter.getErrorMessage(), JsonKey.ORGANISATION));
      }
      // if requested orgId is not blank then its channel should match with requested channel
      if (StringUtils.isNotBlank(requestedChannel)
          && !requestedChannel.equalsIgnoreCase(fetchedOrgById.getChannel())) {
        throwParameterMismatchException(JsonKey.CHANNEL, JsonKey.ORGANISATION_ID);
      }
      if (fetchedOrgById.isTenant()) {
        if (StringUtils.isNotBlank(requestedChannel)
            && !fetchedRootOrgIdByChannel.equalsIgnoreCase(fetchedOrgById.getId())) {
          throwParameterMismatchException(JsonKey.CHANNEL, JsonKey.ORGANISATION_ID);
        }
        userMap.put(JsonKey.ROOT_ORG_ID, fetchedOrgById.getId());
        userMap.put(JsonKey.CHANNEL, fetchedOrgById.getChannel());
      } else {
        if (StringUtils.isNotBlank(requestedChannel)) {
          userMap.put(JsonKey.ROOT_ORG_ID, fetchedRootOrgIdByChannel);
        } else {
          // fetch rootorgid by requested orgid channel
          String rootOrgId =
              orgService.getRootOrgIdFromChannel(fetchedOrgById.getChannel(), context);
          userMap.put(JsonKey.ROOT_ORG_ID, rootOrgId);
          userMap.put(JsonKey.CHANNEL, fetchedOrgById.getChannel());
        }
      }
    }
  }

  private String validateExternalIdAndGetOrgId(
      Map<String, Object> userMap, RequestContext context) {
    String orgExternalId = (String) userMap.get(JsonKey.ORG_EXTERNAL_ID);
    String channel = (String) userMap.get(JsonKey.CHANNEL);
    String orgId =
        orgExternalService.getOrgIdFromOrgExternalIdAndProvider(orgExternalId, channel, context);
    if (StringUtils.isBlank(orgId)) {
      logger.debug(
          context,
          "SSOUserServiceImpl:validateExternalIdAndGetOrgId: No organisation with orgExternalId = "
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
      logger.debug(
          context,
          "SSOUserServiceImpl:validateExternalIdAndGetOrgId Mismatch of organisation from orgExternalId="
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
    return orgId;
  }

  private void throwParameterMismatchException(String... param) {
    ProjectCommonException.throwClientErrorException(
        ResponseCode.parameterMismatch,
        MessageFormat.format(
            ResponseCode.parameterMismatch.getErrorMessage(), StringFormatter.joinByComma(param)));
  }

  protected void populateProfileUserType(
      Map<String, Object> userMap, RequestContext requestContext) {
    Map<String, String> userTypeAndSubType = new HashMap<>();
    userMap.remove(JsonKey.PROFILE_USERTYPE);
    if (userMap.containsKey(JsonKey.USER_TYPE)) {
      userTypeAndSubType.put(JsonKey.TYPE, (String) userMap.get(JsonKey.USER_TYPE));
      if (userMap.containsKey(JsonKey.USER_SUB_TYPE)) {
        userTypeAndSubType.put(JsonKey.SUB_TYPE, (String) userMap.get(JsonKey.USER_SUB_TYPE));
      } else {
        userTypeAndSubType.put(JsonKey.SUB_TYPE, null);
      }
      try {
        ObjectMapper mapper = new ObjectMapper();
        if (!userMap.containsKey(JsonKey.PROFILE_USERTYPES)) {
          List<Map<String, String>> userTypeAndSubTypes = new ArrayList<>();
          userTypeAndSubTypes.add(userTypeAndSubType);
          userMap.put(JsonKey.PROFILE_USERTYPES, mapper.writeValueAsString(userTypeAndSubTypes));
        }
        userMap.put(JsonKey.PROFILE_USERTYPE, mapper.writeValueAsString(userTypeAndSubType));
      } catch (Exception ex) {
        logger.error(requestContext, "Exception occurred while mapping", ex);
        ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
      }
      userMap.remove(JsonKey.USER_TYPE);
      userMap.remove(JsonKey.USER_SUB_TYPE);
    }
  }
}
