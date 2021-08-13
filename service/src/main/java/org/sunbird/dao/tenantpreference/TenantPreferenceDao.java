package org.sunbird.dao.tenantpreference;

import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

import java.util.List;
import java.util.Map;

public interface TenantPreferenceDao {

  List<Map<String, Object>> getTenantPreferenceById(String orgId, String key, RequestContext context);

  Response insertTenantPreference(Map<String, Object> tenantPreference, RequestContext context);

  Response updateTenantPreference(Map<String, Object> tenantPreference, Map<String, Object> clusteringKeys, RequestContext context);

}
