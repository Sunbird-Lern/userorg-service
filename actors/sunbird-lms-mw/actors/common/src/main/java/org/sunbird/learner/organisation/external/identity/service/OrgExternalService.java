package org.sunbird.learner.organisation.external.identity.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.helper.ServiceFactory;

public class OrgExternalService {

  private final String KEYSPACE_NAME = "sunbird";
  private final String ORG_EXTERNAL_IDENTITY = "org_external_identity";

  public String getOrgIdFromOrgExternalIdAndProvider(String externalId, String provider) {
    Map<String, Object> dbRequestMap = new HashMap<>();
    dbRequestMap.put(JsonKey.EXTERNAL_ID, externalId.toLowerCase());
    dbRequestMap.put(JsonKey.PROVIDER, provider.toLowerCase());
    Response response =
        getCassandraOperation().getRecordsByCompositeKey(
            KEYSPACE_NAME, ORG_EXTERNAL_IDENTITY, dbRequestMap);
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

  private CassandraOperation getCassandraOperation(){
    return ServiceFactory.getInstance();
  }
}
