package org.sunbird.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerUtil;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.Util;
import org.sunbird.telemetry.util.TelemetryUtil;

public class UserTncService {
  private static LoggerUtil logger = new LoggerUtil(UserTncService.class);
  private ObjectMapper mapper = new ObjectMapper();
  private ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);

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
    CassandraOperation cassandraOperation = ServiceFactory.getInstance();
    Util.DbInfo usrDbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
    Response response =
        cassandraOperation.getRecordById(
            usrDbInfo.getKeySpace(), usrDbInfo.getTableName(), userId, context);
    if (null != response && null != response.getResult()) {
      List<Map<String, Object>> dbUserList =
          (List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE);
      if (CollectionUtils.isNotEmpty(dbUserList)) {
        Map<String, Object> user = dbUserList.get(0);
        // Check whether user account is locked or not
        if (null != user.get(JsonKey.IS_DELETED)
            && BooleanUtils.isTrue((Boolean) user.get(JsonKey.IS_DELETED))) {
          ProjectCommonException.throwClientErrorException(ResponseCode.userAccountlocked);
        }
        return user;
      }
    } else {
      throw new ProjectCommonException(
          ResponseCode.userNotFound.getErrorCode(),
          ResponseCode.userNotFound.getErrorMessage(),
          ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
    }
    return Collections.emptyMap();
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

  public void validateOrgAdminTnc(
      RequestContext context, String tncType, Map<String, Object> user) {
    // check if it is org admin TnC and user is not an admin of the organisation
    if (JsonKey.ORG_ADMIN_TNC.equals(tncType) && !isOrgAdmin(user, context)) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.invalidParameterValue,
          MessageFormat.format(
              ResponseCode.invalidParameterValue.getErrorMessage(),
              user.get(JsonKey.ROOT_ORG_ID),
              JsonKey.ORGANISATION_ID));
    }
  }

  private boolean isOrgAdmin(Map<String, Object> user, RequestContext context) {
    CassandraOperation cassandraOperation = ServiceFactory.getInstance();
    Map<String, Object> searchMap = new LinkedHashMap<>(2);
    searchMap.put(JsonKey.USER_ID, user.get(JsonKey.ID));
    searchMap.put(JsonKey.ORGANISATION_ID, user.get(JsonKey.ROOT_ORG_ID));
    Response res =
        cassandraOperation.getRecordsByCompositeKey(
            JsonKey.SUNBIRD, JsonKey.USER_ORG, searchMap, context);
    List<Map<String, Object>> orgDetails = (List<Map<String, Object>>) res.get(JsonKey.RESPONSE);
    if (CollectionUtils.isNotEmpty(orgDetails)) {
      Map<String, Object> org = orgDetails.get(0);
      if (MapUtils.isNotEmpty(org)) {
        List<String> roles = (List<String>) org.get(JsonKey.ROLES);
        return roles.contains(JsonKey.ORG_ADMIN);
      }
    }
    return false;
  }

  // Convert Acceptance tnc object as a Json String in cassandra table
  public Map<String, Object> convertTncStringToJsonMap(Map<String, String> allTncAcceptedMap) {
    Map<String, Object> allTncMap = new HashMap<>();
    for (Map.Entry<String, String> mapItr : allTncAcceptedMap.entrySet()) {
      try {
        allTncMap.put(mapItr.getKey(), mapper.readValue(mapItr.getValue(), Map.class));
      } catch (JsonProcessingException e) {
        logger.error("JsonParsing error while parsing tnc acceptance", e);
        ProjectCommonException.throwClientErrorException(ResponseCode.invalidRequestData);
      }
    }
    return allTncMap;
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
    logger.info(context, "UserTnCActor:syncUserDetails: Trigger sync of user details to ES");
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS'Z'");
    userMap.put(JsonKey.TNC_ACCEPTED_ON, simpleDateFormat.format(new Date()));
    esService.update(
        ProjectUtil.EsType.user.getTypeName(), (String) userMap.get(JsonKey.ID), userMap, context);
  }
}
