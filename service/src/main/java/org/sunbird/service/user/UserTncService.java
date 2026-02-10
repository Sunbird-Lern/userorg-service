package org.sunbird.service.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.user.impl.UserRoleServiceImpl;
import org.sunbird.service.user.impl.UserServiceImpl;
import org.sunbird.telemetry.util.TelemetryUtil;
import org.sunbird.util.DataCacheHandler;

public class UserTncService {
  private static final LoggerUtil logger = new LoggerUtil(UserTncService.class);
  private final UserService userService = UserServiceImpl.getInstance();
  private final UserRoleService userRoleService = UserRoleServiceImpl.getInstance();
  private final ObjectMapper mapper = new ObjectMapper();

  public String getTncType(Request request) {
    String tncType = (String) request.getRequest().get(JsonKey.TNC_TYPE);
    // if tncType is null , continue to use the same field for user tnc acceptance
    if (StringUtils.isBlank(tncType)) {
      tncType = JsonKey.TNC_CONFIG;
    }
    return tncType;
  }

  public void validateLatestTncVersion(Request request, String tncType) {
    String latestTnC =
        getSystemSettingByFieldAndKey(tncType, JsonKey.LATEST_VERSION, request.getRequestContext());

    String acceptedTnC = (String) request.getRequest().get(JsonKey.VERSION);

    if (!acceptedTnC.equalsIgnoreCase(latestTnC)) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.invalidParameterValue,
          MessageFormat.format(
              ResponseCode.invalidParameterValue.getErrorMessage(), acceptedTnC, JsonKey.VERSION));
    }
  }

  private String getSystemSettingByFieldAndKey(String field, String key, RequestContext context) {
    String value = DataCacheHandler.getConfigSettings().get(field);
    String keyValue = "";
    if (StringUtils.isNotBlank(value)) {
      try {
        Map<String, Object> valueMap = mapper.readValue(value, Map.class);
        keyValue = (String) valueMap.getOrDefault(key, "");
      } catch (Exception e) {
        logger.error(
            context,
            "getSystemSettingByFieldAndKey: Exception occurred with error message = "
                + e.getMessage(),
            e);
      }
    }
    return keyValue;
  }

  public Map<String, Object> getUserById(String userId, RequestContext context) {
    Map<String, Object> user = userService.getUserDetailsById(userId, context);
    if (null != user.get(JsonKey.IS_DELETED)
        && BooleanUtils.isTrue((Boolean) user.get(JsonKey.IS_DELETED))) {
      ProjectCommonException.throwClientErrorException(ResponseCode.userAccountlocked);
    }
    return user;
  }

  public Response updateUser(Map<String, Object> user, RequestContext context) {
    return userService.updateUser(user, context);
  }

  public void isAccountManagedUser(boolean isManagedUser, Map<String, Object> user) {
    // If user account isManagedUser(passed in request) and managedBy is empty, not a valid
    // scenario
    String userId = (String) user.get(JsonKey.ID);
    if (isManagedUser && StringUtils.isBlank((String) user.get(JsonKey.MANAGED_BY))) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.invalidParameterValue,
          MessageFormat.format(
              ResponseCode.invalidParameterValue.getErrorMessage(), userId, JsonKey.USER_ID));
    }
  }

  public void validateRoleForTnc(RequestContext context, String tncType, Map<String, Object> user) {
    // check if it is org admin TnC and user is not an admin of the organisation
    // OR check if it is report viewer tnc and user not having the report viewer role
    if ((JsonKey.ORG_ADMIN_TNC.equals(tncType) && !roleCheck(user, JsonKey.ORG_ADMIN, context))
        || (JsonKey.REPORT_VIEWER_TNC.equals(tncType)
            && !(roleCheck(user, JsonKey.REPORT_VIEWER, context)
                || roleCheck(user, JsonKey.REPORT_ADMIN, context)))) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.invalidParameterValue,
          MessageFormat.format(
              ResponseCode.invalidParameterValue.getErrorMessage(),
              user.get(JsonKey.ROOT_ORG_ID),
              JsonKey.ORGANISATION_ID));
    }
  }

  private boolean roleCheck(Map<String, Object> user, String role, RequestContext context) {
    AtomicBoolean isRoleExists = new AtomicBoolean(false);
    List<Map<String, Object>> dbUserRoleList =
        userRoleService.getUserRoles((String) user.get(JsonKey.ID), role, context);
    if (CollectionUtils.isNotEmpty(dbUserRoleList)) {
      ObjectMapper mapper = new ObjectMapper();
      dbUserRoleList.forEach(
          e -> {
            if (role.equals(e.get(JsonKey.ROLE))) {
              String scope = (String) e.get(JsonKey.SCOPE);
              if (StringUtils.isNotBlank(scope)) {
                List<Map<String, Object>> scopeList = new ArrayList<>();
                try {
                  scopeList =
                      mapper.readValue(scope, new TypeReference<List<Map<String, Object>>>() {});
                } catch (JsonProcessingException ex) {
                  logger.error("JsonParsing error while parsing role scope", ex);
                }
                scopeList.forEach(
                    scopeMap -> {
                      if (scopeMap
                          .get(JsonKey.ORGANISATION_ID)
                          .equals((String) user.get(JsonKey.ROOT_ORG_ID))) {
                        isRoleExists.set(true);
                      }
                    });
              }
            }
          });
    }
    return isRoleExists.get();
  }

  public void generateTelemetry(
      Map<String, Object> userMap, String lastAcceptedVersion, Map<String, Object> context) {
    Map<String, Object> targetObject = null;
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    targetObject =
        TelemetryUtil.generateTargetObject(
            (String) userMap.get(JsonKey.USER_ID),
            JsonKey.USER,
            JsonKey.UPDATE,
            lastAcceptedVersion);
    TelemetryUtil.telemetryProcessingCall(userMap, targetObject, correlatedObject, context);
  }

  public void syncUserDetails(Map<String, Object> userMap, RequestContext context) {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS'Z'");
    userMap.put(JsonKey.TNC_ACCEPTED_ON, simpleDateFormat.format(new Date()));
    userService.updateUserDataToES((String) userMap.get(JsonKey.ID), userMap, context);
  }
}
