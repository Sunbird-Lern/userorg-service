package org.sunbird.dao.tenantpreference.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.dao.tenantpreference.TenantPreferenceDao;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.common.ProjectUtil;

public class TenantPreferenceDaoImpl implements TenantPreferenceDao {

  private final CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private static TenantPreferenceDao preferenceDao;

  public static TenantPreferenceDao getInstance() {
    if (preferenceDao == null) {
      preferenceDao = new TenantPreferenceDaoImpl();
    }
    return preferenceDao;
  }

  @Override
  public List<Map<String, Object>> getTenantPreferenceById(
      String orgId, String key, RequestContext context) {
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put(JsonKey.ORG_ID, orgId);
    properties.put(JsonKey.KEY, key);
    Response tenantPreferences =
        cassandraOperation.getRecordsByProperties(
                ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE), JsonKey.TENANT_PREFERENCE_V2_DB, properties, context);
    return (List<Map<String, Object>>) tenantPreferences.get(JsonKey.RESPONSE);
  }

  @Override
  public Response insertTenantPreference(
      Map<String, Object> tenantPreference, RequestContext context) {
    return cassandraOperation.insertRecord(
            ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE), JsonKey.TENANT_PREFERENCE_V2_DB, tenantPreference, context);
  }

  @Override
  public Response updateTenantPreference(
      Map<String, Object> tenantPreference,
      Map<String, Object> clusteringKeys,
      RequestContext context) {
    return cassandraOperation.updateRecord(
            ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE),
        JsonKey.TENANT_PREFERENCE_V2_DB,
        tenantPreference,
        clusteringKeys,
        context);
  }
}
