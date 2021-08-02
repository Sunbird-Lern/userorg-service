package org.sunbird.service.organisation;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.dao.organisation.impl.OrgDaoImpl;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.util.Util;

public class OrgExternalService {
  private LoggerUtil logger = new LoggerUtil(OrgDaoImpl.class);
  private final String KEYSPACE_NAME = JsonKey.SUNBIRD;
  private final String ORG_EXTERNAL_IDENTITY = JsonKey.ORG_EXT_ID_DB;

  public String getOrgIdFromOrgExternalIdAndProvider(
      String externalId, String provider, RequestContext context) {
    Map<String, Object> dbRequestMap = new LinkedHashMap<>(3);
    dbRequestMap.put(JsonKey.PROVIDER, provider.toLowerCase());
    dbRequestMap.put(JsonKey.EXTERNAL_ID, externalId.toLowerCase());
    Response response =
        getCassandraOperation()
            .getRecordsByCompositeKey(KEYSPACE_NAME, ORG_EXTERNAL_IDENTITY, dbRequestMap, context);
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

  public Map<String, Object> getOrgByOrgExternalIdAndProvider(
      String externalId, String provider, RequestContext context) {
    String orgId = getOrgIdFromOrgExternalIdAndProvider(externalId, provider, context);
    if (StringUtils.isNotBlank(orgId)) {
      Util.DbInfo orgDbInfo = Util.dbInfoMap.get(JsonKey.ORG_DB);
      Response orgResponse =
          getCassandraOperation()
              .getRecordById(KEYSPACE_NAME, orgDbInfo.getTableName(), orgId, context);
      List<Map<String, Object>> orgResList =
          (List<Map<String, Object>>) orgResponse.getResult().get(JsonKey.RESPONSE);
      if (CollectionUtils.isNotEmpty(orgResList)) {
        Map<String, Object> orgMap = orgResList.get(0);
        if (MapUtils.isNotEmpty(orgMap)) {
          String orgLocation = (String) orgMap.get(JsonKey.ORG_LOCATION);
          List<Map<String, String>> orgLoc = new ArrayList<>();
          if (StringUtils.isNotBlank(orgLocation)) {
            try {
              ObjectMapper mapper = new ObjectMapper();
              orgLoc = mapper.readValue(orgLocation, List.class);
            } catch (Exception e) {
              logger.info(
                  context,
                  "Exception occurred while converting orgLocation to List<Map<String,String>>.");
            }
          }
          orgMap.put(JsonKey.ORG_LOCATION, orgLoc);
          orgMap.put(JsonKey.HASHTAGID, orgMap.get(JsonKey.ID));
          return orgMap;
        }
      }
    }
    return Collections.emptyMap();
  }

  private CassandraOperation getCassandraOperation() {
    return ServiceFactory.getInstance();
  }
}
