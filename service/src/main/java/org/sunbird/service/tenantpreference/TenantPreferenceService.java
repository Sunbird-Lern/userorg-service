package org.sunbird.service.tenantpreference;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections.CollectionUtils;
import org.sunbird.dao.tenantpreference.TenantPreferenceDao;
import org.sunbird.dao.tenantpreference.impl.TenantPreferenceDaoImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.response.ResponseMessage;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.util.DataSecurityLevelsEnum;
import org.sunbird.common.ProjectUtil;

public class TenantPreferenceService {

  private final LoggerUtil logger = new LoggerUtil(TenantPreferenceService.class);
  private final ObjectMapper mapper = new ObjectMapper();
  private final TenantPreferenceDao preferenceDao = TenantPreferenceDaoImpl.getInstance();
  private final List<String> dataSecurityLevels =
      Stream.of(DataSecurityLevelsEnum.values()).map(Enum::name).collect(Collectors.toList());

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
      return getOrgPreference(orgPreference, context);
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

  public boolean validateTenantDataSecurityPolicy(
      String orgId, String key, Map<String, Object> data, RequestContext context) {
    List<Map<String, Object>> defaultDataSecurityPolicy =
        preferenceDao.getTenantPreferenceById(JsonKey.DEFAULT, key, context);
    if (CollectionUtils.isEmpty(defaultDataSecurityPolicy)) {
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

      String strSystemSecurityLevel = (String) defaultData.get(JsonKey.LEVEL);
      Map<String, Object> defaultJobsConfig = (Map<String, Object>) defaultData.get(JsonKey.JOB);
      Map<String, String> defaultJobsLevelConfig =
          defaultJobsConfig
              .entrySet()
              .stream()
              .collect(
                  Collectors.toMap(
                      Map.Entry::getKey,
                      p -> ((Map<String, String>) p.getValue()).get(JsonKey.LEVEL)));

      String strTenantSecurityLevel = (String) data.get(JsonKey.LEVEL);

      if (compareSecurityLevel(strTenantSecurityLevel, strSystemSecurityLevel))
        throw new ProjectCommonException(
            ResponseCode.invalidTenantSecurityLevelLower,
            MessageFormat.format(
                ResponseCode.invalidTenantSecurityLevelLower.getErrorMessage(),
                strTenantSecurityLevel,
                strSystemSecurityLevel),
            ResponseCode.CLIENT_ERROR.getResponseCode());

      Map<String, Object> inputJobsConfig = (Map<String, Object>) data.get(JsonKey.JOB);
      Iterator<String> inputJobsKeyItr = inputJobsConfig.keySet().iterator();
      while (inputJobsKeyItr.hasNext()) {
        String jobName = inputJobsKeyItr.next();
        if (defaultJobsLevelConfig.containsKey(jobName)) {
          Map<String, String> jobConfig = (Map<String, String>) inputJobsConfig.get(jobName);

          if (!jobConfig.containsKey(JsonKey.LEVEL)) {
            throw new ProjectCommonException(
                ResponseCode.invalidRequestData,
                ResponseCode.invalidRequestData.getErrorMessage(),
                ResponseCode.CLIENT_ERROR.getResponseCode());
          }

          String ipJobSecurityLevel = jobConfig.get(JsonKey.LEVEL);
          if (!dataSecurityLevels.contains(ipJobSecurityLevel)) {
            throw new ProjectCommonException(
                ResponseCode.invalidSecurityLevel,
                MessageFormat.format(
                    ResponseCode.invalidSecurityLevel.getErrorMessage(),
                    ipJobSecurityLevel,
                    jobName),
                ResponseCode.CLIENT_ERROR.getResponseCode());
          }

          if (compareSecurityLevel(ipJobSecurityLevel, defaultJobsLevelConfig.get(jobName))) {
            throw new ProjectCommonException(
                ResponseCode.invalidSecurityLevelLower,
                MessageFormat.format(
                    ResponseCode.invalidSecurityLevelLower.getErrorMessage(),
                    ipJobSecurityLevel,
                    jobName,
                    defaultJobsLevelConfig.get(jobName)),
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
    } catch (NullPointerException npe) {
      logger.error(
          context,
          "TenantPreferenceService:Exception while parsing default dataSecurityPolicy preferences "
              + npe.getMessage(),
          npe);
      throw new ProjectCommonException(
          ResponseCode.invalidRequestData,
          ResponseCode.invalidRequestData.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    return true;
  }

  public boolean validateDataSecurityPolicyConfig(Map<String, Object> data) {
    Map<String, Object> inputJobsConfig = (Map<String, Object>) data.get(JsonKey.JOB);

    if (!data.containsKey(JsonKey.LEVEL)) {
      throw new ProjectCommonException(
          ResponseCode.invalidRequestData,
          ResponseCode.invalidRequestData.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    String strTenantLevel = (String) data.get(JsonKey.LEVEL);
    Iterator<String> inputJobsKeyItr = inputJobsConfig.keySet().iterator();
    while (inputJobsKeyItr.hasNext()) {
      String jobName = inputJobsKeyItr.next();
      Map<String, String> jobConfig = (Map<String, String>) inputJobsConfig.get(jobName);

      if (!jobConfig.containsKey(JsonKey.LEVEL)) {
        throw new ProjectCommonException(
            ResponseCode.invalidRequestData,
            ResponseCode.invalidRequestData.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }

      String strJobLevel = jobConfig.get(JsonKey.LEVEL);

      if (!dataSecurityLevels.contains(strJobLevel)) {
        throw new ProjectCommonException(
            ResponseCode.invalidSecurityLevel,
            MessageFormat.format(
                ResponseCode.invalidSecurityLevel.getErrorMessage(), strJobLevel, jobName),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }

      if (compareSecurityLevel(strJobLevel, strTenantLevel)) {
        throw new ProjectCommonException(
            ResponseCode.invalidSecurityLevelLower,
            MessageFormat.format(
                ResponseCode.invalidSecurityLevelLower.getErrorMessage(),
                strJobLevel,
                jobName,
                strTenantLevel),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
    }
    return true;
  }

  public Map<String, Object> getDataSecurityPolicyPref(
      String orgId, String key, RequestContext context) {
    List<Map<String, Object>> orgPreference =
        preferenceDao.getTenantPreferenceById(orgId, key, context);

    if (!orgId.equalsIgnoreCase(JsonKey.DEFAULT) && CollectionUtils.isEmpty(orgPreference))
      orgPreference = preferenceDao.getTenantPreferenceById(JsonKey.DEFAULT, key, context);

    if (CollectionUtils.isNotEmpty(orgPreference)) {
      return getOrgPreference(orgPreference, context);
    } else
      throw new ProjectCommonException(
          ResponseCode.errorParamExists,
          MessageFormat.format(
              ResponseCode.resourceNotFound.getErrorMessage(),
              (ProjectUtil.formatMessage(ResponseMessage.Message.AND_FORMAT, key, orgId))),
          ResponseCode.CLIENT_ERROR.getResponseCode());
  }

  private boolean compareSecurityLevel(String strFromLevel, String strToLevel) {
    int iFromLevel = DataSecurityLevelsEnum.valueOf(strFromLevel).getDataSecurityLevelValue();
    int iToLevel = DataSecurityLevelsEnum.valueOf(strToLevel).getDataSecurityLevelValue();
    return iFromLevel < iToLevel;
  }

  private Map<String, Object> getOrgPreference(
      List<Map<String, Object>> orgPreference, RequestContext context) {
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
      throw new ProjectCommonException(
          ResponseCode.invalidRequestData,
          ResponseCode.invalidRequestData.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }
}
