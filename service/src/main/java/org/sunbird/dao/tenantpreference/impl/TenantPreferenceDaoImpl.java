package org.sunbird.dao.tenantpreference.impl;

import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.dao.tenantpreference.TenantPreferenceDao;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TenantPreferenceDaoImpl implements TenantPreferenceDao {

  private final String TABLE_NAME = JsonKey.TENANT_PREFERENCE_V2_DB;
  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();

  private static TenantPreferenceDao preferenceDao;

  public static TenantPreferenceDao getInstance() {
    if (preferenceDao == null) {
      if (preferenceDao == null) {
        preferenceDao = new TenantPreferenceDaoImpl();
      }
    }
    return preferenceDao;
  }

  @Override
  public List<Map<String, Object>> getTenantPreferenceById(String orgId, String key, RequestContext context) {
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put(JsonKey.ORG_ID, orgId);
    properties.put(JsonKey.KEY, key);
    Response tenantPreferences =
      cassandraOperation.getRecordsByProperties(
        JsonKey.SUNBIRD,
        TABLE_NAME,
        properties,
        context);
    List<Map<String, Object>> preferencesList =
      (List<Map<String, Object>>) tenantPreferences.get(JsonKey.RESPONSE);
    return preferencesList;
  }

  @Override
  public Response insertTenantPreference(Map<String, Object> tenantPreference, RequestContext context) {
    return cassandraOperation.insertRecord(
      JsonKey.SUNBIRD,
      TABLE_NAME,
      tenantPreference,
      context);
  }

  @Override
  public Response updateTenantPreference(Map<String, Object> tenantPreference, Map<String, Object> clusteringKeys, RequestContext context) {
    return cassandraOperation.updateRecord(
      JsonKey.SUNBIRD,
      TABLE_NAME,
      tenantPreference,
      clusteringKeys,
      context);
  }
}
