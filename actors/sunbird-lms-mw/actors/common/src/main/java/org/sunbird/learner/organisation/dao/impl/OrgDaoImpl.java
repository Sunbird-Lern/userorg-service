package org.sunbird.learner.organisation.dao.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.learner.organisation.dao.OrgDao;
import org.sunbird.learner.organisation.external.identity.service.OrgExternalService;
import org.sunbird.learner.util.Util;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

public class OrgDaoImpl implements OrgDao {

  private LoggerUtil logger = new LoggerUtil(OrgDaoImpl.class);
  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private OrgExternalService orgExternalService = new OrgExternalService();
  private static OrgDao orgDao = null;

  public static OrgDao getInstance() {
    if (orgDao == null) {
      orgDao = new OrgDaoImpl();
    }
    return orgDao;
  }

  @Override
  public Map<String, Object> getOrgById(String orgId, RequestContext context) {
    if (StringUtils.isNotBlank(orgId)) {
      Util.DbInfo orgDb = Util.dbInfoMap.get(JsonKey.ORG_DB);
      Response response =
          cassandraOperation.getRecordById(
              orgDb.getKeySpace(), orgDb.getTableName(), orgId, context);
      List<Map<String, Object>> responseList =
          (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
      if (CollectionUtils.isNotEmpty(responseList)) {
        Map<String, Object> orgMap = responseList.get(0);
        String orgLocation = (String) orgMap.get(JsonKey.ORG_LOCATION);
        List orgLocationList = new ArrayList<>();
        if (StringUtils.isNotBlank(orgLocation)) {
          try {
            ObjectMapper mapper = new ObjectMapper();
            orgLocationList = mapper.readValue(orgLocation, List.class);
          } catch (Exception e) {
            logger.info(
                context,
                "Exception occurred while converting orgLocation to List<Map<String,String>>.");
          }
        }
        orgMap.put(JsonKey.ORG_LOCATION, orgLocationList);
        orgMap.put(JsonKey.HASHTAGID, orgMap.get(JsonKey.ID));
        orgMap.remove(JsonKey.CONTACT_DETAILS);
        return orgMap;
      }
    }
    return Collections.emptyMap();
  }

  @Override
  public Map<String, Object> getOrgByExternalId(
      String externalId, String provider, RequestContext context) {
    String orgId =
        orgExternalService.getOrgIdFromOrgExternalIdAndProvider(externalId, provider, context);
    return getOrgById(orgId, context);
  }
}
