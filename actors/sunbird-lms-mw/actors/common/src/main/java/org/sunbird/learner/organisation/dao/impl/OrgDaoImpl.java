package org.sunbird.learner.organisation.dao.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerUtil;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.RequestContext;
import org.sunbird.dto.SearchDTO;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.organisation.dao.OrgDao;
import org.sunbird.learner.util.Util;
import scala.concurrent.Future;

public class OrgDaoImpl implements OrgDao {

  private LoggerUtil logger = new LoggerUtil(OrgDaoImpl.class);
  private ElasticSearchService esUtil = EsClientFactory.getInstance(JsonKey.REST);
  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private static OrgDao orgDao = null;

  public static OrgDao getInstance() {
    if (orgDao == null) {
      orgDao = new OrgDaoImpl();
    }
    return orgDao;
  }

  @Override
  public Map<String, Object> getOrgById(String orgId, RequestContext context) {
    Util.DbInfo orgDb = Util.dbInfoMap.get(JsonKey.ORG_DB);
    Response response =
        cassandraOperation.getRecordById(orgDb.getKeySpace(), orgDb.getTableName(), orgId, context);
    List<Map<String, Object>> responseList =
        (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    if (CollectionUtils.isNotEmpty(responseList)) {
      Map<String, Object> orgMap = responseList.get(0);
      orgMap.remove(JsonKey.CONTACT_DETAILS);
      return orgMap;
    }
    return Collections.EMPTY_MAP;
  }

  @Override
  public Map<String, Object> esGetOrgByExternalId(
      String externalId, String provider, RequestContext context) {
    Map<String, Object> map = null;
    SearchDTO searchDto = new SearchDTO();
    Map<String, Object> filter = new HashMap<>();
    filter.put(JsonKey.EXTERNAL_ID, externalId);
    filter.put(JsonKey.PROVIDER, provider);
    searchDto.getAdditionalProperties().put(JsonKey.FILTERS, filter);
    Future<Map<String, Object>> esResponseF =
        esUtil.search(searchDto, ProjectUtil.EsType.organisation.getTypeName(), context);
    Map<String, Object> esResponse =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(esResponseF);
    List<Map<String, Object>> list = (List<Map<String, Object>>) esResponse.get(JsonKey.CONTENT);
    if (!list.isEmpty()) {
      map = list.get(0);
      map.put(JsonKey.CONTACT_DETAILS, String.valueOf(map.get(JsonKey.CONTACT_DETAILS)));
    }
    return map;
  }
}
