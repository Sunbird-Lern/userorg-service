package org.sunbird.service.tenantpreference;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.sunbird.dao.tenantpreference.TenantPreferenceDao;
import org.sunbird.dao.tenantpreference.impl.TenantPreferenceDaoImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

public class TenantPreferenceService {

  private final LoggerUtil logger = new LoggerUtil(TenantPreferenceService.class);
  private final ObjectMapper mapper = new ObjectMapper();
  private final TenantPreferenceDao preferenceDao = TenantPreferenceDaoImpl.getInstance();

  public Map<String, Object> validateAndGetTenantPreferencesById(
      String orgId, String key, String operationType, RequestContext context) {
    List<Map<String, Object>> orgPreference =
        preferenceDao.getTenantPreferenceById(orgId, key, context);
    if (JsonKey.CREATE.equalsIgnoreCase(operationType)
        && CollectionUtils.isNotEmpty(orgPreference)) {
      throw new ProjectCommonException(
          ResponseCode.preferenceAlreadyExists,
          MessageFormat.format(ResponseCode.preferenceAlreadyExists.getErrorMessage(), key, orgId),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    } else if (((JsonKey.GET.equalsIgnoreCase(operationType))
            || (JsonKey.UPDATE.equalsIgnoreCase(operationType)))
        && CollectionUtils.isEmpty(orgPreference)) {
      throw new ProjectCommonException(
          ResponseCode.preferenceNotFound,
          MessageFormat.format(ResponseCode.preferenceNotFound.getErrorMessage(), key, orgId),
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
}
