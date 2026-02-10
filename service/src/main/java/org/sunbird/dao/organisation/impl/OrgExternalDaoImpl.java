package org.sunbird.dao.organisation.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.dao.organisation.OrgExternalDao;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.common.ProjectUtil;

public class OrgExternalDaoImpl implements OrgExternalDao {

  private final CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private static final String KEYSPACE_NAME = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE);
  private static final String ORG_EXT_TABLE_NAME = JsonKey.ORG_EXT_ID_DB;

  @Override
  public Response addOrgExtId(Map<String, Object> orgExtMap, RequestContext context) {
    return cassandraOperation.insertRecord(KEYSPACE_NAME, ORG_EXT_TABLE_NAME, orgExtMap, context);
  }

  @Override
  public void deleteOrgExtId(Map<String, String> orgExtMap, RequestContext context) {
    cassandraOperation.deleteRecord(KEYSPACE_NAME, ORG_EXT_TABLE_NAME, orgExtMap, context);
  }

  @Override
  public String getOrgIdFromOrgExternalIdAndProvider(
      String externalId, String provider, RequestContext context) {
    Map<String, Object> dbRequestMap = new LinkedHashMap<>(3);
    dbRequestMap.put(JsonKey.PROVIDER, provider.toLowerCase());
    dbRequestMap.put(JsonKey.EXTERNAL_ID, externalId.toLowerCase());
    Response response =
        cassandraOperation.getRecordsByCompositeKey(
            KEYSPACE_NAME, ORG_EXT_TABLE_NAME, dbRequestMap, context);
    List<Map<String, Object>> orgList =
        (List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE);
    if (CollectionUtils.isNotEmpty(orgList)) {
      Map<String, Object> orgExternalMap = orgList.get(0);
      if (MapUtils.isNotEmpty(orgExternalMap)) {
        return (String) orgExternalMap.get(JsonKey.ORG_ID);
      }
    }
    return null;
  }
}
