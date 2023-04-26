package org.sunbird.service.tenantpreference;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.*;
import org.apache.commons.collections.CollectionUtils;
import org.sunbird.dao.tenantpreference.TenantPreferenceDao;
import org.sunbird.dao.tenantpreference.impl.TenantPreferenceDaoImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.exception.ResponseMessage;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.util.ProjectUtil;

public class TenantPreferenceService {

  private final LoggerUtil logger = new LoggerUtil(TenantPreferenceService.class);
  private final ObjectMapper mapper = new ObjectMapper();
  private final TenantPreferenceDao preferenceDao = TenantPreferenceDaoImpl.getInstance();
  private final List<String> dataSecurityLevels = Arrays.asList("L1", "L2", "L3", "L4");

  public Map<String, Object> validateAndGetTenantPreferencesById(
      String orgId, String key, String operationType, RequestContext context) {
    List<Map<String, Object>> orgPreference =
        preferenceDao.getTenantPreferenceById(orgId, key, context);
    if (JsonKey.CREATE.equalsIgnoreCase(operationType)
        && CollectionUtils.isNotEmpty(orgPreference)) {
      throw new ProjectCommonException(
          ResponseCode.errorParamExists,
          MessageFormat.format(
              ResponseCode.resourceNotFound.getErrorMessage(),
              (ProjectUtil.formatMessage(ResponseMessage.Message.AND_FORMAT, key, orgId))),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    } else if (((JsonKey.GET.equalsIgnoreCase(operationType))
            || (JsonKey.UPDATE.equalsIgnoreCase(operationType)))
        && CollectionUtils.isEmpty(orgPreference)) {
      throw new ProjectCommonException(
          ResponseCode.resourceNotFound,
          MessageFormat.format(
              ResponseCode.resourceNotFound.getErrorMessage(),
              (ProjectUtil.formatMessage(ResponseMessage.Message.AND_FORMAT, key, orgId))),
          ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
    }
    if (CollectionUtils.isNotEmpty(orgPreference)) {
      try {
        String data = (String) orgPreference.get(0).get(JsonKey.DATA);
        Map<String, Object> map = mapper.readValue(data, new TypeReference<>() {});
        orgPreference.get(0).put(JsonKey.DATA, map);
        return orgPreference.get(0);
      } catch (Exception e) {
        logger.error(
            context,
            "TenantPreferenceService:Exception while reading preferences " + e.getMessage(),
            e);
      }
    }
    return Collections.emptyMap();
  }

  public Response createPreference(
      String orgId,
      String key,
      Map<String, Object> data,
      String createdBy,
      RequestContext context) {
    try {
      Map<String, Object> dbMap = new HashMap<>();
      dbMap.put(JsonKey.ORG_ID, orgId);
      dbMap.put(JsonKey.KEY, key);
      dbMap.put(JsonKey.DATA, mapper.writeValueAsString(data));
      dbMap.put(JsonKey.CREATED_BY, createdBy);
      dbMap.put(JsonKey.CREATED_ON, new Timestamp(Calendar.getInstance().getTimeInMillis()));
      return preferenceDao.insertTenantPreference(dbMap, context);
    } catch (Exception e) {
      logger.error(
          context,
          "TenantPreferenceService:Exception while adding preferences " + e.getMessage(),
          e);
    }
    return null;
  }

  public Response updatePreference(
      String orgId,
      String key,
      Map<String, Object> data,
      String updatedBy,
      RequestContext context) {
    try {
      Map<String, Object> preference = new HashMap<>();
      Map<String, Object> clusteringKeys = new HashMap<>();
      clusteringKeys.put(JsonKey.KEY, key);
      clusteringKeys.put(JsonKey.ORG_ID, orgId);
      preference.put(JsonKey.DATA, mapper.writeValueAsString(data));
      preference.put(JsonKey.UPDATED_BY, updatedBy);
      preference.put(JsonKey.UPDATED_ON, new Timestamp(Calendar.getInstance().getTimeInMillis()));
      return preferenceDao.updateTenantPreference(preference, clusteringKeys, context);
    } catch (Exception e) {
      logger.error(
          context,
          "TenantPreferenceService:Exception while updating preferences " + e.getMessage(),
          e);
    }
    return null;
  }

  public boolean validateDataSecurityPolicy(
      String orgId, String key, Map<String, Object> data, RequestContext context) {
    boolean validation = false;
    List<Map<String, Object>> defaultDataSecurityPolicy =
        preferenceDao.getTenantPreferenceById(JsonKey.DEFAULT, key, context);
    if (defaultDataSecurityPolicy == null || defaultDataSecurityPolicy.isEmpty()) {
      throw new ProjectCommonException(
          ResponseCode.resourceNotFound,
          MessageFormat.format(
              ResponseCode.resourceNotFound.getErrorMessage(),
              (ProjectUtil.formatMessage(ResponseMessage.Message.AND_FORMAT, key, orgId))),
          ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
    }

    String defaultDataString = (String) defaultDataSecurityPolicy.get(0).get(JsonKey.DATA);
    try {
      Map<String, Object> defaultData =
          mapper.readValue(defaultDataString, new TypeReference<>() {});

      Map<String, Object> defaultJobsConfig = (Map<String, Object>) defaultData.get(JsonKey.JOB);
      Map<String, Integer> defaultJobsLevelConfig = new HashMap<>();
      Iterator defaultJobsKeyItr = defaultJobsConfig.keySet().iterator();
      while (defaultJobsKeyItr.hasNext()) {
        String jobName = (String) defaultJobsKeyItr.next();
        Map<String, String> jobConfig = (Map<String, String>) defaultJobsConfig.get(jobName);
        defaultJobsLevelConfig.put(
            jobName, Integer.parseInt(jobConfig.getOrDefault("level", "L0").replace("L", "")));
      }

      Map<String, Object> inputJobsConfig = (Map<String, Object>) data.get(JsonKey.JOB);
      Iterator inputJobsKeyItr = inputJobsConfig.keySet().iterator();
      while (inputJobsKeyItr.hasNext()) {
        String jobName = (String) inputJobsKeyItr.next();
        if (defaultJobsLevelConfig.containsKey(jobName)) {
          Map<String, String> jobConfig = (Map<String, String>) inputJobsConfig.get(jobName);
          String ipJobSecurityLevel = jobConfig.getOrDefault("level", "L0");
          if (!dataSecurityLevels.contains(ipJobSecurityLevel)) {
            // throw invalid Level configured error
            throw new ProjectCommonException(
                ResponseCode.invalidSecurityLevel,
                MessageFormat.format(
                    ResponseCode.invalidSecurityLevel.getErrorMessage(),
                    ipJobSecurityLevel,
                    jobName),
                ResponseCode.CLIENT_ERROR.getResponseCode());
          }

          int inputJobSecurityLevel = Integer.parseInt(ipJobSecurityLevel.replace("L", ""));
          int defaultJobSecurityLevel = defaultJobsLevelConfig.get(jobName);

          if (inputJobSecurityLevel < defaultJobSecurityLevel) {
            // throw inputJobSecurityLevel less than defaultJobSecurityLevel error
            throw new ProjectCommonException(
                ResponseCode.invalidSecurityLevelLower,
                MessageFormat.format(
                    ResponseCode.invalidSecurityLevelLower.getErrorMessage(),
                    ipJobSecurityLevel,
                    jobName,
                    "L" + defaultJobSecurityLevel),
                ResponseCode.CLIENT_ERROR.getResponseCode());
          }
        } else {
          throw new ProjectCommonException(
              ResponseCode.defaultSecurityLevelConfigMissing,
              MessageFormat.format(
                  ResponseCode.defaultSecurityLevelConfigMissing.getErrorMessage(), jobName),
              ResponseCode.CLIENT_ERROR.getResponseCode());
        }
      }
      validation = true;
    } catch (JsonProcessingException e) {
      logger.error(
          context,
          "TenantPreferenceService:Exception while parsing default dataSecurityPolicy preferences "
              + e.getMessage(),
          e);
      throw new ProjectCommonException(
          ResponseCode.resourceNotFound,
          MessageFormat.format(
              ResponseCode.resourceNotFound.getErrorMessage(),
              (ProjectUtil.formatMessage(ResponseMessage.Message.AND_FORMAT, key, orgId))),
          ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
    }
    return validation;
  }
}
